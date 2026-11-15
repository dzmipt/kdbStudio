package studio.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Activity log of AI agents (mcp.log)
 */
public class McpLog {

    private static final Logger log = LogManager.getLogger("MCP");

    public static void info(String message, Object... params) {
        log.info(message, params);
    }

    public static void warn(String message, Object... params) {
        log.warn(message, params);
    }

    public static void error(String message, Throwable e) {
        log.error(message, e);
    }

    /**
     * A copy of the json where values of properties named password are masked
     */
    public static JsonElement maskPasswords(JsonElement json) {
        if (json == null) return null;
        if (json.isJsonArray()) {
            com.google.gson.JsonArray result = new com.google.gson.JsonArray();
            for (JsonElement e: json.getAsJsonArray()) result.add(maskPasswords(e));
            return result;
        }
        if (!json.isJsonObject()) return json;
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry: json.getAsJsonObject().entrySet()) {
            if (entry.getKey().toLowerCase().contains("password")) {
                result.addProperty(entry.getKey(), "***");
            } else {
                result.add(entry.getKey(), maskPasswords(entry.getValue()));
            }
        }
        return result;
    }
}
