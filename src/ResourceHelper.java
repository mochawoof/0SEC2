//mochahelper v1.1
import java.net.URL;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
public class ResourceHelper {
    //resources
    public static Image getResourceAsImage(String path) {
        return getResourceAsImageIcon(path).getImage();
    }
    public static ImageIcon getResourceAsImageIcon(String path) {
        return new ImageIcon(getResource(path));
    }
    public static String getResourceAsString(String path) {
        URL resource = getResource(path);
        String ret = "";
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(resource.openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                ret += line + System.lineSeparator();
            }
            in.close();
        } catch (IOException e) {}
        return ret;
    }
    public static String[] getResourceAsStringArray(String path) {
        return getResourceAsString(path).split(System.lineSeparator());
    }
    public static URL getResource(String path) {
        return Main.class.getResource(path);
    }
}
