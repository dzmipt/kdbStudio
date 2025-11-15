package studio.kdb.config;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

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

    public final static ColorMap DEFAULT_EDITOR_COLORS;
    static {
        DEFAULT_EDITOR_COLORS = new ColorMap();
        DEFAULT_EDITOR_COLORS.put(EditorColorToken.BACKGROUND, Color.WHITE);
        DEFAULT_EDITOR_COLORS.put(EditorColorToken.SELECTED, RSyntaxTextArea.getDefaultSelectionColor());
        DEFAULT_EDITOR_COLORS.put(EditorColorToken.CURRENT_LINE_HIGHLIGHT, RSyntaxTextArea.getDefaultCurrentLineHighlightColor());
        DEFAULT_EDITOR_COLORS.freeze();
    }

    public ColorMap() {
        map = new LinkedHashMap<>();
    }


    public ColorMap(ColorMap colorMap) {
         this.map = new LinkedHashMap<>(colorMap.map);
    }

    public ColorMap cloneMap() {
        if (unmodifiable) return this;
        return new ColorMap(this);
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
        return map.put(name.toLowerCase(), color);
    }

    public Color get(String name) {
        return map.get(name.toLowerCase());
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public <T extends Enum<T>> Color get(T token) {
        return get(token.name().toLowerCase());
    }

    public <T extends Enum<T>> Color put(T token, Color color) {
        return put(token.name(), color);
    }

    public <T extends Enum<T>> ColorMap enforce(T[] keys) {
        Map<String,Color> map = new LinkedHashMap<>();
        for (T key: keys) {
            String name = key.name().toLowerCase();
            Color color = this.map.get(name);
            if (color == null) throw new IllegalArgumentException("Expected color token: " + name);
            map.put(name, color);
        }
        this.map = map;
        freeze();
        return this;
    }

    public <T extends Enum<T>> ColorMap filter(ColorMap defaults) {
        Map<String,Color> map = new LinkedHashMap<>();
        for (String name: defaults.keySet()) {
            Color color = this.map.get(name);
            if (color == null) color = defaults.get(name);
            map.put(name, color);
        }
        this.map = map;
        freeze();
        return this;
    }

}
