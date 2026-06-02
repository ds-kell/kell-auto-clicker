package kell.autoclick.contants;

public class GlobalConst {
    private GlobalConst() {}
    public static final String EVENT_CLICK   = "CLICK";
    public static final String EVENT_PRESS   = "PRESS";
    public static final String EVENT_SCROLL  = "SCROLL";
    public static final String EVENT_DAD     = "DAD";
    public static final String EVENT_COMMENT = "COMMENT";

    // File separator used in save/load
    public static final String FILE_SEP = "|";
    public static final String COMMENT_PREFIX = "#";

    // App config directory (next to the exe / jar)
    public static final String APP_DIR = System.getProperty("user.dir");
    public static final String CONFIG_FILE = APP_DIR + "/auto-clicker-config.properties";
    public static final String LOG_FILE    = APP_DIR + "/auto-clicker.log";
}
