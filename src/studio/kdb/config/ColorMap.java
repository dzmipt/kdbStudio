package studio.kdb.config;

import java.awt.*;
import java.util.*;


public class ColorMap {

    private Map<String, Color> map;
    private transient boolean unmodifiable = false;

    public final static ColorMap EMPTY = new ColorMap();
    static {
        EMPTY.map = Collections.emptyMap();
        EMPTY.unmodifiable = true;
    }

    public final static ColorMap DEFAULT_COLOR_TOKEN_MAP;
    static {
        DEFAULT_COLOR_TOKEN_MAP = new ColorMap();
        for (ColorToken token: ColorToken.values()) {
            DEFAULT_COLOR_TOKEN_MAP.put(token, token.getColor());
        }
        DEFAULT_COLOR_TOKEN_MAP.freeze();
    }

    public ColorMap() {
        map = new LinkedHashMap<>();
    }


    public ColorMap(ColorMap colorMap) {
         this.map = new LinkedHashMap<>(colorMap.map);
    }

    public ColorMap cloneMap() {
        if (unmodifiable) return this;
        ColorMap colorMap = new ColorMap();
        colorMap.map.putAll(this.map);
        return colorMap;
    }

    public boolean isUnmodifiable() {
        return unmodifiable;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColorMap)) return false;
        ColorMap colorMap = (ColorMap) o;
        return Objects.equals(map, colorMap.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    public void freeze() {
        if (unmodifiable) return;
        map = Collections.unmodifiableMap(map);
        unmodifiable = true;
    }

    public Color put(String name, Color color) {
        return map.put(name, color);
    }

    public Color get(String name) {
        return map.get(name);
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Color put(ColorToken token, Color color) {
        return put(token.name(), color);
    }

    public Color get(ColorToken token) {
        return get(token.name());
    }

    public Color put(GridColorToken token, Color color) {
        return put(token.name(), color);
    }

    public Color get(GridColorToken token) {
        return get(token.name());
    }

    public ColorMap filterColorToken() {
        return filter(ColorToken.values(), DEFAULT_COLOR_TOKEN_MAP);
    }

    public <T extends Enum<T>> ColorMap enforce(T[] keys) {
        Map<String,Color> map = new LinkedHashMap<>();
        for (T key: keys) {
            String name = key.name();
            Color color = this.map.get(name);
            if (color == null) throw new IllegalArgumentException("Expected color token: " + name);
            map.put(name, color);
        }
        this.map = map;
        freeze();
        return this;
    }

    public <T extends Enum<T>> ColorMap filter(T[] keys, ColorMap defaults) {
        Map<String,Color> map = new LinkedHashMap<>();
        for (T key: keys) {
            String name = key.name();
            Color color = this.map.get(name);
            if (color == null) color = defaults.get(name);
            map.put(name, color);
        }
        this.map = map;
        freeze();
        return this;
    }

}
