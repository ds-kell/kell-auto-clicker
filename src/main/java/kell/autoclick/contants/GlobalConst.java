package kell.autoclick.contants;

public class GlobalConst {
    private GlobalConst() {}
    public static final int EVENT_CLICK   = 1;
    public static final int EVENT_PRESS   = 2;
    public static final int EVENT_SCROLL  = 3;
    public static final int EVENT_DAD     = 4;
    public static final int EVENT_COMMENT = 5;

    // File separator used in save/load
    public static final String FILE_SEP = "|";
    public static final String COMMENT_PREFIX = "#";

    // App config directory (next to the exe / jar)
    public static final String APP_DIR = System.getProperty("user.dir");
    public static final String CONFIG_FILE = APP_DIR + "/auto-clicker-config.properties";
    public static final String LOG_FILE    = APP_DIR + "/auto-clicker.log";
}
