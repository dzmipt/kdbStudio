package studio.mcp;

import com.google.gson.*;
import kx.K4Exception;
import kx.KMessage;
import kx.ProgressCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.Server;
import studio.kdb.Session;
import studio.mcp.ui.AgentQueriesWindow;
import studio.mcp.ui.QueryConfirmationDialog;
import studio.ui.*;
import studio.ui.action.QueryResult;
import studio.ui.action.QueryTask;
import studio.utils.QConnection;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tools exposed by Studio to AI agents.
 */
public class StudioTools {

    public static final String INSTRUCTIONS =
            "This server controls a running Studio for kdb+ (a desktop IDE for kdb+/q). " +
            "You can list and read editor tabs, edit them, open new tabs, read query results and run q queries " +
            "on kdb+ servers which the user has permitted. Every query must be confirmed by the user in Studio, " +
            "so a query may be denied. Prefer small, read-only queries and limit result sizes " +
            "(e.g. use `select[10]` or `10#`). Tab and result ids are valid while Studio is running.";

    private static final Logger log = LogManager.getLogger();
    private static final Logger queryLog = LogManager.getLogger("Query");
    private static final AtomicInteger queryIndex = new AtomicInteger(1);
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().serializeNulls().create();

    private final McpService service;

    // Ids of windows, editor tabs and result tabs
    private static final Map<Object, String> ids = new WeakHashMap<>();
    private static final Map<String, Integer> idCounters = new HashMap<>();

    // Only one confirmation dialog at a time
    private final ReentrantLock confirmationLock = new ReentrantLock(true);

    private static class BackgroundSession {
        final Server server;
        final Session session;
        int users = 0; // queries using or waiting for the connection; guarded by StudioTools
        boolean retired = false; // replaced by a newer connection or closed
        final Set<String> clients = java.util.concurrent.ConcurrentHashMap.newKeySet(); // MCP sessions which used it

        BackgroundSession(Server server, Session session) {
            this.server = server;
            this.session = session;
        }
    }

    private final Map<String, BackgroundSession> backgroundSessions = new HashMap<>();
    private boolean closed = false;

    public StudioTools(McpService service) {
        this.service = service;
    }

    // ---------------------------------------------------------------- helpers

    private static synchronized String idOf(Object obj, String prefix) {
        String id = ids.get(obj);
        if (id == null) {
            int next = idCounters.merge(prefix, 1, Integer::sum);
            id = prefix + next;
            ids.put(obj, id);
        }
        return id;
    }

    interface EdtCallable<T> {
        T call() throws Exception;
    }

