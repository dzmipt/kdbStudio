package studio.kdb.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.ui.Util;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class ConfigConverter implements JsonConverter{

    private final static Logger log = LogManager.getLogger();

    @Override
    public boolean convert(JsonObject json) {
        ConfigVersion version = getVersion(json);
        if (version == ConfigVersion.LAST) return false;

        log.info("Detected old version: {}", version);
        try {
            convertTo21(json);
            convertTo22(json);
            convertTo23(json);
            log.info("Successfully converted to version: {}", ConfigVersion.LAST);
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

    private void convertTo21(JsonObject json) {
        if (ConfigVersion.V2_1.compareTo(getVersion(json))<=0 ) return;

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

    private void convertTo22(JsonObject json) {
        if (ConfigVersion.V2_2.compareTo(getVersion(json))<=0 ) return;

        JsonElement bgColor = json.get("backgroundColor");

        if (bgColor != null) {
            JsonObject editorColors = new JsonObject();
            editorColors.add(EditorColorToken.BACKGROUND.toString().toLowerCase(), bgColor);
            json.add("editorColors", editorColors);

            json.remove("backgroundColor");
        }
    }

    private void convertTo23(JsonObject json) {
        if (ConfigVersion.V2_3.compareTo(getVersion(json))<=0 ) return;

        JsonObject jsonTokenColor = json.getAsJsonObject("tokenColors");
        if (jsonTokenColor == null) return;

        JsonObject jsonTokens = new JsonObject();
        for(ColorToken token: ColorToken.values()) {
            String name = token.name().toLowerCase();
            if (!jsonTokenColor.has(name)) continue;

            String colorValue = jsonTokenColor.get(name).getAsString();
            Color color = Util.stringToColor(colorValue);
            jsonTokens.addProperty(name, token.getDefaultStyle().derive(color).toString());
        }

        json.add(Config.TOKEN_STYLE_CONFIG, jsonTokens);
        json.remove("tokenColors");
    }

}
