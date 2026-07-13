package studio.core;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.io.IoBuilder;
import studio.kdb.Config;
import studio.kdb.Lm;
import studio.kdb.Server;
import studio.kdb.Workspace;
import studio.ui.StudioIcon;
import studio.ui.StudioWindow;
import studio.ui.Util;
import studio.ui.WindowFactory;
import studio.ui.action.WorkspaceSaver;
import studio.ui.iminspector.Inspector;
import studio.utils.Content;
import studio.utils.FileReaderWriter;
import studio.utils.FileWatcher;
import studio.utils.WindowsAppUserMode;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Studio {

    private static final Logger log = LogManager.getLogger();

    private static boolean macOSSystemMenu = false;

    public static void initLogger() {
        PrintStream stdoutStream = IoBuilder.forLogger("stdout").setLevel(Level.INFO).buildPrintStream();
        PrintStream stderrStream = IoBuilder.forLogger("stderr").setLevel(Level.ERROR).buildPrintStream();
        System.setOut(stdoutStream);
        System.setErr(stderrStream);

        if (Config.getInstance().getBoolean(Config.LOG_DEBUG) ) {
            log.info("Setting up DEBUG log level");
            Configurator.setRootLevel(Level.DEBUG);
        }
    }

    public static void main(final String[] args) {
        initLogger();
        WindowsAppUserMode.setMainId();

        if(Util.MAC_OS_X){
            System.setProperty("apple.laf.useScreenMenuBar","true");
            //     System.setProperty("apple.awt.brushMetalLook", "true");
            System.setProperty("apple.awt.showGrowBox","true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name","Studio for kdb+");
            System.setProperty("com.apple.mrj.application.live-resize","true");
            System.setProperty("com.apple.macos.smallTabs","true");
            System.setProperty("com.apple.mrj.application.growbox.intrudes","false");
            System.setProperty("flatlaf.useWindowDecorations", "true");
        }

        FlatLightLaf.installLafInfo();
        FlatMacLightLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatMacDarkLaf.installLafInfo();
        System.setProperty("apple.awt.application.appearance", "system");
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");

        Locale.setDefault(Locale.US);

        SwingUtilities.invokeLater( ()-> init(args) );
    }

    public static boolean hasMacOSSystemMenu() {
        return macOSSystemMenu;
    }

    private static void registerForMacOSMenuJava9() {
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

        if (Util.Java8Minus) {
            JOptionPane.showMessageDialog(null,
                    String.format("Unfortunately java %s is not supported", System.getProperty("java.version")),
                    "Unsupported Java Version", JOptionPane.ERROR_MESSAGE );
            System.exit(1);
        }

        registerForMacOSMenuJava9();
        macOSSystemMenu = true;
    }

    private static void initTaskbarIcon() {
        try {
            Taskbar.getTaskbar().setIconImage(Util.LOGO_ICON.getImage());
        } catch (UnsupportedOperationException e) {
            log.debug("Can't set icon", e);
        }
    }


    private static Server getInitServer() {
        List<Server> serverHistory = Config.getInstance().getServerHistory();
        return serverHistory.size() == 0 ? Server.NO_SERVER : serverHistory.get(0);
    }

    private static void installInputMapScanner() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke( "ctrl shift alt Z" );
        Toolkit.getDefaultToolkit().addAWTEventListener( e -> {
            if( e.getID() == KeyEvent.KEY_RELEASED &&
                    ((KeyEvent)e).getKeyCode() == keyStroke.getKeyCode() &&
                    (((KeyEvent)e).getModifiersEx() & KEY_MODIFIERS_MASK) == (keyStroke.getModifiers() & KEY_MODIFIERS_MASK)  ) {
                new Inspector().alignAndShow();
            }
        }, AWTEvent.KEY_EVENT_MASK );
    }

    public static void initLF() {
        installInputMapScanner();
        FlatInspector.install( "ctrl shift alt X" );
        FlatUIDefaultsInspector.install( "ctrl shift alt Y" );

        String lookAndFeelClassName = Config.getInstance().getString(Config.LOOK_AND_FEEL);
        try {
            UIManager.setLookAndFeel(lookAndFeelClassName);
            StudioIcon.updateUI();
        } catch (Exception e) {
            // go on with default one
            log.warn("Can't set LookAndFeel from Config {}", lookAndFeelClassName, e);
        }

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

        UIManager.put("TabbedPane.rotateTabRuns", false);
        UIManager.put("TabbedPane.showTabSeparators", true);

        UIManager.put("TabbedPane.tabInsets", new Insets(1, 6, 1, 6));
    }


    private static final int KEY_MODIFIERS_MASK =
            InputEvent.CTRL_DOWN_MASK |
                    InputEvent.SHIFT_DOWN_MASK |
                    InputEvent.ALT_DOWN_MASK |
                    InputEvent.META_DOWN_MASK;

    private static void addExtraDebugLogging() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke("ctrl shift meta P");
        Toolkit.getDefaultToolkit().addAWTEventListener( e -> {
            if( e.getID() == KeyEvent.KEY_RELEASED &&
                    ((KeyEvent)e).getKeyCode() == keyStroke.getKeyCode() &&
                    (((KeyEvent)e).getModifiersEx() & KEY_MODIFIERS_MASK) == (keyStroke.getModifiers() & KEY_MODIFIERS_MASK)  ) {

                log.info("DDEBUG output:");
                KeyboardFocusManager kfm =
                        KeyboardFocusManager.getCurrentKeyboardFocusManager();

                log.info("DDEBUG activeWindow  = " + kfm.getActiveWindow());
                log.info("DDEBUG focusedWindow = " + kfm.getFocusedWindow());
                log.info("DDEBUG focusOwner    = " + kfm.getFocusOwner());

                for (Window w : Window.getWindows()) {
                    log.info(
                            "\nDDEBUG Window: " + w +
                                    "\nDDEBUG  visible=" + w.isVisible() +
                                    "\nDDEBUG  active=" + w.isActive() +
                                    "\nDDEBUG  focusable=" + w.isFocusableWindow() +
                                    "\nDDEBUG  showing=" + w.isShowing() +
                                    "\nDDEBUG  type=" + w.getType()
                    );
                }

                log.info("DDEBUG end");
            }
        }, AWTEvent.KEY_EVENT_MASK  );

