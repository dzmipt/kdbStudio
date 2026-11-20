package studio.mcp;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import studio.kdb.config.McpTokenMode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP server over the Streamable HTTP transport (JSON responses only, no SSE).
 */
public class McpServer {

    public static final String PATH = "/mcp";
    public static final String SESSION_HEADER = "Mcp-Session-Id";
    public static final String LATEST_PROTOCOL_VERSION = "2025-06-18";
    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS =
            Set.of("2024-11-05", "2025-03-26", "2025-06-18", "2025-11-25");
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    // JSON-RPC error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    public static class Settings {
        private final int port;
        private final boolean remote;
        private final McpTokenMode tokenMode;
        private final String token;

        public Settings(int port, boolean remote, McpTokenMode tokenMode, String token) {
            this.port = port;
            this.remote = remote;
            this.tokenMode = tokenMode;
            this.token = token == null ? "" : token;
        }

        public int getPort() {
            return port;
        }

        public boolean isRemote() {
            return remote;
        }

        public McpTokenMode getTokenMode() {
            return tokenMode;
        }

        public String getToken() {
            return token;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Settings)) return false;
            Settings s = (Settings) o;
            return port == s.port && remote == s.remote && tokenMode == s.tokenMode && token.equals(s.token);
        }

        @Override
        public int hashCode() {
            return Objects.hash(port, remote, tokenMode, token);
        }
    }

    private final Settings settings;
    private final String serverName;
    private final String serverVersion;
    private final String instructions;
    private final Map<String, McpTool> tools = new LinkedHashMap<>();
    private final McpSessions sessions;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private HttpServer httpServer;
    private ExecutorService executor;

    public McpServer(Settings settings, String serverName, String serverVersion, String instructions,
                     List<McpTool> toolList, McpSessions sessions) {
        this.settings = settings;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.instructions = instructions;
        this.sessions = sessions;
        for (McpTool tool: toolList) {
            tools.put(tool.getName(), tool);
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public McpSessions getSessions() {
        return sessions;
    }

    public synchronized void start() throws IOException {
        InetAddress address = settings.isRemote() ? null : InetAddress.getLoopbackAddress();
        InetSocketAddress socketAddress = address == null ? new InetSocketAddress(settings.getPort())
                                                          : new InetSocketAddress(address, settings.getPort());
        httpServer = HttpServer.create(socketAddress, 0);
        AtomicInteger threadIndex = new AtomicInteger();
        executor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "MCP-" + threadIndex.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        httpServer.setExecutor(executor);
        httpServer.createContext(PATH, this::handle);
        httpServer.start();
        McpLog.info("MCP server started on {}:{}", settings.isRemote() ? "0.0.0.0" : "127.0.0.1", getPort());
    }

    // The actual port (useful when started with port 0)
    public int getPort() {
        HttpServer server = httpServer;
        return server == null ? settings.getPort() : server.getAddress().getPort();
    }

    public synchronized void stop() {
        if (httpServer == null) return;
        httpServer.stop(0);
        executor.shutdownNow();
        httpServer = null;
        executor = null;
        sessions.clear();
        McpLog.info("MCP server stopped");
    }

    private static boolean isLocalOrigin(String origin) {
        try {
            URI uri = new URI(origin);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase();
            // Only web pages (http/https) can be malicious; other schemes (e.g. vscode-file) come from desktop apps
            if (!scheme.equals("http") && !scheme.equals("https")) return true;
            String host = uri.getHost();
            if (host == null) return false;
            host = host.toLowerCase();
            return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]") || host.equals("::1");
        } catch (Exception e) {
            return false;
        }
    }

    // null if the token is valid; otherwise the reason of the failure
    private String checkToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) return "no Authorization header";
        String prefix = "Bearer ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) return "Authorization header is not 'Bearer <token>'";
        String token = header.substring(prefix.length()).trim();
        if (settings.getToken().isEmpty()) return "the access token is not configured";
        boolean valid = MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                settings.getToken().getBytes(StandardCharsets.UTF_8));
        return valid ? null : "wrong token (was the token regenerated after the client was configured?)";
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            handleInternal(exchange);
        } catch (Throwable e) {
            McpLog.error("Unexpected error in MCP request handling", e);
            try {
                sendText(exchange, 500, "Internal error");
            } catch (IOException ignored) {}
        } finally {
            exchange.close();
        }
    }

    private void handleInternal(HttpExchange exchange) throws IOException {
        InetSocketAddress remoteSocket = exchange.getRemoteAddress();
        boolean remote = !remoteSocket.getAddress().isLoopbackAddress();
        String remoteAddress = remoteSocket.getAddress().getHostAddress();

        if (!exchange.getRequestURI().getPath().equals(PATH)) {
            sendText(exchange, 404, "Not found");
            return;
        }

        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && !isLocalOrigin(origin)) {
            McpLog.warn("Rejected request from {} with origin {}", remoteAddress, origin);
            sendText(exchange, 403, "Forbidden origin");
            return;
        }

        if (settings.getTokenMode().isRequired(remote)) {
            String failure = checkToken(exchange);
            if (failure != null) {
                McpLog.warn("Authentication failed for {} request from {}{}: {}. Token mode: {}; request headers: {}",
                        exchange.getRequestMethod(), remoteAddress, remote ? " (remote)" : " (local)", failure,
                        settings.getTokenMode(), exchange.getRequestHeaders().keySet());
                String description = "Studio MCP server: " + failure +
                        ". Send the access token from Studio Settings, AI Agent (MCP) tab as 'Authorization: Bearer <token>'.";
                exchange.getResponseHeaders().add("WWW-Authenticate",
                        "Bearer error=\"invalid_token\", error_description=\"" + description.replace("\"", "'") + "\"");
                sendText(exchange, 401, description);
                return;
            }
        }

        String method = exchange.getRequestMethod();
        String sessionId = exchange.getRequestHeaders().getFirst(SESSION_HEADER);
        if (method.equals("DELETE")) {
            McpSessions.Session session = sessions.remove(sessionId);
            if (session != null) {
                McpLog.info("Agent {} disconnected (session {})", session.getDisplayName(), session.getId());
            }
            sendText(exchange, 200, "");
            return;
        }
        if (!method.equals("POST")) {
            exchange.getResponseHeaders().add("Allow", "POST, DELETE");
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        McpSessions.Session session = null;
        if (sessionId != null) {
            session = sessions.get(sessionId);
            if (session == null) {
                sendText(exchange, 404, "Unknown session");
                return;
            }
            sessions.touch(session);
        }

        byte[] body = readBody(exchange);
        if (body == null) {
            sendText(exchange, 413, "Request is too large");
            return;
        }

        JsonElement request;
        try {
            request = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
        } catch (JsonParseException e) {
            sendJson(exchange, 400, error(JsonNull.INSTANCE, PARSE_ERROR, "Parse error"), null);
            return;
        }

        String[] newSessionId = new String[1];
        McpCallContext context = new McpCallContext(session, remoteAddress);
        JsonElement response;
        if (request.isJsonArray()) {
            JsonArray responses = new JsonArray();
            for (JsonElement message: request.getAsJsonArray()) {
                JsonObject r = processMessage(message, context, newSessionId);
                if (r != null) responses.add(r);
            }
            response = responses.isEmpty() ? null : responses;
        } else {
            response = processMessage(request, context, newSessionId);
        }

        if (response == null) {
            sendText(exchange, 202, "");
        } else {
            sendJson(exchange, 200, response, newSessionId[0]);
        }
    }

    // returns null for notifications and responses
    private JsonObject processMessage(JsonElement message, McpCallContext context, String[] newSessionId) {
        if (!message.isJsonObject()) {
            return error(JsonNull.INSTANCE, INVALID_REQUEST, "Invalid request");
        }
        JsonObject json = message.getAsJsonObject();
        JsonElement id = json.get("id");
        if (!json.has("method")) return null; // a response from the client - ignore

        JsonElement methodElement = json.get("method");
        if (!methodElement.isJsonPrimitive() || !methodElement.getAsJsonPrimitive().isString()) {
            return error(id == null ? JsonNull.INSTANCE : id, INVALID_REQUEST, "Invalid request: method should be a string");
        }
        String method = methodElement.getAsString();
        JsonObject params = json.has("params") && json.get("params").isJsonObject() ?
                json.getAsJsonObject("params") : new JsonObject();

        if (id == null || id.isJsonNull()) {
            // notification
            return null;
        }

        try {
            JsonElement result;
            switch (method) {
                case "initialize":
                    result = initialize(params, context, newSessionId);
                    break;
                case "ping":
                    result = new JsonObject();
                    break;
                case "tools/list":
                    result = listTools();
                    break;
                case "tools/call":
                    result = callTool(params, context);
                    break;
                default:
                    return error(id, METHOD_NOT_FOUND, "Method not found: " + method);
            }
            JsonObject response = new JsonObject();
            response.addProperty("jsonrpc", "2.0");
            response.add("id", id);
            response.add("result", result);
            return response;
        } catch (InvalidParamsException e) {
            return error(id, INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            McpLog.error("Error processing " + method, e);
            return error(id, INTERNAL_ERROR, "Internal error: " + e.getMessage());
        }
    }

    private static class InvalidParamsException extends Exception {
        InvalidParamsException(String message) {
            super(message);
        }
    }

    private static String getString(JsonObject json, String name) {
        if (json == null) return null;
        JsonElement e = json.get(name);
        if (e == null || !e.isJsonPrimitive()) return null;
        return e.getAsString();
    }

    private JsonObject initialize(JsonObject params, McpCallContext context, String[] newSessionId) {
        String requested = getString(params, "protocolVersion");
        String version = requested != null && SUPPORTED_PROTOCOL_VERSIONS.contains(requested) ?
                            requested : LATEST_PROTOCOL_VERSION;

        JsonObject clientInfo = params.has("clientInfo") && params.get("clientInfo").isJsonObject() ?
                params.getAsJsonObject("clientInfo") : null;
        String clientName = getString(clientInfo, "name");
        String clientVersion = getString(clientInfo, "version");

        McpSessions.Session session = sessions.create(clientName, clientVersion, context.getRemoteAddress());
        newSessionId[0] = session.getId();
        McpLog.info("Agent {} connected from {} (session {}, protocol {})",
                session.getDisplayName(), context.getRemoteAddress(), session.getId(), version);

        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", version);
        JsonObject capabilities = new JsonObject();
        JsonObject toolsCapability = new JsonObject();
        toolsCapability.addProperty("listChanged", false);
        capabilities.add("tools", toolsCapability);
        result.add("capabilities", capabilities);
        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", serverName);
        serverInfo.addProperty("version", serverVersion);
        result.add("serverInfo", serverInfo);
        if (instructions != null) result.addProperty("instructions", instructions);
        return result;
    }

    private JsonObject listTools() {
        JsonArray list = new JsonArray();
        for (McpTool tool: tools.values()) {
            list.add(tool.toJson());
        }
        JsonObject result = new JsonObject();
        result.add("tools", list);
        return result;
    }

    private JsonObject callTool(JsonObject params, McpCallContext context) throws InvalidParamsException {
        String name = getString(params, "name");
        if (name == null) throw new InvalidParamsException("Tool name is missing");
        McpTool tool = tools.get(name);
        if (tool == null) throw new InvalidParamsException("Unknown tool: " + name);

        JsonElement args = params.get("arguments");
        JsonObject arguments = args != null && args.isJsonObject() ? args.getAsJsonObject() : new JsonObject();

        long start = System.currentTimeMillis();
        String agent = context.getClientName();
        McpLog.info("[{}] call {} {}", agent, name, gson.toJson(McpLog.maskPasswords(arguments)));

        String text;
        boolean isError = false;
        try {
            text = tool.getHandler().call(arguments, context);
            McpLog.info("[{}] {} completed in {} ms", agent, name, System.currentTimeMillis() - start);
        } catch (McpToolException e) {
            text = e.getMessage();
            isError = true;
            McpLog.info("[{}] {} failed: {}", agent, name, e.getMessage());
        } catch (Exception e) {
            McpLog.error("Tool " + name + " failed with unexpected error", e);
            text = "Unexpected error: " + e;
            isError = true;
        }

        JsonObject result = new JsonObject();
        JsonArray content = new JsonArray();
        JsonObject textContent = new JsonObject();
        textContent.addProperty("type", "text");
        textContent.addProperty("text", text == null ? "" : text);
        content.add(textContent);
        result.add("content", content);
        result.addProperty("isError", isError);
        return result;
    }

    private static JsonObject error(JsonElement id, int code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id == null ? JsonNull.INSTANCE : id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return response;
    }

    private static byte[] readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if (out.size() > MAX_BODY_SIZE) return null;
            }
            return out.toByteArray();
        }
    }

    private void sendJson(HttpExchange exchange, int code, JsonElement json, String sessionId) throws IOException {
        byte[] bytes = gson.toJson(json).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        if (sessionId != null) {
            exchange.getResponseHeaders().add(SESSION_HEADER, sessionId);
        }
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendText(HttpExchange exchange, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0) {
            exchange.sendResponseHeaders(code, -1);
            return;
        }
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
