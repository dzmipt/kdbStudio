package studio.kdb.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;

import java.util.HashSet;
import java.util.Set;

public class ConfigConverter implements JsonConverter{

    private final static Logger log = LogManager.getLogger();

    @Override
    public boolean convert(JsonObject json) {
        ConfigVersion version = getVersion(json);
        if (version == ConfigVersion.LAST) return false;

        try {
            convertTo21(json);
        } catch (Exception e) {
            log.error("Error during conversion", e);
        }

        json.add(Config.CONFIG_VERSION, ConfigType.ENUM.toJson(ConfigVersion.LAST));
        return true;
    }

    private ConfigVersion getVersion(JsonObject json) {
        JsonElement jsonElement = json.get(Config.CONFIG_VERSION);
        if (jsonElement == null) {
            if (json.isEmpty()) return ConfigVersion.V_NO;
            else return ConfigVersion.V2_0;
        }

        try {
            return (ConfigVersion)ConfigType.ENUM.fromJson(jsonElement, ConfigVersion.V_NO);
        } catch (Exception e) {
            log.debug("Error in parsing version", e);
            return ConfigVersion.V_NO;
        }
    }

    void convertTo21(JsonObject json) {
        json = json.getAsJsonObject("chartColorSets");
        if (json == null) return;

        json = json.getAsJsonObject("set");
        Set<String> keys = new HashSet<>(json.keySet());
        for (String key: keys) {
            JsonElement colors = json.get(key);
            if (! colors.isJsonArray()) continue;

            JsonObject newValue = new JsonObject();
            newValue.add("background",
                    ConfigType.COLOR.toJson(ColorSchema.DEFAULT.getBackground()));
            newValue.add("grid",
                    ConfigType.COLOR.toJson(ColorSchema.DEFAULT.getGrid()));
            newValue.add("colors", colors);
            json.add(key, newValue);
        }
    }

}
