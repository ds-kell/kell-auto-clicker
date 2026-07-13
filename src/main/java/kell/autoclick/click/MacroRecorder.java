package kell.autoclick.click;

import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;

import kell.autoclick.contants.GlobalConst;
import kell.autoclick.frame.MainFrame;
import kell.autoclick.model.Item;
import kell.autoclick.model.ItemClick;
import kell.autoclick.model.ItemPress;

import static kell.autoclick.click.AutoClickerConfig.*;

final class MacroRecorder {

    private record ActiveEvent(int row, long startedAtMs) {
    }

    private final MainFrame mainFrame;
    private final BooleanSupplier playbackStopped;
    private final Consumer<String> showError;
    private final Set<Integer> keysDown = ConcurrentHashMap.newKeySet();
    private final Set<Integer> mouseButtonsDown = ConcurrentHashMap.newKeySet();
    private final Map<Integer, ActiveEvent> activeKeys = new ConcurrentHashMap<>();
    private final Map<Integer, ActiveEvent> activeMouseButtons = new ConcurrentHashMap<>();

    private volatile boolean recording = false;
    private volatile boolean nativeHookReady = false;
    private volatile long lastRecordedEventAtMs = 0;

    MacroRecorder(MainFrame mainFrame, BooleanSupplier playbackStopped, Consumer<String> showError) {
        this.mainFrame = mainFrame;
        this.playbackStopped = playbackStopped;
        this.showError = showError;
    }

    boolean isRecording() {
        return recording;
    }

    void setNativeHookReady(boolean nativeHookReady) {
        this.nativeHookReady = nativeHookReady;
    }

    void toggle() {
        if (recording) {
            stop();
        } else {
            start();
        }
    }

    void start() {
        if (!nativeHookReady) {
            showError.accept("Global hook is unavailable. Grant permissions and restart the app.");
            return;
        }
        if (!playbackStopped.getAsBoolean()) {
            showError.accept("Stop playback before recording.");
            return;
        }
        recording = true;
        lastRecordedEventAtMs = 0;
        keysDown.clear();
        mouseButtonsDown.clear();
        activeKeys.clear();
        activeMouseButtons.clear();
        mainFrame.getRecordBtn().setEnabled(false);
        mainFrame.getStartBtn().setEnabled(false);
        mainFrame.showRecordControl(this::stop);
        mainFrame.setProgressText("Recording macro... press F9 or Stop when done.");
    }

    void stop() {
        recording = false;
        lastRecordedEventAtMs = 0;
        keysDown.clear();
        mouseButtonsDown.clear();
        activeKeys.clear();
        activeMouseButtons.clear();
        mainFrame.getRecordBtn().setEnabled(true);
        mainFrame.getStartBtn().setEnabled(true);
        mainFrame.hideRecordControl();
        mainFrame.setProgressText("Recording stopped. Steps: %d"
                .formatted(mainFrame.getTableItems().getListItems().size()));
    }

    void recordMouseEvent(NativeMouseEvent event, String action) {
        if (!recording || isAppWindowClick(event)) {
            return;
        }
        String button = switch (event.getButton()) {
            case NativeMouseEvent.BUTTON1 -> "Left";
            case NativeMouseEvent.BUTTON3 -> "Right";
            default -> "";
        };
        if (button.isEmpty()) {
            return;
        }
        if (ACTION_DOWN.equals(action) && !mouseButtonsDown.add(event.getButton())) {
            return;
        }
        if (ACTION_UP.equals(action) && !mouseButtonsDown.remove(event.getButton())) {
            return;
        }

        long now = System.currentTimeMillis();
        int x = event.getX();
        int y = event.getY();
        SwingUtilities.invokeLater(() -> addRecordedMouseEvent(event.getButton(), x, y, button, action, now));
    }

    void recordMouseWheelEvent(NativeMouseWheelEvent event) {
        if (!recording || isAppWindowClick(event)) {
            return;
        }
        long now = System.currentTimeMillis();
        int wheelRotation = event.getWheelRotation();
        SwingUtilities.invokeLater(() -> addRecordedScrollEvent(wheelRotation, now));
    }

    void recordKeyPressed(NativeKeyEvent event) {
        if (!recording || AutoClickerUtils.isRecordControlKey(event.getKeyCode())) {
            return;
        }
        int awtKeyCode = AutoClickerUtils.nativeKeyToAwtKeyCode(event.getKeyCode());
        if (awtKeyCode == KeyEvent.VK_UNDEFINED || !keysDown.add(awtKeyCode)) {
            return;
        }
        long now = System.currentTimeMillis();
        String keyName = NativeKeyEvent.getKeyText(event.getKeyCode());
        SwingUtilities.invokeLater(() -> addRecordedKeyEvent(keyName, awtKeyCode, ACTION_DOWN, now));
    }

