package kell.autoclick.click;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import kell.autoclick.contants.GlobalConst;
import kell.autoclick.frame.MainFrame;
import kell.autoclick.model.Item;
import kell.autoclick.model.ItemClick;
import kell.autoclick.model.ItemDAD;
import kell.autoclick.model.ItemPress;

import static kell.autoclick.click.AutoClickerConfig.*;

final class MacroPlayer {

    private static final Logger LOG = Logger.getLogger(MacroPlayer.class.getName());

    private final Random random;
    private final Object lock;
    private final BooleanSupplier shouldContinue;
    private final BooleanSupplier isStopped;

    MacroPlayer(
            Random random,
            Object lock,
            BooleanSupplier shouldContinue,
            BooleanSupplier isStopped
    ) {
        this.random = random;
        this.lock = lock;
        this.shouldContinue = shouldContinue;
        this.isStopped = isStopped;
    }

    void executeCycle(List<Item> items, MainFrame mainFrame) throws AWTException, InterruptedException {
        for (int i = 0; i < items.size() && shouldContinue.getAsBoolean(); i++) {
            Item item = items.get(i);
            if (!GlobalConst.EVENT_COMMENT.equals(item.getTypeEvent())) {
                executeItem(item, i, mainFrame);
                waitBeforeNextItem(item);
            }
        }
    }

    private void executeItem(Item item, int rowIdx, MainFrame mainFrame)
            throws AWTException {
        SwingUtilities.invokeLater(() -> mainFrame.getTableItems().setHighlightRow(rowIdx));
        switch (item) {
            case ItemClick click -> clickEvent(click);
            case ItemPress press -> keyEvent(press);
            case ItemDAD dad -> performDAD(dad);
            case Item scroll when GlobalConst.EVENT_SCROLL.equals(scroll.getTypeEvent()) -> performScroll(scroll);
            default -> { /* unsupported item type */ }
        }
    }

    private void waitBeforeNextItem(Item item) throws InterruptedException {
        long waitUntil = System.currentTimeMillis() + item.getNextTime();
        if (!item.isRecorded()) {
            waitUntil += random.nextInt(CLICK_DELAY_RANDOM_BOUND)
                    * CLICK_DELAY_RANDOM_MULTIPLIER_MS;
        }

        synchronized (lock) {
            while (!isStopped.getAsBoolean()) {
                long remaining = waitUntil - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                lock.wait(remaining);
            }
        }
    }

    private void clickEvent(ItemClick item) throws AWTException {
        if (item.isRecorded()) {
            recordedMouseEvent(item);
            return;
        }
        var robot = new Robot();
        var cur = MouseInfo.getPointerInfo().getLocation();

        int clickX = item.getXAxis()
                + random.nextInt(MOUSE_MOVE_RANDOM_BOUND)
                - MOUSE_MOVE_RANDOM_OFFSET;
        int clickY = item.getYAxis()
                + random.nextInt(MOUSE_MOVE_RANDOM_BOUND)
                - MOUSE_MOVE_RANDOM_OFFSET;

        smoothMouseMove(robot, cur.x, cur.y, clickX, clickY);

        int btn = item.getAwtButtonMask();
        robot.mousePress(btn);
        robot.delay(CLICK_HOLD_BASE_MS + random.nextInt(CLICK_HOLD_RANDOM_MS));
        robot.mouseRelease(btn);
    }

    private void recordedMouseEvent(ItemClick item) throws AWTException {
        var robot = new Robot();
        int btn = item.getAwtButtonMask();

        robot.mouseMove(item.getXAxis(), item.getYAxis());
        if (item.isMouseDownEvent()) {
            robot.mousePress(btn);
        } else if (item.isMouseUpEvent()) {
            robot.mouseRelease(btn);
        } else {
            robot.mousePress(btn);
            robot.delay(Math.max(item.getPressTime(), CLICK_HOLD_BASE_MS));
            robot.mouseRelease(btn);
        }
    }

