package kell.autoclick.click;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import kell.autoclick.frame.*;
import kell.autoclick.model.*;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.*;

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
        MSG_FRAME.init();
        MAIN_FRAME.getFrame().setVisible(true);
    }

    public void start() {
        eventListener();
        tableListener();
        globalKeyListener();
        windowFocus();
    }

    // ── Logger ────────────────────────────────────────────────────────────────
    private void setupFileLogger() {
        try {
            FileHandler fh = new FileHandler(GlobalConst.LOG_FILE, 5 * 1024 * 1024, 3, true);
            fh.setFormatter(new SimpleFormatter());
            Logger root = Logger.getLogger("");
            root.addHandler(fh);
            root.setLevel(Level.INFO);
            Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
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
            java.util.List<Item> items = MAIN_FRAME.getTableItems().getListItems();
            if (row >= items.size()) return;
            Object data = MAIN_FRAME.getTableItems().getModel().getValueAt(row, col);
            if (data == null) return;
            Item item = items.get(row);
            try {
                switch (col) {
                    case 1: item.setName((String) data); break;          // Action label
                    case 4: item.setPressTime(Integer.parseInt(((String) data).trim())); break; // Hold
                    case 5: item.setNextTime(Integer.parseInt(((String) data).trim())); break;  // Delay
                }
            } catch (NumberFormatException ignored) {}
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
        MAIN_FRAME.getFrame().setVisible(true);
        MAIN_FRAME.getFrame().setEnabled(true);
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
        MAIN_FRAME.getFrame().setVisible(!MAIN_FRAME.isHideFrame());
        MAIN_FRAME.getFrame().setEnabled(false);
        enableInput(false);
        isStopClick = false;
        performClick();
    }


    // ── Main click loop ───────────────────────────────────────────────────────
    private void performClick() {
        Thread clickThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    // Snapshot the list to avoid ConcurrentModification
                    List<Item> items = new ArrayList<>(MAIN_FRAME.getTableItems().getListItems());
                    while (!isStopClick && repeatTimes > 0) {
                        // Time-based stop check
                        if (repeatByTime && System.currentTimeMillis() >= endTimeMs) break;

                        for (int i = 0; i < items.size(); i++) {
                            if (isStopClick) break;
                            if (repeatByTime && System.currentTimeMillis() >= endTimeMs) { isStopClick = true; break; }

                            Item item = items.get(i);
                            if (item.getTypeEvent() == GlobalConst.EVENT_COMMENT) continue;

                            final int rowIdx = i;
                            SwingUtilities.invokeLater(() -> MAIN_FRAME.getTableItems().setHighlightRow(rowIdx));

                            if (item.getTypeEvent() == GlobalConst.EVENT_CLICK) {
                                clickEvent((ItemClick) item);
                            } else if (item.getTypeEvent() == GlobalConst.EVENT_PRESS) {
                                CountDownLatch latch = new CountDownLatch(1);
                                singlePressEvent((ItemPress) item, latch);
                                latch.await();
                            } else if (item.getTypeEvent() == GlobalConst.EVENT_SCROLL) {
                                performScroll(item);
                            } else if (item.getTypeEvent() == GlobalConst.EVENT_DAD) {
                                performDAD((ItemDAD) item);
                            }
                            lock.wait(item.getNextTime() + random.nextInt(10) * 10);
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
                    SwingUtilities.invokeLater(AutoClicker.this::stopClickUI);
                }
            } catch (AWTException | InterruptedException ex) {
                stopClickThreads();
                SwingUtilities.invokeLater(AutoClicker.this::stopClickUI);
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
        Robot robot = new Robot();
        Point cur = MouseInfo.getPointerInfo().getLocation();
        int startX = cur.x, startY = cur.y;
        int targetX = item.getXAxis(), targetY = item.getYAxis();

        smoothMouseMove(robot, startX, startY, targetX, targetY);

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
        Robot robot = new Robot();
        // Move to start position first
        Point cur = MouseInfo.getPointerInfo().getLocation();
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
        javax.swing.Timer timer = new javax.swing.Timer(item.getPressTime(), e -> {
            if (isStopClick) { ((javax.swing.Timer) e.getSource()).stop(); latch.countDown(); return; }
            try {
                Robot robot = new Robot();
                robot.keyPress(item.getKeyCode());
                robot.keyRelease(item.getKeyCode());
            } catch (AWTException ex) {
                LOG.log(Level.SEVERE, "Key press error", ex);
            }
            ((javax.swing.Timer) e.getSource()).stop();
            latch.countDown();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void performScroll(Item item) {
        try { new Robot().mouseWheel(item.getPressTime()); }
        catch (AWTException e) { LOG.log(Level.SEVERE, "Scroll error", e); }
    }


    // ── Show/hide sub-frames ──────────────────────────────────────────────────
    private void showAddPointFrame() {
        ADD_POINT_FRAME.reset();
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                if (ADD_POINT_FRAME.topPanel.getBounds().contains(e.getPoint())) return;
                String btn = ADD_POINT_FRAME.getSelectedButton();
                MAIN_FRAME.getTableItems().addItem(new ItemClick(e.getX(), e.getY(), btn));
                ADD_POINT_FRAME.getFrame().setVisible(false);
                ADD_POINT_FRAME.getFrame().removeMouseListener(this);
            }
        };
        ADD_POINT_FRAME.getFrame().addMouseListener(ma);
        ADD_POINT_FRAME.getFrame().setVisible(true);
    }

    private boolean isDescendantOf(Component child, Component ancestor) {
        Component c = child;
        while (c != null) {
            if (c == ancestor) return true;
            c = c.getParent();
        }
        return false;
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
        MouseAdapter picker = new MouseAdapter() {
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
        Logger.getLogger(GlobalScreen.class.getPackage().getName()).setLevel(Level.OFF);
        try { GlobalScreen.registerNativeHook(); }
        catch (NativeHookException ex) { LOG.severe("Native hook failed: " + ex.getMessage()); System.exit(1); }
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_F7 && isStopClick) {
                    startClick();
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F8 && !isStopClick) {
                    stopClick();
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
        MSG_FRAME.getFrame().setVisible(!isMainFrame);
        MSG_FRAME.getMessageLabel().setText(message);
    }

    /** Non-blocking toast — delegates to MainFrame. */
    private void showToast(String message) {
        MAIN_FRAME.showToast(message);
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
        List<Item> items = MAIN_FRAME.getTableItems().getListItems();
        if (items.isEmpty()) { changeFrame(false, "List is empty"); return; }

        String name = JOptionPane.showInputDialog(MAIN_FRAME.getFrame(),
            "Enter file name (without extension):", "Save As", JOptionPane.PLAIN_MESSAGE);
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) name = "data_" + System.currentTimeMillis();

        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Choose save folder");
        if (fc.showSaveDialog(MAIN_FRAME.getFrame()) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath() + File.separator + name + ".json";
        try (Writer w = new BufferedWriter(new FileWriter(path))) {
            // Serialize as a JsonArray of typed objects
            JsonArray arr = new JsonArray();
            for (Item item : items) {
                arr.add(itemToJson(item));
            }
            GSON.toJson(arr, w);
            changeFrame(false, "Saved: " + path);
            LOG.info("Saved to: " + path);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Save error", e);
            changeFrame(false, "Save failed: " + e.getMessage());
        }
    }

    private JsonObject itemToJson(Item item) {
        JsonObject o = new JsonObject();
        o.addProperty("type", item.getTypeEvent());
        o.addProperty("name", item.getName());
        o.addProperty("pressTime", item.getPressTime());
        o.addProperty("nextTime", item.getNextTime());
        switch (item.getTypeEvent()) {
            case GlobalConst.EVENT_CLICK: {
                ItemClick ic = (ItemClick) item;
                o.addProperty("x", ic.getXAxis());
                o.addProperty("y", ic.getYAxis());
                o.addProperty("mouseButton", ic.getButton());
                break;
            }
            case GlobalConst.EVENT_PRESS: {
                ItemPress ip = (ItemPress) item;
                o.addProperty("keyCode", ip.getKeyCode());
                o.addProperty("multiPress", ip.isMultiPress());
                break;
            }
            case GlobalConst.EVENT_DAD: {
                ItemDAD id = (ItemDAD) item;
                o.addProperty("x1", id.getXAxis1());
                o.addProperty("y1", id.getYAxis1());
                o.addProperty("x2", id.getXAxis2());
                o.addProperty("y2", id.getYAxis2());
                break;
            }
            // SCROLL and COMMENT only need the base fields already added
        }
        return o;
    }

    // ── Load / Append (JSON) ──────────────────────────────────────────────────
    private void loadData(boolean append) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(append ? "Append file" : "Load file");
        if (fc.showOpenDialog(MAIN_FRAME.getFrame()) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        List<Item> loaded = parseFile(file);
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
        String fname = file.getName().toLowerCase();
        try {
            if (fname.endsWith(".json")) {
                return parseJson(file);
            } else {
                return parseLegacyTxt(file);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Load error", e);
            changeFrame(false, "Load failed: " + e.getMessage());
            return null;
        }
    }

    private List<Item> parseJson(File file) throws IOException {
        List<Item> items = new ArrayList<>();
        try (Reader r = new BufferedReader(new FileReader(file))) {
            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                int type      = o.get("type").getAsInt();
                String name   = o.get("name").getAsString();
                int pressTime = o.has("pressTime") ? o.get("pressTime").getAsInt() : 0;
                int nextTime  = o.has("nextTime")  ? o.get("nextTime").getAsInt()  : 100;

                switch (type) {
                    case GlobalConst.EVENT_CLICK: {
                        int x      = o.get("x").getAsInt();
                        int y      = o.get("y").getAsInt();
                        String btn = o.has("mouseButton") ? o.get("mouseButton").getAsString() : "Left";
                        items.add(new ItemClick(name, x, y, pressTime, nextTime, btn));
                        break;
                    }
                    case GlobalConst.EVENT_PRESS: {
                        int kc      = o.get("keyCode").getAsInt();
                        boolean mp  = o.has("multiPress") && o.get("multiPress").getAsBoolean();
                        items.add(new ItemPress(name, kc, pressTime, nextTime, mp));
                        break;
                    }
                    case GlobalConst.EVENT_DAD: {
                        items.add(new ItemDAD(
                            o.get("x1").getAsInt(), o.get("y1").getAsInt(),
                            o.get("x2").getAsInt(), o.get("y2").getAsInt(),
                            pressTime, nextTime));
                        break;
                    }
                    case GlobalConst.EVENT_COMMENT:
                        items.add(new ItemComment(name));
                        break;
                    default:
                        items.add(new Item(name, pressTime, nextTime, type));
                }
            }
        }
        return items;
    }

    /** Backward-compatible reader for old pipe-delimited .txt files. */
    private List<Item> parseLegacyTxt(File file) throws IOException {
        List<Item> items = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith(GlobalConst.COMMENT_PREFIX)) { items.add(new ItemComment(line)); continue; }
                String[] p = line.split("\\|");
                if (p.length < 2) continue;
                int type = toInt(p[0]); if (type < 0) continue;
                String name = p[1];
                int pressTime = p.length > 2 ? toInt(p[2]) : 0;
                int nextTime  = p.length > 3 ? toInt(p[3]) : 100;
                switch (type) {
                    case GlobalConst.EVENT_CLICK:
                        if (p.length >= 6) items.add(new ItemClick(name, toInt(p[4]), toInt(p[5]), pressTime, nextTime));
                        break;
                    case GlobalConst.EVENT_PRESS:
                        if (p.length >= 6) items.add(new ItemPress(name, toInt(p[4]), pressTime, nextTime, Boolean.parseBoolean(p[5])));
                        break;
                    case GlobalConst.EVENT_DAD:
                        if (p.length >= 8) items.add(new ItemDAD(toInt(p[4]), toInt(p[5]), toInt(p[6]), toInt(p[7]), pressTime, nextTime));
                        break;
                    default: items.add(new Item(name, pressTime, nextTime, type));
                }
            }
        }
        return items;
    }

    private int toInt(String s) { try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return -1; } }
}
