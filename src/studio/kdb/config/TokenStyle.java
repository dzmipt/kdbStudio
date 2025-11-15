package studio.kdb.config;

import studio.ui.Util;

import java.awt.*;
import java.util.Objects;

public class TokenStyle {

    private final Color color;
    private final FontStyle style;

    public TokenStyle(Color color, FontStyle style) {
        this.color = color;
        this.style = style;
    }

    public Color getColor() {
        return color;
    }

    public FontStyle getStyle() {
        return style;
    }

    TokenStyle derive(Color color) {
        if (this.color.equals(color)) return this;

        return new TokenStyle(color, this.style);
    }

    TokenStyle derive(FontStyle style) {
        if (this.style.equals(style)) return this;

        return new TokenStyle(this.color, style);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TokenStyle)) return false;
        TokenStyle that = (TokenStyle) o;
        return Objects.equals(color, that.color) && style == that.style;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, style);
    }

    @Override
    public String toString() {
        if (style == FontStyle.Plain) return Util.colorToString(color);
        return Util.colorToString(color) + ":" + style.name();
    }

    public static TokenStyle fromString(String value) {
        FontStyle style = FontStyle.Plain;
        int index = value.indexOf(':');
        if (index > -1) {
            style = FontStyle.valueOf(value.substring(index+1).trim());
            value = value.substring(0, index);
        }
        Color color = Util.stringToColor(value);
        return new TokenStyle(color, style);
    }
}
