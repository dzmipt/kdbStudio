package studio.mcp;

/**
 * An error reported to the agent as a tool result with isError=true
 */
public class McpToolException extends Exception {
    public McpToolException(String message) {
        super(message);
    }
}
