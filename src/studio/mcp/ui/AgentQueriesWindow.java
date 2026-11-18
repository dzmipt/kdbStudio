package studio.mcp.ui;

import studio.kdb.Config;
import studio.kdb.Server;
import studio.ui.EditorTab;
import studio.ui.EditorsPanel;
import studio.ui.StudioWindow;
import studio.ui.WindowFactory;
import studio.ui.action.QueryResult;

import javax.swing.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * A Studio window which shows queries executed by AI agents on background connections:
 * one editor tab per server with the appended queries, and a result tab per query.
 * The window is not saved in the workspace. All methods should be called on the EDT.
 */
public class AgentQueriesWindow {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static StudioWindow window = null;
    private static final Map<String, EditorTab> tabs = new HashMap<>();

    public static boolean isOpen() {
        return window != null && window.isDisplayable();
    }

    /**
     * Opens (or brings to front) the window from the UI
     */
    public static void open() {
        if (isOpen()) {
            WindowFactory.activate(window);
            return;
        }
        create(true);
    }

    private static void create(boolean focus) {
        tabs.clear();
        window = WindowFactory.newAgentWindow(focus);
    }

    // Returns the window if it is open or should be opened according to the settings
    private static StudioWindow getWindow() {
        if (isOpen()) return window;
        if (!Config.getInstance().getBoolean(Config.MCP_SHOW_AGENT_WINDOW)) return null;
        create(false);
        return window;
    }

    private static String key(Server server) {
        if (server.inServerTree()) return "server:" + server.getFullName();
        return "adhoc:" + server.getHost() + ":" + server.getPort();
    }

    private static EditorTab getTab(StudioWindow window, Server server) {
        String key = key(server);
        EditorTab tab = tabs.get(key);
        if (tab != null && tab.isAddedToPanel() && tab.getStudioWindow() == window) return tab;

        EditorTab active = window.getActiveEditor();
        tab = active.getEditorsPanel().addTab(server);
        tab.loadFile(null);
        // drop the initial empty tab of the new window
        if (active.getServer() == Server.NO_SERVER && !active.isModified()
                && active.getTextArea().getText().isEmpty() && active != tab) {
            active.getEditorsPanel().closeTabForced(active);
        }
        // the new tab is selected; make it the active editor of the window (title, server, actions)
        window.updateEditor(tab);
        tabs.put(key, tab);
        return tab;
    }

    private static void append(EditorTab tab, String text) {
        boolean modified = tab.isModified();
        JTextArea textArea = tab.getTextArea();
        String current = textArea.getText();
        String prefix = current.isEmpty() || current.endsWith("\n\n") ? "" : current.endsWith("\n") ? "\n" : "\n\n";
        textArea.append(prefix + text + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
        // appended by Studio, not changed by the user
        if (!modified) tab.setModified(false);
        EditorsPanel.refreshEditorTitle(tab);
    }

    private static String header(String agent, String outcome) {
        return "/ " + LocalTime.now().format(TIME_FORMAT) + " " + agent + ": " + outcome;
    }

    /**
     * A query was executed (successfully or with an error)
     */
    public static void queryExecuted(String agent, Server server, String query, String outcome, QueryResult result) {
        StudioWindow window = getWindow();
        if (window == null) return;
        EditorTab tab = getTab(window, server);
        String time = result.getError() == null ? ", " + result.getExecutionTimeInMS() + " ms" : ", error";
        append(tab, header(agent, outcome + time) + "\n" + query);
        window.addResultTab(result, "Executed by AI agent " + agent + " at server: " + server.getDescription(true));
    }

    /**
     * A query was denied (by the user or the timeout); it is added as a comment
     */
    public static void queryDenied(String agent, Server server, String query, String reason) {
        StudioWindow window = getWindow();
        if (window == null) return;
        EditorTab tab = getTab(window, server);
        StringBuilder text = new StringBuilder(header(agent, reason + " (not executed)"));
        for (String line: query.split("\n")) {
            // a line with only "/" would start a block comment in q
            if (line.trim().isEmpty()) continue;
            text.append("\n/ ").append(line);
        }
        append(tab, text.toString());
    }
}
