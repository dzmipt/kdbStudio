package studio.mcp;

import studio.kdb.Config;
import studio.kdb.Lm;
import studio.kdb.ServerTreeNode;
import studio.kdb.config.McpAccess;
import studio.kdb.config.McpTokenMode;

import javax.swing.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns the MCP server lifecycle and the state shared between the server and the UI.
 */
public class McpService {

    public enum State {DISABLED, RUNNING, FAILED}

    public interface Listener {
        // Always called on the EDT
        void mcpStateChanged();
    }

    private static final McpService instance = new McpService();

    private final McpGrants grants = new McpGrants();
    private final McpSessions sessions = new McpSessions();
    private final McpRunningQueries runningQueries = new McpRunningQueries();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private McpServer server = null;
    private StudioTools tools = null;
    private State state = State.DISABLED;
    private String error = null;
    private boolean initialized = false;

    public static McpService getInstance() {
        return instance;
    }

    private McpService() {
        grants.addListener(this::fire);
        sessions.addListener(this::fire);
        runningQueries.addListener(this::fire);
    }

    public McpGrants getGrants() {
        return grants;
    }

    public McpRunningQueries getRunningQueries() {
        return runningQueries;
    }

    public McpSessions getSessions() {
        return sessions;
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized String getError() {
        return error;
    }

    public synchronized int getPort() {
        return server == null ? Config.getInstance().getInt(Config.MCP_PORT) : server.getPort();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fire() {
        if (SwingUtilities.isEventDispatchThread()) {
            for (Listener listener : listeners) listener.mcpStateChanged();
        } else {
            SwingUtilities.invokeLater(this::fire);
        }
    }

    public static McpPermissions getPermissions() {
        return new McpPermissions(Config.getInstance().getStringArray(Config.MCP_ALLOWED_SERVERS));
    }

    public static void setPermissions(McpPermissions permissions) {
        Config.getInstance().setStringArray(Config.MCP_ALLOWED_SERVERS, permissions.toList());
    }

    public static String generateToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // The token from settings; generated if it doesn't exist yet
    public static String getToken() {
        Config config = Config.getInstance();
        String token = config.getString(Config.MCP_TOKEN);
        if (token.isEmpty()) {
            token = generateToken();
            config.setString(Config.MCP_TOKEN, token);
        }
        return token;
    }

    private void cleanupPermissions(ServerTreeNode root) {
        McpPermissions permissions = getPermissions();
        McpPermissions cleaned = permissions.cleanup(root);
        if (cleaned != permissions) {
            McpLog.info("Permissions of removed servers/folders are cleaned up: {} -> {}", permissions, cleaned);
            setPermissions(cleaned);
        }
    }

    /**
     * Called at startup: subscribes for server tree changes and starts the server if enabled
     */
    public synchronized void init() {
        if (initialized) return;
        initialized = true;
        Config config = Config.getInstance();
        config.getServerConfig().addTreeListener(this::cleanupPermissions);
        cleanupPermissions(config.getServerTree());
        applyConfig();
    }

    private static McpServer.Settings getSettingsFromConfig() {
        Config config = Config.getInstance();
        return new McpServer.Settings(
                config.getInt(Config.MCP_PORT),
                config.getEnum(Config.MCP_ACCESS) == McpAccess.REMOTE,
                config.getEnum(Config.MCP_TOKEN_MODE),
                getToken());
    }

    /**
     * Starts, stops or restarts the server according to the settings
     */
    public void applyConfig() {
        boolean enabled = Config.getInstance().getBoolean(Config.MCP_ENABLED);
        synchronized (this) {
            McpServer.Settings settings = enabled ? getSettingsFromConfig() : null;
            if (enabled && server != null && server.getSettings().equals(settings)) return;
            if (!enabled && server == null && state == State.DISABLED) return;

            stopInternal();
            grants.revokeAll();
            if (enabled) {
                startInternal(settings);
            }
        }
        fire();
    }

    private void startInternal(McpServer.Settings settings) {
        StudioTools newTools = new StudioTools(this);
        McpServer newServer = new McpServer(settings, "kdbStudio", Lm.version, StudioTools.INSTRUCTIONS,
                newTools.getTools(), sessions);
        try {
            newServer.start();
            server = newServer;
            tools = newTools;
            state = State.RUNNING;
            error = null;
        } catch (IOException e) {
            McpLog.error("Failed to start MCP server on port " + settings.getPort(), e);
            state = State.FAILED;
            error = "Failed to start on port " + settings.getPort() + ": " + e.getMessage();
        }
    }

    private void stopInternal() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (tools != null) {
            tools.close();
            tools = null;
        }
        state = State.DISABLED;
        error = null;
    }

    public void shutdown() {
        synchronized (this) {
            stopInternal();
        }
    }

    public static McpTokenMode getTokenMode() {
        return Config.getInstance().getEnum(Config.MCP_TOKEN_MODE);
    }
}
