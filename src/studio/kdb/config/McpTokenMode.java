package studio.kdb.config;

// When the MCP server requires the access token
public enum McpTokenMode {
    NONE("not required"),
    REMOTE_ONLY("required for remote connections"),
    ALL("required for all connections");

    private final String description;

    McpTokenMode(String description) {
        this.description = description;
    }

    public boolean isRequired(boolean remote) {
        return this == ALL || (this == REMOTE_ONLY && remote);
    }

    @Override
    public String toString() {
        return description;
    }
}
