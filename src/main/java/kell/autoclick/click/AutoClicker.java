package kell.autoclick.click;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.PopupMenu;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kell.autoclick.contants.GlobalConst;
import kell.autoclick.frame.AddCommentFrame;
import kell.autoclick.frame.AddDADFrame;
import kell.autoclick.frame.AddPointFrame;
import kell.autoclick.frame.AddPressKeyFrame;
import kell.autoclick.frame.AddScrollFrame;
import kell.autoclick.frame.MainFrame;
import kell.autoclick.frame.MessageFrame;
import kell.autoclick.model.Item;
import kell.autoclick.model.ItemClick;
import kell.autoclick.model.ItemComment;
import kell.autoclick.model.ItemDAD;
import kell.autoclick.model.ItemPress;

public class AutoClicker {

    // ── Frames ────────────────────────────────────────────────────────────────
    private final MainFrame        MAIN_FRAME        = new MainFrame();
    // Dialogs are created after MAIN_FRAME so they can use it as owner
    private final AddPointFrame    ADD_POINT_FRAME;
    private final AddPressKeyFrame ADD_PRESS_FRAME;
    private final AddScrollFrame   ADD_SCROLL_FRAME;
    private final AddDADFrame      ADD_DAD_FRAME;
    private final AddCommentFrame  ADD_COMMENT_FRAME;
    private final MessageFrame     MSG_FRAME         = new MessageFrame();

    // ── State ─────────────────────────────────────────────────────────────────
    private volatile boolean isStopClick  = true;
    private volatile boolean infinite     = false;
    private volatile boolean repeatByTime = false;
    private volatile long    repeatTimes  = 0;
    private volatile long    totalRepeat  = 0;
    private volatile long    doneCount    = 0;
    /** End time in ms (System.currentTimeMillis) when repeatByTime mode is active */
    private volatile long    endTimeMs    = 0;

    public  final Object         lock           = new Object();
    private final Vector<Thread> runningThreads = new Vector<>();
    private final Random         random         = new Random();

    private static final Logger LOG = Logger.getLogger(AutoClicker.class.getName());

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    /** Tray icon used to hide/restore the window while clicking on Windows. */
    private TrayIcon trayIcon;

    // ── Gson (JSON serialization) ─────────────────────────────────────────────
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    // ── Constructor ───────────────────────────────────────────────────────────
    public AutoClicker() {
        setupFileLogger();
        MAIN_FRAME.init();
        JFrame owner = MAIN_FRAME.getFrame();
        // Create dialogs with owner so JDialog parent relationship is correct
        ADD_POINT_FRAME   = new AddPointFrame();
        ADD_PRESS_FRAME   = new AddPressKeyFrame(owner);
        ADD_SCROLL_FRAME  = new AddScrollFrame(owner);
        ADD_DAD_FRAME     = new AddDADFrame(owner);
        ADD_COMMENT_FRAME = new AddCommentFrame(owner);
        MSG_FRAME.initWithOwner(owner);
        MAIN_FRAME.getFrame().setVisible(true);
    }

    public void start() {
        eventListener();
        tableListener();
        // Always wire the in-window F7/F8 fallback BEFORE attempting the
        // global native hook. If the native hook fails (Windows AV, macOS
        // Accessibility not granted, etc.), users still have the hotkeys
        // when the main window is focused.
        MAIN_FRAME.setHotkeyHandlers(this::onF7, this::onF8);
        globalKeyListener();
        windowFocus();
    }

    private void onF7() {
        if (isStopClick) startClick();
    }

    private void onF8() {
        if (!isStopClick) stopClick();
    }

    // ── Logger ────────────────────────────────────────────────────────────────
    private void setupFileLogger() {
        try {
            var fh = new FileHandler(GlobalConst.LOG_FILE, 5 * 1024 * 1024, 3, true);
            fh.setFormatter(new SimpleFormatter());
            var root = Logger.getLogger("");
            root.addHandler(fh);
            root.setLevel(Level.INFO);
            Logger.getLogger(GlobalScreen.class.getPackageName()).setLevel(Level.OFF);
        } catch (IOException e) {
            LOG.warning("Could not set up file logger: " + e.getMessage());
        }
    }

    // ── Window focus ──────────────────────────────────────────────────────────
    private void windowFocus() {
        // JDialog with owner handles focus automatically — no manual management needed
    }


