package kell.autoclick.click;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;
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
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;

import kell.autoclick.contants.GlobalConst;
import kell.autoclick.frame.AddCommentFrame;
import kell.autoclick.frame.AddDADFrame;
import kell.autoclick.frame.AddPointFrame;
import kell.autoclick.frame.AddPressKeyFrame;
import kell.autoclick.frame.AddScrollFrame;
import kell.autoclick.frame.MainFrame;
import kell.autoclick.frame.MessageFrame;
import kell.autoclick.model.Item;

import static kell.autoclick.click.AutoClickerConfig.*;

public class AutoClicker {
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
    private final MacroPlayer macroPlayer;
    private final MacroRecorder macroRecorder;
    private final MacroManualRecorder macroManualRecorder;
    private final MacroFileStore macroFileStore = new MacroFileStore();

    private static final Logger LOG = Logger.getLogger(AutoClicker.class.getName());

    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");
    private static final boolean IS_MAC =
            System.getProperty("os.name", "").toLowerCase().contains("mac");

    /** Tray icon used to hide/restore the window while clicking on Windows. */
    private TrayIcon trayIcon;

    // ── Constructor ───────────────────────────────────────────────────────────
    public AutoClicker() {
        setupFileLogger();
        mainFrame.init();
        Random random = new Random();
        macroPlayer = new MacroPlayer(random, lock, this::shouldContinueClicking, () -> isStopClick);
        macroRecorder = new MacroRecorder(mainFrame, () -> isStopClick, message -> changeFrame(false, message));
        JFrame owner = mainFrame.getFrame();
        // Create dialogs with owner so JDialog parent relationship is correct
        addPointFrame   = new AddPointFrame();
        addPressFrame   = new AddPressKeyFrame(owner);
        addScrollFrame  = new AddScrollFrame(owner);
        addDadFrame     = new AddDADFrame(owner);
        addCommentFrame = new AddCommentFrame(owner);
        macroManualRecorder = new MacroManualRecorder(
                mainFrame,
                addPointFrame,
                addPressFrame,
                addScrollFrame,
                addDadFrame,
                addCommentFrame,
                message -> changeFrame(false, message)
        );
        msgFrame.initWithOwner(owner);
        mainFrame.getFrame().setVisible(true);
    }

    public void start() {
        eventListener();
        tableListener();
        // Always wire the in-window F7/F8/F9 fallback BEFORE attempting the
        // global native hook. If the native hook fails (Windows AV, macOS
        // Accessibility not granted, etc.), users still have the hotkeys
        // when the main window is focused.
        mainFrame.setHotkeyHandlers(this::onF7, this::onF8, this::onF9);
        globalKeyListener();
        windowFocus();
    }

    private void onF7() {
        if (isStopClick) startClick();
    }

    private void onF8() {
        if (!isStopClick) stopClick();
    }

    private void onF9() {
        macroRecorder.toggle();
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

        macroManualRecorder.bindActions();
        mainFrame.getStartBtn().addActionListener(_ -> startClick());
        mainFrame.getStopBtn().addActionListener(_ -> stopClick());
        mainFrame.getRecordBtn().addActionListener(_ -> macroRecorder.start());
        mainFrame.getDeleteBtn().addActionListener(_ -> deleteSelected());
        mainFrame.getResetBtn().addActionListener(_ -> resetData());
        mainFrame.getSaveBtn().addActionListener(_ -> saveData());
        mainFrame.getLoadBtn().addActionListener(_ -> loadData(false));
        mainFrame.getAppendBtn().addActionListener(_ -> loadData(true));
        mainFrame.getMoveUpBtn().addActionListener(_ -> mainFrame.getTableItems().moveSelectedUp());
        mainFrame.getMoveDownBtn().addActionListener(_ -> mainFrame.getTableItems().moveSelectedDown());

        msgFrame.getConfirmBtn().addActionListener(_ -> changeFrame(true));

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
        Integer value = AutoClickerUtils.parseInteger(String.valueOf(data));
        if (value != null) {
            item.setPressTime(value);
        }
    }

