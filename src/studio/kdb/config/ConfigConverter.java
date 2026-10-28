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

    public final static String CUSTOM_STYLE_SCHEMA = "Custom";

    private boolean convertedTo24 = false;
    private boolean customStyleSchemaCreated = false;
    private Color legacyBackgroundColor = Color.WHITE;

    // true if the config was converted from a version before 2.4 (style schemas)
    public boolean isConvertedTo24() {
        return convertedTo24;
    }

    // true if the "Custom" style schema was created from the user's style settings
    public boolean isCustomStyleSchemaCreated() {
        return customStyleSchemaCreated;
    }

    // The editor background color before the conversion to 2.4
    public Color getLegacyBackgroundColor() {
        return legacyBackgroundColor;
    }

    @Override
    public boolean convert(JsonObject json) {
        ConfigVersion version = getVersion(json);
        if (version == ConfigVersion.LAST) return false;

        log.info("Detected old version: {}", version);
        try {
            convertTo21(json);
            convertTo22(json);
            convertTo23(json);
            convertTo24(json);
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

        json.add(LEGACY_TOKENS, jsonTokens);
        json.remove("tokenColors");
    }

    public final static String LEGACY_EDITOR_COLORS = "editorColors";
    public final static String LEGACY_TOKENS = "tokens";
    public final static String LEGACY_GRID_COLORS = "gridColors";

    // Style settings (editorColors, tokens, gridColors) are moved into the "Custom" style schema
    private void convertTo24(JsonObject json) {
        if (ConfigVersion.V2_4.compareTo(getVersion(json))<=0 ) return;
        convertedTo24 = true;

        StyleSchema legacyDefault = new StyleSchema(null,
                ColorMap.DEFAULT_EDITOR_COLORS, TokenStyleMap.DEFAULT, GridColorConfig.DEFAULT);

        ColorMap editorColors = legacyDefault.getEditorColors();
        JsonElement jsonEditorColors = json.remove(LEGACY_EDITOR_COLORS);
        if (jsonEditorColors != null) {
            try {
                editorColors = (ColorMap) ConfigType.COLOR_MAP.fromJson(jsonEditorColors, ColorMap.DEFAULT_EDITOR_COLORS);
            } catch (Exception e) {
                log.error("Error parsing old editorColors", e);
            }
        }

        TokenStyleMap tokens = legacyDefault.getTokens();
        JsonElement jsonTokens = json.remove(LEGACY_TOKENS);
        if (jsonTokens != null) {
            try {
                tokens = (TokenStyleMap) ConfigType.TOKEN_STYLE_MAP.fromJson(jsonTokens, TokenStyleMap.DEFAULT);
            } catch (Exception e) {
                log.error("Error parsing old tokens", e);
            }
        }

        GridColorConfig gridColors = legacyDefault.getGridColors();
        JsonElement jsonGridColors = json.remove(LEGACY_GRID_COLORS);
        if (jsonGridColors != null) {
            try {
                gridColors = (GridColorConfig) ConfigType.GRID_COLOR_CONFIG.fromJson(jsonGridColors, GridColorConfig.DEFAULT);
            } catch (Exception e) {
                log.error("Error parsing old gridColors", e);
            }
        }

        legacyBackgroundColor = editorColors.get(EditorColorToken.BACKGROUND);

        StyleSchema schema = new StyleSchema(null, editorColors, tokens, gridColors);
        if (schema.equals(legacyDefault)) {
            log.info("Style settings were not customized");
            return;
        }

        schema = schema.newBasedOn(StyleSchemas.LIGHT);
        JsonObject jsonSchemas = json.getAsJsonObject(Config.STYLE_SCHEMAS);
        if (jsonSchemas == null) {
            jsonSchemas = new JsonObject();
            json.add(Config.STYLE_SCHEMAS, jsonSchemas);
        }
        jsonSchemas.add(CUSTOM_STYLE_SCHEMA, schema.toJson());
        customStyleSchemaCreated = true;
        log.info("Style settings are converted to the style schema {}", CUSTOM_STYLE_SCHEMA);
    }

}