    private void performDAD(ItemDAD item) throws AWTException {
        var robot = new Robot();
        var cur = MouseInfo.getPointerInfo().getLocation();
        smoothMouseMove(robot, cur.x, cur.y, item.getXAxis1(), item.getYAxis1());

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(DRAG_HOLD_BASE_MS + random.nextInt(DRAG_HOLD_RANDOM_MS));
        smoothMouseMove(robot, item.getXAxis1(), item.getYAxis1(),
                item.getXAxis2(), item.getYAxis2());
        robot.delay(DRAG_SETTLE_BASE_MS + random.nextInt(DRAG_SETTLE_RANDOM_MS));
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void keyEvent(ItemPress item) throws AWTException {
        var robot = new Robot();
        if (item.isRecorded() && item.isKeyDownEvent()) {
            robot.keyPress(item.getKeyCode());
            return;
        }
        if (item.isRecorded() && item.isKeyUpEvent()) {
            robot.keyRelease(item.getKeyCode());
            return;
        }

        robot.keyPress(item.getKeyCode());
        robot.delay(Math.max(item.getPressTime(), DEFAULT_PRESS_TIME));
        robot.keyRelease(item.getKeyCode());
    }

    private void performScroll(Item item) {
        try {
            new Robot().mouseWheel(item.getPressTime());
        } catch (AWTException exception) {
            LOG.log(Level.SEVERE, "Scroll error", exception);
        }
    }

    private void smoothMouseMove(Robot robot, int startX, int startY, int endX, int endY) {
        double dx = endX - (double) startX;
        double dy = endY - (double) startY;
        double distance = Math.hypot(dx, dy);
        if (distance < 1) {
            robot.mouseMove(endX, endY);
            return;
        }

        int steps = Math.clamp(
                (int) Math.round(distance / randomBetween(PIXELS_PER_STEP_MIN, PIXELS_PER_STEP_MAX)),
                MIN_STEPS,
                MAX_STEPS
        );

        double perpendicularX = -dy / distance;
        double perpendicularY = dx / distance;
        double curve1 = Math.min(distance * randomBetween(CURVE_RATIO_MIN, CURVE_RATIO_MAX), MAX_CURVE_PIXELS);
        if (random.nextBoolean()) {
            curve1 = -curve1;
        }
        double curve2 = curve1 * randomBetween(SECOND_CURVE_RATIO_MIN, SECOND_CURVE_RATIO_MAX);
        if (random.nextDouble() < 0.15) {
            curve2 = -curve2 * 0.5;
        }

        double control1Progress = randomBetween(CONTROL_1_PROGRESS_MIN, CONTROL_1_PROGRESS_MAX);
        double control2Progress = randomBetween(CONTROL_2_PROGRESS_MIN, CONTROL_2_PROGRESS_MAX);
        double control1X = startX + dx * control1Progress + perpendicularX * curve1;
        double control1Y = startY + dy * control1Progress + perpendicularY * curve1;
        double control2X = startX + dx * control2Progress + perpendicularX * curve2;
        double control2Y = startY + dy * control2Progress + perpendicularY * curve2;

        double wobbleAmplitude = randomBetween(WOBBLE_AMPLITUDE_MIN, WOBBLE_AMPLITUDE_MAX);
        double wobbleFrequency = randomBetween(WOBBLE_FREQUENCY_MIN, WOBBLE_FREQUENCY_MAX);
        double wobblePhase = randomBetween(0, Math.PI * 2);

        int previousX = startX;
        int previousY = startY;
        for (int i = 1; i <= steps; i++) {
            double progress = i / (double) steps;
            double t = easeInOutCubic(progress);
            double oneMinusT = 1.0 - t;
            double x = oneMinusT * oneMinusT * oneMinusT * startX
                    + 3 * oneMinusT * oneMinusT * t * control1X
                    + 3 * oneMinusT * t * t * control2X
                    + t * t * t * endX;
            double y = oneMinusT * oneMinusT * oneMinusT * startY
                    + 3 * oneMinusT * oneMinusT * t * control1Y
                    + 3 * oneMinusT * t * t * control2Y
                    + t * t * t * endY;

            double envelope = Math.sin(progress * Math.PI);
            double wobble = Math.sin(progress * Math.PI * 2 * wobbleFrequency + wobblePhase)
                    * wobbleAmplitude
                    * envelope;
            x += perpendicularX * wobble;
            y += perpendicularY * wobble;

            int currentX = (int) Math.round(x);
            int currentY = (int) Math.round(y);
            if (currentX != previousX || currentY != previousY) {
                robot.mouseMove(currentX, currentY);
                previousX = currentX;
                previousY = currentY;
            }
            robot.delay(randomBetweenInt(DELAY_MIN_MS, DELAY_MAX_MS));
        }

        robot.mouseMove(endX, endY);
    }

    private double easeInOutCubic(double t) {
        if (t < 0.5) {
            return 4 * t * t * t;
        }
        return 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private double randomBetween(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private int randomBetweenInt(int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }
}
