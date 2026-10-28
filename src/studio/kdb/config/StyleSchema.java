package studio.kdb.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;
import java.util.Objects;

/**
 * Colors for the editor, syntax tokens and the result grid. Immutable.
 */
public class StyleSchema {

    public final static String BASED_ON = "basedOn";
    public final static String EDITOR_COLORS = "editorColors";
    public final static String TOKENS = "tokens";
    public final static String GRID_COLORS = "gridColors";

    private final String basedOn;
    private final ColorMap editorColors;
    private final TokenStyleMap tokens;
    private final GridColorConfig gridColors;

    public StyleSchema(String basedOn, ColorMap editorColors, TokenStyleMap tokens, GridColorConfig gridColors) {
        this.basedOn = basedOn;

        ColorMap colors = new ColorMap(editorColors);
        colors.freeze();
        this.editorColors = colors;

        TokenStyleMap tokenMap = new TokenStyleMap(tokens);
        tokenMap.freeze();
        this.tokens = tokenMap;

        GridColorConfig grid = gridColors.cloneConfig();
        grid.freeze();
        this.gridColors = grid;
    }

    /**
     * The name of the built-in schema this schema comes from. It is used to fill values which are missing in the json.
     * null for predefined schemas.
     */
    public String getBasedOn() {
        return basedOn;
    }

    public ColorMap getEditorColors() {
        return editorColors;
    }

    public Color getBackgroundColor() {
        return editorColors.get(EditorColorToken.BACKGROUND);
    }

    public TokenStyleMap getTokens() {
        return tokens;
    }

    public GridColorConfig getGridColors() {
        return gridColors;
    }

    public StyleSchema newBasedOn(String basedOn) {
        return new StyleSchema(basedOn, editorColors, tokens, gridColors);
    }

    public StyleSchema newEditorColors(ColorMap editorColors) {
        return new StyleSchema(basedOn, editorColors, tokens, gridColors);
    }

    public StyleSchema newTokens(TokenStyleMap tokens) {
        return new StyleSchema(basedOn, editorColors, tokens, gridColors);
    }

    public StyleSchema newGridColors(GridColorConfig gridColors) {
        return new StyleSchema(basedOn, editorColors, tokens, gridColors);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (basedOn != null) json.addProperty(BASED_ON, basedOn);
        json.add(EDITOR_COLORS, ConfigType.COLOR_MAP.toJson(editorColors));
        json.add(TOKENS, ConfigType.TOKEN_STYLE_MAP.toJson(tokens));
        json.add(GRID_COLORS, ConfigType.GRID_COLOR_CONFIG.toJson(gridColors));
        return json;
    }

    /**
     * Parses the schema. Values missing in the json are taken from the fallback.
     * If fallback is null, all values have to be defined in the json.
     * An empty gridColors object means grid colors derived from the Look and Feel.
     */
    public static StyleSchema fromJson(JsonObject json, StyleSchema fallback) {
        JsonElement jsonBasedOn = json.get(BASED_ON);
        String basedOn = jsonBasedOn == null ? null : jsonBasedOn.getAsString();

        ColorMap editorColors = parseEditorColors(json.getAsJsonObject(EDITOR_COLORS), fallback);
        TokenStyleMap tokens = parseTokens(json.getAsJsonObject(TOKENS), fallback);
        GridColorConfig gridColors = parseGridColors(json.getAsJsonObject(GRID_COLORS), fallback);

        return new StyleSchema(basedOn, editorColors, tokens, gridColors);
    }

    private static Color getColor(JsonObject json, String name) {
        if (json == null || !json.has(name)) return null;
        return (Color) ConfigType.COLOR.fromJson(json.get(name), null);
    }

    private static ColorMap parseEditorColors(JsonObject json, StyleSchema fallback) {
        ColorMap map = new ColorMap();
        for (EditorColorToken token: EditorColorToken.values()) {
            String name = token.name().toLowerCase();
            Color color = getColor(json, name);
            if (color == null) {
                if (fallback == null) throw new IllegalArgumentException("Missing editor color: " + name);
                color = fallback.getEditorColors().get(token);
            }
            map.put(token, color);
        }
        return map;
    }

    private static TokenStyleMap parseTokens(JsonObject json, StyleSchema fallback) {
        TokenStyleMap map = new TokenStyleMap();
        for (ColorToken token: ColorToken.values()) {
            String name = token.name().toLowerCase();
            TokenStyle style;
            if (json != null && json.has(name)) {
                style = TokenStyle.fromString(json.get(name).getAsString());
            } else {
                if (fallback == null) throw new IllegalArgumentException("Missing token: " + name);
                style = fallback.getTokens().get(token);
            }
            map.set(token, style);
        }
        return map;
    }

    private static GridColorConfig parseGridColors(JsonObject json, StyleSchema fallback) {
        if (json == null) {
            if (fallback == null) throw new IllegalArgumentException("Missing gridColors");
            return fallback.getGridColors();
        }

        if (json.isEmpty()) return GridColorConfig.DEFAULT;

        GridColorConfig fallbackGrid = fallback == null ? null : fallback.getGridColors();
        if (fallbackGrid != null && fallbackGrid.isDefaultConfig()) fallbackGrid = null;

        ColorMap fg = parseGridColorMap(json.getAsJsonObject("foreground"), GridColorToken.FG,
                fallbackGrid == null ? null : fallbackGrid.getForeground(), "foreground");
        ColorMap bg = parseGridColorMap(json.getAsJsonObject("background"), GridColorToken.BG,
                fallbackGrid == null ? null : fallbackGrid.getBackground(), "background");
        return new GridColorConfig(fg, bg);
    }

    private static ColorMap parseGridColorMap(JsonObject json, GridColorToken[] tokens, ColorMap fallback, String what) {
        ColorMap map = new ColorMap();
        for (GridColorToken token: tokens) {
            String name = token.name().toLowerCase();
            Color color = getColor(json, name);
            if (color == null) {
                if (fallback == null) throw new IllegalArgumentException("Missing grid " + what + " color: " + name);
                color = fallback.get(token);
            }
            map.put(token, color);
        }
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StyleSchema)) return false;
        StyleSchema that = (StyleSchema) o;
        return Objects.equals(basedOn, that.basedOn) &&
                Objects.equals(editorColors, that.editorColors) &&
                Objects.equals(tokens, that.tokens) &&
                Objects.equals(gridColors, that.gridColors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basedOn, editorColors, tokens, gridColors);
    }
}
