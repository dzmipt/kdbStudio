package studio.kdb.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * A style schema as it is defined in the config. Values may be missing: they are taken from another schema
 * when the definition is resolved into a {@link StyleSchema}. Immutable.
 */
public class StyleSchemaDefinition {

    private final static Logger log = LogManager.getLogger();

    private final static String BASED_ON = "basedOn";
    private final static String EDITOR_COLORS = "editorColors";
    private final static String TOKENS = "tokens";
    private final static String GRID_COLORS = "gridColors";
    private final static String FOREGROUND = "foreground";
    private final static String BACKGROUND = "background";

    private final String basedOn;
    private final Map<EditorColorToken, Color> editorColors;
    private final Map<ColorToken, TokenStyle> tokens;
    // true means grid colors derived from Look and Feel (GridColorConfig.DEFAULT)
    private final boolean gridColorsFromLookAndFeel;
    private final Map<GridColorToken, Color> gridForeground;
    private final Map<GridColorToken, Color> gridBackground;

    private StyleSchemaDefinition(String basedOn,
                                  Map<EditorColorToken, Color> editorColors,
                                  Map<ColorToken, TokenStyle> tokens,
                                  boolean gridColorsFromLookAndFeel,
                                  Map<GridColorToken, Color> gridForeground,
                                  Map<GridColorToken, Color> gridBackground) {
        this.basedOn = basedOn;
        this.editorColors = Collections.unmodifiableMap(editorColors);
        this.tokens = Collections.unmodifiableMap(tokens);
        this.gridColorsFromLookAndFeel = gridColorsFromLookAndFeel;
        this.gridForeground = Collections.unmodifiableMap(gridForeground);
        this.gridBackground = Collections.unmodifiableMap(gridBackground);
    }

    /**
     * The complete definition of the schema
     */
    public static StyleSchemaDefinition of(StyleSchema schema) {
        Map<EditorColorToken, Color> editorColors = new EnumMap<>(EditorColorToken.class);
        for (EditorColorToken token: EditorColorToken.values()) {
            editorColors.put(token, schema.getEditorColors().get(token));
        }

        Map<ColorToken, TokenStyle> tokens = new EnumMap<>(ColorToken.class);
        for (ColorToken token: ColorToken.values()) {
            tokens.put(token, schema.getTokens().get(token));
        }

        GridColorConfig grid = schema.getGridColors();
        Map<GridColorToken, Color> fg = new EnumMap<>(GridColorToken.class);
        Map<GridColorToken, Color> bg = new EnumMap<>(GridColorToken.class);
        if (! grid.isDefaultConfig()) {
            for (GridColorToken token: GridColorToken.FG) fg.put(token, grid.getColor(token, true));
            for (GridColorToken token: GridColorToken.BG) bg.put(token, grid.getColor(token, false));
        }

        return new StyleSchemaDefinition(schema.getBasedOn(), editorColors, tokens, grid.isDefaultConfig(), fg, bg);
    }

    /**
     * The name of the built-in schema this schema is based on; null if it is not defined.
     */
    public String getBasedOn() {
        return basedOn;
    }

    /**
     * Builds the schema. Values which are not defined are taken from the fallback.
     * @throws IllegalArgumentException if a value is not defined and fallback is null
     */
    public StyleSchema resolve(StyleSchema fallback) {
        ColorMap editorColorMap = new ColorMap();
        for (EditorColorToken token: EditorColorToken.values()) {
            Color color = editorColors.get(token);
            if (color == null) color = require(fallback, "editor color " + token).getEditorColors().get(token);
            editorColorMap.put(token, color);
        }

        TokenStyleMap tokenMap = new TokenStyleMap();
        for (ColorToken token: ColorToken.values()) {
            TokenStyle style = tokens.get(token);
            if (style == null) style = require(fallback, "token " + token).getTokens().get(token);
            tokenMap.set(token, style);
        }

        return new StyleSchema(basedOn, editorColorMap, tokenMap, resolveGridColors(fallback));
    }

    private GridColorConfig resolveGridColors(StyleSchema fallback) {
        if (gridColorsFromLookAndFeel) return GridColorConfig.DEFAULT;
        if (gridForeground.isEmpty() && gridBackground.isEmpty()) {
            return require(fallback, "grid colors").getGridColors();
        }

        GridColorConfig fallbackGrid = fallback == null ? null : fallback.getGridColors();
        ColorMap fg = new ColorMap();
        ColorMap bg = new ColorMap();
        for (GridColorToken token: GridColorToken.FG) {
            Color color = gridForeground.get(token);
            if (color == null) color = getGridColor(fallbackGrid, token, true);
            fg.put(token, color);
        }
        for (GridColorToken token: GridColorToken.BG) {
            Color color = gridBackground.get(token);
            if (color == null) color = getGridColor(fallbackGrid, token, false);
            bg.put(token, color);
        }
        return new GridColorConfig(fg, bg);
    }

