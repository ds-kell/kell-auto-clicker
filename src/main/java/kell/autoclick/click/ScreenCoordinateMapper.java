package kell.autoclick.click;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;

import kell.autoclick.model.ItemClick;

/** Captures and restores an F9 mouse position relative to its monitor. */
final class ScreenCoordinateMapper {

    private ScreenCoordinateMapper() {
    }

    /**
     * JNativeHook reports native pixels while AWT may expose DPI-scaled coordinates.
     * Reading the pointer through AWT puts a freshly recorded event in the exact
     * coordinate system expected by Robot, including mixed-DPI monitor setups.
     */
    static Point recordingPoint(int nativeX, int nativeY) {
        try {
            var pointerInfo = MouseInfo.getPointerInfo();
            return pointerInfo != null
                    ? pointerInfo.getLocation()
                    : new Point(nativeX, nativeY);
        } catch (HeadlessException _) {
            return new Point(nativeX, nativeY);
        }
    }

    static void captureScreen(ItemClick item) {
        try {
            GraphicsDevice[] devices = screenDevices();
            int screenIndex = findScreenContaining(devices, item.getXAxis(), item.getYAxis());
            if (screenIndex < 0) {
                return;
            }
            GraphicsDevice device = devices[screenIndex];
            Rectangle bounds = device.getDefaultConfiguration().getBounds();
            item.setRecordedScreen(
                    device.getIDstring(),
                    screenIndex,
                    bounds.x,
                    bounds.y,
                    bounds.width,
                    bounds.height
            );
        } catch (HeadlessException _) {
            // Recording still works with absolute coordinates in a headless environment.
        }
    }

    static Point replayPoint(ItemClick item) {
        if (!item.hasRecordedScreen()) {
            return new Point(item.getXAxis(), item.getYAxis());
        }
        try {
            GraphicsDevice[] devices = screenDevices();
            GraphicsDevice target = findTargetScreen(devices, item);
            if (target == null) {
                return new Point(item.getXAxis(), item.getYAxis());
            }
            return scalePoint(
                    item.getXAxis(),
                    item.getYAxis(),
                    new Rectangle(
                            item.getRecordedScreenX(),
                            item.getRecordedScreenY(),
                            item.getRecordedScreenWidth(),
                            item.getRecordedScreenHeight()
                    ),
                    target.getDefaultConfiguration().getBounds()
            );
        } catch (HeadlessException _) {
            return new Point(item.getXAxis(), item.getYAxis());
        }
    }

    static Point scalePoint(int x, int y, Rectangle source, Rectangle target) {
        double relativeX = (x - (double) source.x) / source.width;
        double relativeY = (y - (double) source.y) / source.height;
        int scaledX = target.x + (int) Math.round(relativeX * target.width);
        int scaledY = target.y + (int) Math.round(relativeY * target.height);
        return new Point(
                Math.clamp(scaledX, target.x, target.x + target.width - 1),
                Math.clamp(scaledY, target.y, target.y + target.height - 1)
        );
    }

    private static GraphicsDevice[] screenDevices() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    }

    private static int findScreenContaining(GraphicsDevice[] devices, int x, int y) {
        for (int i = 0; i < devices.length; i++) {
            if (devices[i].getDefaultConfiguration().getBounds().contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private static GraphicsDevice findTargetScreen(GraphicsDevice[] devices, ItemClick item) {
        if (item.getRecordedScreenId() != null) {
            var idMatch = Arrays.stream(devices)
                    .filter(device -> item.getRecordedScreenId().equals(device.getIDstring()))
                    .findFirst();
            if (idMatch.isPresent()) {
                return idMatch.get();
            }
        }
        int index = item.getRecordedScreenIndex();
        return index >= 0 && index < devices.length ? devices[index] : null;
    }
}
