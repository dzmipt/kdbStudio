package studio.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import studio.kdb.Config;
import studio.kdb.Lm;
import studio.kdb.Server;
import studio.kdb.Workspace;
import studio.ui.StudioWindow;
import studio.ui.Util;
import studio.ui.action.WorkspaceSaver;
import studio.utils.*;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

public class Studio {

    private static final Logger log = LogManager.getLogger();

    private static boolean macOSSystemMenu = false;

    public static void initLogger() {
        PrintStream stdoutStream = IoBuilder.forLogger("stdout").setLevel(Level.INFO).buildPrintStream();
        PrintStream stderrStream = IoBuilder.forLogger("stderr").setLevel(Level.ERROR).buildPrintStream();
        System.setOut(stdoutStream);
        System.setErr(stderrStream);
    }

    public static void main(final String[] args) {
        initLogger();
        WindowsAppUserMode.setMainId();

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
        OSXAdapter.setQuitHandler(StudioWindow.class, StudioWindow.class.getDeclaredMethod("quit"));
        OSXAdapter.setAboutHandler(StudioWindow.class, StudioWindow.class.getDeclaredMethod("about"));
        OSXAdapter.setPreferencesHandler(StudioWindow.class, StudioWindow.class.getDeclaredMethod("settings"));
    }

    private static void registerForMacOSMenuJava9() throws Exception {
        Desktop desktop = Desktop.getDesktop();
        desktop.setPreferencesHandler(e -> StudioWindow.settings());
        desktop.setAboutHandler(e -> StudioWindow.about());
        desktop.setQuitHandler( (e,response) -> {
            StudioWindow.quit();
            response.cancelQuit();
        });
    }

    private static void registerForMacOSMenu() {
        if (!Util.MAC_OS_X) return;

        try {
//            if (Util.Java8Minus) registerForMacOSMenuJava8();
//            else registerForMacOSMenuJava9();
            registerForMacOSMenuJava9();
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
        return serverHistory.size() == 0 ? Server.NO_SERVER : serverHistory.get(0);
    }

    private static void initLF() {
        if (Util.MAC_OS_X) {
            InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);

            im = (InputMap) UIManager.get("TextPane.focusInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);

            im = (InputMap) UIManager.get("Table.ancestorInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), "copy");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), "selectAll");

        }
    }

    private static void debugFocusTransfer() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
                e -> {
//                    log.info("Property: {}; old: {};\nnew: {}", e.getPropertyName(), e.getOldValue(), e.getNewValue());
                    if (e.getPropertyName().equals("focusOwner")) {
                        String oldV = e.getOldValue() == null ? "null": e.getOldValue().getClass().toString();
                        String newV = e.getNewValue() == null ? "null": e.getNewValue().getClass().toString();
                        log.info("focusOwner {} -> {}", oldV, newV);
                    }
                }

        );

        try {
            Field mrfoField = KeyboardFocusManager.class.getDeclaredField("mostRecentFocusOwners");
            mrfoField.setAccessible(true);
            final Map delegate = (Map) mrfoField.get(null);
            HashMap map = new HashMap() {
                @Override
                public Object put(Object key, Object value) {
                    if (value instanceof Reference) {
                        log.info("KFM: Value: {}", ((Reference)value).get().getClass());
                    } else if (value == null) {
                        log.info("KFM: Value is null");
                    }
//                    log.info("stacktrace", new Throwable());
                    return delegate.put(key, value);
                }

                @Override
                public Object get(Object key) {
                    return delegate.get(key);
                }
            };
            mrfoField.set(null, map);
        } catch (Exception e) {
            log.error("Exception", e);
        }
    }

    //Executed on the Event Dispatcher Thread
    private static void init(String[] args) {
//        debugFocusTransfer();
        log.info("Start Studio with args {}", Arrays.asList(args));
        log.info("Process pid is {}", ProcessHandle.current().pid());
        initLF();
        registerForMacOSMenu();
        initTaskbarIcon();
        FileWatcher.start();

        if (args.length > 0) {
            new StudioWindow(getInitServer(), args[0]);
        } else {
            Workspace workspace = Config.getInstance().loadWorkspace();
            // Reload files from disk if it was modified somewhere else
            for (Workspace.Window window: workspace.getWindows()) {
                for (Workspace.Tab tab: window.getAllTabs()) {
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
                new StudioWindow(getInitServer(), filename);
            } else {
                StudioWindow.loadWorkspace(workspace);
            }
        }

        WorkspaceSaver.init();

        String hash = Lm.getNotesHash();
        if (! Config.getInstance().getNotesHash().equals(hash) ) {
            StudioWindow.getAllStudioWindows()[0].about();
            Config.getInstance().setNotesHash(hash);
        }
    }
}
