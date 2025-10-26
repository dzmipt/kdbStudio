package studio.kdb.config;

import java.awt.*;
import java.util.LinkedHashMap;

public class ColorMap extends LinkedHashMap<String, Color> {

    public Color put(ColorToken token, Color color) {
        return put(token.name(), color);
    }

    public Color get(ColorToken token) {
        return get(token.name());
    }
}
