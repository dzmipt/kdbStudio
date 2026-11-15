package studio.kdb.config;

// Which network interfaces the MCP server listens on
public enum McpAccess {
    LOCAL("this machine only"),
    REMOTE("any machine");

    private final String description;

    McpAccess(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
