package studio.kdb.config;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TokenStyleMap {

    private Map<ColorToken, TokenStyle> map;
    private transient boolean unmodifiable = false;

    public final static TokenStyleMap DEFAULT = new TokenStyleMap();
    static {
        DEFAULT.freeze();
    }

    public TokenStyleMap() {
        map = new HashMap<>();
        for(ColorToken token: ColorToken.values()) {
            map.put(token, token.getDefaultStyle());
        }
    }

    public TokenStyleMap(TokenStyleMap map) {
        this.map = new HashMap<>(map.map);
    }

    public TokenStyle get(ColorToken key) {
        return map.get(key);
    }

    public Color getColor(ColorToken key) {
        return get(key).getColor();
    }

    public void set(ColorToken key, TokenStyle value) {
        map.put(key, value);
    }

    public void set(ColorToken key, Color color) {
        map.put(key, get(key).derive(color));
    }

    public void set(ColorToken key, FontStyle style) {
        map.put(key, get(key).derive(style));
    }

    public void freeze() {
        if (unmodifiable) return;
        map = Collections.unmodifiableMap(map);
        unmodifiable = true;
    }

    public TokenStyleMap cloneMap() {
        if (unmodifiable) return this;
        return new TokenStyleMap(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TokenStyleMap)) return false;
        TokenStyleMap that = (TokenStyleMap) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(map);
    }
}
