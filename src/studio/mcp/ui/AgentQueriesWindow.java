package studio.mcp.ui;

import studio.kdb.Config;
import studio.kdb.Server;
import studio.ui.EditorTab;
import studio.ui.EditorsPanel;
import studio.ui.StudioWindow;
import studio.ui.WindowFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Studio window where queries from AI agents (with a server or an ad-hoc connection) are executed
 * as if the user ran them: one editor tab per server with the appended queries, and a result tab per query.
 * The window can be saved in the workspace. All methods should be called on the EDT.
 */
public class AgentQueriesWindow {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    // The header comment of every query added by Studio
    private static final Pattern HEADER = Pattern.compile("^/ \\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}:\\d{2} ", Pattern.MULTILINE);

    private static StudioWindow window = null;

    public static boolean isOpen() {
        return window != null && window.isDisplayable();
    }

    /**
     * Agent queries go through the window if it is open or it should be opened according to the settings
     */
    public static boolean isActive() {
        return isOpen() || Config.getInstance().getBoolean(Config.MCP_SHOW_AGENT_WINDOW);
    }

    /**
     * Opens (or brings to front) the window from the UI
     */
    public static void open() {
        if (isOpen()) {
            WindowFactory.activate(window);
            return;
        }
        attach(WindowFactory.newAgentWindow(true));
    }

    /**
     * Registers the window (a new one or restored from the workspace)
     */
    public static void attach(StudioWindow newWindow) {
        newWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // release the window with its result tabs (they can hold large results)
                if (window == newWindow) window = null;
            }
        });
        window = newWindow;
    }

    // Returns the window if it is open or should be opened according to the settings
    private static StudioWindow getWindow() {
        if (isOpen()) return window;
        if (!Config.getInstance().getBoolean(Config.MCP_SHOW_AGENT_WINDOW)) return null;
        attach(WindowFactory.newAgentWindow(false));
        return window;
    }

    private static boolean sameServer(Server s1, Server s2) {
        if (s1 == Server.NO_SERVER || s2 == Server.NO_SERVER) return false;
        if (s1.inServerTree() || s2.inServerTree()) {
            return s1.inServerTree() && s2.inServerTree() && s1.getFullName().equals(s2.getFullName());
        }
        return s1.getHost().equals(s2.getHost()) && s1.getPort() == s2.getPort()
                && Objects.equals(s1.getUsername(), s2.getUsername())
                && Objects.equals(s1.getAuthenticationMechanism(), s2.getAuthenticationMechanism());
    }

    /**
     * The tab of the window for the server: an existing tab with this server (even if it was opened by the user),
     * or a new one
     */
    private static EditorTab getTab(StudioWindow window, Server server) {
        List<EditorTab> editors = window.getRootEditorsPanel().getAllEditors(false);
        for (EditorTab editor: editors) {
            if (sameServer(editor.getServer(), server)) {
                // the connection details could be changed in the server list
                if (!editor.getServer().equals(server) && !editor.getQueryExecutor().running()) {
                    editor.setServer(server);
                    EditorsPanel.refreshEditorTitle(editor);
                }
                return editor;
            }
        }

        EditorTab active = window.getActiveEditor();
        EditorTab tab = active.getEditorsPanel().addTab(server);
        tab.loadFile(null);
        // drop the initial empty tab of a new window
        if (editors.size() == 1 && active.getServer() == Server.NO_SERVER && !active.isModified()
                && active.getTextArea().getText().isEmpty() && active != tab) {
            active.getEditorsPanel().closeTabForced(active);
        }
        // the new tab is selected; make it the active editor of the window (title, server, actions)
        window.updateEditor(tab);
        return tab;
    }

    private static void append(EditorTab tab, String text, boolean newBlock) {
        boolean modified = tab.isModified();
        JTextArea textArea = tab.getTextArea();
        String current = textArea.getText();
        String prefix;
        if (newBlock) prefix = current.isEmpty() || current.endsWith("\n\n") ? "" : current.endsWith("\n") ? "\n" : "\n\n";
        else prefix = current.isEmpty() || current.endsWith("\n") ? "" : "\n";
        textArea.append(prefix + text + "\n");
        trimHistory(textArea);
        textArea.setCaretPosition(textArea.getDocument().getLength());
        // appended by Studio, not changed by the user
        if (!modified) tab.setModified(false);
        EditorsPanel.refreshEditorTitle(tab);
    }

    // Keeps the last N queries (added by Studio) in the tab
    static void trimHistory(JTextArea textArea) {
        int maxQueries = Math.max(1, Config.getInstance().getInt(Config.MCP_AGENT_WINDOW_MAX_QUERIES));
        String text = textArea.getText();
        List<Integer> headers = new ArrayList<>();
        Matcher matcher = HEADER.matcher(text);
        while (matcher.find()) headers.add(matcher.start());
        if (headers.size() <= maxQueries) return;
        int cut = headers.get(headers.size() - maxQueries);
        try {
            textArea.getDocument().remove(0, cut);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String header(String agent, String outcome) {
        return "/ " + LocalDateTime.now().format(TIME_FORMAT) + " " + agent + ": " + outcome;
    }

    /**
     * The tab for the server where an agent query is executed
     * @return the tab (with the server) or null if the window is not active
     */
    public static EditorTab getTab(Server server) {
        StudioWindow window = getWindow();
        if (window == null) return null;
        return getTab(window, server);
    }

    // The query is about to be executed in the tab
    public static void queryStarted(EditorTab tab, String agent, String query, String outcome) {
        append(tab, header(agent, outcome) + "\n" + query, true);
    }

    public static void queryCancelled(EditorTab tab) {
        if (tab.isAddedToPanel()) append(tab, "/ ^ cancelled by the user", false);
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
        append(tab, text.toString(), true);
    }
}
