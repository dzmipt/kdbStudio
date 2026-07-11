package studio.ui.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.Workspace;
import studio.kdb.config.ConfigType;
import studio.ui.StudioWindow;
import studio.utils.LineEnding;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkspaceSaver {

    private final static String WINDOWS = "windows";
    private final static String SELECTED = "selected";
    private final static String LOCATION = "location";
    private final static String SERVER_LIST_LOCATION = "serverListLocation";
    private final static String RESULT_DIVIDER_LOCATION = "resultDividerLocation";
    private final static String TABS = "tabs";
    private final static String DIVIDER_LOCATION = "dividerLocation";
    private final static String LEFT = "left";
    private final static String RIGHT = "right";
    private final static String UP = "up";
    private final static String DOWN = "down";
    private final static String FILENAME = "filename";
    private final static String SERVER_FULLNAME = "serverFullname";
    private final static String SERVER_CONNECTION = "serverConnection";
    private final static String SERVER_AUTH = "serverAuth";
    private final static String CONTENT = "content";
    private final static String MODIFIED = "modified";
    private final static String LINE_ENDING = "lineEnding";
    private final static String CARET = "caret";

    private final static int PERIOD_IN_SEC = 10;

    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static boolean enabled = true;
    private static Workspace workspace;

    private final static Logger log = LogManager.getLogger();

    public static void init () {
        scheduler.scheduleAtFixedRate(WorkspaceSaver::timer, PERIOD_IN_SEC, PERIOD_IN_SEC, TimeUnit.SECONDS);
    }

    public synchronized static void setEnabled(boolean value) {
        enabled = value;
    }

    private static void timer() {
        try {
            SwingUtilities.invokeAndWait(() -> setWorkspace(StudioWindow.getWorkspace()));
            save(workspace);
        } catch (Exception e) {
            log.error("Exception during getting workspace", e);
        }
    }

    public static synchronized void save(Workspace workspace) {
        if (!enabled) return;
        Config.getInstance().saveWorkspace(workspace);
    }

    private static synchronized void setWorkspace(Workspace w) {
        if (!enabled) return;
        workspace = w;
    }


    public static Workspace fromJson(JsonObject json) {
        Workspace workspace = new Workspace();

        JsonArray array = json.getAsJsonArray(WINDOWS);
        for (JsonElement jsonElement: array) {
            JsonObject jsonWindow = jsonElement.getAsJsonObject();

            Workspace.TopWindow window = workspace.addWindow(isSelected(jsonWindow));
            fromJsonTopWindow(jsonWindow, window);
        }
        return workspace;
    }

    private static void fromJsonTopWindow(JsonObject json, Workspace.TopWindow window) {
        Rectangle location = (Rectangle) ConfigType.BOUNDS.fromJson(json.get(LOCATION), null);
        Rectangle serverListLocation = (Rectangle) ConfigType.BOUNDS.fromJson(json.get(SERVER_LIST_LOCATION), null);
        double resultDividerLocation = json.get(RESULT_DIVIDER_LOCATION).getAsDouble();

        window.setLocation(location);
        window.setServerListBounds(serverListLocation);
        window.setResultDividerLocation(resultDividerLocation);
        fromJsonWindow(json, window);
    }

    private static void fromJsonWindow(JsonObject json, Workspace.Window window) {
        if (json.has(TABS)) {
            JsonArray jsonArray = json.get(TABS).getAsJsonArray();
            for (JsonElement jsonElement: jsonArray) {
                JsonObject jsonTab = jsonElement.getAsJsonObject();
                Workspace.Tab tab = window.addTab(isSelected(jsonTab));
                fromJsonTab(jsonTab, tab);
            }
        } else {
            double dividerLocation = json.get(DIVIDER_LOCATION).getAsDouble();
            boolean verticalSplit;
            JsonObject jsonLeft;
            JsonObject jsonRight;
            if (json.has(LEFT)) {
                verticalSplit = false;
                jsonLeft = json.getAsJsonObject(LEFT).getAsJsonObject();
                jsonRight = json.getAsJsonObject(RIGHT).getAsJsonObject();
            } else {
                verticalSplit = true;
                jsonLeft = json.getAsJsonObject(UP).getAsJsonObject();
                jsonRight = json.getAsJsonObject(DOWN).getAsJsonObject();
            }
            Workspace.Window leftWindow = window.addLeft(verticalSplit, dividerLocation);
            Workspace.Window rightWindow = window.addRight();
            fromJsonWindow(jsonLeft, leftWindow);
            fromJsonWindow(jsonRight, rightWindow);
        }

    }

    private static void fromJsonTab(JsonObject json, Workspace.Tab tab) {
        if (json.has(CONTENT)) {
            tab.addContent(json.get(CONTENT).getAsString());
        }
        if (json.has(MODIFIED)) {
            tab.setModified(json.get(MODIFIED).getAsBoolean());
        }
        if (json.has(LINE_ENDING)) {
            tab.setLineEnding(LineEnding.valueOf(json.get(LINE_ENDING).getAsString()));
        }
        if (json.has(CARET)) {
            tab.setCaret(json.get(CARET).getAsInt());
        }

        if (json.has(FILENAME)) {
            tab.addFilename(json.get(FILENAME).getAsString());
        }

        if (json.has(SERVER_FULLNAME)) {
            String serverFullName = json.get(SERVER_FULLNAME).getAsString();
            Server server = Config.getInstance().getServerConfig().getServer(serverFullName);
            if (server != Server.NO_SERVER) {
                tab.addServer(server);
                return;
            }
        }

        if (! json.has(SERVER_CONNECTION)) return;
        String serverConnection = json.get(SERVER_CONNECTION).getAsString();

        String auth = Config.getInstance().getDefaultAuthMechanism();
        if (json.has(SERVER_AUTH)) {
            auth = json.get(SERVER_AUTH).getAsString();
        }

        Server server = Config.getInstance().getServerConfig().lookup(serverConnection, auth);
        tab.addServer(server);
    }

    private static boolean isSelected(JsonObject json) {
        JsonElement jsonElement = json.get(SELECTED);
        if (jsonElement == null) return false;
        return jsonElement.getAsBoolean();
    }

    public static JsonObject toJson(Workspace workspace) {
        Workspace.TopWindow[] windows = workspace.getWindows();
        JsonArray jsonArray = new JsonArray(windows.length);
        int selectedWindow = workspace.getSelectedWindow();
        for(int index = 0; index<windows.length; index++) {
            JsonObject jsonWindow = toJsonTopWindow(windows[index]);
            jsonWindow.addProperty(SELECTED, selectedWindow == index);
            jsonArray.add(jsonWindow);
        }

        JsonObject json = new JsonObject();
        json.add(WINDOWS, jsonArray);
        return json;
    }

    private static JsonObject toJsonTopWindow(Workspace.TopWindow window) {
        JsonObject json = toJsonWindow(window);
        json.add(LOCATION, ConfigType.BOUNDS.toJson(window.getLocation()));
        json.add(SERVER_LIST_LOCATION, ConfigType.BOUNDS.toJson(window.getServerListBounds()));
        json.addProperty(RESULT_DIVIDER_LOCATION, window.getResultDividerLocation());
        return json;
    }

    private static JsonObject toJsonWindow(Workspace.Window window) {
        JsonObject json = new JsonObject();
        Workspace.Window left = window.getLeft();
        if (left != null) {
            boolean verticalSplit = window.isVerticalSplit();
            json.add(verticalSplit ? UP : LEFT, toJsonWindow(left));
            json.add(verticalSplit ? DOWN: RIGHT, toJsonWindow(window.getRight()));
            json.addProperty(DIVIDER_LOCATION, window.getDividerLocation());
        } else {
            Workspace.Tab[] tabs = window.getAllTabs();
            int selectedTab = window.getSelectedTab();
            JsonArray jsonArray = new JsonArray(tabs.length);
            for (int index=0; index<tabs.length; index++) {
                JsonObject jsonTab = toJsonTab(tabs[index]);
                jsonTab.addProperty(SELECTED, index == selectedTab);
                jsonArray.add(jsonTab);
            }
            json.add(TABS, jsonArray);
        }

        return json;
    }

    private static JsonObject toJsonTab(Workspace.Tab tab) {
        JsonObject json = new JsonObject();
        String content = tab.getContent();
        if (content != null) {
            json.addProperty(CONTENT, content);
            json.addProperty(MODIFIED, tab.isModified());
            json.addProperty(LINE_ENDING, tab.getLineEnding().name());
            json.addProperty(CARET, tab.getCaret());
        }

        String filename = tab.getFilename();
        if (filename != null) {
            json.addProperty(FILENAME, filename);
        }

        String fullName = tab.getServerFullName();
        if (fullName != null) {
            json.addProperty(SERVER_FULLNAME, fullName);
        }
        json.addProperty(SERVER_CONNECTION, tab.getServerConnection());
        json.addProperty(SERVER_AUTH, tab.getServerAuth());

        return json;
    }

}
