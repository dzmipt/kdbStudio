package studio.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AgentConfigTemplatesTest {

    private static final String URL = "http://127.0.0.1:7878/mcp";

    @Test
    public void allClientsRender() {
        for (AgentConfigTemplates.Client client: AgentConfigTemplates.Client.values()) {
            String withToken = AgentConfigTemplates.render(client, URL, "TOKEN");
            String withoutToken = AgentConfigTemplates.render(client, URL, null);
            assertTrue(withToken.contains(URL), client.name());
            assertTrue(withToken.contains("TOKEN"), client.name());
            assertFalse(withoutToken.contains("TOKEN"), client.name());
            if (withToken.startsWith("{")) {
                JsonParser.parseString(withToken);
            }
        }
    }

    @Test
    public void claudeCode() {
        assertEquals("claude mcp add --transport http kdbstudio " + URL + " --header \"Authorization: Bearer T\"",
                AgentConfigTemplates.render(AgentConfigTemplates.Client.CLAUDE_CODE, URL, "T"));
    }

    @Test
    public void vscode() {
        JsonObject json = JsonParser.parseString(
                AgentConfigTemplates.render(AgentConfigTemplates.Client.COPILOT_VSCODE, URL, "T")).getAsJsonObject();
        JsonObject server = json.getAsJsonObject("servers").getAsJsonObject("kdbstudio");
        assertEquals("http", server.get("type").getAsString());
        assertEquals("Bearer T", server.getAsJsonObject("headers").get("Authorization").getAsString());
    }

    @Test
    public void url() {
        assertEquals("http://127.0.0.1:7878/mcp", AgentConfigTemplates.url("127.0.0.1", 7878));
        assertEquals("http://[::1]:1/mcp", AgentConfigTemplates.url("::1", 1));
    }
}
