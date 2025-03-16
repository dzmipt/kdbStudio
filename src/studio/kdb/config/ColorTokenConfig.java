package studio.kdb.config;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ColorTokenConfig {

    private Map<ColorToken, Color> colors = new HashMap<>();

    public void setColor(ColorToken token, Color color) {
        if (color.equals(token.getColor())) {
            colors.remove(token);
        } else {
            colors.put(token, color);
        }
    }

    public Color getColor(ColorToken token) {
        Color color = colors.get(token);
        if (color == null) return token.getColor();
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColorTokenConfig)) return false;
        ColorTokenConfig that = (ColorTokenConfig) o;
        return colors.equals(that.colors);
    }

    @Override
    public int hashCode() {
        return colors.hashCode();
    }
}
