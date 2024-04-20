public class Main {
    public static String TITLE = "0SEC2";
    public static String VERSION = "0.5.3"; //Change this if you're saving your changes!
    public static String VERSION_TAG = "BETA DEV";
    public static String HELP_LINK = "https://mochawoof.github.io/0sec2";
    public static String CREDIT = "❤ Dog & Contributors"; //Add your name here!!!

    public static String[] configDefs = new String[] {
        "version=" + VERSION,
        "appsDir=./apps",
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
        "intellijthemes.FlatGruvboxDarkHardIJTheme"
    };
    public static String[] THEMES = new String[] {
        "Light",
        "Dark",
        "Mac Light",
        "Mac Dark",
        "Wildcat Light",
        "Wildcat Dark",
        "Gruvbox"
    };
    
    //Unique identification number to separate instances
    public static App app;
    public static Config config;
    
    /**
     * Initializes config, window settings, colors, and runs the App
     */
    public static void main(String[] args) {
        config = new Config(".config", configDefs, true);
        app = new App();
    }
}