    private void updateNextTime(Item item, Object data) {
        Integer value = AutoClickerUtils.parseInteger(String.valueOf(data));
        if (value != null) {
            item.setNextTime(value);
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
        if (macroRecorder.isRecording()) {
            changeFrame(false, "Stop recording before starting playback.");
            return;
        }
        if (mainFrame.getTableItems().getListItems().isEmpty()) {
            changeFrame(false, "No actions added yet. Add at least one action to start."); return;
        }
        if (mainFrame.getInfiniteRBtn().isSelected()) {
            infinite = true; repeatByTime = false;
            totalRepeat.set(-1);
            repeatTimes.set(Long.MAX_VALUE);
        } else if (mainFrame.getRepeatByTimeRBtn().isSelected()) {
            infinite = false; repeatByTime = true;
            long h = AutoClickerUtils.parseLong(mainFrame.getHoursField().getText(), 0);
            long m = AutoClickerUtils.parseLong(mainFrame.getMinutesField().getText(), 0);
            long s = AutoClickerUtils.parseLong(mainFrame.getSecondsField().getText(), 0);
            long totalSec = h * 3600 + m * 60 + s;
            if (totalSec <= 0) { changeFrame(false, "Please enter a valid duration (h/m/s)."); return; }
            endTimeMs = System.currentTimeMillis() + totalSec * 1000;
            totalRepeat.set(totalSec);
            repeatTimes.set(Long.MAX_VALUE);
        } else {
            infinite = false; repeatByTime = false;
            String input = mainFrame.getRepeatTimesTxt().getText().trim();
            if (input.isEmpty()) { changeFrame(false, "Please enter the number of repeat cycles."); return; }
            Long repeatCount = AutoClickerUtils.parseLong(input);
            if (repeatCount == null) {
                changeFrame(false, "Please enter a valid whole number.");
                return;
            }
            repeatTimes.set(repeatCount);
            if (repeatTimes.get() < 1) { changeFrame(false, "Repeat count must be at least 1."); return; }
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
        startPlaybackThread();
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
    private void startPlaybackThread() {
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
        macroPlayer.executeCycle(items, mainFrame);
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
                mainFrame.showToast("Global F7/F8/F9 disabled — focus the window to use hotkeys"));
            mainFrame.getRecordBtn().setEnabled(false);
            return;
        }
        macroRecorder.setNativeHookReady(true);
        GlobalScreen.addNativeMouseListener(new NativeMouseListener() {
            @Override
            public void nativeMousePressed(NativeMouseEvent event) {
                macroRecorder.recordMouseEvent(event, ACTION_DOWN);
            }

            @Override
            public void nativeMouseReleased(NativeMouseEvent event) {
                macroRecorder.recordMouseEvent(event, ACTION_UP);
            }
        });
        GlobalScreen.addNativeMouseWheelListener(new NativeMouseWheelListener() {
            @Override
            public void nativeMouseWheelMoved(NativeMouseWheelEvent event) {
                macroRecorder.recordMouseWheelEvent(event);
            }
        });
        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_F7) {
                    SwingUtilities.invokeLater(AutoClicker.this::onF7);
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F8) {
                    SwingUtilities.invokeLater(AutoClicker.this::onF8);
                } else if (e.getKeyCode() == NativeKeyEvent.VC_F9) {
                    SwingUtilities.invokeLater(AutoClicker.this::onF9);
                } else {
                    macroRecorder.recordKeyPressed(e);
                }
            }
            @Override public void nativeKeyReleased(NativeKeyEvent event) {
                macroRecorder.recordKeyReleased(event);
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
            macroFileStore.save(new ArrayList<>(items), path);
            changeFrame(false, "Saved: %s".formatted(path));
            LOG.info(() -> "Saved to: %s".formatted(path));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Save error", e);
            changeFrame(false, "Save failed: %s".formatted(e.getMessage()));
        }
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
        List<Item> loaded;
        try {
            loaded = macroFileStore.load(file);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Load error", e);
            changeFrame(false, "Load failed: %s".formatted(e.getMessage()));
            return;
        }
        if (loaded.isEmpty()) {
            return;
        }

        if (append) {
            mainFrame.getTableItems().appendItems(new Vector<>(loaded), file.getName());
        } else {
            mainFrame.getTableItems().setListItems(new Vector<>(loaded));
        }
        String action = append ? "Appended" : "Loaded";
        changeFrame(false, "%s: %s".formatted(action, file.getName()));
        LOG.info(() -> "%s: %s".formatted(action, file.getAbsolutePath()));
    }
}
