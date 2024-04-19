import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.*;
import com.formdev.flatlaf.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public class App extends JFrame {
    private JMenu saveOnClose;
    private ButtonGroup saveOnCloseSubmenu;
    private JMenuItem appsAdd;
    private JMenuItem appsUpdate;
    private JMenuItem appsForceReload;
    private boolean forceReload = false;
    private JMenuItem appsCloseAll;
    
    private JMenu extraStealthMode;
    private ButtonGroup extraStealthModeSubmenu;
    private JMenu extraTheme;
    private ButtonGroup extraThemeSubmenu;
    private JMenuItem extraResetConfig;
    
    private JMenuItem helpAbout;
    private JMenuItem helpHelpDoc;
    
    private JScrollPane content;
    private JPanel contentPanel;
    private JProgressBar saveBar;
    private int FRAME_WIDTH = 500;
    private int FRAME_HEIGHT = 500;
    
    private int ICON_WIDTH = (int)500/4;
    private int ICON_HEIGHT = (int)(ICON_WIDTH * 1.33);
    
    private ArrayList<File> openApps = new ArrayList<File>();
    private ArrayList<Process> openProcesses = new ArrayList<Process>();

    /**
     * Lets the app know when something important's happening
     */
    private boolean busy = false;
    private void busy() {
        busy = true;
        setCursor(Cursor.WAIT_CURSOR);
    }
    private void unbusy() {
        busy = false;
        setCursor(Cursor.DEFAULT_CURSOR);
    }
    /**
     * Get app index from path
     */
    private int getOpenApp(String path) {
        for (int i=0; i < openApps.size(); i++) {
            if (openApps.get(i).getPath().equals(path)) {
                return i;
            }
        }
        return -1;
    }
    /**
     * Running magic
     */
    private void runApp(File app, Config metaConfig, File toLaunch) {
        //Run app in new thread
        new Thread() {
            public void run() {
                Process process;
                try {
                    process = Runtime.getRuntime().exec(toLaunch.toPath().resolve(metaConfig.get("executable")).toFile().getAbsolutePath());
                    unbusy();
                    openApps.add(app);
                    openProcesses.add(process);
                    
                    InputStreamReader errStream = new InputStreamReader(process.getErrorStream());
                    String err = "";
                    int b;
                    while ((b=errStream.read()) != -1) {
                        err += (char)b;
                    }
                    Logger.info(app.getPath() + " returned " + process.exitValue());
                    Logger.info(err);
                    busy();
                    
                    int openAppIndex = getOpenApp(app.getPath());
                    if (openAppIndex > -1) {
                        openApps.remove(openAppIndex);
                        openProcesses.remove(openAppIndex);
                    } else {
                        Logger.error("Couldn't remove, openAppIndex wasn't found!", false);
                    }
                    
                    //Copy everything back
                    if (Main.config.get("saveOnClose").equals("1")) {
                        FileHelper.copyDir(toLaunch.getAbsolutePath(), app.getPath(), true, saveBar);
                    }
                    unbusy();
                } catch (Exception e) {
                    Logger.error("The app hit an error! Try reloading it via Apps -> Force Reload.", e, true);
                    unbusy();
                }
            }
        }.start();
    }
    /**
     * All the launching magic happens here
     */
    private void launchApp(File app, Config metaConfig) {
        if (busy) {
            return;
        }
        //Check if app is already open
        for (File f : openApps) {
            if (f.getPath().equals(app.getPath())) {
                return;
            }
        }
        
        //Copy app files if they aren't already, or if a reload is forced
        Path idFolder = Paths.get("C:\\Windows\\Temp\\0\\");
        File toLaunch = idFolder.resolve(app.getName()).toFile();
        System.out.println("Launching " + toLaunch.getAbsolutePath() + "...");
        
        //SwingWorker to prevent event queue blocking
        busy();
        new SwingWorker() {
            protected Object doInBackground() {
                if (!toLaunch.exists() || forceReload) {
                    FileHelper.copyDir(app.getPath(), toLaunch.getAbsolutePath(), true, saveBar);
                }
                return null;
            }
            protected void done() {
                unbusy();
                runApp(app, metaConfig, toLaunch);
            }
        }.execute();
    }
    /**
     * Copies an app folder to ./apps/
     */
    private void addApp(File app) {
        busy();
        new SwingWorker() {
            protected Object doInBackground() {
                File dir = app;
                File dest = Paths.get("./apps").resolve(dir.getName()).toFile();
                if (dest.exists()) {
                    Logger.error("App already installed!", true);
                } else {
                    if (isValidApp(dir)) {
                        FileHelper.copyDir(dir.getPath(), dest.getAbsolutePath(), true, saveBar);
                        updateApps();
                    } else {
                        Logger.error("Invalid app!", true);
                    }
                }
                Main.config.set("appsAddLastDir", dir.toPath().getParent().toFile().getAbsolutePath());
                unbusy();
                return null;
            }
        }.execute();
    }
    private void removeApp(File app) {
        if (askBox("Warning", "Are you sure you want to permanently delete this app?")) {
            FileHelper.deleteDir(app.getPath(), false);
            updateApps();
        }
    }
    /**
     * Determines if the folder is a valid app
     */
    private boolean isValidApp(File dir) {
        File meta = dir.toPath().resolve(".0meta").toFile();
        if (meta.exists()) {
            return true;
        } else {
            try {
                meta.createNewFile();
                String foundExe = "";
                String foundIcon = "";
                for (File f : dir.listFiles()) {
                    String n = f.getName();
                    if (n.endsWith(".exe")) {
                        foundExe = f.getName();
                    } else if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")) {
                        foundIcon = f.getName();
                    }
                }
                if (foundExe.isEmpty()) {
                    return false;
                }
                Config metaConfig = new Config(meta.getAbsolutePath(), new String[] {
                    "name=" + dir.getName(),
                    "icon=" + foundIcon,
                    "executable=" + foundExe
                }, false);
                metaConfig.save();
                return true;
            } catch (Exception e) {
                Logger.error("Couldn't make new meta file!", e, false);
                return false;
            }
        }
    }
    /**
     * Updates list of all apps
     */
    private void updateApps() {
        //Make sure no apps are running
        if (openApps.size() > 0) {
            return;
        }
        //Cleanup and fetch all apps
        contentPanel.removeAll();
        ArrayList<File> apps = new ArrayList<File>();
        ArrayList<JButton> appButtons = new ArrayList<JButton>();
        revalidate();
        repaint();
        
        File appsDir = new File("./apps");
        File[] rawApps = appsDir.listFiles();
        //Hint if no apps are installed
        boolean showHint = false;
        if (appsDir.exists() && rawApps.length > 0) {
            for (int i=0; i < rawApps.length; i++) {
                final File rawApp = rawApps[i];
                File meta = rawApp.toPath().resolve(".0meta").toFile();
                //First, check if meta file exists
                if (isValidApp(rawApp)) {
                    Config metaConfig = new Config(meta.getAbsolutePath(), new String[] {
                        "name=" + rawApp.getName()
                    }, false);
                    //Check if meta file is valid
                    File exeFile = rawApp.toPath().resolve(metaConfig.get("executable")).toFile();
                    if (metaConfig.loaded && exeFile.exists() && !metaConfig.get("executable").contains(" ")) {
                        apps.add(rawApp);
                        //Make button, grab icon
                        JButton b = new JButton(metaConfig.get("name"));
                        b.setVerticalTextPosition(SwingConstants.BOTTOM);
                        b.setHorizontalTextPosition(SwingConstants.CENTER);
                        b.setToolTipText(rawApp.getName());
                        //Listen for double-click
                        b.addMouseListener(new MouseListener() {
                            public void mouseExited(MouseEvent e) {}
                            public void mouseEntered(MouseEvent e) {}
                            public void mouseReleased(MouseEvent e) {}
                            public void mousePressed(MouseEvent e) {}
                            public void mouseClicked(MouseEvent e) {
                                if (e.getClickCount() > 1) {
                                    launchApp(rawApp, metaConfig);
                                }
                            }
                        });
                        
                        //Popup
                        JPopupMenu popup = new JPopupMenu("Manage " + metaConfig.get("name"));
                        
                        JMenuItem launchItem = new JMenuItem("Launch");
                        launchItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                launchApp(rawApp, metaConfig);
                            }
                        });
                        popup.add(launchItem);
                        
                        JMenuItem removeItem = new JMenuItem("Remove");
                        removeItem.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                removeApp(rawApp);
                            }
                        });
                        popup.add(removeItem);
                        
                        b.addMouseListener(new MouseAdapter() {
                            public void mousePressed(MouseEvent e) {checkPopup(e);}
                            public void mouseReleased(MouseEvent e) {checkPopup(e);}
                            public void mouseClicked(MouseEvent e) {checkPopup(e);}
                            public void checkPopup(MouseEvent e) {
                                if (e.isPopupTrigger()) {
                                    popup.show(e.getComponent(), e.getX(), e.getY());
                                }
                            }
                        });
                        
                        File icon = rawApp.toPath().resolve(metaConfig.get("icon")).toFile();
                        Image bImage;
                        if (icon.exists() && icon.isFile()) {
                            bImage = FileHelper.getFileAsImage(icon.getAbsolutePath());
                        } else {
                            bImage = ResourceHelper.getResourceAsImage("/res/default-app-icon.png");
                        }
                        b.setIcon(new ImageIcon(bImage
                            .getScaledInstance(ICON_WIDTH, ICON_HEIGHT, Image.SCALE_FAST))
                        );
                        b.setSize(ICON_WIDTH, ICON_HEIGHT);
                        appButtons.add(b);
                        contentPanel.add(b);
                    } else {
                        Logger.info("Invalid app meta: " + rawApp.toString());
                    }
                } else {
                    Logger.info("Invalid app: " + rawApp.toString());
                }
            }
            forceReload = true;
        } else {
            showHint = true;
        }
        if (showHint) {
            messageBox("Hint", "There's nothing in here! See Help -> Help Doc to learn how to install apps.");
        }
        revalidate();
        repaint();
    }
    /**
     * Updates submenus
     */
    private void updateSubmenus() {
        if (Main.config.get("saveOnClose").equals("0")) {
            submenuGet(saveOnCloseSubmenu, "0").setSelected(true);
        } else {
            submenuGet(saveOnCloseSubmenu, "1").setSelected(true);
        }
        if (Main.config.get("extraStealthMode").equals("1")) {
            submenuGet(extraStealthModeSubmenu, "1").setSelected(true);
            setTitle("OneNote");
            setIconImage(ResourceHelper.getResourceAsImage("/res/stealth-icon.png"));
        } else {
            submenuGet(extraStealthModeSubmenu, "0").setSelected(true);
            setTitle(Main.TITLE + " " + Main.VERSION_TAG);
            setIconImage(ResourceHelper.getResourceAsImage("/res/icon.png"));
        }
        
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf." + Main.config.get("extraTheme"));
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            Logger.error("Couldn't load theme, resetting to default...", e, false);
            Main.config.reset("extraTheme");
            updateSubmenus();
        }
        submenuGet(extraThemeSubmenu, Main.config.get("extraTheme")).setSelected(true);
    }
    /**
     * Gets the JRadioButtonMenuItem with the given action command
     */
    private JRadioButtonMenuItem submenuGet(ButtonGroup submenu, String text) {
        java.util.Enumeration<AbstractButton> buttons = submenu.getElements();
        while (buttons.hasMoreElements()) {
            JRadioButtonMenuItem e = (JRadioButtonMenuItem)buttons.nextElement();
            if (e.getActionCommand().equals(text)) {
                return e;
            }
        }
        return null;
    }
    /**
     * Populates a JRadioButtonMenuItem submenu. displays sets their user-facing text, options sets their actual value
     */
    private ButtonGroup makeSubmenu(JMenu parent, String[] options, String[] displays, ActionListener listener) {
        ButtonGroup submenu = new ButtonGroup();
        for (int i=0; i < options.length; i++) {
            JRadioButtonMenuItem b = new JRadioButtonMenuItem(displays[i]);
            b.setActionCommand(options[i]);
            b.addActionListener(listener);
            submenu.add(b);
            if (i == 0) {
                b.setSelected(true);
            }
            parent.add(b);
        }
        return submenu;
    }
    /**
     * Binds menu item events
     */
    private void setupMenuEvents() {
        appsAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(Main.config.get("appsAddLastDir"));
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(Main.app) == JFileChooser.APPROVE_OPTION) {
                    addApp(chooser.getSelectedFile());
                }
            }
        });
        appsUpdate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateApps();
            }
        });
        appsCloseAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!busy) {
                    for (Process p : openProcesses) {
                        p.destroyForcibly();
                    }
                    openApps = new ArrayList<File>();
                    openProcesses = new ArrayList<Process>();
                    unbusy();
                }
            }
        });
        appsForceReload.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!busy) {
                    if (forceReload) {
                        Logger.warn("Force Reload is already on!", true);
                    } else if (askBox("Warning", "Are you sure you want to force the next app you launch to reload all of its files?")) {
                        forceReload = true;
                    }
                }
            }
        });
        helpAbout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                messageBox("About " + Main.TITLE,
                    "v" + Main.VERSION + " " + Main.VERSION_TAG + "\n" +
                    Main.CREDIT
                );
            }
        });
        helpHelpDoc.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI(Main.HELP_DOC));
                } catch (Exception x) {
                    Logger.error("Couldn't open help doc!", x, true);
                }
            }
        });
        extraResetConfig.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (askBox("Warning", "Are you sure you want to reset your config?")) {
                    Main.config.resetAll();
                    updateSubmenus();
                }
            }
        });
        //Drag n drop support
        setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> dropped = (java.util.List<File>)e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (dropped.size() > 0) {
                        addApp(dropped.get(0));
                    }
                } catch (Exception x) {
                    Logger.error("Drag n drop failed!", x, false);
                }
            }
        });
    }
    /**
     * Get the appropriate message box icons for a string
     */
    private int stringToIcon(String str) {
        int icon = JOptionPane.PLAIN_MESSAGE;
        if (str.toLowerCase().equals("error")) {
            icon = JOptionPane.ERROR_MESSAGE;
        } else if (str.toLowerCase().equals("info")) {
            icon = JOptionPane.INFORMATION_MESSAGE;
        } else if (str.toLowerCase().equals("warning")) {
            icon = JOptionPane.WARNING_MESSAGE;
        }
        return icon;
    }
    public void messageBox(String title, String msg) {
        JOptionPane.showMessageDialog(this, msg, title, stringToIcon(title));
    }
    public boolean askBox(String title, String msg) {
        int o = JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.OK_CANCEL_OPTION, stringToIcon(title));
        if (o == JOptionPane.OK_OPTION) {
            return true;
        } else {
            return false;
        }
    }
    /**
     * Closes only if not busy
     */
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            if (!busy) {
                System.exit(0);
            }
        } else {
            super.processWindowEvent(e);
        }
    }
    /**
     * Initializes frame
     */
    public App() {
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setTitle(Main.TITLE + " " + Main.VERSION_TAG);
        setIconImage(ResourceHelper.getResourceAsImage("/res/icon.png"));
        setLayout(new BorderLayout());
        
        JMenuBar bar = new JMenuBar();
        add(bar, BorderLayout.PAGE_START);
        
        JMenu appMenu = new JMenu("Apps");
        appMenu.setMnemonic(KeyEvent.VK_A);
        bar.add(appMenu);
            saveOnClose = new JMenu("Save on Close...");
            saveOnClose.setMnemonic(KeyEvent.VK_S);
            saveOnCloseSubmenu = makeSubmenu(saveOnClose, new String[]{"1", "0"}, new String[] {"On", "Off"},
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Main.config.set("saveOnClose", e.getActionCommand());
                        updateSubmenus();
                    }
                });
            appMenu.add(saveOnClose);
            appsAdd = new JMenuItem("Add", KeyEvent.VK_A);
            appMenu.add(appsAdd);
            appsUpdate = new JMenuItem("Update", KeyEvent.VK_U);
            appMenu.add(appsUpdate);
            appsCloseAll = new JMenuItem("Close All", KeyEvent.VK_C);
            appMenu.add(appsCloseAll);
            appsForceReload = new JMenuItem("Force Reload", KeyEvent.VK_F);
            appMenu.add(appsForceReload);
        
        JMenu extraMenu = new JMenu("Extra");
        extraMenu.setMnemonic(KeyEvent.VK_E);
        bar.add(extraMenu);
            extraStealthMode = new JMenu("Stealth Mode...");
            extraStealthMode.setMnemonic(KeyEvent.VK_S);
            extraStealthModeSubmenu = makeSubmenu(extraStealthMode, new String[]{"1", "0"}, new String[] {"On", "Off"},
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Main.config.set("extraStealthMode", e.getActionCommand());
                        updateSubmenus();
                    }
                });
            extraMenu.add(extraStealthMode);
            
            extraTheme = new JMenu("Theme...");
            extraTheme.setMnemonic(KeyEvent.VK_T);
            extraThemeSubmenu = makeSubmenu(extraTheme, Main.THEME_CLASSES, Main.THEMES,
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Main.config.set("extraTheme", e.getActionCommand());
                        updateSubmenus();
                    }
            });
            extraMenu.add(extraTheme);
            
            extraResetConfig = new JMenuItem("Reset Config", KeyEvent.VK_R);
            extraMenu.add(extraResetConfig);
        
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        bar.add(helpMenu);
            helpAbout = new JMenuItem("About", KeyEvent.VK_A);
            helpMenu.add(helpAbout);
            helpHelpDoc = new JMenuItem("Help Doc", KeyEvent.VK_H);
            helpMenu.add(helpHelpDoc);
        
        //Update everything early for responsiveness
        updateSubmenus();
        setVisible(true);
                
        content = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        content.getVerticalScrollBar().setUnitIncrement(10);
        add(content, BorderLayout.CENTER);
        
        contentPanel = new JPanel();
        contentPanel.setLayout(new WrapLayout(WrapLayout.CENTER));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        content.getViewport().add(contentPanel, BorderLayout.CENTER);
        
        saveBar = new JProgressBar(0, 0);
        add(saveBar, BorderLayout.PAGE_END);

        setupMenuEvents();
        updateApps();
    }
}
