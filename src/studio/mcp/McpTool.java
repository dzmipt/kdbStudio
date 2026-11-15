package studio.mcp;

import com.google.gson.JsonObject;

/**
 * A tool exposed to agents
 */
public class McpTool {

    @FunctionalInterface
    public interface Handler {
        /**
         * @return text content of the result
         * @throws McpToolException to report an error to the agent
         */
        String call(JsonObject arguments, McpCallContext context) throws Exception;
    }

    private final String name;
    private final String description;
    private final JsonObject inputSchema;
    private final boolean readOnly;
    private final Handler handler;

    public McpTool(String name, String description, JsonObject inputSchema, boolean readOnly, Handler handler) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.readOnly = readOnly;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public JsonObject getInputSchema() {
        return inputSchema;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public Handler getHandler() {
        return handler;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("description", description);
        json.add("inputSchema", inputSchema);
        JsonObject annotations = new JsonObject();
        annotations.addProperty("readOnlyHint", readOnly);
        annotations.addProperty("destructiveHint", false);
        json.add("annotations", annotations);
        return json;
    }
}
