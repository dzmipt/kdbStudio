package studio.kdb.config;

import com.google.gson.JsonObject;

public interface JsonConverter {

    /**
     * Converts the json to the latest version.
     * @param json the config to convert
     * @param defaults default values (including the default config from the plugin folder)
     * @return true if the json was changed
     */
    boolean convert(JsonObject json, JsonObject defaults);
}
