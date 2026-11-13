package studio.kdb.config;

import java.awt.*;
import java.util.Objects;

/**
 * Colors for the editor, syntax tokens and the result grid. Immutable.
 */
public class StyleSchema {

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
     * The name of the built-in schema this schema comes from. It is used to fill values which are missing in the config.
     * null for built-in schemas.
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
