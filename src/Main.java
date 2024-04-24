public class Main {
    public static String TITLE = "0SEC2";
    public static String VERSION = "1.0.0"; // Change this if you're pushing your changes!
    public static String VERSION_TAG = "";
    public static String HELP_LINK = "https://mochawoof.github.io/0sec2";
    public static String CREDIT = "❤ mochawoof & Contributors"; // Add your name here!!!

    public static String[] configDefs = new String[] {
            "version=" + VERSION,
            "appsDir=./apps",
            "tempDir=C:\\Windows\\Temp\\0",
            "extraStealthMode=0",
            "extraTheme=FlatDarkLaf",
            "saveOnClose=1",
            "appsAddLastDir="
    };

    public static String[] THEME_CLASSES = new String[] {
            "FlatLightLaf",
            "FlatDarkLaf",
            "themes.FlatMacLightLaf",
            "themes.FlatMacDarkLaf",
            "intellijthemes.FlatArcOrangeIJTheme",
            "intellijthemes.FlatArcDarkOrangeIJTheme",
            "intellijthemes.FlatMonokaiProIJTheme",
            "intellijthemes.FlatGradiantoNatureGreenIJTheme",
            "intellijthemes.FlatGradiantoDarkFuchsiaIJTheme"
    };
    public static String[] THEMES = new String[] {
            "Light",
            "Dark",
            "Mac Light",
            "Mac Dark",
            "Wildcat Light",
            "Wildcat Dark",
            "Monokai",
            "Green Gang",
            "Purple Gang"
    };

    // Unique identification number to separate instances
    public static App app;
    public static Config config;

    /**
     * Initializes config, version tag and runs the App
     */
    public static void main(String[] args) {
        if (Main.class.getResource("Main.class").toString().startsWith("file:")) {
            VERSION_TAG += " DEV";
            VERSION_TAG = VERSION_TAG.trim();
        }
        config = new Config(".config", configDefs, true);
        config.comment = "Changing the config directly might break 0SEC2! If that happens, use Extra -> Reset Config.";
        app = new App();
    }
}