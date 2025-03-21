package studio.kdb.config;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ColorTokenConfig {

    private final Map<ColorToken, Color> colors;

    public final static ColorTokenConfig DEFAULT = new ColorTokenConfig(new HashMap<>());

    public ColorTokenConfig(Map<ColorToken, Color> colors) {
        this.colors = new HashMap<>();
        for (ColorToken token: colors.keySet()) {
            Color color = colors.get(token);
            if (!color.equals(token.getColor())) {
                this.colors.put(token, color);
            }
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
