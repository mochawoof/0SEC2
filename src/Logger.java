public class Logger {
    public static void error(String msg, Exception e, boolean loud) {
        error(msg + "\n\n" + e.toString(), loud);
    }
    public static void error(String msg, boolean loud) {
        System.err.println(msg);
        if (loud) {
            Main.app.messageBox("Error", msg);
        }
    }
    public static void warn(String msg, boolean loud) {
        info("WARNING: " + msg, false);
        if (loud) {
            Main.app.messageBox("Warning", msg);
        }
    }
    public static void info(String msg, boolean loud) {
        System.out.println(msg);
        if (loud) {
            Main.app.messageBox("Info", msg);
        }
    }
    public static void info(String msg) {
        info(msg, false);
    }
}