//        Toolkit.getDefaultToolkit().addAWTEventListener(
//                e -> {
//                    if (e instanceof MouseEvent) {
//                        MouseEvent me = (MouseEvent) e;
//                        log.info("DDEBUG mouseEvent: " +
//                                me.getID() + " " +
//                                        me.getComponent());
//                    }
//                },
//                AWTEvent.MOUSE_EVENT_MASK
//        );

    }

    private static String getObjectClass(Object obj) {
        return obj == null ? "null": obj.getClass().toString();
    }

    private static void debugFocusTransfer() {
//        addExtraDebugLogging();

        KeyboardFocusManager custom = new DefaultKeyboardFocusManager() {
            @Override
            public boolean dispatchEvent(AWTEvent e) {
                if (e instanceof FocusEvent) {
                    FocusEvent fe = (FocusEvent) e;
                    String type = fe.getID() == FocusEvent.FOCUS_GAINED ? "FOCUS_GAINED" :
                                        fe.getID() == FocusEvent.FOCUS_LOST ? "FOCUS_LOST" : "unknown";

                    log.info("FocusManger: focusEvent type:{}, temporary:{}, oposite:{}, cause:{}",
                                    type, fe.isTemporary(), getObjectClass(fe.getOppositeComponent()), fe.getCause());
                }
                return super.dispatchEvent(e);
            }
        };
        KeyboardFocusManager.setCurrentKeyboardFocusManager(custom);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(
                e -> {
                    String name = e.getPropertyName();
                    if (name.equals("focusOwner") || name.equals("permanentFocusOwner")) {
                        log.info("{}: focusOwner {} -> {}", name, getObjectClass(e.getOldValue()), getObjectClass(e.getNewValue()));
                    }
                }

        );

//        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
//            if (e.getID() == FocusEvent.FOCUS_GAINED || e.getID() == FocusEvent.FOCUS_LOST) {
//                log.info("AWT focus event {}", e);
//            }
//        }, AWTEvent.FOCUS_EVENT_MASK);

    }

    //Executed on the Event Dispatcher Thread
    private static void init(String[] args) {
        WindowFactory.init();

//        debugFocusTransfer();
        log.info("Start Studio with args {}", Arrays.asList(args));
        log.info("Process pid is {}", ProcessHandle.current().pid());
        initLF();
        registerForMacOSMenu();
        initTaskbarIcon();
        FileWatcher.start();

        if (args.length > 0) {
            WindowFactory.newStudioWindow(getInitServer(), args[0]);
        } else {
            Workspace workspace = Config.getInstance().getWorkspace();
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
                List<String> mruFiles = Config.getInstance().getStringArray(Config.MRU_FILES);
                String filename = mruFiles.isEmpty() ? null : mruFiles.get(0);
                WindowFactory.newStudioWindow(getInitServer(), filename);
            } else {
                StudioWindow.loadWorkspace(workspace);
            }
        }

        WorkspaceSaver.init();

        String hash = Lm.getNotesHash();
        if (! Config.getInstance().getString(Config.NOTES_HASH).equals(hash) ) {
            StudioWindow.about();
            Config.getInstance().setString(Config.NOTES_HASH, hash);
        }
    }
}
