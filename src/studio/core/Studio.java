package studio.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import studio.kdb.Config;
import studio.kdb.Lm;
import studio.kdb.Server;
import studio.kdb.Workspace;
import studio.ui.StudioPanel;
import studio.ui.Util;
import studio.ui.action.WorkspaceSaver;
import studio.utils.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Studio {

    private static final Logger log = LogManager.getLogger();

    private static boolean macOSSystemMenu = false;

    private static void initLogger() {
        String env = System.getProperty("env");
        if (env != null) {
            log.info("Set environment to {}", env);
            System.setProperty("log4j.studio.envSuffix", "/" + env);
            ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).reconfigure();
            Config.setEnvironment(env);
        }

        PrintStream stdoutStream = IoBuilder.forLogger("stdout").setLevel(Level.INFO).buildPrintStream();
        PrintStream stderrStream = IoBuilder.forLogger("stderr").setLevel(Level.ERROR).buildPrintStream();
        System.setOut(stdoutStream);
        System.setErr(stderrStream);
    }

    public static void main(final String[] args) {
        initLogger();
        WindowsAppUserMode.setMainId();

        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        if(System.getProperty("os.name","").contains("OS X")){ 
            System.setProperty("apple.laf.useScreenMenuBar","true");
            //     System.setProperty("apple.awt.brushMetalLook", "true");
            System.setProperty("apple.awt.showGrowBox","true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name","Studio for kdb+");
            System.setProperty("com.apple.mrj.application.live-resize","true");
            System.setProperty("com.apple.macos.smallTabs","true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
        }

        if(Config.getInstance().getLookAndFeel()!=null){
            try {
                UIManager.setLookAndFeel(Config.getInstance().getLookAndFeel());
            } catch (Exception e) {
                // go on with default one
                log.warn("Can't set LookAndFeel from Config {}", Config.getInstance().getLookAndFeel(), e);
            }
        }

        studio.ui.I18n.setLocale(Locale.getDefault());

        UIManager.put("Table.font",new javax.swing.plaf.FontUIResource("Monospaced",Font.PLAIN,UIManager.getFont("Table.font").getSize()));
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");

        Locale.setDefault(Locale.US);

        SwingUtilities.invokeLater( ()-> init(args) );
    }

    public static boolean hasMacOSSystemMenu() {
        return macOSSystemMenu;
    }

    private static void registerForMacOSMenuJava8() throws Exception {
        // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
        // use as delegates for various com.apple.eawt.ApplicationListener methods
        OSXAdapter.setQuitHandler(StudioPanel.class, StudioPanel.class.getDeclaredMethod("quit"));
        OSXAdapter.setAboutHandler(StudioPanel.class, StudioPanel.class.getDeclaredMethod("about"));
        OSXAdapter.setPreferencesHandler(StudioPanel.class, StudioPanel.class.getDeclaredMethod("settings"));
    }

    private static void registerForMacOSMenuJava9() throws Exception {
        // Using reflection to be compilable on Java8

        Class settingsClass = Class.forName("java.awt.desktop.PreferencesHandler");
        Class quitClass = Class.forName("java.awt.desktop.QuitHandler");
        Class aboutClass = Class.forName("java.awt.desktop.AboutHandler");

        Object handler = Proxy.newProxyInstance(Studio.class.getClassLoader(), new Class[]{settingsClass, quitClass, aboutClass},
                (o, method, objects) -> {
                    String name = method.getName();
                    if (name.equals("handlePreferences")) StudioPanel.settings();
                    else if (name.equals("handleAbout")) StudioPanel.about();
                    else if (name.equals("handleQuitRequestWith")) {
                        StudioPanel.quit();
                        try {
                            Class quiteResponseClass = Class.forName("java.awt.desktop.QuitResponse");
                            quiteResponseClass.getDeclaredMethod("cancelQuit").invoke(objects[1]);
                        } catch (Exception e) {
                            log.error("Error in cancelQuit()", e);
                        }
                    }
                    return null;
                }
        );

        Class desktopClass = Class.forName("java.awt.Desktop");
        Object desktop = desktopClass.getMethod("getDesktop").invoke(desktopClass);

        desktopClass.getMethod("setPreferencesHandler", settingsClass).invoke(desktop, handler);
        desktopClass.getMethod("setQuitHandler", quitClass).invoke(desktop, handler);
        desktopClass.getMethod("setAboutHandler", aboutClass).invoke(desktop, handler);
    }

    private static void registerForMacOSMenu() {
        if (!Util.MAC_OS_X) return;

        try {
            if (Util.Java8Minus) registerForMacOSMenuJava8();
            else registerForMacOSMenuJava9();

            macOSSystemMenu = true;
        } catch (Exception e) {
            log.error("Failed to set MacOS handlers", e);
        }
    }

    private static void initTaskbarIcon() {
        if (Util.Java8Minus) return; // we are running Java 8

        try {
            // We are using reflection to keep supporting Java 8. The code is equivalent to
            // Taskbar.getTaskbar().setIconImage(Util.LOGO_ICON.getImage());

            Class taskbarClass = Class.forName("java.awt.Taskbar");
            Object taskbar = taskbarClass.getDeclaredMethod("getTaskbar").invoke(taskbarClass);
            taskbarClass.getDeclaredMethod("setIconImage", Image.class).invoke(taskbar, Util.LOGO_ICON.getImage());
        } catch (Exception e) {
            log.error("Failed to set Taskbar icon", e);
        }
    }


    private static Server getInitServer() {
        List<Server> serverHistory = Config.getInstance().getServerHistory();
        return serverHistory.size() == 0 ? null : serverHistory.get(0);
    }

    //Executed on the Event Dispatcher Thread
    private static void init(String[] args) {
        log.info("Start Studio with args {}", Arrays.asList(args));
        registerForMacOSMenu();
        initTaskbarIcon();
        FileWatcher.start();

        if (args.length > 0) {
            new StudioPanel().addTab(getInitServer(), args[0]);
        } else {
            Workspace workspace = Config.getInstance().loadWorkspace();
            // Reload files from disk if it was modified somewhere else
            for (Workspace.Window window: workspace.getWindows()) {
                for (Workspace.Tab tab: window.getTabs()) {
                    if (tab.getFilename() != null && !tab.isModified()) {
                        try {
                            Content content = FileReaderWriter.read(tab.getFilename());
                            tab.addContent(content);
                        } catch(IOException e) {
                            log.error("Can't load file " + tab.getFilename() + " from disk", e);
                            tab.setModified(true);
                        }
                    }
                }
            }


            if (workspace.getWindows().length == 0) {
                String[] mruFiles = Config.getInstance().getMRUFiles();
                String filename = mruFiles.length == 0 ? null : mruFiles[0];
                new StudioPanel().addTab(getInitServer(), filename);
            } else {
                StudioPanel.loadWorkspace(workspace);
            }
        }

        WorkspaceSaver.init();

        String hash = Lm.getNotesHash();
        if (! Config.getInstance().getNotesHash().equals(hash) ) {
            StudioPanel.getPanels()[0].about();
            Config.getInstance().setNotesHash(hash);
        }
    }
}
