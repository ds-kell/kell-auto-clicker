package kell.autoclick.click;

final class AutoClickerConfig {

    private AutoClickerConfig() {
    }

    static final String JSON_EXTENSION = ".json";
    static final String JSON_FILTER_EXTENSION = "json";
    static final String JSON_FIELD_MOUSE_BUTTON = "mouseButton";
    static final String JSON_FIELD_MOUSE_ACTION = "mouseAction";
    static final String JSON_FIELD_KEY_ACTION = "keyAction";
    static final String JSON_FIELD_MULTI_PRESS = "multiPress";
    static final String JSON_FIELD_NEXT_TIME = "nextTime";
    static final String JSON_FIELD_PRESS_TIME = "pressTime";
    static final String JSON_FIELD_TYPE = "type";
    static final String JSON_FIELD_NAME = "name";
    static final String JSON_FIELD_KEY_CODE = "keyCode";
    static final String JSON_FIELD_X = "x";
    static final String JSON_FIELD_Y = "y";
    static final String JSON_FIELD_X1 = "x1";
    static final String JSON_FIELD_Y1 = "y1";
    static final String JSON_FIELD_X2 = "x2";
    static final String JSON_FIELD_Y2 = "y2";
    static final String JSON_FIELD_RECORDED = "recorded";
    static final String ACTION_DOWN = "down";
    static final String ACTION_UP = "up";
    static final String ACTION_MOVE = "move";

    static final int DEFAULT_PRESS_TIME = 0;
    static final int DEFAULT_NEXT_TIME = 100;
    static final int DEFAULT_RECORDED_DELAY_MS = 500;
    static final int RECORDED_MOVE_SAMPLE_INTERVAL_MS = 16;
    static final int RECORDED_MOVE_MIN_DISTANCE_PX = 3;
    static final int RECORDED_MOVE_JITTER_BOUND = 5;
    static final int RECORDED_MOVE_JITTER_OFFSET = 2;

    static final int MOUSE_MOVE_RANDOM_BOUND = 7;
    static final int MOUSE_MOVE_RANDOM_OFFSET = 3;
    static final int CLICK_HOLD_BASE_MS = 30;
    static final int CLICK_HOLD_RANDOM_MS = 40;
    static final int DRAG_HOLD_BASE_MS = 80;
    static final int DRAG_HOLD_RANDOM_MS = 60;
    static final int DRAG_SETTLE_BASE_MS = 40;
    static final int DRAG_SETTLE_RANDOM_MS = 40;
    static final int CLICK_DELAY_RANDOM_BOUND = 10;
    static final long CLICK_DELAY_RANDOM_MULTIPLIER_MS = 10L;

    static final int MIN_STEPS = 25;
    static final int MAX_STEPS = 140;
    static final double PIXELS_PER_STEP_MIN = 3.5;
    static final double PIXELS_PER_STEP_MAX = 7.0;
    static final double SHORT_MOVE_DISTANCE_PX = 80.0;
    static final double MEDIUM_MOVE_DISTANCE_PX = 300.0;
    static final int SHORT_MOVE_MIN_STEPS = 6;
    static final int MEDIUM_MOVE_MIN_STEPS = 14;
    static final double CURVE_RATIO_MIN = 0.05;
    static final double CURVE_RATIO_MAX = 0.10;
    static final double MAX_CURVE_PIXELS = 50.0;
    static final double CONTROL_1_PROGRESS_MIN = 0.20;
    static final double CONTROL_1_PROGRESS_MAX = 0.40;
    static final double CONTROL_2_PROGRESS_MIN = 0.60;
    static final double CONTROL_2_PROGRESS_MAX = 0.82;
    static final double SECOND_CURVE_RATIO_MIN = 0.40;
    static final double SECOND_CURVE_RATIO_MAX = 0.95;
    static final int DELAY_MIN_MS = 2;
    static final int DELAY_MAX_MS = 6;
    static final int RECORDED_RANDOM_PATH_MIN_STEPS = 8;
    static final int RECORDED_RANDOM_PATH_MAX_STEPS = 45;
    static final double RECORDED_RANDOM_PATH_PIXELS_PER_STEP_MIN = 8.0;
    static final double RECORDED_RANDOM_PATH_PIXELS_PER_STEP_MAX = 16.0;
    static final int RECORDED_RANDOM_PATH_DELAY_MIN_MS = 0;
    static final int RECORDED_RANDOM_PATH_DELAY_MAX_MS = 1;
    static final double WOBBLE_AMPLITUDE_MIN = 0.0;
    static final double WOBBLE_AMPLITUDE_MAX = 1.8;
    static final double WOBBLE_FREQUENCY_MIN = 0.7;
    static final double WOBBLE_FREQUENCY_MAX = 1.8;
}
