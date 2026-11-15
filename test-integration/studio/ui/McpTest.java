package studio.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.kdb.*;
import studio.kdb.config.McpTokenMode;
import studio.mcp.McpService;
import studio.utils.QConnection;

import javax.swing.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class McpTest extends StudioTest {

    private static final String DIALOG_TITLE = "AI Agent Query Confirmation";

    private final HttpClient client = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();
    private final AtomicInteger requestId = new AtomicInteger(1);
    private String sessionId;

    @BeforeClass
    public static void mock() {
        MockQSession.mock();
    }

    private static Server server(String folder, String name) {
        ServerTreeNode parent = new ServerTreeNode().add(folder);
        return new Server(name, new QConnection("host", 5000, "", "", false), studio.core.DefaultAuthenticationMechanism.NAME, null, parent);
    }

    @Before
    public void startMcp() throws Exception {
        Config config = Config.getInstance();
        try {
            config.getServerConfig().addServer(server("permitted", "srv"));
            config.getServerConfig().addServer(server("other", "srv2"));
        } catch (IllegalArgumentException e) {
            // already added by a previous test
        }
        config.setInt(Config.MCP_PORT, 0);
        config.setEnum(Config.MCP_TOKEN_MODE, McpTokenMode.NONE);
        config.setStringArray(Config.MCP_ALLOWED_SERVERS, List.of("folder:permitted"));
        config.setBoolean(Config.MCP_ENABLED, true);
        McpService.getInstance().init();
        McpService.getInstance().applyConfig();
        assertEquals(McpService.State.RUNNING, McpService.getInstance().getState());

        HttpResponse<String> response = post("initialize",
                "{\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test-agent\",\"version\":\"1\"}}");
        sessionId = response.headers().firstValue("Mcp-Session-Id").orElseThrow();
    }

    @After
    public void stopMcp() {
        Config.getInstance().setBoolean(Config.MCP_ENABLED, false);
        McpService.getInstance().applyConfig();
    }

    private HttpResponse<String> post(String method, String params) throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":" + requestId.getAndIncrement() + ",\"method\":\"" + method +
                "\",\"params\":" + params + "}";
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + McpService.getInstance().getPort() + "/mcp"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (sessionId != null) builder.header("Mcp-Session-Id", sessionId);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    // returns {isError, text}
    private Object[] call(String tool, String arguments) throws Exception {
        HttpResponse<String> response = post("tools/call", "{\"name\":\"" + tool + "\",\"arguments\":" + arguments + "}");
        assertEquals(200, response.statusCode());
        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("result");
        String text = result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
        return new Object[] {result.get("isError").getAsBoolean(), text};
    }

    private String callOk(String tool, String arguments) throws Exception {
        Object[] result = call(tool, arguments);
        assertFalse("Tool " + tool + " failed: " + result[1], (Boolean) result[0]);
        return (String) result[1];
    }

    private CompletableFuture<Object[]> callAsync(String tool, String arguments) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call(tool, arguments);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private DialogFixture confirmationDialog() {
        return WindowFinder.findDialog(new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                return DIALOG_TITLE.equals(dialog.getTitle()) && dialog.isShowing();
            }
        }).withTimeout(10, TimeUnit.SECONDS).using(robot());
    }

    // Robot clicks are occasionally lost under Xvfb while the modal dialog is opening; press the button on the EDT
    private void clickInDialog(String buttonText) {
        DialogFixture dialog = confirmationDialog();
        JButton button = dialog.button(JButtonMatcher.withText(buttonText)).target();
        org.assertj.swing.edt.GuiActionRunner.execute(() -> button.doClick());
    }

    private String newTab(String text) throws Exception {
        String json = callOk("new_tab", "{\"text\":\"" + text + "\",\"server\":\"permitted/srv\"}");
        return JsonParser.parseString(json).getAsJsonObject().get("id").getAsString();
    }

    @Test
    public void testListServers() throws Exception {
        JsonObject json = JsonParser.parseString(callOk("list_servers", "{}")).getAsJsonObject();
        JsonArray servers = json.getAsJsonArray("servers");
        assertEquals(1, servers.size());
        JsonObject server = servers.get(0).getAsJsonObject();
        assertEquals("permitted/srv", server.get("name").getAsString());
        assertFalse(server.has("password"));
        assertFalse(server.has("user"));
    }

    @Test
    public void testTabs() throws Exception {
        String id = newTab("1+1");
        String tabs = callOk("list_tabs", "{}");
        assertTrue(tabs, tabs.contains("\"" + id + "\""));

        callOk("edit_tab", "{\"tab\":\"" + id + "\",\"find\":\"1+1\",\"text\":\"til 3\"}");
        JsonObject tab = JsonParser.parseString(callOk("get_tab", "{\"tab\":\"" + id + "\"}")).getAsJsonObject();
        assertEquals("til 3", tab.get("text").getAsString());
        assertEquals("permitted/srv", tab.get("server").getAsString());

        callOk("edit_tab", "{\"tab\":\"" + id + "\",\"start\":0,\"text\":\"x:\"}");
        tab = JsonParser.parseString(callOk("get_tab", "{\"tab\":\"" + id + "\"}")).getAsJsonObject();
        assertEquals("x:til 3", tab.get("text").getAsString());

        Object[] result = call("set_tab_server", "{\"tab\":\"" + id + "\",\"server\":\"other/srv2\"}");
        assertTrue((Boolean) result[0]);
        assertTrue((String) result[1], ((String) result[1]).contains("not permitted"));
    }

    @Test
    public void testExecuteInTabConfirmed() throws Exception {
        String id = newTab("");
        CompletableFuture<Object[]> future = callAsync("execute_query", "{\"tab\":\"" + id + "\",\"query\":\"til 3\"}");
        clickInDialog("Run");

        Object[] result = future.get(10, TimeUnit.SECONDS);
        assertFalse((String) result[1], (Boolean) result[0]);
        // the mock connection echoes the query
        assertTrue((String) result[1], ((String) result[1]).contains("\"til 3\""));
        assertTrue((String) result[1], ((String) result[1]).contains("Result id: r"));

        String results = callOk("list_results", "{}");
        assertTrue(results, results.contains("til 3"));
    }

    @Test
    public void testExecuteDenied() throws Exception {
        CompletableFuture<Object[]> future = callAsync("execute_query", "{\"server\":\"permitted/srv\",\"query\":\"til 3\"}");
        clickInDialog("Deny");
        Object[] result = future.get(10, TimeUnit.SECONDS);
        assertTrue((Boolean) result[0]);
        assertTrue((String) result[1], ((String) result[1]).contains("denied"));
    }

    @Test
    public void testGrant() throws Exception {
        McpService.getInstance().getGrants().revokeAll();
        CompletableFuture<Object[]> future = callAsync("execute_query", "{\"server\":\"permitted/srv\",\"query\":\"1\"}");
        clickInDialog("Run and allow for");
        Object[] result = future.get(10, TimeUnit.SECONDS);
        assertFalse((String) result[1], (Boolean) result[0]);
        assertEquals(1, McpService.getInstance().getGrants().getActive().size());

        // no confirmation now
        String text = callOk("execute_query", "{\"server\":\"permitted/srv\",\"query\":\"2\",\"format\":\"json\"}");
        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        assertEquals("permitted/srv (host:5000)", json.get("server").getAsString());
        assertEquals(2, McpService.getInstance().getGrants().getActive().get(0).getQueryCount());
        McpService.getInstance().getGrants().revokeAll();
    }

    @Test
    public void testNotPermitted() throws Exception {
        Object[] result = call("execute_query", "{\"server\":\"other/srv2\",\"query\":\"1\"}");
        assertTrue((Boolean) result[0]);
        assertTrue((String) result[1], ((String) result[1]).contains("not permitted"));

        result = call("execute_query", "{\"connection\":{\"host\":\"h\",\"port\":1},\"query\":\"1\"}");
        assertTrue((Boolean) result[0]);
        assertTrue((String) result[1], ((String) result[1]).contains("Ad-hoc connections are not allowed"));
    }

    @Test
    public void testPermissionsCleanup() throws Exception {
        Config config = Config.getInstance();
        Server extra = server("temp", "x");
        config.getServerConfig().addServer(extra);
        config.setStringArray(Config.MCP_ALLOWED_SERVERS, List.of("folder:permitted", "server:temp/x"));
        config.getServerConfig().removeServer(config.getServerConfig().getServer("temp/x"));
        assertEquals(List.of("folder:permitted"), config.getStringArray(Config.MCP_ALLOWED_SERVERS));
    }
}
