import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileWriter;
public class Config {
    private Properties props = new Properties();
    private String filePath;
    private String[] fileDefs = new String[0];
    public String comment = "";
    public boolean loaded = false;
    private boolean autoSave = false;
    public void save() {
        try {
            FileWriter writer = new FileWriter(filePath);
            props.store(writer, comment);
            writer.close();
        } catch (Exception e) {
            Logger.error("Couldn't save config file!", e, false);
        }
    }
    /**
     * Sets property and saves
     */
    public void set(String prop, Object value) {
        props.setProperty(prop, value.toString());
        if (autoSave) {
            save();
        }
    }
    /**
     * Gets property. If not found, it checks fileDefs for defaults, sets the default property and returns it.
     * If no default is found, returns null
     */
    public String get(String prop) {
        if (props.getProperty(prop) != null) {
            return props.getProperty(prop);
        } else {
            for (String d : fileDefs) {
                if (d.split("=")[0].equals(prop)) {
                    set(prop, d.split("=")[1]);
                    return get(prop);
                }
            }
            return null;
        }
    }
    /**
     * Removes a property, then resets all unset
     */
    public void reset(String prop) {
        props.remove(prop);
        resetUnset();
    }
    /**
     * Resets config to default
     */
    public void resetAll() {
        props = new Properties();
        resetUnset();
    }
    /**
     * Sets all default properties if they don't exist already
     */
    private void resetUnset() {
        for (String d : fileDefs) {
            if (props.getProperty(d.split("=")[0]) == null) {
                String value = "";
                if (d.split("=").length > 1) {
                    value = d.split("=")[1];
                }
                props.setProperty(d.split("=")[0], value);
            }
        }
        if (autoSave) {
            save();
        }
    }
    /**
     * Initializes filePath, fileDefs (defaults) and resets if file isn't found
     */
    public Config(String path, String[] defs, boolean autoSave) {
        filePath = path;
        fileDefs = defs;
        this.autoSave = autoSave;
        try {
            FileInputStream fileStream = new FileInputStream(path);
            props.load(fileStream);
            fileStream.close();
            resetUnset();
            loaded = true;
        } catch (Exception e) {
            Logger.error("Couldn't load config file " + path + "!", e, false);
            if (autoSave) {
                Logger.info("Making config file...");
                resetAll();
            }
        }
    }
    public Config(String path) {
        this(path, new String[0], false);
    }
}