    private static Color getGridColor(GridColorConfig fallbackGrid, GridColorToken token, boolean isForeground) {
        if (fallbackGrid == null || fallbackGrid.isDefaultConfig()) {
            throw new IllegalArgumentException("Missing grid " + (isForeground ? FOREGROUND : BACKGROUND) + " color " + token);
        }
        return fallbackGrid.getColor(token, isForeground);
    }

    private static StyleSchema require(StyleSchema fallback, String what) {
        if (fallback == null) throw new IllegalArgumentException("Missing " + what);
        return fallback;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (basedOn != null) json.addProperty(BASED_ON, basedOn);

        if (! editorColors.isEmpty()) {
            JsonObject jsonEditorColors = new JsonObject();
            editorColors.forEach( (token, color) -> jsonEditorColors.add(name(token), ConfigType.COLOR.toJson(color)) );
            json.add(EDITOR_COLORS, jsonEditorColors);
        }

        if (! tokens.isEmpty()) {
            JsonObject jsonTokens = new JsonObject();
            tokens.forEach( (token, style) -> jsonTokens.addProperty(name(token), style.toString()) );
            json.add(TOKENS, jsonTokens);
        }

        if (gridColorsFromLookAndFeel) {
            json.add(GRID_COLORS, new JsonObject());
        } else if (! gridForeground.isEmpty() || ! gridBackground.isEmpty()) {
            JsonObject jsonGrid = new JsonObject();
            if (! gridForeground.isEmpty()) jsonGrid.add(FOREGROUND, toJson(gridForeground));
            if (! gridBackground.isEmpty()) jsonGrid.add(BACKGROUND, toJson(gridBackground));
            json.add(GRID_COLORS, jsonGrid);
        }

        return json;
    }

    private static JsonObject toJson(Map<GridColorToken, Color> map) {
        JsonObject json = new JsonObject();
        map.forEach( (token, color) -> json.add(name(token), ConfigType.COLOR.toJson(color)) );
        return json;
    }

    /**
     * Parses the definition. Unknown keys are ignored.
     * An empty gridColors object means grid colors derived from the Look and Feel.
     */
    public static StyleSchemaDefinition fromJson(JsonObject json) {
        JsonElement jsonBasedOn = json.get(BASED_ON);
        String basedOn = jsonBasedOn == null ? null : jsonBasedOn.getAsString();

        Map<EditorColorToken, Color> editorColors = new EnumMap<>(EditorColorToken.class);
        JsonObject jsonEditorColors = json.getAsJsonObject(EDITOR_COLORS);
        for (EditorColorToken token: EditorColorToken.values()) {
            Color color = getColor(jsonEditorColors, token);
            if (color != null) editorColors.put(token, color);
        }

        Map<ColorToken, TokenStyle> tokens = new EnumMap<>(ColorToken.class);
        JsonObject jsonTokens = json.getAsJsonObject(TOKENS);
        if (jsonTokens != null) {
            for (ColorToken token: ColorToken.values()) {
                JsonElement value = jsonTokens.get(name(token));
                if (value != null) tokens.put(token, TokenStyle.fromString(value.getAsString()));
            }
        }

        boolean gridColorsFromLookAndFeel = false;
        Map<GridColorToken, Color> fg = new EnumMap<>(GridColorToken.class);
        Map<GridColorToken, Color> bg = new EnumMap<>(GridColorToken.class);
        JsonObject jsonGrid = json.getAsJsonObject(GRID_COLORS);
        if (jsonGrid != null) {
            if (jsonGrid.isEmpty()) {
                gridColorsFromLookAndFeel = true;
            } else {
                JsonObject jsonFg = jsonGrid.getAsJsonObject(FOREGROUND);
                for (GridColorToken token: GridColorToken.FG) {
                    Color color = getColor(jsonFg, token);
                    if (color != null) fg.put(token, color);
                }
                JsonObject jsonBg = jsonGrid.getAsJsonObject(BACKGROUND);
                for (GridColorToken token: GridColorToken.BG) {
                    Color color = getColor(jsonBg, token);
                    if (color != null) bg.put(token, color);
                }
            }
        }

        return new StyleSchemaDefinition(basedOn, editorColors, tokens, gridColorsFromLookAndFeel, fg, bg);
    }

    private static String name(Enum<?> token) {
        return token.name().toLowerCase();
    }

    private static Color getColor(JsonObject json, Enum<?> token) {
        if (json == null) return null;
        JsonElement value = json.get(name(token));
        if (value == null) return null;
        try {
            return (Color) ConfigType.COLOR.fromJson(value, null);
        } catch (RuntimeException e) {
            log.warn("Can't parse color {} for {}", value, token, e);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StyleSchemaDefinition)) return false;
        StyleSchemaDefinition that = (StyleSchemaDefinition) o;
        return gridColorsFromLookAndFeel == that.gridColorsFromLookAndFeel &&
                Objects.equals(basedOn, that.basedOn) &&
                editorColors.equals(that.editorColors) &&
                tokens.equals(that.tokens) &&
                gridForeground.equals(that.gridForeground) &&
                gridBackground.equals(that.gridBackground);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basedOn, editorColors, tokens, gridColorsFromLookAndFeel, gridForeground, gridBackground);
    }
}
