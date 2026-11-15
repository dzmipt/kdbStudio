package studio.mcp;

/**
 * Who is calling a tool
 */
public class McpCallContext {
    private final McpSessions.Session session; // can be null if the client doesn't use sessions
    private final String remoteAddress;

    public McpCallContext(McpSessions.Session session, String remoteAddress) {
        this.session = session;
        this.remoteAddress = remoteAddress;
    }

    public McpSessions.Session getSession() {
        return session;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getClientName() {
        if (session == null || session.getClientName() == null) return "AI agent";
        return session.getClientName();
    }
}
