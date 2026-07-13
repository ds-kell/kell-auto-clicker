package kell.autoclick.click;

import java.awt.AWTException;
import java.awt.Frame;
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
import java.util.Optional;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
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

    private static final String JSON_EXTENSION = ".json";
    private static final String JSON_FILTER_EXTENSION = "json";
    private static final String JSON_FIELD_MOUSE_BUTTON = "mouseButton";
    private static final String JSON_FIELD_MULTI_PRESS = "multiPress";
    private static final String JSON_FIELD_NEXT_TIME = "nextTime";
    private static final String JSON_FIELD_PRESS_TIME = "pressTime";
    private static final String JSON_FIELD_TYPE = "type";
    private static final String JSON_FIELD_NAME = "name";
    private static final String JSON_FIELD_KEY_CODE = "keyCode";
    private static final String JSON_FIELD_X = "x";
    private static final String JSON_FIELD_Y = "y";
    private static final String JSON_FIELD_X1 = "x1";
    private static final String JSON_FIELD_Y1 = "y1";
    private static final String JSON_FIELD_X2 = "x2";
    private static final String JSON_FIELD_Y2 = "y2";
    private static final int DEFAULT_PRESS_TIME = 0;
    private static final int DEFAULT_NEXT_TIME = 100;
    private static final int MOUSE_MOVE_RANDOM_BOUND = 7;
    private static final int MOUSE_MOVE_RANDOM_OFFSET = 3;
    private static final int CLICK_HOLD_BASE_MS = 30;
    private static final int CLICK_HOLD_RANDOM_MS = 40;
    private static final int DRAG_HOLD_BASE_MS = 80;
    private static final int DRAG_HOLD_RANDOM_MS = 60;
    private static final int DRAG_SETTLE_BASE_MS = 40;
    private static final int DRAG_SETTLE_RANDOM_MS = 40;
    private static final int SMOOTH_MOVE_BASE_STEPS = 400;
    private static final int SMOOTH_MOVE_RANDOM_STEPS = 400;
    private static final int SMOOTH_MOVE_WOBBLE_CHECK_BOUND = 11;
    private static final int SMOOTH_MOVE_WOBBLE_MODULO = 3;
    private static final int SMOOTH_MOVE_WOBBLE_INTERVAL = 50;
    private static final int SMOOTH_MOVE_WOBBLE_BOUND = 6;
    private static final int CLICK_DELAY_RANDOM_BOUND = 10;
    private static final long CLICK_DELAY_RANDOM_MULTIPLIER_MS = 10L;

    // ── Frames ────────────────────────────────────────────────────────────────
    private final MainFrame mainFrame = new MainFrame();
    // Dialogs are created after mainFrame so they can use it as owner
    private final AddPointFrame    addPointFrame;
    private final AddPressKeyFrame addPressFrame;
    private final AddScrollFrame   addScrollFrame;
    private final AddDADFrame      addDadFrame;
    private final AddCommentFrame  addCommentFrame;
    private final MessageFrame     msgFrame         = new MessageFrame();

    // ── State ─────────────────────────────────────────────────────────────────
    private volatile boolean isStopClick  = true;
    private volatile boolean infinite     = false;
    private volatile boolean repeatByTime = false;
    private final AtomicLong repeatTimes = new AtomicLong();
    private final AtomicLong totalRepeat = new AtomicLong();
    private final AtomicLong doneCount = new AtomicLong();
    /** End time in ms (System.currentTimeMillis) when repeatByTime mode is active */
    private volatile long    endTimeMs    = 0;

    public  final Object         lock           = new Object();
    private final Vector<Thread> runningThreads = new Vector<>();
    private final Random         random         = new Random();

    private static final Logger LOG = Logger.getLogger(AutoClicker.class.getName());

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final boolean IS_MAC =
            System.getProperty("os.name", "").toLowerCase().contains("mac");

    /** Tray icon used to hide/restore the window while clicking on Windows. */
    private TrayIcon trayIcon;

    // ── Gson (JSON serialization) ─────────────────────────────────────────────
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    // ── Constructor ───────────────────────────────────────────────────────────
    public AutoClicker() {
        setupFileLogger();
        mainFrame.init();
        JFrame owner = mainFrame.getFrame();
        // Create dialogs with owner so JDialog parent relationship is correct
        addPointFrame   = new AddPointFrame();
        addPressFrame   = new AddPressKeyFrame(owner);
        addScrollFrame  = new AddScrollFrame(owner);
        addDadFrame     = new AddDADFrame(owner);
        addCommentFrame = new AddCommentFrame(owner);
        msgFrame.initWithOwner(owner);
        mainFrame.getFrame().setVisible(true);
    }

    public void start() {
        eventListener();
        tableListener();
        // Always wire the in-window F7/F8 fallback BEFORE attempting the
        // global native hook. If the native hook fails (Windows AV, macOS
        // Accessibility not granted, etc.), users still have the hotkeys
        // when the main window is focused.
        mainFrame.setHotkeyHandlers(this::onF7, this::onF8);
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
        } catch (IOException exception) {
            LOG.log(Level.WARNING, exception, () ->
                    "Could not set up file logger: %s".formatted(exception.getMessage()));
        }
    }

    // ── Window focus ──────────────────────────────────────────────────────────
    private void windowFocus() {
        // JDialog with owner handles focus automatically — no manual management needed
    }

    /** Restore and focus the existing app when its icon is opened again. */
    public void showMainWindow() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = mainFrame.getFrame();
            removeTrayIcon();
            restoreMainWindow(frame);
        });
    }


    // ── Event listeners ───────────────────────────────────────────────────────
    private void eventListener() {
        // Repeat mode toggles
        mainFrame.getInfiniteRBtn().addActionListener(_ -> {
            infinite = true; repeatByTime = false;
        });
        mainFrame.getRepeatByCountRBtn().addActionListener(_ -> {
            infinite = false; repeatByTime = false;
        });
        mainFrame.getRepeatByTimeRBtn().addActionListener(_ -> {
            infinite = false; repeatByTime = true;
        });

        mainFrame.getHideMainFrameRBtn().addItemListener(e ->
            mainFrame.setHideFrame(e.getStateChange() == ItemEvent.SELECTED));

        mainFrame.getAddClickBtn().addActionListener(_ -> showAddPointFrame());
        // Add Key: not disabled — handled entirely by MouseListener in MainFrame.applyTheme()

        mainFrame.getAddScrollBtn().addActionListener(_ -> {
            showAddScrollFrame(true);
            addScrollFrame.getWheelAmtTxt().requestFocus();
        });
        mainFrame.getAddDADBtn().addActionListener(_ -> showAddDADFrame(true));

        // DAD frame buttons
        addDadFrame.getFromBtn().addActionListener(_ -> pickDADPoint(true));
        addDadFrame.getToBtn().addActionListener(_ -> pickDADPoint(false));
        addDadFrame.getAddBtn().addActionListener(_ -> addDADEvent());
        addDadFrame.getCancelBtn().addActionListener(_ -> showAddDADFrame(false));
        addDadFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddDADFrame(false); }
        });

        mainFrame.getAddCommentBtn().addActionListener(_ -> showAddCommentFrame(true));
        mainFrame.getStartBtn().addActionListener(_ -> startClick());
        mainFrame.getStopBtn().addActionListener(_ -> stopClick());
        mainFrame.getDeleteBtn().addActionListener(_ -> deleteSelected());
        mainFrame.getResetBtn().addActionListener(_ -> resetData());
        mainFrame.getSaveBtn().addActionListener(_ -> saveData());
        mainFrame.getLoadBtn().addActionListener(_ -> loadData(false));
        mainFrame.getAppendBtn().addActionListener(_ -> loadData(true));
        mainFrame.getMoveUpBtn().addActionListener(_ -> mainFrame.getTableItems().moveSelectedUp());
        mainFrame.getMoveDownBtn().addActionListener(_ -> mainFrame.getTableItems().moveSelectedDown());

        msgFrame.getConfirmBtn().addActionListener(_ -> changeFrame(true));

        addPressFrame.getAddBtn().addActionListener(_ -> addPressKey());
        addPressFrame.getCancelBtn().addActionListener(_ -> showAddPressFrame(false));
        addPressFrame.getIsSinglePressRBtn().addItemListener(e -> {
            addPressFrame.setIsSinglePress(e.getStateChange() == ItemEvent.SELECTED);
            addPressFrame.getIsMultiPressRBtn().setSelected(!addPressFrame.getIsSinglePress());
        });
        addPressFrame.getIsMultiPressRBtn().addItemListener(e -> {
            addPressFrame.setIsSinglePress(e.getStateChange() != ItemEvent.SELECTED);
            addPressFrame.getIsSinglePressRBtn().setSelected(addPressFrame.getIsSinglePress());
        });
        addPressFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddPressFrame(false); }
        });

        addScrollFrame.getAddBtn().addActionListener(_ -> addScrollEvent());
        addScrollFrame.getCancelBtn().addActionListener(_ -> showAddScrollFrame(false));
        addScrollFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddScrollFrame(false); }
        });

        addCommentFrame.getAddBtn().addActionListener(_ -> addComment());
        addCommentFrame.getCancelBtn().addActionListener(_ -> showAddCommentFrame(false));
        addCommentFrame.getDialog().addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent ignoredEvent) { showAddCommentFrame(false); }
        });
    }

    // ── Table listener ────────────────────────────────────────────────────────
    private void tableListener() {
        mainFrame.getTableItems().getModel().addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row < 0 || col < 0) return;
            var items = mainFrame.getTableItems().getListItems();
            if (row >= items.size()) return;
            var data = mainFrame.getTableItems().getModel().getValueAt(row, col);
            if (data == null) return;
            var item = items.get(row);
            switch (col) {
                case 2 -> item.setName((String) data); // Name
                case 5 -> updatePressTime(item, data); // Hold
                case 6 -> updateNextTime(item, data);  // Delay
                default -> { /* no-op */ }
            }
        });
    }

    private void updatePressTime(Item item, Object data) {
        Integer value = parseInteger(data);
        if (value != null) {
            item.setPressTime(value);
        }
    }

    private void updateNextTime(Item item, Object data) {
        Integer value = parseInteger(data);
        if (value != null) {
            item.setNextTime(value);
        }
    }

    private Integer parseInteger(Object data) {
        String value = String.valueOf(data).trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            LOG.log(Level.FINE, "Ignoring invalid integer table value: {0}", value);
            return null;
        }
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
        var f = mainFrame.getFrame();

        // Restore from tray (Windows). Safe no-op on other platforms.
        removeTrayIcon();
        restoreMainWindow(f);

        mainFrame.getTableItems().setHighlightRow(-1);
        mainFrame.setProgressText("Stopped. Done: " + doneCount.get());
    }

    /** Full stop — call from EDT (button click) or native hook thread. */
    public void stopClick() {
        stopClickThreads();
        SwingUtilities.invokeLater(this::stopClickUI);
    }

    public void resetData() {
        if (mainFrame.getTableItems().getTable().isEditing())
            mainFrame.getTableItems().getTable().getCellEditor().stopCellEditing();
        infinite = false; repeatByTime = false;
        repeatTimes.set(0);
        totalRepeat.set(0);
        doneCount.set(0);
        mainFrame.getTableItems().clearTable();
        mainFrame.getInfiniteRBtn().setSelected(false);
        mainFrame.getRepeatByCountRBtn().setSelected(true);
        mainFrame.setHideFrame(true);
        mainFrame.getHideMainFrameRBtn().setSelected(true);
        mainFrame.getRepeatTimesTxt().setText("");
        mainFrame.getHoursField().setText("0");
        mainFrame.getMinutesField().setText("0");
        mainFrame.getSecondsField().setText("0");
        mainFrame.setProgressText("Ready");
    }

    public void deleteSelected() {
        if (mainFrame.getTableItems().getTable().getSelectedRows().length < 1) {
            changeFrame(false, mainFrame.getTableItems().getListItems().isEmpty()
                    ? "The list is empty." : "Please select a row to delete.");
            return;
        }
        mainFrame.getTableItems().deleteSelectedRows();
    }

    public void startClick() {
        if (mainFrame.getTableItems().getListItems().isEmpty()) {
            changeFrame(false, "No actions added yet. Add at least one action to start."); return;
        }
        if (mainFrame.getInfiniteRBtn().isSelected()) {
            infinite = true; repeatByTime = false;
            totalRepeat.set(-1);
            repeatTimes.set(Long.MAX_VALUE);
        } else if (mainFrame.getRepeatByTimeRBtn().isSelected()) {
            infinite = false; repeatByTime = true;
            long h = parseLong(mainFrame.getHoursField().getText(), 0);
            long m = parseLong(mainFrame.getMinutesField().getText(), 0);
            long s = parseLong(mainFrame.getSecondsField().getText(), 0);
            long totalSec = h * 3600 + m * 60 + s;
            if (totalSec <= 0) { changeFrame(false, "Please enter a valid duration (h/m/s)."); return; }
            endTimeMs = System.currentTimeMillis() + totalSec * 1000;
            totalRepeat.set(totalSec);
            repeatTimes.set(Long.MAX_VALUE);
        } else {
            infinite = false; repeatByTime = false;
            String input = mainFrame.getRepeatTimesTxt().getText().trim();
            if (input.isEmpty()) { changeFrame(false, "Please enter the number of repeat cycles."); return; }
            try {
                repeatTimes.set(Long.parseLong(input));
                if (repeatTimes.get() < 1) { changeFrame(false, "Repeat count must be at least 1."); return; }
            } catch (NumberFormatException _) { changeFrame(false, "Please enter a valid whole number."); return; }
            totalRepeat.set(repeatTimes.get());
        }
        doneCount.set(0);
        var f = mainFrame.getFrame();
        if (mainFrame.isHideFrame()) {
            hideMainWindowForRun(f);
        }
        f.setEnabled(false);
        enableInput(false);
        isStopClick = false;
        performClick();
    }

    private void hideMainWindowForRun(JFrame frame) {
        if (IS_WINDOWS) {
            installTrayIcon();
            frame.setVisible(false);
            return;
        }
        if (IS_MAC) {
            frame.setState(Frame.ICONIFIED);
            return;
        }
        frame.setVisible(false);
    }

    private void restoreMainWindow(JFrame frame) {
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }
        if ((frame.getExtendedState() & Frame.ICONIFIED) != 0) {
            frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
        }
        frame.setEnabled(true);
        frame.toFront();
        frame.requestFocus();
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
            stopItem.addActionListener(_ -> stopClick());
            popup.add(stopItem);
            var exitItem = new MenuItem("Exit");
            exitItem.addActionListener(_ -> System.exit(0));
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "Kell Auto Clicker — running", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(_ -> stopClick());
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException | RuntimeException ex) {
            LOG.log(Level.WARNING, "Failed to install tray icon — keeping window visible", ex);
            trayIcon = null;
            mainFrame.getFrame().setVisible(true);
        }
    }

    /** Remove the tray icon if installed. Safe to call any time. */
    private void removeTrayIcon() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }
    }


    // ── Main click loop ───────────────────────────────────────────────────────
    private void performClick() {
        var clickThread = new Thread(() -> {
            try {
                runClickLoop();
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

    private void runClickLoop() throws AWTException, InterruptedException {
        synchronized (lock) {
            var items = new ArrayList<>(mainFrame.getTableItems().getListItems());
            while (shouldContinueClicking()) {
                executeCycle(items);
                finishCycle();
            }
            stopClickThreads();
            SwingUtilities.invokeLater(this::stopClickUI);
        }
    }

    private boolean shouldContinueClicking() {
        return !isStopClick
                && repeatTimes.get() > 0
                && (!repeatByTime || System.currentTimeMillis() < endTimeMs);
    }

    private void executeCycle(List<Item> items) throws AWTException, InterruptedException {
        for (int i = 0; i < items.size() && shouldContinueClicking(); i++) {
            Item item = items.get(i);
            if (!GlobalConst.EVENT_COMMENT.equals(item.getTypeEvent())) {
                executeItem(item, i);
                waitBeforeNextItem(item.getNextTime());
            }
        }
    }

    private void executeItem(Item item, int rowIdx) throws AWTException, InterruptedException {
        SwingUtilities.invokeLater(() -> mainFrame.getTableItems().setHighlightRow(rowIdx));
        switch (item) {
            case ItemClick click -> clickEvent(click);
            case ItemPress press -> {
                var latch = new CountDownLatch(1);
                singlePressEvent(press, latch);
                latch.await();
            }
            case ItemDAD dad -> performDAD(dad);
            case Item scroll when GlobalConst.EVENT_SCROLL.equals(scroll.getTypeEvent()) -> performScroll(scroll);
            default -> { /* unsupported item type */ }
        }
    }

    private void waitBeforeNextItem(int delayMs) throws InterruptedException {
        long waitUntil = System.currentTimeMillis()
                + delayMs
                + random.nextInt(CLICK_DELAY_RANDOM_BOUND) * CLICK_DELAY_RANDOM_MULTIPLIER_MS;
        while (!isStopClick && System.currentTimeMillis() < waitUntil) {
            lock.wait(waitUntil - System.currentTimeMillis());
        }
    }

    private void finishCycle() {
        if (!infinite && !repeatByTime) {
            repeatTimes.decrementAndGet();
        }
        long done = doneCount.incrementAndGet();

        final long total = totalRepeat.get();
        if (repeatByTime) {
            long remaining = Math.clamp(
                    (endTimeMs - System.currentTimeMillis()) / 1000,
                    0L,
                    Long.MAX_VALUE
            );
            SwingUtilities.invokeLater(() -> mainFrame.updateProgressTime(done, remaining));
        } else {
            SwingUtilities.invokeLater(() -> mainFrame.updateProgress(done, total));
        }
    }


    // ── Action implementations ────────────────────────────────────────────────

    /**
     * Human-like mouse move + click (left or right).
     * Reuses the smooth-move algorithm from the original code.
     */
    private void clickEvent(ItemClick item) throws AWTException {
        var robot = new Robot();
        var cur = MouseInfo.getPointerInfo().getLocation();
        int targetX = item.getXAxis();
        int targetY = item.getYAxis();

        smoothMouseMove(robot, cur.x, cur.y, targetX, targetY);

        // Small random offset to look human
        robot.mouseMove(
                targetX + random.nextInt(MOUSE_MOVE_RANDOM_BOUND) - MOUSE_MOVE_RANDOM_OFFSET,
                targetY + random.nextInt(MOUSE_MOVE_RANDOM_BOUND) - MOUSE_MOVE_RANDOM_OFFSET
        );
        int btn = item.isRightClick() ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;
        robot.mousePress(btn);
        robot.delay(CLICK_HOLD_BASE_MS + random.nextInt(CLICK_HOLD_RANDOM_MS));
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
        robot.delay(DRAG_HOLD_BASE_MS + random.nextInt(DRAG_HOLD_RANDOM_MS));

        // Drag to destination with smooth move
        smoothMouseMove(robot, item.getXAxis1(), item.getYAxis1(),
                                item.getXAxis2(), item.getYAxis2());

        robot.delay(DRAG_SETTLE_BASE_MS + random.nextInt(DRAG_SETTLE_RANDOM_MS));
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * Smooth human-like mouse movement from (x1,y1) to (x2,y2).
     * Uses the same curved-path algorithm as the original clickEvent.
     */
    private void smoothMouseMove(Robot robot, int x1, int y1, int x2, int y2) {
        int steps = SMOOTH_MOVE_BASE_STEPS + random.nextInt(SMOOTH_MOVE_RANDOM_STEPS);
        double dx = (x2 - x1) / (double) steps;
        double dy = (y2 - y1) / (double) steps;
        int wobble = 0;
        int prevX = x1;
        int prevY = y1;
        for (int i = 1; i <= steps; i++) {
            int nx = x1 + (int)(dx * i);
            // Occasionally add a small perpendicular wobble
            if (random.nextInt(SMOOTH_MOVE_WOBBLE_CHECK_BOUND) % SMOOTH_MOVE_WOBBLE_MODULO == 0
                    && i % SMOOTH_MOVE_WOBBLE_INTERVAL == 0) {
                wobble = random.nextInt(SMOOTH_MOVE_WOBBLE_BOUND) - MOUSE_MOVE_RANDOM_OFFSET;
            }
            int ny = y1 + (int)(dy * i) + wobble;
            if (nx != prevX || ny != prevY) {
                robot.mouseMove(nx, ny);
            }
            prevX = nx;
            prevY = ny;
        }
        // Ensure we land exactly on target
        robot.mouseMove(x2, y2);
    }

    private void singlePressEvent(ItemPress item, CountDownLatch latch) {
        var timer = new javax.swing.Timer(item.getPressTime(), null);
        timer.addActionListener(_ -> {
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
        } catch (AWTException exception) {
            LOG.log(Level.SEVERE, "Scroll error", exception);
        }
    }


    // ── Show/hide sub-frames ──────────────────────────────────────────────────
    private void showAddPointFrame() {
        addPointFrame.reset();
        mainFrame.getFrame().setVisible(false);
        var ma = new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                // Ignore clicks on the hint badge so users can click "through" it
                // by accident without creating a bad item.
                if (addPointFrame.hintBounds().contains(e.getPoint())) return;

                String btn;
                if (SwingUtilities.isRightMouseButton(e)) {
                    btn = "Right";
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    btn = "Left";
                } else {
                    return; // ignore middle/extra buttons
                }
                mainFrame.getTableItems().addItem(new ItemClick(e.getX(), e.getY(), btn));
                addPointFrame.getFrame().setVisible(false);
                addPointFrame.getFrame().removeMouseListener(this);
                restoreMainFrameAfterPointPick();
            }
        };
        addPointFrame.setOnCancel(() -> {
            addPointFrame.getFrame().removeMouseListener(ma);
            restoreMainFrameAfterPointPick();
        });
        addPointFrame.getFrame().addMouseListener(ma);
        addPointFrame.getFrame().setVisible(true);
    }

    private void restoreMainFrameAfterPointPick() {
        mainFrame.getFrame().setVisible(true);
        mainFrame.getFrame().toFront();
    }

    private void showAddPressFrame(boolean show) {
        mainFrame.getFrame().setEnabled(!show);
        addPressFrame.setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
        if (show) addPressFrame.getKeyTxt().requestFocus();
    }

    private void showAddScrollFrame(boolean show) {
        mainFrame.getFrame().setEnabled(!show);
        addScrollFrame.setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
        if (show) addScrollFrame.getWheelAmtTxt().requestFocus();
    }

    private void showAddDADFrame(boolean show) {
        if (show) addDadFrame.resetCoords();
        mainFrame.getFrame().setEnabled(!show);
        addDadFrame.getDialog().setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
    }

    private void pickDADPoint(boolean isFrom) {
        addDadFrame.getDialog().setVisible(false);
        addPointFrame.setOnCancel(() -> {});
        var picker = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                addPointFrame.getFrame().setVisible(false);
                addPointFrame.getFrame().removeMouseListener(this);
                if (isFrom) addDadFrame.setFrom(e.getX(), e.getY());
                else        addDadFrame.setTo(e.getX(), e.getY());
                addDadFrame.getDialog().setVisible(true);
                addDadFrame.getDialog().toFront();
            }
        };
        addPointFrame.getFrame().addMouseListener(picker);
        addPointFrame.getFrame().setVisible(true);
    }

    private void addDADEvent() {
        if (!addDadFrame.isFromSet() || !addDadFrame.isToSet()) {
            changeFrame(false, "Please pick both the start and end points first."); return;
        }
        int nextTime = parseInt(addDadFrame.getNextTimeTxt().getText(), 500);
        mainFrame.getTableItems().addItem(new ItemDAD(
            addDadFrame.getFromX(), addDadFrame.getFromY(),
            addDadFrame.getToX(),   addDadFrame.getToY(), 0, nextTime));
        showAddDADFrame(false);
    }

    private void showAddCommentFrame(boolean show) {
        mainFrame.getFrame().setEnabled(!show);
        if (show) addCommentFrame.getCommentTxt().setText("");
        addCommentFrame.getDialog().setVisible(show);
        if (!show) { mainFrame.getFrame().setEnabled(true); mainFrame.getFrame().toFront(); }
        if (show) addCommentFrame.getCommentTxt().requestFocus();
    }

    // ── Add-item actions ──────────────────────────────────────────────────────
    private void addPressKey() {
        String input = addPressFrame.getKeyTxt().getText();
        if (input.isEmpty()) { changeFrame(false, "Please enter a key to press."); return; }
        int keyCode   = KeyEvent.getExtendedKeyCodeForChar(input.charAt(0));
        int pressTime = parseInt(addPressFrame.getPressTimeTxt().getText(), 100);
        int nextTime  = parseInt(addPressFrame.getNextTimeTxt().getText(), 500);
        mainFrame.getTableItems().addItem(
            new ItemPress(input, keyCode, pressTime, nextTime, addPressFrame.getIsSinglePress()));
        addPressFrame.getKeyTxt().setText("");
        showAddPressFrame(false);
    }

    private void addScrollEvent() {
        String input = addScrollFrame.getWheelAmtTxt().getText();
        if (input.isEmpty()) { changeFrame(false, "Enter wheel amount"); return; }
        int nextTime = parseInt(addScrollFrame.getNextTimeTxt().getText(), 500);
        mainFrame.getTableItems().addItem(
            new Item(input, parseInt(input, 1), nextTime, GlobalConst.EVENT_SCROLL));
        addScrollFrame.getWheelAmtTxt().setText("");
        showAddScrollFrame(false);
    }

    private void addComment() {
        String text = addCommentFrame.getCommentTxt().getText().trim();
        if (text.isEmpty()) { changeFrame(false, "Enter comment text"); return; }
        if (!text.startsWith(GlobalConst.COMMENT_PREFIX)) text = GlobalConst.COMMENT_PREFIX + " " + text;
        mainFrame.getTableItems().addItem(new ItemComment(text));
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
            LOG.log(Level.WARNING, ex, () ->
                    "Global hotkeys disabled (native hook failed): %s".formatted(ex.getMessage()));
            SwingUtilities.invokeLater(() ->
                mainFrame.showToast("Global F7/F8 disabled — focus the window to use hotkeys"));
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
            @Override public void nativeKeyReleased(NativeKeyEvent ignoredEvent) {
                // Only key-press events are relevant for starting/stopping the clicker.
            }
            @Override public void nativeKeyTyped(NativeKeyEvent ignoredEvent) {
                // Function keys are handled from nativeKeyPressed instead of typed events.
            }
        });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────
    private void changeFrame(boolean isMainFrame) { changeFrame(isMainFrame, ""); }
    private void changeFrame(boolean isMainFrame, String message) {
        mainFrame.getFrame().setEnabled(isMainFrame);
        if (addPressFrame.getDialog().isVisible()) addPressFrame.getDialog().setEnabled(isMainFrame);
        if (isMainFrame) {
            msgFrame.hideMessage();
        } else {
            msgFrame.showMessage(message);
        }
    }

    private void enableInput(boolean enabled) {
        mainFrame.getInfiniteRBtn().setEnabled(enabled);
        mainFrame.getRepeatByCountRBtn().setEnabled(enabled);
        mainFrame.getRepeatByTimeRBtn().setEnabled(enabled);
        mainFrame.getHideMainFrameRBtn().setEnabled(enabled);
        mainFrame.getRepeatTimesTxt().setEnabled(enabled && mainFrame.getRepeatByCountRBtn().isSelected());
        mainFrame.getHoursField().setEnabled(enabled && mainFrame.getRepeatByTimeRBtn().isSelected());
        mainFrame.getMinutesField().setEnabled(enabled && mainFrame.getRepeatByTimeRBtn().isSelected());
        mainFrame.getSecondsField().setEnabled(enabled && mainFrame.getRepeatByTimeRBtn().isSelected());
    }
    private int  parseInt(String s, int fallback)  { try { return Integer.parseInt(s.trim()); } catch (NumberFormatException _) { return fallback; } }
    private long parseLong(String s, long fallback) { try { return Long.parseLong(s.trim()); }  catch (NumberFormatException _) { return fallback; } }


    // ── Save (JSON) ───────────────────────────────────────────────────────────
    private void saveData() {
        var items = mainFrame.getTableItems().getListItems();
        if (items.isEmpty()) { changeFrame(false, "List is empty"); return; }

        var fc = new JFileChooser();
        fc.setDialogTitle("Save As");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", JSON_FILTER_EXTENSION));
        fc.setSelectedFile(new File("data_" + System.currentTimeMillis() + JSON_EXTENSION));

        if (fc.showSaveDialog(mainFrame.getFrame()) != JFileChooser.APPROVE_OPTION) return;

        var chosen = fc.getSelectedFile();
        // Make sure the file ends with .json (the filter doesn't enforce it on
        // every L&F — Aqua in particular lets the user type any name).
        if (!chosen.getName().toLowerCase().endsWith(JSON_EXTENSION)) {
            chosen = new File(chosen.getParentFile(), chosen.getName() + JSON_EXTENSION);
        }

        // Confirm overwrite. JFileChooser doesn't ask by default on Linux/macOS.
        if (chosen.exists()) {
            int ans = JOptionPane.showConfirmDialog(mainFrame.getFrame(),
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
            changeFrame(false, "Saved: %s".formatted(path));
            LOG.info(() -> "Saved to: %s".formatted(path));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Save error", e);
            changeFrame(false, "Save failed: %s".formatted(e.getMessage()));
        }
    }

    private JsonObject itemToJson(Item item) {
        var o = new JsonObject();
        o.addProperty(JSON_FIELD_TYPE, item.getTypeEvent());
        o.addProperty(JSON_FIELD_NAME, item.getName());
        o.addProperty(JSON_FIELD_PRESS_TIME, item.getPressTime());
        o.addProperty(JSON_FIELD_NEXT_TIME, item.getNextTime());
        switch (item) {
            case ItemClick ic -> {
                o.addProperty(JSON_FIELD_X, ic.getXAxis());
                o.addProperty(JSON_FIELD_Y, ic.getYAxis());
                o.addProperty(JSON_FIELD_MOUSE_BUTTON, ic.getButton());
            }
            case ItemPress ip -> {
                o.addProperty(JSON_FIELD_KEY_CODE, ip.getKeyCode());
                o.addProperty(JSON_FIELD_MULTI_PRESS, ip.isMultiPress());
            }
            case ItemDAD id -> {
                o.addProperty(JSON_FIELD_X1, id.getXAxis1());
                o.addProperty(JSON_FIELD_Y1, id.getYAxis1());
                o.addProperty(JSON_FIELD_X2, id.getXAxis2());
                o.addProperty(JSON_FIELD_Y2, id.getYAxis2());
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
        fc.setFileFilter(new FileNameExtensionFilter(
                "Auto-clicker files (*.json, *.txt)",
                JSON_FILTER_EXTENSION,
                "txt"
        ));
        if (fc.showOpenDialog(mainFrame.getFrame()) != JFileChooser.APPROVE_OPTION) return;

        var file = fc.getSelectedFile();
        if (file == null || !file.exists()) {
            changeFrame(false, "File not found: %s".formatted(file == null ? "(none)" : file.getName()));
            return;
        }
        var loaded = parseFile(file);
        if (loaded.isEmpty()) return;

        if (append) {
            mainFrame.getTableItems().appendItems(new Vector<>(loaded), file.getName());
        } else {
            mainFrame.getTableItems().setListItems(new Vector<>(loaded));
        }
        String action = append ? "Appended" : "Loaded";
        changeFrame(false, "%s: %s".formatted(action, file.getName()));
        LOG.info(() -> "%s: %s".formatted(action, file.getAbsolutePath()));
    }

    /**
     * Parses both new JSON format (.json) and legacy pipe-delimited format (.txt).
     * Returns an empty list on error (error message already shown).
     */
    private List<Item> parseFile(File file) {
        var fname = file.getName().toLowerCase();
        try {
            return fname.endsWith(JSON_EXTENSION) ? parseJson(file) : parseLegacyTxt(file);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Load error", e);
            changeFrame(false, "Load failed: %s".formatted(e.getMessage()));
            return List.of();
        }
    }

    private List<Item> parseJson(File file) throws IOException {
        var items = new ArrayList<Item>();
        try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            var arr = JsonParser.parseReader(r).getAsJsonArray();
            for (var el : arr) {
                items.add(jsonToItem(el.getAsJsonObject()));
            }
        }
        return items;
    }

    private Item jsonToItem(JsonObject o) {
        String type = resolveType(o);
        String name = o.get(JSON_FIELD_NAME).getAsString();
        int pressTime = getInt(o, JSON_FIELD_PRESS_TIME, DEFAULT_PRESS_TIME);
        int nextTime = getInt(o, JSON_FIELD_NEXT_TIME, DEFAULT_NEXT_TIME);

        return switch (type) {
            case GlobalConst.EVENT_CLICK -> jsonToClick(o, name, pressTime, nextTime);
            case GlobalConst.EVENT_PRESS -> jsonToPress(o, name, pressTime, nextTime);
            case GlobalConst.EVENT_DAD -> jsonToDad(o, pressTime, nextTime);
            case GlobalConst.EVENT_COMMENT -> new ItemComment(name);
            default -> new Item(name, pressTime, nextTime, type);
        };
    }

    private ItemClick jsonToClick(JsonObject o, String name, int pressTime, int nextTime) {
        String btn = o.has(JSON_FIELD_MOUSE_BUTTON)
                ? o.get(JSON_FIELD_MOUSE_BUTTON).getAsString()
                : "Left";
        return new ItemClick(
                name,
                o.get(JSON_FIELD_X).getAsInt(),
                o.get(JSON_FIELD_Y).getAsInt(),
                pressTime,
                nextTime,
                btn
        );
    }

    private ItemPress jsonToPress(JsonObject o, String name, int pressTime, int nextTime) {
        boolean multiPress = o.has(JSON_FIELD_MULTI_PRESS)
                && o.get(JSON_FIELD_MULTI_PRESS).getAsBoolean();
        return new ItemPress(
                name,
                o.get(JSON_FIELD_KEY_CODE).getAsInt(),
                pressTime,
                nextTime,
                multiPress
        );
    }

    private ItemDAD jsonToDad(JsonObject o, int pressTime, int nextTime) {
        return new ItemDAD(
                o.get(JSON_FIELD_X1).getAsInt(),
                o.get(JSON_FIELD_Y1).getAsInt(),
                o.get(JSON_FIELD_X2).getAsInt(),
                o.get(JSON_FIELD_Y2).getAsInt(),
                pressTime,
                nextTime
        );
    }

    private static int getInt(JsonObject o, String fieldName, int fallback) {
        return o.has(fieldName) ? o.get(fieldName).getAsInt() : fallback;
    }

    /** Backward-compatible reader for old pipe-delimited .txt files. */
    private List<Item> parseLegacyTxt(File file) throws IOException {
        var items = new ArrayList<Item>();
        try (var r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                parseLegacyLine(line).ifPresent(items::add);
            }
        }
        return items;
    }

    private Optional<Item> parseLegacyLine(String rawLine) {
        String line = rawLine.trim();
        if (line.isEmpty()) {
            return Optional.empty();
        }
        if (line.startsWith(GlobalConst.COMMENT_PREFIX)) {
            return Optional.of(new ItemComment(line));
        }

        var parts = line.split("\\|");
        if (parts.length < 2) {
            return Optional.empty();
        }

        String type = legacyTypeToString(parts[0].trim());
        if (type == null) {
            return Optional.empty();
        }
        return legacyPartsToItem(parts, type);
    }

    private Optional<Item> legacyPartsToItem(String[] parts, String type) {
        String name = parts[1];
        int pressTime = parts.length > 2 ? toInt(parts[2]) : DEFAULT_PRESS_TIME;
        int nextTime = parts.length > 3 ? toInt(parts[3]) : DEFAULT_NEXT_TIME;

        return switch (type) {
            case GlobalConst.EVENT_CLICK -> legacyClick(parts, name, pressTime, nextTime);
            case GlobalConst.EVENT_PRESS -> legacyPress(parts, name, pressTime, nextTime);
            case GlobalConst.EVENT_DAD -> legacyDad(parts, pressTime, nextTime);
            default -> Optional.of(new Item(name, pressTime, nextTime, type));
        };
    }

    private Optional<Item> legacyClick(String[] parts, String name, int pressTime, int nextTime) {
        if (parts.length < 6) {
            return Optional.empty();
        }
        return Optional.of(new ItemClick(
                name,
                toInt(parts[4]),
                toInt(parts[5]),
                pressTime,
                nextTime
        ));
    }

    private Optional<Item> legacyPress(String[] parts, String name, int pressTime, int nextTime) {
        if (parts.length < 6) {
            return Optional.empty();
        }
        return Optional.of(new ItemPress(
                name,
                toInt(parts[4]),
                pressTime,
                nextTime,
                Boolean.parseBoolean(parts[5])
        ));
    }

    private Optional<Item> legacyDad(String[] parts, int pressTime, int nextTime) {
        if (parts.length < 8) {
            return Optional.empty();
        }
        return Optional.of(new ItemDAD(
                toInt(parts[4]),
                toInt(parts[5]),
                toInt(parts[6]),
                toInt(parts[7]),
                pressTime,
                nextTime
        ));
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
        var el = o.get(JSON_FIELD_TYPE);
        if (el == null) {
            return "";
        }
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
        catch (NumberFormatException _) { return -1; }
    }
}