    void recordKeyReleased(NativeKeyEvent event) {
        if (!recording || AutoClickerUtils.isRecordControlKey(event.getKeyCode())) {
            return;
        }
        int awtKeyCode = AutoClickerUtils.nativeKeyToAwtKeyCode(event.getKeyCode());
        if (awtKeyCode == KeyEvent.VK_UNDEFINED || !keysDown.remove(awtKeyCode)) {
            return;
        }
        long now = System.currentTimeMillis();
        String keyName = NativeKeyEvent.getKeyText(event.getKeyCode());
        SwingUtilities.invokeLater(() -> addRecordedKeyEvent(keyName, awtKeyCode, ACTION_UP, now));
    }

    private void addRecordedMouseEvent(int nativeButton, int x, int y, String button, String action, long eventTimeMs) {
        if (!recording) {
            return;
        }
        updateRecordedDelay(eventTimeMs);
        var recordedClick = new ItemClick(
                "%s %s at %d, %d".formatted(button, action, x, y),
                x,
                y,
                DEFAULT_PRESS_TIME,
                DEFAULT_RECORDED_DELAY_MS,
                button
        );
        recordedClick.setRecorded(true);
        recordedClick.setMouseAction(action);
        mainFrame.getTableItems().addItem(recordedClick);
        int row = mainFrame.getTableItems().getListItems().size() - 1;
        if (ACTION_DOWN.equals(action)) {
            activeMouseButtons.put(nativeButton, new ActiveEvent(row, eventTimeMs));
        } else {
            updateHoldDuration(activeMouseButtons.remove(nativeButton), eventTimeMs);
        }
        lastRecordedEventAtMs = eventTimeMs;
        updateProgress();
    }

    private void addRecordedKeyEvent(String keyName, int keyCode, String action, long eventTimeMs) {
        if (!recording) {
            return;
        }
        updateRecordedDelay(eventTimeMs);
        var recordedKey = new ItemPress(
                "%s %s".formatted(keyName, action),
                keyCode,
                DEFAULT_PRESS_TIME,
                DEFAULT_RECORDED_DELAY_MS,
                false
        );
        recordedKey.setRecorded(true);
        recordedKey.setKeyAction(action);
        mainFrame.getTableItems().addItem(recordedKey);
        int row = mainFrame.getTableItems().getListItems().size() - 1;
        if (ACTION_DOWN.equals(action)) {
            activeKeys.put(keyCode, new ActiveEvent(row, eventTimeMs));
        } else {
            updateHoldDuration(activeKeys.remove(keyCode), eventTimeMs);
        }
        lastRecordedEventAtMs = eventTimeMs;
        updateProgress();
    }

    private void addRecordedScrollEvent(int wheelRotation, long eventTimeMs) {
        if (!recording) {
            return;
        }
        updateRecordedDelay(eventTimeMs);
        var recordedScroll = new Item(
                "Scroll %d".formatted(wheelRotation),
                wheelRotation,
                DEFAULT_RECORDED_DELAY_MS,
                GlobalConst.EVENT_SCROLL
        );
        recordedScroll.setRecorded(true);
        mainFrame.getTableItems().addItem(recordedScroll);
        lastRecordedEventAtMs = eventTimeMs;
        updateProgress();
    }

    private void updateRecordedDelay(long eventTimeMs) {
        if (lastRecordedEventAtMs <= 0) {
            return;
        }
        mainFrame.getTableItems().updateLastDelay(
                AutoClickerUtils.clampToInt(eventTimeMs - lastRecordedEventAtMs)
        );
    }

    private void updateHoldDuration(ActiveEvent activeEvent, long endedAtMs) {
        if (activeEvent == null) {
            return;
        }
        mainFrame.getTableItems().updateHoldTime(
                activeEvent.row(),
                AutoClickerUtils.clampToInt(endedAtMs - activeEvent.startedAtMs())
        );
    }

    private void updateProgress() {
        mainFrame.setProgressText("Recording... steps: %d"
                .formatted(mainFrame.getTableItems().getListItems().size()));
    }

    private boolean isAppWindowClick(NativeMouseEvent event) {
        return mainFrame.isInsideRecordControl(event.getX(), event.getY())
                || AutoClickerUtils.isInsideWindow(mainFrame.getFrame(), event.getX(), event.getY());
    }
}
