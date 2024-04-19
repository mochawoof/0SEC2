import javax.swing.JProgressBar;
import java.io.File;
import java.nio.file.*;
import javax.swing.ImageIcon;
import java.awt.Image;
public class FileHelper {
    /**
     * Copy files, delete files, etc. All return bools indicating whether the operation succeeded
     */
    public static boolean deleteFile(String path, boolean loud) {
        boolean success = new File(path).delete();
        if (!success) {
            Logger.error("Couldn't delete file!", loud);
        }
        return success;
    }
    public static boolean deleteDir(String path, boolean loud) {
        File base = new File(path);
        boolean success = true;
        for (File f : base.listFiles()) {
            if (f.isDirectory()) {
                deleteDir(f.getPath(), false);
            } else {
                if (!deleteFile(f.getPath(), false)) {
                    success = false;
                }
            }
        }
        deleteFile(base.getPath(), false);
        if (!success) {
            Logger.error("Couldn't delete folder!", loud);
        }
        return success;
    }
    public static boolean copyFile(String path, String dest, boolean loud) {
        File destFile = new File(dest);
        if (!destFile.exists() || !destFile.isDirectory()) {
            destFile.mkdirs();
        }
        try {
            Files.copy(Paths.get(path), Paths.get(dest).resolve(Paths.get(path).getFileName()), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            Logger.error("Couldn't copy file!", e, loud);
            return false;
        }
    }
    public static boolean copyDir(String path, String dest, boolean loud, JProgressBar bar) {
        File base = Paths.get(path).toAbsolutePath().toFile();
        File destBase = Paths.get(dest).toAbsolutePath().toFile();
        System.out.println(base + " -> " + destBase);
        boolean success = true;
        
        //Bar only updates if it's there and if loud is on
        if (bar != null && loud) {
            bar.setValue(0);
            bar.setMaximum(base.listFiles().length);
        }
        
        for (File f : base.listFiles()) {
            if (f.isDirectory()) {
                copyDir(f.getPath(), destBase.toPath().resolve(f.getName()).toFile().getPath(), false, bar);
            } else {
                if (!copyFile(f.getPath(), destBase.getPath(), false)) {
                    success = false;
                }
            }
            
            if (bar != null && loud) {
                bar.setValue(bar.getValue() + 1);
            }
        }
        if (!success) {
            Logger.error("Couldn't copy folder!", loud);
        }
        
        if (bar != null && loud) {
            bar.setValue(0);
            bar.setMaximum(0);
        }
        return success;
    }
    public static Image getFileAsImage(String path) {
        return getFileAsImageIcon(path).getImage();
    }
    public static ImageIcon getFileAsImageIcon(String path) {
        return new ImageIcon(path);
    }
}
