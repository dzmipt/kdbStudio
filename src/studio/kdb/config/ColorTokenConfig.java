package studio.kdb.config;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ColorTokenConfig {

    private final Map<ColorToken, Color> colors;

    public final static ColorTokenConfig DEFAULT = new ColorTokenConfig(new ColorMap());

    public ColorTokenConfig(ColorMap colors) {
        this.colors = new HashMap<>();
        for (ColorToken token: ColorToken.values()) {
            Color color = colors.get(token);
            if (color == null) continue;
            if (token.getColor().equals(color)) continue;
            this.colors.put(token, color);
        }
    }

    public ColorMap getMap() {
        ColorMap map = new ColorMap();
        for (ColorToken token: ColorToken.values()) {
            Color color = colors.get(token);
            if (color == null) continue;
            if (token.getColor().equals(color)) continue;
            map.put(token.name(), color);
        }
        return map;
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
