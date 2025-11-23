package studio.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.utils.Transferables;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.net.URL;

public class Util {
    public final static Icon BLANK_ICON = new BlankIcon(16,16);
    public final static ImageIcon LOGO_ICON = getImage("/logo.png");
    public final static ImageIcon QUESTION_ICON = getImage("/question.png");
    public final static ImageIcon INFORMATION_ICON = getImage("/information.png");
    public final static ImageIcon WARNING_ICON = getImage("/warning.png");
    public final static ImageIcon ERROR_ICON = getImage("/error.png");
    public final static ImageIcon ERROR_SMALL_ICON = getImage("/errorSmall.png");
    public final static ImageIcon CHECK_ICON = getImage("/check.png");

    public final static ImageIcon UNDO_ICON = getImage("/undo.png");
    public final static ImageIcon REDO_ICON =getImage("/redo.png");
    public final static ImageIcon COPY_ICON = getImage("/copy.png");
    public final static ImageIcon CUT_ICON = getImage("/cut.png");
    public final static ImageIcon PASTE_ICON = getImage("/paste.png");
    public final static ImageIcon NEW_DOCUMENT_ICON = getImage("/document_new.png");
    public final static ImageIcon FIND_ICON = getImage("/find.png");
    public final static ImageIcon REPLACE_ICON = getImage("/replace.png");
    public final static ImageIcon FOLDER_ICON = getImage("/folder.png");
    public final static ImageIcon TEXT_TREE_ICON = getImage("/text_tree.png");
    public final static ImageIcon SERVER_INFORMATION_ICON = getImage("/server_information.png");
    public final static ImageIcon ADD_SERVER_ICON = getImage("/server_add.png");
    public final static ImageIcon DELETE_SERVER_ICON = getImage("/server_delete.png");
    public final static ImageIcon DISKS_ICON = getImage("/disks.png");
    public final static ImageIcon SAVE_AS_ICON = getImage("/save_as.png");
    public final static ImageIcon EXPORT_ICON = getImage("/export.png");
    public final static ImageIcon CHART_ICON = getImage("/chartSmall.png");
    public final static ImageIcon STOP_ICON = getImage("/stop.png");
    public final static ImageIcon EXCEL_ICON = getImage("/excel_icon.gif");
    public final static ImageIcon TABLE_SQL_RUN_ICON = getImage("/table_sql_run.png");
    public final static ImageIcon EXECUTE_AND_CHART = getImage("/executeAndChart.png");
    public final static ImageIcon RUN_ICON = getImage("/element_run.png");
    public final static ImageIcon REFRESH_ICON = getImage("/refresh.png");
    public final static ImageIcon ABOUT_ICON = getImage("/about.png");
    public final static ImageIcon TEXT_ICON = getImage("/text.png");
    public final static ImageIcon TABLE_ICON = getImage("/table.png");
    public final static ImageIcon CONSOLE_ICON = getImage("/console.png");
    public final static ImageIcon DATA_COPY_ICON = getImage("/data_copy.png");
    public final static ImageIcon CHART_BIG_ICON = getImage("/chart.png");

    public final static Icon COMMA_ICON = new AlteringIcon("comma.png");
    public final static Icon COMMA_CROSSED_ICON = getImage("/comma_crossed.png");

    public final static ImageIcon UPLOAD_ICON = new AlteringIcon("upload.png");

    public final static Icon ASC_ICON = new AlteringIcon("asc.png");
    public final static Icon DESC_ICON = new AlteringIcon("desc.png");

    public final static Icon SEARCH_WHOLE_WORD_ICON = new AlteringIcon("searchWholeWord.png");
    public final static Icon SEARCH_WHOLE_WORD_SHADED_ICON = new AlteringIcon("searchWholeWord_shaded.png");
    public final static Icon SEARCH_REGEX_ICON = new AlteringIcon("searchRegex.png");
    public final static Icon SEARCH_REGEX_SHADED_ICON = new AlteringIcon("searchRegex_shaded.png");
    public final static Icon SEARCH_CASE_SENSITIVE_ICON = new AlteringIcon("searchCaseSensitive.png");
    public final static Icon SEARCH_CASE_SENSITIVE_SHADED_ICON = new AlteringIcon("searchCaseSensitive_shaded.png");

