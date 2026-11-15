package studio.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Configuration snippets for adding the Studio MCP server to various AI clients.
 * Client configuration formats change over time; keep all of them in this class.
 */
public class AgentConfigTemplates {

    public static final String SERVER_NAME = "kdbstudio";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public enum Client {
        CLAUDE_CODE("Claude Code",
                "Run in a terminal (add --scope user to make it available in all projects):"),
        COPILOT_VSCODE("GitHub Copilot - VS Code",
                "Add to .vscode/mcp.json in the workspace, or to the user configuration " +
                "(command 'MCP: Open User Configuration'). Use Copilot Chat in Agent mode."),
        COPILOT_JETBRAINS("GitHub Copilot - JetBrains IDEs",
                "Copilot Chat > Agent mode > Configure tools (MCP), then add to mcp.json:"),
        COPILOT_VISUAL_STUDIO("GitHub Copilot - Visual Studio",
                "Add to .mcp.json in the solution folder or to %USERPROFILE%\\.mcp.json. Use Copilot Chat in Agent mode."),
        COPILOT_CLI("GitHub Copilot CLI",
                "Add to ~/.copilot/mcp-config.json (or use the /mcp add command inside copilot):"),
        CURSOR("Cursor",
                "Add to ~/.cursor/mcp.json (all projects) or .cursor/mcp.json in the project:"),
        WINDSURF("Windsurf",
                "Add to ~/.codeium/windsurf/mcp_config.json:"),
        GEMINI_CLI("Gemini CLI",
                "Add to ~/.gemini/settings.json (or .gemini/settings.json in the project):"),
        CODEX_CLI("OpenAI Codex CLI",
                "Add to ~/.codex/config.toml:"),
        STDIO_BRIDGE("Claude Desktop / other stdio-only clients",
                "Clients which can only start local processes can connect through the mcp-remote bridge (requires Node.js). " +
                "For Claude Desktop add to claude_desktop_config.json (Settings > Developer > Edit Config):");

        private final String displayName;
        private final String instructions;

        Client(String displayName, String instructions) {
            this.displayName = displayName;
            this.instructions = instructions;
        }

        public String getInstructions() {
            return instructions;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static String url(String host, int port) {
        if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
        return "http://" + host + ":" + port + McpServer.PATH;
    }

    private static JsonObject headers(String token) {
        JsonObject headers = new JsonObject();
        headers.addProperty("Authorization", "Bearer " + token);
        return headers;
    }

    private static String wrap(String rootKey, JsonObject server) {
        JsonObject servers = new JsonObject();
        servers.add(SERVER_NAME, server);
        JsonObject root = new JsonObject();
        root.add(rootKey, servers);
        return gson.toJson(root);
    }

    /**
     * @param token null if the token is not required
     */
    public static String render(Client client, String url, String token) {
        JsonObject server = new JsonObject();
        switch (client) {
            case CLAUDE_CODE:
                return "claude mcp add --transport http " + SERVER_NAME + " " + url +
                        (token == null ? "" : " --header \"Authorization: Bearer " + token + "\"");
            case COPILOT_VSCODE:
            case COPILOT_VISUAL_STUDIO:
                server.addProperty("type", "http");
                server.addProperty("url", url);
                if (token != null) server.add("headers", headers(token));
                return wrap("servers", server);
            case COPILOT_JETBRAINS:
                server.addProperty("url", url);
                if (token != null) {
                    JsonObject requestInit = new JsonObject();
                    requestInit.add("headers", headers(token));
                    server.add("requestInit", requestInit);
                }
                return wrap("servers", server);
            case COPILOT_CLI:
                server.addProperty("type", "http");
                server.addProperty("url", url);
                if (token != null) server.add("headers", headers(token));
                JsonArray tools = new JsonArray();
                tools.add("*");
                server.add("tools", tools);
                return wrap("mcpServers", server);
            case CURSOR:
                server.addProperty("url", url);
                if (token != null) server.add("headers", headers(token));
                return wrap("mcpServers", server);
            case WINDSURF:
                server.addProperty("serverUrl", url);
                if (token != null) server.add("headers", headers(token));
                return wrap("mcpServers", server);
            case GEMINI_CLI:
                server.addProperty("httpUrl", url);
                if (token != null) server.add("headers", headers(token));
                return wrap("mcpServers", server);
            case CODEX_CLI:
                return "[mcp_servers." + SERVER_NAME + "]\n" +
                        "url = \"" + url + "\"\n" +
                        (token == null ? "" : "http_headers = { \"Authorization\" = \"Bearer " + token + "\" }\n");
            case STDIO_BRIDGE:
                server.addProperty("command", "npx");
                JsonArray args = new JsonArray();
                args.add("-y");
                args.add("mcp-remote");
                args.add(url);
                if (token != null) {
                    args.add("--header");
                    args.add("Authorization:${KDBSTUDIO_AUTH}");
                    JsonObject env = new JsonObject();
                    env.addProperty("KDBSTUDIO_AUTH", "Bearer " + token);
                    server.add("args", args);
                    server.add("env", env);
                } else {
                    server.add("args", args);
                }
                if (!url.startsWith("https")) {
                    args.add("--allow-http");
                }
                return wrap("mcpServers", server);
            default:
                throw new IllegalArgumentException("Unknown client " + client);
        }
    }
}