    private static <T> T onEdt(EdtCallable<T> callable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return callable.call();
        Object[] result = new Object[1];
        Exception[] error = new Exception[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = callable.call();
                } catch (Exception e) {
                    error[0] = e;
                }
            });
        } catch (InvocationTargetException e) {
            throw new McpToolException("Unexpected error: " + e.getCause());
        }
        if (error[0] != null) throw error[0];
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
    }

    private static String getString(JsonObject args, String name, boolean required) throws McpToolException {
        JsonElement e = args.get(name);
        if (e == null || e.isJsonNull()) {
            if (required) throw new McpToolException("Missing required argument: " + name);
            return null;
        }
        if (!e.isJsonPrimitive()) throw new McpToolException("Argument " + name + " should be a string");
        return e.getAsString();
    }

    private static Integer getInt(JsonObject args, String name) throws McpToolException {
        JsonElement e = args.get(name);
        if (e == null || e.isJsonNull()) return null;
        try {
            return e.getAsInt();
        } catch (RuntimeException ex) {
            throw new McpToolException("Argument " + name + " should be an integer");
        }
    }

    private static Boolean getBoolean(JsonObject args, String name) throws McpToolException {
        JsonElement e = args.get(name);
        if (e == null || e.isJsonNull()) return null;
        try {
            return e.getAsBoolean();
        } catch (RuntimeException ex) {
            throw new McpToolException("Argument " + name + " should be a boolean");
        }
    }

    private static String toJson(JsonElement json) {
        return gson.toJson(json);
    }

    private static String describeServer(Server server) {
        if (server == null || server == Server.NO_SERVER) return null;
        if (server.inServerTree()) return server.getFullName();
        return server.getHost() + ":" + server.getPort();
    }

    // ---------------------------------------------------------------- schema builder

    private static class Schema {
        private final JsonObject properties = new JsonObject();
        private final JsonArray required = new JsonArray();

        Schema prop(String name, String type, String description, boolean isRequired) {
            JsonObject p = new JsonObject();
            p.addProperty("type", type);
            p.addProperty("description", description);
            properties.add(name, p);
            if (isRequired) required.add(name);
            return this;
        }

        Schema enumProp(String name, String description, String... values) {
            JsonObject p = new JsonObject();
            p.addProperty("type", "string");
            p.addProperty("description", description);
            JsonArray array = new JsonArray();
            for (String v: values) array.add(v);
            p.add("enum", array);
            properties.add(name, p);
            return this;
        }

        Schema connection() {
            JsonObject p = new Schema()
                    .prop("host", "string", "Host name", true)
                    .prop("port", "integer", "Port", true)
                    .prop("user", "string", "User name", false)
                    .prop("password", "string", "Password", false)
                    .prop("authMethod", "string", "Authentication method; default is the Studio default", false)
                    .prop("tls", "boolean", "Use TLS (tcps)", false)
                    .build();
            p.addProperty("description", "Ad-hoc connection to a server which is not in the Studio server list " +
                    "(only if the user allows ad-hoc connections)");
            properties.add("connection", p);
            return this;
        }

        JsonObject build() {
            JsonObject schema = new JsonObject();
            schema.addProperty("type", "object");
            schema.add("properties", properties);
            if (!required.isEmpty()) schema.add("required", required);
            return schema;
        }
    }

    // ---------------------------------------------------------------- tools

    public List<McpTool> getTools() {
        List<McpTool> tools = new ArrayList<>();
        tools.add(new McpTool("list_servers",
                "Lists kdb+ servers from the Studio server list which the user permitted for AI agents: full name, " +
                "folder, host, port, TLS and authentication method. Credentials are never returned. Also tells " +
                "whether ad-hoc connections are allowed.",
                new Schema().prop("filter", "string", "Optional case-insensitive substring of the name or host", false).build(),
                true, this::listServers));
        tools.add(new McpTool("list_tabs",
                "Lists editor tabs in all Studio windows: id, window, title, file name, server, modified, active and running flags.",
                new Schema().build(), true, this::listTabs));
        tools.add(new McpTool("get_tab",
                "Returns the text of an editor tab together with the selection and caret position (character offsets).",
                new Schema().prop("tab", "string", "Tab id from list_tabs", true).build(),
                true, this::getTab));
        tools.add(new McpTool("edit_tab",
                "Edits the text of an editor tab (the edit can be undone by the user). Modes: " +
                "1) only `text` - replaces the whole text; " +
                "2) `find` and `text` - replaces the single exact occurrence of `find` with `text`; " +
                "3) `start` (and optional `end`) character offsets with `text` - replaces the range, or inserts at `start` if `end` is omitted.",
                new Schema()
                        .prop("tab", "string", "Tab id from list_tabs", true)
                        .prop("text", "string", "New text", true)
                        .prop("find", "string", "Exact text to be replaced; must occur exactly once", false)
                        .prop("start", "integer", "Start offset (0-based)", false)
                        .prop("end", "integer", "End offset (exclusive)", false)
                        .build(),
                false, this::editTab));
        tools.add(new McpTool("new_tab",
                "Opens a new editor tab with the given text.",
                new Schema()
                        .prop("text", "string", "Text of the new tab", false)
                        .prop("server", "string", "Full name of a permitted server for the tab", false)
                        .connection()
                        .prop("window", "string", "Window id from list_tabs; default is the active window", false)
                        .build(),
                false, this::newTab));
        tools.add(new McpTool("set_tab_server",
                "Changes the server of an editor tab. Specify either `server` (full name of a permitted server) or `connection`.",
                new Schema()
                        .prop("tab", "string", "Tab id from list_tabs", true)
                        .prop("server", "string", "Full name of a permitted server", false)
                        .connection()
                        .build(),
                false, this::setTabServer));
        tools.add(new McpTool("execute_query",
                "Runs a q query after the user confirms it in Studio. Specify exactly one of: " +
                "`tab` - runs on the tab's server as if the user pressed Execute, the result is also shown in Studio; " +
                "`server` - full name of a permitted server, runs on a background connection; " +
                "`connection` - ad-hoc connection, runs on a background connection. " +
                "Returns the result as text (default) or JSON, limited by maxRows and the Studio limits.",
                new Schema()
                        .prop("query", "string", "q query", true)
                        .prop("tab", "string", "Tab id from list_tabs", false)
                        .prop("server", "string", "Full name of a permitted server", false)
                        .connection()
                        .enumProp("format", "Result format, default text", "text", "json")
                        .prop("maxRows", "integer", "Maximum number of rows to return (capped by Studio settings)", false)
                        .build(),
                false, this::executeQuery));
        tools.add(new McpTool("list_results",
                "Lists query results shown in Studio result tabs: id, window, title, server, query, execution time, type, row count, error.",
                new Schema().prop("window", "string", "Optional window id", false).build(),
                true, this::listResults));
        tools.add(new McpTool("get_result",
                "Returns the content of a result tab as text (default) or JSON.",
                new Schema()
                        .prop("result", "string", "Result id from list_results or execute_query", true)
                        .enumProp("format", "Result format, default text", "text", "json")
                        .prop("maxRows", "integer", "Maximum number of rows to return (capped by Studio settings)", false)
                        .build(),
                true, this::getResult));
        return tools;
    }

    // ---------------------------------------------------------------- servers

    private String listServers(JsonObject args, McpCallContext context) throws Exception {
        String filter = getString(args, "filter", false);
        String lowerFilter = filter == null ? null : filter.toLowerCase();
        Config config = Config.getInstance();
        // the server tree is modified on the EDT
        List<Server> servers = onEdt(() -> McpService.getPermissions().allowedServers(config.getServerTree()));

        JsonArray list = new JsonArray();
        for (Server server: servers) {
            if (lowerFilter != null && !server.getFullName().toLowerCase().contains(lowerFilter)
                    && !server.getHost().toLowerCase().contains(lowerFilter)) continue;
            JsonObject json = new JsonObject();
            json.addProperty("name", server.getFullName());
            json.addProperty("folder", server.getFolderName());
            json.addProperty("host", server.getHost());
            json.addProperty("port", server.getPort());
            json.addProperty("tls", server.getUseTLS());
            json.addProperty("authMethod", server.getAuthenticationMechanism());
            list.add(json);
        }
        JsonObject result = new JsonObject();
        result.add("servers", list);
        result.addProperty("adHocConnectionsAllowed", config.getBoolean(Config.MCP_ALLOW_ADHOC));
        if (servers.isEmpty()) {
            result.addProperty("note", "No servers are permitted. The user can permit servers in Studio Settings, AI Agent (MCP) tab.");
        }
        return toJson(result);
    }

    // Resolves a server from the server list and checks the permission
    private static Server resolvePermittedServer(String name) throws Exception {
        // the server tree is modified on the EDT
        return onEdt(() -> resolvePermittedServerOnEdt(name));
    }

    private static Server resolvePermittedServerOnEdt(String name) throws McpToolException {
        Server server = Config.getInstance().getServerConfig().getServer(name);
        if (server == Server.NO_SERVER) {
            throw new McpToolException("Server '" + name + "' is not found in the Studio server list");
        }
        if (!McpService.getPermissions().isAllowed(server)) {
            throw new McpToolException("Server '" + name + "' is not permitted for AI agents. " +
                    "The user can permit it in Studio Settings, AI Agent (MCP) tab.");
        }
        return server;
    }

    private static void checkAdHocAllowed() throws McpToolException {
        if (!Config.getInstance().getBoolean(Config.MCP_ALLOW_ADHOC)) {
            throw new McpToolException("Ad-hoc connections are not allowed. " +
                    "The user can allow them in Studio Settings, AI Agent (MCP) tab.");
        }
    }

    private static Server adHocServer(JsonObject connection) throws McpToolException {
        checkAdHocAllowed();
        String host = getString(connection, "host", true);
        Integer port = getInt(connection, "port");
        if (port == null) throw new McpToolException("Missing required argument: connection.port");
        String user = getString(connection, "user", false);
        String password = getString(connection, "password", false);
        String authMethod = getString(connection, "authMethod", false);
        Boolean tls = getBoolean(connection, "tls");

        if (authMethod == null) authMethod = DefaultAuthenticationMechanism.NAME;
        List<String> methods = Arrays.asList(AuthenticationManager.getInstance().getAuthenticationMechanisms());
        if (!methods.contains(authMethod)) {
            throw new McpToolException("Unknown authentication method '" + authMethod + "'. Available: " + methods);
        }
        QConnection conn = new QConnection(host, port, user == null ? "" : user, password == null ? "" : password,
                tls != null && tls);
        return new Server("", conn, authMethod, null, null);
    }

    /**
     * A permitted server from `server` or `connection` argument; null if none is specified
     */
    private static Server serverFromArgs(JsonObject args) throws Exception {
        String name = getString(args, "server", false);
        JsonElement connection = args.get("connection");
        boolean hasConnection = connection != null && connection.isJsonObject();
        if (name != null && hasConnection) throw new McpToolException("Specify either server or connection, not both");
        if (name != null) return resolvePermittedServer(name);
        if (hasConnection) return adHocServer(connection.getAsJsonObject());
        return null;
    }

    /**
     * Checks that the server (e.g. of a tab) can be used by agents.
     * @return the up-to-date server from the server list, or the ad-hoc server itself
     */
    private static Server checkPermitted(Server server) throws Exception {
        if (server == null || server == Server.NO_SERVER) {
            throw new McpToolException("No server is set");
        }
        if (server.inServerTree()) {
            return resolvePermittedServer(server.getFullName());
        }
        checkAdHocAllowed();
        return server;
    }

    // ---------------------------------------------------------------- tabs

    private static List<EditorTab> allEditors() {
        List<EditorTab> editors = new ArrayList<>();
        WindowFactory.forEachEditors(editors::add);
        return editors;
    }

    private static EditorTab findEditor(String id) throws McpToolException {
        for (EditorTab editor: allEditors()) {
            if (idOf(editor, "t").equals(id)) return editor;
        }
        throw new McpToolException("Tab '" + id + "' is not found. Use list_tabs to get tab ids.");
    }

    private static StudioWindow findWindow(String id) throws McpToolException {
        for (StudioWindow window: WindowFactory.allStudioWindows()) {
            if (idOf(window, "w").equals(id)) return window;
        }
        throw new McpToolException("Window '" + id + "' is not found");
    }

    // The last active window, but not the AI agent queries window if there is another one
    private static StudioWindow defaultWindow() {
        StudioWindow window = WindowFactory.getLastActiveWindow();
        if (window == null || !window.isAgentWindow()) return window;
        for (StudioWindow w: WindowFactory.allStudioWindows()) {
            if (!w.isAgentWindow()) return w;
        }
        return window;
    }

    private static JsonObject tabInfo(EditorTab editor) {
        StudioWindow window = editor.getStudioWindow();
        JsonObject json = new JsonObject();
        json.addProperty("id", idOf(editor, "t"));
        json.addProperty("window", idOf(window, "w"));
        if (window.isAgentWindow()) json.addProperty("agentQueriesWindow", true);
        json.addProperty("title", editor.getTitle());
        json.addProperty("file", editor.getFilename());
        json.addProperty("server", describeServer(editor.getServer()));
        json.addProperty("modified", editor.isModified());
        json.addProperty("active", window.getActiveEditor() == editor);
        json.addProperty("running", editor.getQueryExecutor().running());
        return json;
    }

    private String listTabs(JsonObject args, McpCallContext context) throws Exception {
        return onEdt(() -> {
            JsonArray tabs = new JsonArray();
            for (EditorTab editor: allEditors()) {
                tabs.add(tabInfo(editor));
            }
            JsonObject result = new JsonObject();
            result.add("tabs", tabs);
            return toJson(result);
        });
    }

    private String getTab(JsonObject args, McpCallContext context) throws Exception {
        String id = getString(args, "tab", true);
        return onEdt(() -> {
            EditorTab editor = findEditor(id);
            JTextArea textArea = editor.getTextArea();
            JsonObject json = tabInfo(editor);
            json.addProperty("text", textArea.getText());
            json.addProperty("selectionStart", textArea.getSelectionStart());
            json.addProperty("selectionEnd", textArea.getSelectionEnd());
            json.addProperty("selectedText", textArea.getSelectedText());
            json.addProperty("caret", textArea.getCaretPosition());
            return toJson(json);
        });
    }

    private String editTab(JsonObject args, McpCallContext context) throws Exception {
        String id = getString(args, "tab", true);
        String text = getString(args, "text", true);
        String find = getString(args, "find", false);
        Integer start = getInt(args, "start");
        Integer end = getInt(args, "end");
        if (find != null && start != null) throw new McpToolException("Specify either find or start/end, not both");
        if (end != null && start == null) throw new McpToolException("end requires start");

        return onEdt(() -> {
            EditorTab editor = findEditor(id);
            org.fife.ui.rtextarea.RTextArea textArea = editor.getTextArea();
            String current = textArea.getText();
            int from, to;
            String operation;
            if (find != null) {
                if (find.isEmpty()) throw new McpToolException("find should not be empty");
                from = current.indexOf(find);
                if (from == -1) throw new McpToolException("Text to find is not found in the tab");
                if (current.indexOf(find, from + 1) != -1) {
                    throw new McpToolException("Text to find occurs more than once; provide a longer unique fragment");
                }
                to = from + find.length();
                operation = "replace found text";
            } else if (start != null) {
                from = start;
                to = end == null ? start : end;
                if (from < 0 || to < from || to > current.length()) {
                    throw new McpToolException("Invalid range [" + from + ", " + to + "); text length is " + current.length());
                }
                operation = end == null ? "insert" : "replace range";
            } else {
                from = 0;
                to = current.length();
                operation = "replace all";
            }

            textArea.beginAtomicEdit();
            try {
                textArea.replaceRange(text, from, to);
            } finally {
                textArea.endAtomicEdit();
            }
            McpLog.info("[{}] edit tab {} ({}): {} [{}, {}) removed {} chars, inserted {} chars",
                    context.getClientName(), id, editor.getTitle(), operation, from, to, to - from, text.length());
            return "Edited. New text length is " + textArea.getText().length() + " characters.";
        });
    }

    private String newTab(JsonObject args, McpCallContext context) throws Exception {
        String text = getString(args, "text", false);
        String windowId = getString(args, "window", false);
        Server server = serverFromArgs(args);

        return onEdt(() -> {
            StudioWindow window = windowId == null ? defaultWindow() : findWindow(windowId);
            if (window == null) throw new McpToolException("No Studio window is open");
            EditorTab active = window.getActiveEditor();
            Server tabServer = server == null ? active.getServer() : server;
            EditorTab editor = active.getEditorsPanel().addTab(tabServer);
            editor.loadFile(null);
            if (text != null && !text.isEmpty()) {
                editor.getTextArea().setText(text);
                editor.getTextArea().setCaretPosition(0);
            }
            String id = idOf(editor, "t");
            McpLog.info("[{}] new tab {} ({}) with server {}, {} chars",
                    context.getClientName(), id, editor.getTitle(), describeServer(tabServer), text == null ? 0 : text.length());
            return toJson(tabInfo(editor));
        });
    }

    private static void setEditorServer(EditorTab editor, Server server) {
        StudioWindow window = editor.getStudioWindow();
        if (window.getActiveEditor() == editor) {
            window.setServer(server);
        } else {
            editor.setServer(server);
            EditorsPanel.refreshEditorTitle(editor);
        }
    }

    private String setTabServer(JsonObject args, McpCallContext context) throws Exception {
        String id = getString(args, "tab", true);
        Server server = serverFromArgs(args);
        if (server == null) throw new McpToolException("Specify server or connection");

        return onEdt(() -> {
            EditorTab editor = findEditor(id);
            if (editor.getQueryExecutor().running()) {
                throw new McpToolException("A query is running in the tab");
            }
            setEditorServer(editor, server);
            McpLog.info("[{}] tab {} server is set to {}", context.getClientName(), id, describeServer(server));
            return toJson(tabInfo(editor));
        });
    }

    // ---------------------------------------------------------------- results

    private static int maxRows(JsonObject args) throws McpToolException {
        int limit = Config.getInstance().getInt(Config.MCP_MAX_ROWS);
        Integer requested = getInt(args, "maxRows");
        if (requested == null) return limit;
        return Math.max(0, Math.min(limit, requested));
    }

    private static boolean jsonFormat(JsonObject args) throws McpToolException {
        String format = getString(args, "format", false);
        if (format == null || format.equals("text")) return false;
        if (format.equals("json")) return true;
        throw new McpToolException("Unknown format '" + format + "'; use text or json");
    }

    private static String errorMessage(Throwable error) {
        if (error instanceof K4Exception) return "'" + error.getMessage();
        String message = error.getMessage();
        return message == null ? error.toString() : message;
    }

    private static String formatResult(String serverDescription, String query, QueryResult result, String resultId,
                                       boolean json, int maxRows) {
        int maxChars = Config.getInstance().getInt(Config.MCP_MAX_CHARS);
        K.KBase obj = result.getResult();
        long time = result.getExecutionTimeInMS();
        if (json) {
            JsonObject wrapper = new JsonObject();
            wrapper.addProperty("server", serverDescription);
            if (resultId != null) wrapper.addProperty("resultId", resultId);
            wrapper.addProperty("executionTimeMs", time);
            wrapper.add("result", ResultRenderer.renderJsonLimited(obj, maxRows, Math.max(100, maxChars - 200)));
            return toJson(wrapper);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Server: ").append(serverDescription).append('\n');
        if (resultId != null) builder.append("Result id: ").append(resultId).append(" (shown in Studio)\n");
        builder.append("Execution time: ").append(time).append(" ms\n");
        builder.append("Type: ").append(ResultRenderer.kind(obj));
        if (obj != null) builder.append(", count: ").append(String.format("%,d", ResultRenderer.rowCount(obj)));
        builder.append("\n\n");
        builder.append(ResultRenderer.renderText(obj, maxRows, maxChars));
        return ResultRenderer.limit(builder.toString(), maxChars);
    }

    private static List<ResultTab> resultTabs(StudioWindow window) {
        List<ResultTab> list = new ArrayList<>();
        int count = window.countResultTabs();
        for (int index = 0; index < count; index++) list.add(window.getResultTab(index));
        return list;
    }

    private String listResults(JsonObject args, McpCallContext context) throws Exception {
        String windowId = getString(args, "window", false);
        return onEdt(() -> {
            List<StudioWindow> windows = windowId == null ? WindowFactory.allStudioWindows() : List.of(findWindow(windowId));
            JsonArray list = new JsonArray();
            for (StudioWindow window: windows) {
                for (ResultTab tab: resultTabs(window)) {
                    QueryResult result = tab.getQueryResult();
                    JsonObject json = new JsonObject();
                    json.addProperty("id", idOf(tab, "r"));
                    json.addProperty("window", idOf(window, "w"));
                    json.addProperty("title", tab.getTitle());
                    json.addProperty("server", describeServer(result.getServer()));
                    json.addProperty("query", Util.limitString(result.getQuery(), 500));
                    json.addProperty("executionTimeMs", result.getExecutionTimeInMS());
                    if (result.getError() != null) {
                        json.addProperty("error", errorMessage(result.getError()));
                    } else {
                        json.addProperty("type", ResultRenderer.kind(result.getResult()));
                        json.addProperty("count", ResultRenderer.rowCount(result.getResult()));
                    }
                    list.add(json);
                }
            }
            JsonObject result = new JsonObject();
            result.add("results", list);
            return toJson(result);
        });
    }

    private String getResult(JsonObject args, McpCallContext context) throws Exception {
        String id = getString(args, "result", true);
        boolean json = jsonFormat(args);
        int maxRows = maxRows(args);
        QueryResult result = onEdt(() -> {
            for (StudioWindow window: WindowFactory.allStudioWindows()) {
                for (ResultTab tab: resultTabs(window)) {
                    if (idOf(tab, "r").equals(id)) return tab.getQueryResult();
                }
            }
            throw new McpToolException("Result '" + id + "' is not found. Use list_results to get result ids.");
        });
        if (result.getError() != null) {
            throw new McpToolException("The result is an error: " + errorMessage(result.getError()));
        }
        return formatResult(describeServer(result.getServer()), result.getQuery(), result, id, json, maxRows);
    }

    // ---------------------------------------------------------------- query execution

    private static String grantKey(Server server) {
        if (server.inServerTree()) return McpGrants.serverKey(server.getFullName());
        return McpGrants.adHocKey(server.getHost(), server.getPort());
    }

    private static String grantLabel(Server server) {
        if (server.inServerTree()) return server.getDescription(true);
        return server.getHost() + ":" + server.getPort() + " (ad-hoc)";
    }

    // The query is not confirmed by the user
    private static class QueryDeniedException extends McpToolException {
        private final String reason;

        QueryDeniedException(String message, String reason) {
            super(message);
            this.reason = reason;
        }
    }

    /**
     * Asks the user (or uses an active grant). Throws QueryDeniedException if the query is not confirmed.
     * @return how the query was confirmed
     */
    private String confirm(McpCallContext context, Server server, String target, String query) throws Exception {
        String key = grantKey(server);
        String agent = context.getClientName();
        McpGrants grants = service.getGrants();
        if (grants.useGrant(key)) {
            McpLog.info("[{}] query on {} auto-approved by grant", agent, grantLabel(server));
            return "auto-approved by a time-limited permission";
        }

        confirmationLock.lockInterruptibly();
        try {
            // a grant could be given while waiting for the previous dialog
            if (grants.useGrant(key)) {
                McpLog.info("[{}] query on {} auto-approved by grant", agent, grantLabel(server));
                return "auto-approved by a time-limited permission";
            }
            int timeout = Math.max(10, Config.getInstance().getInt(Config.MCP_CONFIRMATION_TIMEOUT_SEC));
            Object[] outcome = onEdt(() -> {
                StudioWindow window = WindowFactory.getLastActiveWindow();
                if (window != null) WindowFactory.activate(window);
                QueryConfirmationDialog dialog = new QueryConfirmationDialog(window,
                        agent, grantLabel(server), target, query, timeout);
                QueryConfirmationDialog.Decision decision = dialog.showDialog();
                return new Object[] {decision, dialog.getInterval()};
            });
            QueryConfirmationDialog.Decision decision = (QueryConfirmationDialog.Decision) outcome[0];
            studio.kdb.config.McpGrantInterval interval = (studio.kdb.config.McpGrantInterval) outcome[1];

            switch (decision) {
                case RUN:
                    McpLog.info("[{}] query on {} confirmed by the user", agent, grantLabel(server));
                    return "confirmed by the user";
                case ALLOW_FOR_INTERVAL:
                    onEdt(() -> Config.getInstance().setEnum(Config.MCP_GRANT_INTERVAL, interval));
                    grants.grant(key, grantLabel(server), interval.getDuration());
                    grants.useGrant(key);
                    McpLog.info("[{}] query on {} confirmed; further queries are allowed for {}",
                            agent, grantLabel(server), interval);
                    return "confirmed by the user, further queries allowed for " + interval;
                case TIMEOUT:
                    McpLog.info("[{}] query on {} denied: no answer from the user", agent, grantLabel(server));
                    throw new QueryDeniedException("The query was not confirmed by the user in time and was denied.",
                            "not confirmed in time");
                default:
                    McpLog.info("[{}] query on {} denied by the user", agent, grantLabel(server));
                    throw new QueryDeniedException("The query was denied by the user.", "denied by the user");
            }
        } finally {
            confirmationLock.unlock();
        }
    }

    private String executeQuery(JsonObject args, McpCallContext context) throws Exception {
        String query = getString(args, "query", true);
        if (query.trim().isEmpty()) throw new McpToolException("The query is empty");
        String tabId = getString(args, "tab", false);
        boolean json = jsonFormat(args);
        int maxRows = maxRows(args);

        Server server = serverFromArgs(args);
        if (tabId != null && server != null) throw new McpToolException("Specify only one of tab, server or connection");
        if (tabId == null && server == null) throw new McpToolException("Specify tab, server or connection");

        if (tabId != null) {
            return executeInTab(context, tabId, query, json, maxRows);
        } else {
            return executeInBackground(context, server, query, json, maxRows);
        }
    }

    private String executeInTab(McpCallContext context, String tabId, String query, boolean json, int maxRows) throws Exception {
        Object[] info = onEdt(() -> {
            EditorTab editor = findEditor(tabId);
            if (editor.getQueryExecutor().running()) throw new McpToolException("A query is already running in the tab");
            return new Object[] {editor.getServer(), editor.getTitle()};
        });
        Server tabServer = (Server) info[0];
        // the up-to-date server from the server list for labels and grants
        Server server = checkPermitted(tabServer);
        String target = "tab " + tabId + " \"" + info[1] + "\"";

        confirm(context, server, target, query);

        CompletableFuture<Object[]> future = new CompletableFuture<>();
        EditorTab.QueryCompletionListener listener =
                (queryResult, resultTab) -> future.complete(new Object[] {queryResult, resultTab});
        McpRunningQueries running = service.getRunningQueries();
        // cancels only the query started by this call (not a query the user starts later in the same tab)
        McpRunningQueries.RunningQuery runningQuery = running.start(context.getClientName(), grantLabel(server),
                target, query,
                () -> SwingUtilities.invokeLater(() -> {
                    try {
                        findEditor(tabId).cancelQuery(listener);
                    } catch (McpToolException e) {
                        // the tab is closed
                    }
                }));
        try {
            return executeInTabStarted(context, tabId, tabServer, server, query, json, maxRows, future, listener, runningQuery);
        } finally {
            running.finish(runningQuery);
        }
    }

    private String executeInTabStarted(McpCallContext context, String tabId, Server tabServer, Server server, String query,
                                       boolean json, int maxRows, CompletableFuture<Object[]> future,
                                       EditorTab.QueryCompletionListener listener,
                                       McpRunningQueries.RunningQuery runningQuery) throws Exception {
        onEdt(() -> {
            // the cancel action is executed on the EDT too, so it is either seen here or cancels the started query
            if (runningQuery.isCancelled()) {
                throw new McpToolException("The query was cancelled by the user");
            }
            EditorTab editor = findEditor(tabId);
            if (!editor.getServer().equals(tabServer)) {
                throw new McpToolException("The server of the tab was changed; the query is not executed");
            }
            if (editor.getQueryExecutor().running()) throw new McpToolException("A query is already running in the tab");
            McpLog.info("[{}] executing in tab {} on {}:\n{}", context.getClientName(), tabId, grantLabel(server), query);
            editor.executeQuery(QueryTask.query(editor.getStudioWindow(), query), listener);
            return null;
        });

        Object[] completed = future.get();
        QueryResult result = (QueryResult) completed[0];
        ResultTab resultTab = (ResultTab) completed[1];
        if (!result.isComplete()) {
            McpLog.info("[{}] query in tab {} was cancelled", context.getClientName(), tabId);
            throw new McpToolException("The query was cancelled by the user");
        }
        if (result.getError() != null) {
            String message = errorMessage(result.getError());
            McpLog.info("[{}] query in tab {} failed: {}", context.getClientName(), tabId, message);
            throw new McpToolException("Error: " + message);
        }
        String resultId = resultTab == null ? null : onEdt(() -> idOf(resultTab, "r"));
        McpLog.info("[{}] query in tab {} completed: type={}, count={}, time={} ms", context.getClientName(), tabId,
                ResultRenderer.kind(result.getResult()), ResultRenderer.rowCount(result.getResult()), result.getExecutionTimeInMS());
        return formatResult(grantLabel(server), query, result, resultId, json, maxRows);
    }

    private static final ProgressCallback NO_PROGRESS = new ProgressCallback() {
        @Override
        public void setCompressed(boolean compressed) {}
        @Override
        public void setMsgLength(int msgLength) {}
        @Override
        public void setCurrentProgress(int total) {}
    };

    // Closes background connections; no new connections are opened afterwards
    public synchronized void close() {
        closed = true;
        for (BackgroundSession background: backgroundSessions.values()) {
            background.retired = true;
            closeSession(background.session);
        }
        backgroundSessions.clear();
    }

    private static void closeSession(Session session) {
        try {
            session.close();
        } catch (Exception e) {
            log.error("Error closing background session", e);
        }
    }

    /**
     * An MCP client disconnected: closes background connections which are no longer used by any connected client
     */
    public synchronized void clientDisconnected(String sessionId) {
        for (Iterator<Map.Entry<String, BackgroundSession>> it = backgroundSessions.entrySet().iterator(); it.hasNext();) {
            BackgroundSession background = it.next().getValue();
            if (!background.clients.remove(sessionId) || !background.clients.isEmpty()) continue;
            McpLog.info("Closing the background connection to {}: the agent disconnected", grantLabel(background.server));
            it.remove();
            background.retired = true;
            if (background.users == 0) closeSession(background.session);
        }
    }

    // Background connections (for Connection Statistics)
    public synchronized List<Session> getBackgroundSessions() {
        List<Session> list = new ArrayList<>();
        for (BackgroundSession background: backgroundSessions.values()) list.add(background.session);
        return list;
    }

    // Background connections are shared by queries with the same server and credentials
    private static String connectionKey(Server server) {
        if (server.inServerTree()) return McpGrants.serverKey(server.getFullName());
        return McpGrants.adHocKey(server.getHost(), server.getPort()) + ":" + server.getUseTLS() + ":" +
                server.getAuthenticationMechanism() + ":" + server.getUsername();
    }

    /**
     * A background connection for the server; it should be released with releaseBackgroundSession()
     */
    private synchronized BackgroundSession acquireBackgroundSession(Server server) throws McpToolException {
        if (closed) throw new McpToolException("The MCP server is stopping");
        String key = connectionKey(server);
        BackgroundSession background = backgroundSessions.get(key);
        if (background == null || !background.server.equals(server)
                || !Objects.equals(background.server.getPassword(), server.getPassword())) {
            if (background != null) {
                // connection details changed (e.g. the server is edited): close it when it is no longer in use
                background.retired = true;
                if (background.users == 0) closeSession(background.session);
            }
            background = new BackgroundSession(server, Session.newDetachedSession(server));
            backgroundSessions.put(key, background);
        }
        background.users++;
        return background;
    }

    private synchronized void releaseBackgroundSession(BackgroundSession background) {
        background.users--;
        if (background.retired && background.users == 0) closeSession(background.session);
    }

    /**
     * A query with a server or an ad-hoc connection: executed in the AI agent queries window if it is active
     * (as if the user ran it), otherwise on a background connection
     */
    private String executeInBackground(McpCallContext context, Server server, String query, boolean json, int maxRows) throws Exception {
        String agent = context.getClientName();
        boolean inWindow = onEdt(AgentQueriesWindow::isActive);
        String outcome;
        try {
            outcome = confirm(context, server, inWindow ? "AI agent queries window" : "background connection", query);
        } catch (QueryDeniedException e) {
            SwingUtilities.invokeLater(() -> AgentQueriesWindow.queryDenied(agent, server, query, e.reason));
            throw e;
        }

        if (inWindow) {
            return executeInAgentWindow(context, server, query, outcome, json, maxRows);
        }

        BackgroundSession background = acquireBackgroundSession(server);
        try {
            if (context.getSession() != null) background.clients.add(context.getSession().getId());
            return executeInBackground(context, background, server, query, outcome, json, maxRows);
        } finally {
            releaseBackgroundSession(background);
        }
    }

    // Agent queries in the same tab of the agent window are executed one after another
    private final Map<String, java.util.concurrent.locks.ReentrantLock> agentTabLocks = new HashMap<>();

    private synchronized java.util.concurrent.locks.ReentrantLock agentTabLock(Server server) {
        return agentTabLocks.computeIfAbsent(connectionKey(server), k -> new java.util.concurrent.locks.ReentrantLock(true));
    }

    private String executeInAgentWindow(McpCallContext context, Server server, String query, String outcome,
                                        boolean json, int maxRows) throws Exception {
        String agent = context.getClientName();
        CompletableFuture<Object[]> future = new CompletableFuture<>();
        EditorTab.QueryCompletionListener listener =
                (queryResult, resultTab) -> future.complete(new Object[] {queryResult, resultTab});
        EditorTab[] tab = new EditorTab[1];
        McpRunningQueries running = service.getRunningQueries();
        // cancels only the query started by this call; a waiting query is not started
        McpRunningQueries.RunningQuery runningQuery = running.start(agent, grantLabel(server), "AI agent queries window", query,
                () -> SwingUtilities.invokeLater(() -> {
                    if (tab[0] != null) tab[0].cancelQuery(listener);
                }));
        java.util.concurrent.locks.ReentrantLock lock = agentTabLock(server);
        try {
            lock.lockInterruptibly();
            try {
                // wait while the tab is busy (e.g. the user runs a query in it)
                while (true) {
                    Boolean started = onEdt(() -> {
                        if (runningQuery.isCancelled()) throw new McpToolException("The query was cancelled by the user");
                        EditorTab editor = AgentQueriesWindow.getTab(server);
                        if (editor == null) throw new McpToolException("The AI agent queries window is closed");
                        if (editor.getQueryExecutor().running()) return false;
                        AgentQueriesWindow.queryStarted(editor, agent, query, outcome);
                        McpLog.info("[{}] executing in the AI agent queries window on {}:\n{}", agent, grantLabel(server), query);
                        tab[0] = editor;
                        editor.executeQuery(QueryTask.query(editor.getStudioWindow(), query), listener);
                        return true;
                    });
                    if (started) break;
                    Thread.sleep(200);
                }

                Object[] completed = future.get();
                QueryResult result = (QueryResult) completed[0];
                ResultTab resultTab = (ResultTab) completed[1];
                if (!result.isComplete()) {
                    SwingUtilities.invokeLater(() -> AgentQueriesWindow.queryCancelled(tab[0]));
                    McpLog.info("[{}] query in the AI agent queries window was cancelled", agent);
                    throw new McpToolException("The query was cancelled by the user");
                }
                if (result.getError() != null) {
                    String message = errorMessage(result.getError());
                    McpLog.info("[{}] query in the AI agent queries window failed: {}", agent, message);
                    throw new McpToolException("Error: " + message);
                }
                String resultId = resultTab == null ? null : onEdt(() -> idOf(resultTab, "r"));
                McpLog.info("[{}] query in the AI agent queries window completed: type={}, count={}, time={} ms", agent,
                        ResultRenderer.kind(result.getResult()), ResultRenderer.rowCount(result.getResult()),
                        result.getExecutionTimeInMS());
                return formatResult(grantLabel(server), query, result, resultId, json, maxRows);
            } finally {
                lock.unlock();
            }
        } finally {
            running.finish(runningQuery);
        }
    }

    private String executeInBackground(McpCallContext context, BackgroundSession background, Server server, String query,
                                       String outcome, boolean json, int maxRows) throws Exception {
        String agent = context.getClientName();
        int index = queryIndex.getAndIncrement();
        Session session = background.session;
        QueryResult result = new QueryResult(server, query, false);
        McpRunningQueries running = service.getRunningQueries();

        // The connection is shared; it is closed by the cancel action only while this query is executing on it.
        // A query cancelled while waiting for the connection is not executed.
        Object state = new Object();
        boolean[] executing = {false};
        McpRunningQueries.RunningQuery runningQuery = running.start(agent, grantLabel(server), "background connection", query,
                () -> {
                    synchronized (state) {
                        if (executing[0]) session.close();
                    }
                });
        try {
            synchronized (background) {
                boolean execute;
                synchronized (state) {
                    execute = !runningQuery.isCancelled();
                    executing[0] = execute;
                }
                if (execute) {
                    queryLog.info("#mcp-{}: query {}({}) from AI agent {}\n{}", index, server.getFullName(),
                            server.getConnectionString(), agent, query);
                    McpLog.info("[{}] executing in background on {}:\n{}", agent, grantLabel(server), query);
                    try {
                        session.validate();
                        KMessage message = session.execute(WindowFactory.getLastActiveWindow(), new K.KString(query), NO_PROGRESS);
                        result.setResult(message);
                    } catch (K4Exception e) {
                        result.setError(e);
                    } catch (Exception e) {
                        session.close();
                        result.setError(e);
                    } finally {
                        synchronized (state) {
                            executing[0] = false;
                        }
                    }
                }
            }
        } finally {
            running.finish(runningQuery);
        }
        boolean cancelled = runningQuery.isCancelled();

        if (cancelled) {
            queryLog.info("#mcp-{}: cancelled", index);
            McpLog.info("[{}] background query on {} was cancelled by the user", agent, grantLabel(server));
            throw new McpToolException("The query was cancelled by the user");
        }

        if (result.getError() != null) {
            String message = errorMessage(result.getError());
            queryLog.info("#mcp-{}: error {}", index, message);
            McpLog.info("[{}] background query on {} failed: {}", agent, grantLabel(server), message);
            throw new McpToolException("Error: " + message);
        }
        queryLog.info("#mcp-{}: type={}, count={}, time={}", index, ResultRenderer.kind(result.getResult()),
                ResultRenderer.rowCount(result.getResult()), result.getExecutionTimeInMS());
        McpLog.info("[{}] background query on {} completed: type={}, count={}, time={} ms", agent,
                grantLabel(server), ResultRenderer.kind(result.getResult()), ResultRenderer.rowCount(result.getResult()),
                result.getExecutionTimeInMS());
        return formatResult(grantLabel(server), query, result, null, json, maxRows);
    }
}