    // ── Event listeners ───────────────────────────────────────────────────────
    private void eventListener() {
        // Repeat mode toggles
        MAIN_FRAME.getInfiniteRBtn().addActionListener(e -> {
            infinite = true; repeatByTime = false;
        });
        MAIN_FRAME.getRepeatByCountRBtn().addActionListener(e -> {
            infinite = false; repeatByTime = false;
        });
        MAIN_FRAME.getRepeatByTimeRBtn().addActionListener(e -> {
            infinite = false; repeatByTime = true;
        });

        MAIN_FRAME.getHideMainFrameRBtn().addItemListener(e ->
            MAIN_FRAME.setHideFrame(e.getStateChange() == ItemEvent.SELECTED));

        MAIN_FRAME.getAddClickBtn().addActionListener(e -> showAddPointFrame());
        // Add Key: not disabled — handled entirely by MouseListener in MainFrame.applyTheme()

        MAIN_FRAME.getAddScrollBtn().addActionListener(e -> {
            showAddScrollFrame(true);
            ADD_SCROLL_FRAME.getWheelAmtTxt().requestFocus();
        });
        MAIN_FRAME.getAddDADBtn().addActionListener(e -> showAddDADFrame(true));

        // DAD frame buttons
        ADD_DAD_FRAME.getFromBtn().addActionListener(e -> pickDADPoint(true));
        ADD_DAD_FRAME.getToBtn().addActionListener(e -> pickDADPoint(false));
        ADD_DAD_FRAME.getAddBtn().addActionListener(e -> addDADEvent());
        ADD_DAD_FRAME.getCancelBtn().addActionListener(e -> showAddDADFrame(false));
        ADD_DAD_FRAME.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { showAddDADFrame(false); }
        });

        MAIN_FRAME.getAddCommentBtn().addActionListener(e -> showAddCommentFrame(true));
        MAIN_FRAME.getStartBtn().addActionListener(e -> startClick());
        MAIN_FRAME.getStopBtn().addActionListener(e -> stopClick());
        MAIN_FRAME.getDeleteBtn().addActionListener(e -> deleteSelected());
        MAIN_FRAME.getResetBtn().addActionListener(e -> resetData());
        MAIN_FRAME.getSaveBtn().addActionListener(e -> saveData());
        MAIN_FRAME.getLoadBtn().addActionListener(e -> loadData(false));
        MAIN_FRAME.getAppendBtn().addActionListener(e -> loadData(true));
        MAIN_FRAME.getMoveUpBtn().addActionListener(e -> MAIN_FRAME.getTableItems().moveSelectedUp());
        MAIN_FRAME.getMoveDownBtn().addActionListener(e -> MAIN_FRAME.getTableItems().moveSelectedDown());

        MSG_FRAME.getConfirmBtn().addActionListener(e -> changeFrame(true));

        ADD_PRESS_FRAME.getAddBtn().addActionListener(e -> addPressKey());
        ADD_PRESS_FRAME.getCancelBtn().addActionListener(e -> showAddPressFrame(false));
        ADD_PRESS_FRAME.getIsSinglePressRBtn().addItemListener(e -> {
            ADD_PRESS_FRAME.setIsSinglePress(e.getStateChange() == ItemEvent.SELECTED);
            ADD_PRESS_FRAME.getIsMultiPressRBtn().setSelected(!ADD_PRESS_FRAME.getIsSinglePress());
        });
        ADD_PRESS_FRAME.getIsMultiPressRBtn().addItemListener(e -> {
            ADD_PRESS_FRAME.setIsSinglePress(!(e.getStateChange() == ItemEvent.SELECTED));
            ADD_PRESS_FRAME.getIsSinglePressRBtn().setSelected(ADD_PRESS_FRAME.getIsSinglePress());
        });
        ADD_PRESS_FRAME.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { showAddPressFrame(false); }
        });

        ADD_SCROLL_FRAME.getAddBtn().addActionListener(e -> addScrollEvent());
        ADD_SCROLL_FRAME.getCancelBtn().addActionListener(e -> showAddScrollFrame(false));
        ADD_SCROLL_FRAME.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { showAddScrollFrame(false); }
        });

        ADD_COMMENT_FRAME.getAddBtn().addActionListener(e -> addComment());
        ADD_COMMENT_FRAME.getCancelBtn().addActionListener(e -> showAddCommentFrame(false));
        ADD_COMMENT_FRAME.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { showAddCommentFrame(false); }
        });
    }

    // ── Table listener ────────────────────────────────────────────────────────
    private void tableListener() {
        MAIN_FRAME.getTableItems().getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || col < 0) return;
            var items = MAIN_FRAME.getTableItems().getListItems();
            if (row >= items.size()) return;
            var data = MAIN_FRAME.getTableItems().getModel().getValueAt(row, col);
            if (data == null) return;
            var item = items.get(row);
            try {
                switch (col) {
                    case 2 -> item.setName((String) data);                              // Name
                    case 5 -> item.setPressTime(Integer.parseInt(((String) data).trim())); // Hold
                    case 6 -> item.setNextTime(Integer.parseInt(((String) data).trim()));  // Delay
                    default -> { /* no-op */ }
                }
            } catch (NumberFormatException ignored) { /* keep previous value */ }
        });
    }


    // ── Click control ─────────────────────────────────────────────────────────

    /** Thread-safe: interrupt click threads. Safe to call from any thread. */
    private void stopClickThreads() {
        isStopClick = true;
        synchronized (lock) { lock.notifyAll(); }
        for (Thread t : runningThreads) t.interrupt();
        runningThreads.clear();
    }

    /** EDT only: update UI after stopping. */
    private void stopClickUI() {
        enableInput(true);
        var f = MAIN_FRAME.getFrame();

        // Restore from tray (Windows). Safe no-op on other platforms.
        removeTrayIcon();
        if (!f.isVisible()) {
            f.setVisible(true);
        }
        f.setEnabled(true);
        f.toFront();
        f.requestFocus();

        MAIN_FRAME.getTableItems().setHighlightRow(-1);
        MAIN_FRAME.setProgressText("Stopped. Done: " + doneCount);
    }

    /** Full stop — call from EDT (button click) or native hook thread. */
    public void stopClick() {
        stopClickThreads();
        SwingUtilities.invokeLater(this::stopClickUI);
    }

    public void resetData() {
        if (MAIN_FRAME.getTableItems().getTable().isEditing())
            MAIN_FRAME.getTableItems().getTable().getCellEditor().stopCellEditing();
        infinite = false; repeatByTime = false;
        repeatTimes = 0; totalRepeat = 0; doneCount = 0;
        MAIN_FRAME.getTableItems().clearTable();
        MAIN_FRAME.getInfiniteRBtn().setSelected(false);
        MAIN_FRAME.getRepeatByCountRBtn().setSelected(true);
        MAIN_FRAME.setHideFrame(true);
        MAIN_FRAME.getHideMainFrameRBtn().setSelected(true);
        MAIN_FRAME.getRepeatTimesTxt().setText("");
        MAIN_FRAME.getHoursField().setText("0");
        MAIN_FRAME.getMinutesField().setText("0");
        MAIN_FRAME.getSecondsField().setText("0");
        MAIN_FRAME.setProgressText("Ready");
    }

    public void deleteSelected() {
        if (MAIN_FRAME.getTableItems().getTable().getSelectedRows().length < 1) {
            changeFrame(false, MAIN_FRAME.getTableItems().getListItems().isEmpty()
                    ? "The list is empty." : "Please select a row to delete.");
            return;
        }
        MAIN_FRAME.getTableItems().deleteSelectedRows();
    }

    public void startClick() {
        if (MAIN_FRAME.getTableItems().getListItems().isEmpty()) {
            changeFrame(false, "No actions added yet. Add at least one action to start."); return;
        }
        if (MAIN_FRAME.getInfiniteRBtn().isSelected()) {
            infinite = true; repeatByTime = false;
            totalRepeat = -1;
            repeatTimes = Long.MAX_VALUE;
        } else if (MAIN_FRAME.getRepeatByTimeRBtn().isSelected()) {
            infinite = false; repeatByTime = true;
            long h = parseLong(MAIN_FRAME.getHoursField().getText(), 0);
            long m = parseLong(MAIN_FRAME.getMinutesField().getText(), 0);
            long s = parseLong(MAIN_FRAME.getSecondsField().getText(), 0);
            long totalSec = h * 3600 + m * 60 + s;
            if (totalSec <= 0) { changeFrame(false, "Please enter a valid duration (h/m/s)."); return; }
            endTimeMs = System.currentTimeMillis() + totalSec * 1000;
            totalRepeat = totalSec;
            repeatTimes = Long.MAX_VALUE;
        } else {
            infinite = false; repeatByTime = false;
            String input = MAIN_FRAME.getRepeatTimesTxt().getText().trim();
            if (input.isEmpty()) { changeFrame(false, "Please enter the number of repeat cycles."); return; }
            try {
                repeatTimes = Long.parseLong(input);
                if (repeatTimes < 1) { changeFrame(false, "Repeat count must be at least 1."); return; }
            } catch (NumberFormatException ex) { changeFrame(false, "Please enter a valid whole number."); return; }
            totalRepeat = repeatTimes;
        }
        doneCount = 0;
        // Hide-while-running:
        //   * Windows: hide the JFrame and install a system tray icon. F8 or
        //     double-click on the tray icon stops and restores the window.
        //   * macOS: setVisible(false)/setVisible(true) cycle has known
        //     NSWindow bugs (focus chain breaks, "getApplicationProperty"
        //     warnings, F8 sometimes auto-closes the restored window). The
        //     user explicitly accepts those bugs in exchange for the hide
        //     behavior, so we honor the checkbox here.
        var f = MAIN_FRAME.getFrame();
        if (MAIN_FRAME.isHideFrame()) {
            if (IS_WINDOWS) {
                installTrayIcon();
            }
            f.setVisible(false);
        }
        f.setEnabled(false);
        enableInput(false);
        isStopClick = false;
        performClick();
    }

    /**
     * Install a tray icon (Windows only). Clicking the icon stops the run
     * and restores the window. The action mirrors F8 / the Stop button.
     */
    private void installTrayIcon() {
        if (trayIcon != null) return; // already installed
        if (!SystemTray.isSupported()) {
            LOG.warning("SystemTray not supported on this platform");
            return;
        }
        try {
            Image image = Toolkit.getDefaultToolkit().createImage(
                    AutoClicker.class.getResource("/static/images/click_pointer.ico"));
            // Fallback to a generic image if the resource isn't found.
            if (image == null) {
                image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
            }

            var popup = new PopupMenu();
            var stopItem = new MenuItem("Stop & Restore");
            stopItem.addActionListener(e -> stopClick());
            popup.add(stopItem);
            var exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> System.exit(0));
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "Kell Auto Clicker — running", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> stopClick()); // double-click on Windows
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to install tray icon — keeping window visible", ex);
            trayIcon = null;
            MAIN_FRAME.getFrame().setVisible(true);
        }
    }

    /** Remove the tray icon if installed. Safe to call any time. */
    private void removeTrayIcon() {
        if (trayIcon != null && SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Exception ignored) { /* tray may already be gone */ }
            trayIcon = null;
        }
    }


    // ── Main click loop ───────────────────────────────────────────────────────
    private void performClick() {
        var clickThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    // Snapshot the list to avoid ConcurrentModification
                    var items = new ArrayList<>(MAIN_FRAME.getTableItems().getListItems());
                    while (!isStopClick && repeatTimes > 0) {
                        // Time-based stop check
                        if (repeatByTime && System.currentTimeMillis() >= endTimeMs) break;

                        for (int i = 0; i < items.size(); i++) {
                            if (isStopClick) break;
                            if (repeatByTime && System.currentTimeMillis() >= endTimeMs) {
                                isStopClick = true;
                                break;
                            }

                            var item = items.get(i);
                            if (GlobalConst.EVENT_COMMENT.equals(item.getTypeEvent())) continue;

                            final int rowIdx = i;
                            SwingUtilities.invokeLater(() -> MAIN_FRAME.getTableItems().setHighlightRow(rowIdx));

                            switch (item) {
                                case ItemClick click -> clickEvent(click);
                                case ItemPress press -> {
                                    var latch = new CountDownLatch(1);
                                    singlePressEvent(press, latch);
                                    latch.await();
                                }
                                case ItemDAD dad -> performDAD(dad);
                                default -> {
                                    if (GlobalConst.EVENT_SCROLL.equals(item.getTypeEvent())) {
                                        performScroll(item);
                                    }
                                }
                            }
                            lock.wait(item.getNextTime() + random.nextInt(10) * 10L);
                        }

                        if (!infinite && !repeatByTime) repeatTimes--;
                        doneCount++;

                        final long done = doneCount;
                        final long total = totalRepeat;
                        if (repeatByTime) {
                            long remaining = Math.max(0, (endTimeMs - System.currentTimeMillis()) / 1000);
                            SwingUtilities.invokeLater(() -> MAIN_FRAME.updateProgressTime(done, remaining));
                        } else {
                            SwingUtilities.invokeLater(() -> MAIN_FRAME.updateProgress(done, total));
                        }
                    }
                    stopClickThreads();
                    SwingUtilities.invokeLater(this::stopClickUI);
                }
            } catch (AWTException | InterruptedException ex) {
                stopClickThreads();
                SwingUtilities.invokeLater(this::stopClickUI);
                LOG.log(Level.SEVERE, "Error in click loop", ex);
            }
        });
        clickThread.setName("AutoClicker-Thread");
        clickThread.setDaemon(true);
        clickThread.start();
        runningThreads.add(clickThread);
    }


    // ── Action implementations ────────────────────────────────────────────────

    /**
     * Human-like mouse move + click (left or right).
     * Reuses the smooth-move algorithm from the original code.
     */
    private void clickEvent(ItemClick item) throws AWTException {
        var robot = new Robot();
        var cur = MouseInfo.getPointerInfo().getLocation();
        int targetX = item.getXAxis(), targetY = item.getYAxis();

        smoothMouseMove(robot, cur.x, cur.y, targetX, targetY);

        // Small random offset to look human
        robot.mouseMove(targetX + random.nextInt(7) - 3, targetY + random.nextInt(7) - 3);
        int btn = item.isRightClick() ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(btn);
        robot.delay(30 + random.nextInt(40)); // hold briefly like a human
        robot.mouseRelease(btn);
    }

    /**
     * DAD: smooth move to From, press, smooth move to To, release.
     */
    private void performDAD(ItemDAD item) throws AWTException {
        var robot = new Robot();
        // Move to start position first
        var cur = MouseInfo.getPointerInfo().getLocation();
        smoothMouseMove(robot, cur.x, cur.y, item.getXAxis1(), item.getYAxis1());

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(80 + random.nextInt(60)); // hold before drag

        // Drag to destination with smooth move
        smoothMouseMove(robot, item.getXAxis1(), item.getYAxis1(),
                                item.getXAxis2(), item.getYAxis2());

        robot.delay(40 + random.nextInt(40)); // settle before release
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * Smooth human-like mouse movement from (x1,y1) to (x2,y2).
     * Uses the same curved-path algorithm as the original clickEvent.
     */
    private void smoothMouseMove(Robot robot, int x1, int y1, int x2, int y2) {
        int steps = 400 + random.nextInt(400); // 400-800 steps
        double dx = (x2 - x1) / (double) steps;
        double dy = (y2 - y1) / (double) steps;
        int wobble = 0, prevX = x1, prevY = y1;
        for (int i = 1; i <= steps; i++) {
            int nx = x1 + (int)(dx * i);
            // Occasionally add a small perpendicular wobble
            if (random.nextInt(11) % 3 == 0 && i % 50 == 0) {
                wobble = random.nextInt(6) - 3;
            }
            int ny = y1 + (int)(dy * i) + wobble;
            if (nx != prevX || ny != prevY) {
                robot.mouseMove(nx, ny);
            }
            prevX = nx; prevY = ny;
        }
        // Ensure we land exactly on target
        robot.mouseMove(x2, y2);
    }

    private void singlePressEvent(ItemPress item, CountDownLatch latch) {
        var timer = new javax.swing.Timer(item.getPressTime(), null);
        timer.addActionListener(e -> {
            if (isStopClick) {
                timer.stop();
                latch.countDown();
                return;
            }
            try {
                var robot = new Robot();
                robot.keyPress(item.getKeyCode());
                robot.keyRelease(item.getKeyCode());
            } catch (AWTException ex) {
                LOG.log(Level.SEVERE, "Key press error", ex);
            }
            timer.stop();
            latch.countDown();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void performScroll(Item item) {
        try {
            new Robot().mouseWheel(item.getPressTime());
        } catch (AWTException e) {
            LOG.log(Level.SEVERE, "Scroll error", e);
        }
    }


    // ── Show/hide sub-frames ──────────────────────────────────────────────────
    private void showAddPointFrame() {
        ADD_POINT_FRAME.reset();
        MAIN_FRAME.getFrame().setVisible(false);
        var ma = new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                // Ignore clicks on the hint badge so users can click "through" it
                // by accident without creating a bad item.
                if (ADD_POINT_FRAME.hintBounds().contains(e.getPoint())) return;

                String btn;
                if (SwingUtilities.isRightMouseButton(e)) {
                    btn = "Right";
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    btn = "Left";
                } else {
                    return; // ignore middle/extra buttons
                }
                MAIN_FRAME.getTableItems().addItem(new ItemClick(e.getX(), e.getY(), btn));
                ADD_POINT_FRAME.getFrame().setVisible(false);
                ADD_POINT_FRAME.getFrame().removeMouseListener(this);
                restoreMainFrameAfterPointPick();
            }
        };
        ADD_POINT_FRAME.setOnCancel(() -> {
            ADD_POINT_FRAME.getFrame().removeMouseListener(ma);
            restoreMainFrameAfterPointPick();
        });
        ADD_POINT_FRAME.getFrame().addMouseListener(ma);
        ADD_POINT_FRAME.getFrame().setVisible(true);
    }

    private void restoreMainFrameAfterPointPick() {
        MAIN_FRAME.getFrame().setVisible(true);
        MAIN_FRAME.getFrame().toFront();
    }

    private void showAddPressFrame(boolean show) {
        MAIN_FRAME.getFrame().setEnabled(!show);
        ADD_PRESS_FRAME.setVisible(show);
        if (!show) { MAIN_FRAME.getFrame().setEnabled(true); MAIN_FRAME.getFrame().toFront(); }
        if (show) ADD_PRESS_FRAME.getKeyTxt().requestFocus();
    }

    private void showAddScrollFrame(boolean show) {
        MAIN_FRAME.getFrame().setEnabled(!show);
        ADD_SCROLL_FRAME.setVisible(show);
        if (!show) { MAIN_FRAME.getFrame().setEnabled(true); MAIN_FRAME.getFrame().toFront(); }
        if (show) ADD_SCROLL_FRAME.getWheelAmtTxt().requestFocus();
    }

    private void showAddDADFrame(boolean show) {
        if (show) ADD_DAD_FRAME.resetCoords();
        MAIN_FRAME.getFrame().setEnabled(!show);
        ADD_DAD_FRAME.getDialog().setVisible(show);
        if (!show) { MAIN_FRAME.getFrame().setEnabled(true); MAIN_FRAME.getFrame().toFront(); }
    }

    private void pickDADPoint(boolean isFrom) {
        ADD_DAD_FRAME.getDialog().setVisible(false);
        ADD_POINT_FRAME.setOnCancel(() -> {});
        var picker = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                ADD_POINT_FRAME.getFrame().setVisible(false);
                ADD_POINT_FRAME.getFrame().removeMouseListener(this);
                if (isFrom) ADD_DAD_FRAME.setFrom(e.getX(), e.getY());
                else        ADD_DAD_FRAME.setTo(e.getX(), e.getY());
                ADD_DAD_FRAME.getDialog().setVisible(true);
                ADD_DAD_FRAME.getDialog().toFront();
            }
        };
        ADD_POINT_FRAME.getFrame().addMouseListener(picker);
        ADD_POINT_FRAME.getFrame().setVisible(true);
    }

    private void addDADEvent() {
        if (!ADD_DAD_FRAME.isFromSet() || !ADD_DAD_FRAME.isToSet()) {
            changeFrame(false, "Please pick both the start and end points first."); return;
        }
        int nextTime = parseInt(ADD_DAD_FRAME.getNextTimeTxt().getText(), 500);
        MAIN_FRAME.getTableItems().addItem(new ItemDAD(
            ADD_DAD_FRAME.getFromX(), ADD_DAD_FRAME.getFromY(),
            ADD_DAD_FRAME.getToX(),   ADD_DAD_FRAME.getToY(), 0, nextTime));
        showAddDADFrame(false);
    }

    private void showAddCommentFrame(boolean show) {
        MAIN_FRAME.getFrame().setEnabled(!show);
        if (show) ADD_COMMENT_FRAME.getCommentTxt().setText("");
        ADD_COMMENT_FRAME.getDialog().setVisible(show);
        if (!show) { MAIN_FRAME.getFrame().setEnabled(true); MAIN_FRAME.getFrame().toFront(); }
        if (show) ADD_COMMENT_FRAME.getCommentTxt().requestFocus();
    }

    // ── Add-item actions ──────────────────────────────────────────────────────
    private void addPressKey() {
        String input = ADD_PRESS_FRAME.getKeyTxt().getText();
        if (input.isEmpty()) { changeFrame(false, "Please enter a key to press."); return; }
        int keyCode   = KeyEvent.getExtendedKeyCodeForChar(input.charAt(0));
        int pressTime = parseInt(ADD_PRESS_FRAME.getPressTimeTxt().getText(), 100);
        int nextTime  = parseInt(ADD_PRESS_FRAME.getNextTimeTxt().getText(), 500);
        MAIN_FRAME.getTableItems().addItem(
            new ItemPress(input, keyCode, pressTime, nextTime, ADD_PRESS_FRAME.getIsSinglePress()));
        ADD_PRESS_FRAME.getKeyTxt().setText("");
        showAddPressFrame(false);
    }

    private void addScrollEvent() {
        String input = ADD_SCROLL_FRAME.getWheelAmtTxt().getText();
        if (input.isEmpty()) { changeFrame(false, "Enter wheel amount"); return; }
        int nextTime = parseInt(ADD_SCROLL_FRAME.getNextTimeTxt().getText(), 500);
        MAIN_FRAME.getTableItems().addItem(
            new Item(input, parseInt(input, 1), nextTime, GlobalConst.EVENT_SCROLL));
        ADD_SCROLL_FRAME.getWheelAmtTxt().setText("");
        showAddScrollFrame(false);
    }

    private void addComment() {
        String text = ADD_COMMENT_FRAME.getCommentTxt().getText().trim();
        if (text.isEmpty()) { changeFrame(false, "Enter comment text"); return; }
        if (!text.startsWith(GlobalConst.COMMENT_PREFIX)) text = GlobalConst.COMMENT_PREFIX + " " + text;
        MAIN_FRAME.getTableItems().addItem(new ItemComment(text));
        showAddCommentFrame(false);
    }


    // ── Global hotkey ─────────────────────────────────────────────────────────
    private void globalKeyListener() {
        Logger.getLogger(GlobalScreen.class.getPackageName()).setLevel(Level.OFF);
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            // Common reasons:
            //   - macOS: app needs Accessibility permission (System Settings).
            //   - Windows: AV/Defender blocking jnativehook's temp DLL.
            //   - Linux:  X11 access not allowed (Wayland often blocks too).
            // Don't kill the app — the in-window F7/F8 fallback still works.
            LOG.log(Level.WARNING, "Global hotkeys disabled (native hook failed): " + ex.getMessage(), ex);
            SwingUtilities.invokeLater(() ->
                MAIN_FRAME.showToast("Global F7/F8 disabled — focus the window to use hotkeys"));
            return;
        }
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_F7) {
                    SwingUtilities.invokeLater(AutoClicker.this::onF7);
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) {
                    SwingUtilities.invokeLater(AutoClicker.this::onF8);
                }
            }
            @Override public void nativeKeyReleased(NativeKeyEvent e) {}
            @Override public void nativeKeyTyped(NativeKeyEvent e) {}
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private void changeFrame(boolean isMainFrame) { changeFrame(isMainFrame, ""); }
    private void changeFrame(boolean isMainFrame, String message) {
        MAIN_FRAME.getFrame().setEnabled(isMainFrame);
        if (ADD_PRESS_FRAME.getDialog().isVisible()) ADD_PRESS_FRAME.getDialog().setEnabled(isMainFrame);
        if (isMainFrame) {
            MSG_FRAME.hideMessage();
        } else {
            MSG_FRAME.showMessage(message);
        }
    }

    private void enableInput(boolean enabled) {
        MAIN_FRAME.getInfiniteRBtn().setEnabled(enabled);
        MAIN_FRAME.getRepeatByCountRBtn().setEnabled(enabled);
        MAIN_FRAME.getRepeatByTimeRBtn().setEnabled(enabled);
        MAIN_FRAME.getHideMainFrameRBtn().setEnabled(enabled);
        MAIN_FRAME.getRepeatTimesTxt().setEnabled(enabled && MAIN_FRAME.getRepeatByCountRBtn().isSelected());
        MAIN_FRAME.getHoursField().setEnabled(enabled && MAIN_FRAME.getRepeatByTimeRBtn().isSelected());
        MAIN_FRAME.getMinutesField().setEnabled(enabled && MAIN_FRAME.getRepeatByTimeRBtn().isSelected());
        MAIN_FRAME.getSecondsField().setEnabled(enabled && MAIN_FRAME.getRepeatByTimeRBtn().isSelected());
    }
    private int  parseInt(String s, int fallback)  { try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return fallback; } }
    private long parseLong(String s, long fallback) { try { return Long.parseLong(s.trim()); }  catch (NumberFormatException e) { return fallback; } }


    // ── Save (JSON) ───────────────────────────────────────────────────────────
    private void saveData() {
        var items = MAIN_FRAME.getTableItems().getListItems();
        if (items.isEmpty()) { changeFrame(false, "List is empty"); return; }

        var fc = new JFileChooser();
        fc.setDialogTitle("Save As");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
        fc.setSelectedFile(new File("data_" + System.currentTimeMillis() + ".json"));

        if (fc.showSaveDialog(MAIN_FRAME.getFrame()) != JFileChooser.APPROVE_OPTION) return;

        var chosen = fc.getSelectedFile();
        // Make sure the file ends with .json (the filter doesn't enforce it on
        // every L&F — Aqua in particular lets the user type any name).
        if (!chosen.getName().toLowerCase().endsWith(".json")) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + ".json");
        }

        // Confirm overwrite. JFileChooser doesn't ask by default on Linux/macOS.
        if (chosen.exists()) {
            int ans = JOptionPane.showConfirmDialog(MAIN_FRAME.getFrame(),
                    chosen.getName() + " already exists. Overwrite?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) return;
        }

        var path = chosen.toPath();
        try {
            // Create the parent directory if it doesn't exist (safer than failing).
            if (path.getParent() != null) Files.createDirectories(path.getParent());

            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                var arr = new JsonArray();
                for (var item : items) arr.add(itemToJson(item));
                GSON.toJson(arr, w);
            }
            changeFrame(false, "Saved: " + path);
            LOG.info("Saved to: " + path);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Save error", e);
            changeFrame(false, "Save failed: " + e.getMessage());
        }
    }

    private JsonObject itemToJson(Item item) {
        var o = new JsonObject();
        o.addProperty("type", item.getTypeEvent());
        o.addProperty("name", item.getName());
        o.addProperty("pressTime", item.getPressTime());
        o.addProperty("nextTime", item.getNextTime());
        switch (item) {
            case ItemClick ic -> {
                o.addProperty("x", ic.getXAxis());
                o.addProperty("y", ic.getYAxis());
                o.addProperty("mouseButton", ic.getButton());
            }
            case ItemPress ip -> {
                o.addProperty("keyCode", ip.getKeyCode());
                o.addProperty("multiPress", ip.isMultiPress());
            }
            case ItemDAD id -> {
                o.addProperty("x1", id.getXAxis1());
                o.addProperty("y1", id.getYAxis1());
                o.addProperty("x2", id.getXAxis2());
                o.addProperty("y2", id.getYAxis2());
            }
            // SCROLL and COMMENT only need the base fields already added
            default -> { /* base fields suffice */ }
        }
        return o;
    }

    // ── Load / Append (JSON) ──────────────────────────────────────────────────
    private void loadData(boolean append) {
        var fc = new JFileChooser();
        fc.setDialogTitle(append ? "Append file" : "Load file");
        fc.setFileFilter(new FileNameExtensionFilter("Auto-clicker files (*.json, *.txt)", "json", "txt"));
        if (fc.showOpenDialog(MAIN_FRAME.getFrame()) != JFileChooser.APPROVE_OPTION) return;

        var file = fc.getSelectedFile();
        if (file == null || !file.exists()) {
            changeFrame(false, "File not found: " + (file == null ? "(none)" : file.getName()));
            return;
        }
        var loaded = parseFile(file);
        if (loaded == null) return;

        if (append) {
            MAIN_FRAME.getTableItems().appendItems(new Vector<>(loaded), file.getName());
        } else {
            MAIN_FRAME.getTableItems().setListItems(new Vector<>(loaded));
        }
        changeFrame(false, (append ? "Appended: " : "Loaded: ") + file.getName());
        LOG.info((append ? "Appended: " : "Loaded: ") + file.getAbsolutePath());
    }

    /**
     * Parses both new JSON format (.json) and legacy pipe-delimited format (.txt).
     * Returns null on error (error message already shown).
     */
    private List<Item> parseFile(File file) {
        var fname = file.getName().toLowerCase();
        try {
            return fname.endsWith(".json") ? parseJson(file) : parseLegacyTxt(file);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Load error", e);
            changeFrame(false, "Load failed: " + e.getMessage());
            return null;
        }
    }

    private List<Item> parseJson(File file) throws IOException {
        var items = new ArrayList<Item>();
        try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            var arr = JsonParser.parseReader(r).getAsJsonArray();
            for (var el : arr) {
                var o = el.getAsJsonObject();
                String type   = resolveType(o);
                String name   = o.get("name").getAsString();
                int pressTime = o.has("pressTime") ? o.get("pressTime").getAsInt() : 0;
                int nextTime  = o.has("nextTime")  ? o.get("nextTime").getAsInt()  : 100;

                Item parsed = switch (type) {
                    case GlobalConst.EVENT_CLICK -> {
                        int x      = o.get("x").getAsInt();
                        int y      = o.get("y").getAsInt();
                        String btn = o.has("mouseButton") ? o.get("mouseButton").getAsString() : "Left";
                        yield new ItemClick(name, x, y, pressTime, nextTime, btn);
                    }
                    case GlobalConst.EVENT_PRESS -> {
                        int kc     = o.get("keyCode").getAsInt();
                        boolean mp = o.has("multiPress") && o.get("multiPress").getAsBoolean();
                        yield new ItemPress(name, kc, pressTime, nextTime, mp);
                    }
                    case GlobalConst.EVENT_DAD -> new ItemDAD(
                            o.get("x1").getAsInt(), o.get("y1").getAsInt(),
                            o.get("x2").getAsInt(), o.get("y2").getAsInt(),
                            pressTime, nextTime);
                    case GlobalConst.EVENT_COMMENT -> new ItemComment(name);
                    default -> new Item(name, pressTime, nextTime, type);
                };
                items.add(parsed);
            }
        }
        return items;
    }

    /** Backward-compatible reader for old pipe-delimited .txt files. */
    private List<Item> parseLegacyTxt(File file) throws IOException {
        var items = new ArrayList<Item>();
        try (var r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith(GlobalConst.COMMENT_PREFIX)) {
                    items.add(new ItemComment(line));
                    continue;
                }
                var p = line.split("\\|");
                if (p.length < 2) continue;
                String type = legacyTypeToString(p[0].trim());
                if (type == null) continue;
                var name = p[1];
                int pressTime = p.length > 2 ? toInt(p[2]) : 0;
                int nextTime  = p.length > 3 ? toInt(p[3]) : 100;
                switch (type) {
                    case GlobalConst.EVENT_CLICK -> {
                        if (p.length >= 6) items.add(new ItemClick(name, toInt(p[4]), toInt(p[5]), pressTime, nextTime));
                    }
                    case GlobalConst.EVENT_PRESS -> {
                        if (p.length >= 6) items.add(new ItemPress(name, toInt(p[4]), pressTime, nextTime, Boolean.parseBoolean(p[5])));
                    }
                    case GlobalConst.EVENT_DAD -> {
                        if (p.length >= 8) items.add(new ItemDAD(toInt(p[4]), toInt(p[5]), toInt(p[6]), toInt(p[7]), pressTime, nextTime));
                    }
                    default -> items.add(new Item(name, pressTime, nextTime, type));
                }
            }
        }
        return items;
    }

    /** Maps old numeric type codes (from legacy .txt files) to the new String constants. */
    private static String legacyTypeToString(String raw) {
        return switch (raw) {
            case "1" -> GlobalConst.EVENT_CLICK;
            case "2" -> GlobalConst.EVENT_PRESS;
            case "3" -> GlobalConst.EVENT_SCROLL;
            case "4" -> GlobalConst.EVENT_DAD;
            case "5" -> GlobalConst.EVENT_COMMENT;
            default  -> null;
        };
    }

    /**
     * Resolves the "type" field from a JSON object, supporting both old integer
     * format (1-5) and new string format ("CLICK", "PRESS", etc.) for backward
     * compatibility with existing save files.
     */
    private static String resolveType(JsonObject o) {
        var el = o.get("type");
        if (el == null) return "";
        if (el.getAsJsonPrimitive().isNumber()) {
            return switch (el.getAsInt()) {
                case 1  -> GlobalConst.EVENT_CLICK;
                case 2  -> GlobalConst.EVENT_PRESS;
                case 3  -> GlobalConst.EVENT_SCROLL;
                case 4  -> GlobalConst.EVENT_DAD;
                case 5  -> GlobalConst.EVENT_COMMENT;
                default -> "";
            };
        }
        return el.getAsString();
    }

    private int toInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}
