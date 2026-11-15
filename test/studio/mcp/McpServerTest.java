package studio.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import studio.kdb.config.McpTokenMode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class McpServerTest {

    private McpServer server;
    private final HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    private static JsonObject schema() {
        return JsonParser.parseString("{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"}}}").getAsJsonObject();
    }

    private void start(McpTokenMode mode) throws IOException {
        McpTool echo = new McpTool("echo", "Echo text", schema(), true,
                (args, ctx) -> ctx.getClientName() + ":" + args.get("text").getAsString());
        McpTool fail = new McpTool("fail", "Always fails", schema(), true,
                (args, ctx) -> { throw new McpToolException("boom"); });
        server = new McpServer(new McpServer.Settings(0, false, mode, "tok"), "kdbStudio", "test", "hello",
                List.of(echo, fail), new McpSessions());
        server.start();
    }

    @AfterEach
    public void stop() {
        if (server != null) server.stop();
    }

    private HttpRequest.Builder request() {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + "/mcp"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream");
    }

    private HttpResponse<String> post(String body, String sessionId, String token) throws Exception {
        HttpRequest.Builder builder = request().POST(HttpRequest.BodyPublishers.ofString(body));
        if (sessionId != null) builder.header(McpServer.SESSION_HEADER, sessionId);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String rpc(int id, String method, String params) {
        return "{\"jsonrpc\":\"2.0\",\"id\":" + id + ",\"method\":\"" + method + "\",\"params\":" + params + "}";
    }

    private static final String INIT = rpc(1, "initialize",
            "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"tester\",\"version\":\"1.0\"}}");

    private String initialize(String token) throws Exception {
        HttpResponse<String> response = post(INIT, null, token);
        assertEquals(200, response.statusCode());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("result");
        assertEquals("2025-06-18", result.get("protocolVersion").getAsString());
        assertEquals("kdbStudio", result.getAsJsonObject("serverInfo").get("name").getAsString());
        assertTrue(result.getAsJsonObject("capabilities").has("tools"));
        String sessionId = response.headers().firstValue(McpServer.SESSION_HEADER).orElse(null);
        assertNotNull(sessionId);
        return sessionId;
    }

    @Test
    public void fullFlow() throws Exception {
        start(McpTokenMode.NONE);
        String session = initialize(null);

        HttpResponse<String> response = post("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", session, null);
        assertEquals(202, response.statusCode());

        response = post(rpc(2, "tools/list", "{}"), session, null);
        JsonArray tools = JsonParser.parseString(response.body()).getAsJsonObject()
                .getAsJsonObject("result").getAsJsonArray("tools");
        assertEquals(2, tools.size());
        assertEquals("echo", tools.get(0).getAsJsonObject().get("name").getAsString());

        response = post(rpc(3, "tools/call", "{\"name\":\"echo\",\"arguments\":{\"text\":\"hi\"}}"), session, null);
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("result");
        assertFalse(result.get("isError").getAsBoolean());
        assertEquals("tester:hi", result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString());

        response = post(rpc(4, "tools/call", "{\"name\":\"fail\",\"arguments\":{}}"), session, null);
        result = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("result");
        assertTrue(result.get("isError").getAsBoolean());
        assertEquals("boom", result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString());

        response = post(rpc(5, "tools/call", "{\"name\":\"nope\"}"), session, null);
        assertEquals(McpServer.INVALID_PARAMS, JsonParser.parseString(response.body()).getAsJsonObject()
                .getAsJsonObject("error").get("code").getAsInt());

        response = post(rpc(6, "unknown/method", "{}"), session, null);
        assertEquals(McpServer.METHOD_NOT_FOUND, JsonParser.parseString(response.body()).getAsJsonObject()
                .getAsJsonObject("error").get("code").getAsInt());

        response = post(rpc(7, "ping", "{}"), session, null);
        assertEquals(200, response.statusCode());

        assertEquals(1, server.getSessions().getActive().size());
        assertEquals("tester 1.0", server.getSessions().getActive().get(0).getDisplayName());

        HttpResponse<String> delete = client.send(request().header(McpServer.SESSION_HEADER, session).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, delete.statusCode());
        assertTrue(server.getSessions().getActive().isEmpty());

        response = post(rpc(8, "ping", "{}"), session, null);
        assertEquals(404, response.statusCode());
    }

    @Test
    public void parseErrorAndGet() throws Exception {
        start(McpTokenMode.NONE);
        HttpResponse<String> response = post("{not json", null, null);
        assertEquals(400, response.statusCode());
        assertEquals(McpServer.PARSE_ERROR, JsonParser.parseString(response.body()).getAsJsonObject()
                .getAsJsonObject("error").get("code").getAsInt());

        response = client.send(request().GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    @Test
    public void tokenRequired() throws Exception {
        start(McpTokenMode.ALL);
        assertEquals(401, post(INIT, null, null).statusCode());
        assertEquals(401, post(INIT, null, "wrong").statusCode());
        initialize("tok");
    }

    @Test
    public void tokenForRemoteOnly() throws Exception {
        start(McpTokenMode.REMOTE_ONLY);
        initialize(null);
    }

    @Test
    public void foreignOriginRejected() throws Exception {
        start(McpTokenMode.NONE);
        HttpResponse<String> response = client.send(request().header("Origin", "https://evil.example.com")
                .POST(HttpRequest.BodyPublishers.ofString(INIT)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());

        response = client.send(request().header("Origin", "http://localhost:3000")
                .POST(HttpRequest.BodyPublishers.ofString(INIT)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    public void maskPasswords() {
        JsonObject json = JsonParser.parseString("{\"connection\":{\"host\":\"h\",\"password\":\"secret\"}}").getAsJsonObject();
        assertEquals("{\"connection\":{\"host\":\"h\",\"password\":\"***\"}}", McpLog.maskPasswords(json).toString());
    }
}