    public final static Icon LINE_ICON = new AlteringIcon("line.png");
//    public final static ImageIcon PLUS_ICON = getImage("/plus.png");
    public final static Icon PLUS2_ICON = new AlteringIcon("plus2.png");
    public final static Icon LEFT_ICON = new AlteringIcon("left.png");
    public final static Icon RIGHT_ICON = new AlteringIcon("right.png");
    public final static ImageIcon LOCK_ICON = getImage("/lock.png");
    public final static ImageIcon LOCK_CROSSED_ICON = getImage("/lock_crossed.png");
    public final static ImageIcon UNLOCK_ICON = getImage("/unlock.png");

    public final static int menuShortcutKeyMask = GraphicsEnvironment.isHeadless() ? KeyEvent.CTRL_DOWN_MASK :
                                                    java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

    public static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));
    public static boolean WINDOWS = (System.getProperty("os.name").toLowerCase().contains("win"));

    public static boolean Java8Minus = System.getProperty("java.version").startsWith("1.");

    private static boolean mockFitToScreen = false;
    private final static Logger log = LogManager.getLogger();

    public static Color blendColors(Color... colors) {
        float ratio = 1f / ((float) colors.length);
        float r = 0, g = 0, b = 0, a = 0;
        for (Color color : colors) {
            r += color.getRed() * ratio;
            g += color.getGreen() * ratio;
            b += color.getBlue() * ratio;
            a += color.getAlpha() * ratio;
        }
        return new Color((int)r, (int)g, (int)b, (int)a);
    }

    public static ImageIcon getImage(String strFilename) {
        URL url = Util.class.getResource(strFilename);
        if (url == null) throw new RuntimeException("Image " + strFilename + " not found");

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.getImage(url);
        return new ImageIcon(image);
    }

    public static void centerChildOnParent(Component child,Component parent) {
        Rectangle pBounds;
        if (parent == null) {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            pBounds = device.getDefaultConfiguration().getBounds();
        } else {
            pBounds = parent.getBounds();
        }

        Dimension oursize = child.getPreferredSize();

        int x = pBounds.x + (pBounds.width - oursize.width) / 2;
        int y = pBounds.y + (pBounds.height - oursize.height) / 2;

        for (GraphicsDevice device: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle bound = device.getDefaultConfiguration().getBounds();
            if (bound.contains(pBounds.getCenterX(), pBounds.getCenterY())) {
                x = Math.max(bound.x, x);
                y = Math.max(bound.y, y);
            }
        }

        child.setLocation(x,y);
    }

    public static KeyStroke getMenuShortcut(int keyCode, int additionalModifiers) {
        @SuppressWarnings("MagicConstant")
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, menuShortcutKeyMask | additionalModifiers);
        return keyStroke;
    }

    public static KeyStroke getMenuShortcut(int keyCode) {
        return getMenuShortcut(keyCode, 0);
    }


    public static String getAcceleratorString(KeyStroke keyStroke) {
        return InputEvent.getModifiersExText(keyStroke.getModifiers()) + (MAC_OS_X ? "": "+") +
                KeyEvent.getKeyText(keyStroke.getKeyCode());
    }

    public static String getTooltipWithAccelerator(String tooltip, KeyStroke keyStroke) {
        if (keyStroke == null) return tooltip;

        return "<html>" + tooltip + " <small>" + Util.getAcceleratorString(keyStroke) +"</small></html>";
    }

    public static String limitString(String text, int limit) {
        if (text.length() <= limit) return text;
        return text.substring(0, limit)  + " ...";
    }

    public static void copyTextToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text.replace((char)0,' ')), null);
    }

    public static void copyHtmlToClipboard(String html) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new HtmlSelection(html.replace((char)0,' ')), null);
    }

    public static void copyToClipboard(String html, String plainText) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new Transferables(
                        new HtmlSelection(html.replace((char)0,' ')),
                        new StringSelection(plainText.replace((char)0,' '))),null);
    }

    public static double getDividerLocation(JSplitPane splitPane) {
        if (splitPane.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
            return ((double)splitPane.getDividerLocation()) / (splitPane.getHeight() - splitPane.getDividerSize());
        } else {
            return ((double)splitPane.getDividerLocation()) / (splitPane.getWidth() - splitPane.getDividerSize());
        }
    }

    public static Rectangle getDefaultBounds(double scale) {
        if (GraphicsEnvironment.isHeadless()) return new Rectangle(0,0, 1,1);

        DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDisplayMode();

        int width = displayMode.getWidth();
        int height = displayMode.getHeight();
        int w = (int) (width * scale);
        int h = (int) (height * scale);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        return new Rectangle(x, y, w, y);
    }

    public static boolean fitToScreen(Rectangle bounds) {
        if (mockFitToScreen) return true;

        boolean fitToScreen = false;
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice device : devices) {
            fitToScreen |= device.getDefaultConfiguration().getBounds().contains(bounds);
        }
        return fitToScreen;
    }

    public static void sizeToViewPort(JComponent component) {
        new ViewPortHierarchyListener(component);
    }

    private static class ViewPortHierarchyListener implements HierarchyListener {
        final JComponent component;
        Component topComponent;
        ViewPortHierarchyListener(JComponent component) {
            this.component = component;
            this.topComponent = component;
            hierarchyChanged(null);
        }

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            JScrollPane scroll = Util.findParent(topComponent, JScrollPane.class);
            if (scroll == null) {
                topComponent.removeHierarchyListener(this);
                topComponent = findParent(topComponent);
                topComponent.addHierarchyListener(this);
            } else {
                addViewPortListener(scroll, component);
                topComponent.removeHierarchyListener(this);
            }
        }

        private void addViewPortListener(JScrollPane scrollPane, JComponent component) {
            JViewport viewport = scrollPane.getViewport();

            viewport.addChangeListener(e -> {
                Dimension prefSize = component.getPreferredSize();
                prefSize.width = viewport.getWidth() - 22;
                component.setPreferredSize(prefSize);
                component.revalidate();
                component.repaint();
            });
        }
    }


    public static JPanel getLineInViewPort() {
        JPanel pnlLine = new JPanel();
        pnlLine.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.GRAY));
        Util.sizeToViewPort(pnlLine);
        return pnlLine;
    }

    public static Component findParent(Component component) {
        while (component.getParent()!=null) component = component.getParent();
        return component;
    }

    public static <T extends Component> T findParent(Component component, Class<T> clazz) {
        while (component != null) {
            component = component.getParent();
            if (component == null) break;
            if (clazz.isInstance(component)) return clazz.cast(component);
        }
        return null;
    }

    public static void setMockFitToScreen(boolean isMock) {
        log.info("mock fitToScreen: {}", isMock);
        mockFitToScreen = isMock;
    }

    public static void openURL(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        }
        catch (Exception e) {
            log.error("Error during URL {} openning", url);
            StudioOptionPane.showError("Error attempting to launch web browser:\n" + e.getLocalizedMessage(), "Error");
        }
    }


    public static JsonObject deepMerge(JsonObject jBase, JsonObject jAdd) {
        JsonObject json = new JsonObject();
        for (String key: jBase.keySet()) {
            if (jAdd.has(key)) {
                JsonElement elBase = jBase.get(key);
                JsonElement elAdd = jAdd.get(key);
                if (elBase.isJsonObject() && elAdd.isJsonObject()) {
                    json.add(key, deepMerge(elBase.getAsJsonObject(), elAdd.getAsJsonObject()));
                } else {
                    json.add(key, elAdd);
                }
            } else {
                json.add(key, jBase.get(key));
            }
        }

        for (String key: jAdd.keySet()) {
            if (! jBase.has(key)) {
                json.add(key, jAdd.get(key));
            }
        }
        return json;
    }

    public static JsonObject deepExclude(JsonObject jBase, JsonObject jMinus) {
        JsonObject json = new JsonObject();
        for (String key: jBase.keySet()) {
            if (jMinus.has(key)) {
                JsonElement elBase = jBase.get(key);
                JsonElement elMinus = jMinus.get(key);
                if (elBase.isJsonObject() && elMinus.isJsonObject()) {
                    json.add(key, deepExclude(elBase.getAsJsonObject(), elMinus.getAsJsonObject()));
                } else {
                    if (! elBase.equals(elMinus)) {
                        json.add(key, elBase);
                    } // we exclude, if elBase equals to elMinus
                }
            } else {
                json.add(key, jBase.get(key));
            }
        }
        return json;
    }

    public static Color stringToColor(String value) {
        return new Color(Integer.parseInt(value.trim(), 16));
    }

    public static String colorToString(Color color) {
        return Integer.toHexString(color.getRGB()).substring(2);
    }

}
