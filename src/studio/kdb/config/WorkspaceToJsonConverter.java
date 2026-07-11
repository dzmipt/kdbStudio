package studio.kdb.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.Workspace;
import studio.utils.LineEnding;
import studio.utils.QConnection;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class WorkspaceToJsonConverter {
    private final static String SELECTED_WINDOW = "selectedWindow";
    private final static String DIVIDER_LOCATION = "dividerLocation";
    private final static String RESULT_DIVIDER_LOCATION = "resultDividerLocation";
    private final static String X = "x";
    private final static String Y = "y";
    private final static String WIDTH = "width";
    private final static String HEIGHT = "height";
    private final static String SERVER_LIST = "serverlist.";
    private final static String WINDOW = "window";
    private final static String TAB = "tab";
    private final static String LEFT = "left";
    private final static String RIGHT = "right";
    private final static String VERTICAL_SPLIT = "verticalSplit";
    private final static String SELECTED_TAB = "selectedTab";
    private final static String FILENAME = "filename";
    private final static String SERVER_FULLNAME = "serverFullname";
    private final static String SERVER_CONNECTION = "serverConnection";
    private final static String SERVER_AUTH = "serverAuth";
    private final static String CONTENT = "content";
    private final static String MODIFIED = "modified";
    private final static String LINE_ENDING = "lineEnding";
    private final static String CARET = "caret";

    private Properties properties;
    private final static Logger log = LogManager.getLogger();

    public WorkspaceToJsonConverter(Properties properties) {
        this.properties = properties;
    }

    public Workspace load() {
        Workspace workspace = new Workspace();
        int selectedWindow = getInt(SELECTED_WINDOW, -1);
        for (int index = 0; ; index++) {
            String prefix = WINDOW + "." + index + ".";
            if (!hasKeysWithPrefix(prefix)) break;

            Workspace.TopWindow window = workspace.addWindow(index == selectedWindow);
            loadTopWindow(prefix, window);
        }

        return workspace;
    }

    private void loadTopWindow(String prefix, Workspace.TopWindow window) {
        double resultDividerLocation = getDouble(prefix + RESULT_DIVIDER_LOCATION, 0.5);
        Rectangle location = getBounds(prefix, Workspace.DEFAULT_BOUNDS);
        Rectangle serverListBounds = getBounds(prefix + SERVER_LIST, Workspace.DEFAULT_BOUNDS);

        window.setResultDividerLocation(resultDividerLocation);
        window.setLocation(location);
        window.setServerListBounds(serverListBounds);

        loadWindow(prefix, window);
    }

    private void loadWindow(String prefix, Workspace.Window window) {
        int selectedTab = getInt(prefix + SELECTED_TAB, -1);
        for (int index = 0; ; index++) {
            String tabPrefix = prefix + TAB + "." + index + ".";
            if (!hasKeysWithPrefix(tabPrefix)) break;

            Workspace.Tab tab = window.addTab(selectedTab == index);
            loadTab(tabPrefix, tab);
        }

        String leftPrefix = prefix + LEFT + ".";
        String rightPrefix = prefix + RIGHT + ".";
        if (hasKeysWithPrefix(leftPrefix)) {
            boolean verticalSplit = getBoolean(prefix + VERTICAL_SPLIT, false);
            double dividerLocation = getDouble(prefix + DIVIDER_LOCATION, 0.5);
            Workspace.Window leftWindow = window.addLeft(verticalSplit, dividerLocation);
            loadWindow(leftPrefix, leftWindow);
        }
        if (hasKeysWithPrefix(rightPrefix)) {
            Workspace.Window rightWindow = window.addRight();
            loadWindow(rightPrefix, rightWindow);
        }
    }

    private void loadTab(String prefix, Workspace.Tab tab) {
        String filename = properties.getProperty(prefix + FILENAME);
        String serverFullName = properties.getProperty(prefix + SERVER_FULLNAME);
        String serverConnection = properties.getProperty(prefix + SERVER_CONNECTION);
        String serverAuth = properties.getProperty(prefix + SERVER_AUTH);
        String content = properties.getProperty(prefix + CONTENT);
        boolean modified = getBoolean(prefix + MODIFIED, false);
        LineEnding lineEnding = getLineEnding(prefix + LINE_ENDING, LineEnding.Unix);
        int caret = getInt(prefix + CARET, 0);

        tab.addFilename(filename)
                .addContent(content)
                .setLineEnding(lineEnding)
                .setCaret(caret)
                .setModified(modified);

        if (serverConnection == null) return;
        QConnection conn = QConnection.get(serverConnection);

        if (serverAuth == null) {
            serverAuth = Config.getInstance().getDefaultAuthMechanism();
        }

        String serverName = "";
        ServerTreeNode parent = null;
        if (serverFullName != null) {
            List<String> path = new ArrayList<>();
            path.add("");
            path.addAll(List.of(serverFullName.split("/")));
            serverName = path.get(path.size()-1);
            path.remove(path.size()-1);
            parent = Config.getInstance().getServerTree().findPath(path, true);
        }

        Server server = new Server(serverName, conn, serverAuth, Color.BLACK, parent);

        tab.addServer(server);
    }

    private int getInt(String key, int defValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, "" + defValue));
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private double getDouble(String key, double defValue) {
        try {
            return Double.parseDouble(properties.getProperty(key, "" + defValue));
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private boolean getBoolean(String key, boolean defValue) {
        return Boolean.parseBoolean(properties.getProperty(key, "" + defValue));
    }

    private LineEnding getLineEnding(String key, LineEnding defValue) {
        try {
            return LineEnding.valueOf(properties.getProperty(key, defValue.name()));
        } catch (IllegalArgumentException e) {
            return defValue;
        }

    }

    private Rectangle getBounds(String prefix, Rectangle defValue) {
        int x = getInt(prefix + X, defValue.x);
        int y = getInt(prefix + Y, defValue.y);
        int width = getInt(prefix + WIDTH, defValue.width);
        int height = getInt(prefix + HEIGHT, defValue.height);

        return new Rectangle(x, y, width, height);
    }

    private boolean hasKeysWithPrefix(String prefix) {
        for (Object key : properties.keySet()) {
            if (key.toString().startsWith(prefix)) return true;
        }
        return false;
    }

}