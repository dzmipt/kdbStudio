package studio.kdb.config;

import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class GridColorConfig {

    private final boolean defaultConfig;
    private ColorMap fgMap = null;
    private ColorMap bgMap = null;

    public final static GridColorConfig DEFAULT = new GridColorConfig();

    public GridColorConfig() {
        defaultConfig = true;
    }

    public GridColorConfig(GridColorConfig config) {
        this.defaultConfig = false;
        this.fgMap = new ColorMap(config.getForeground());
        this.bgMap = new ColorMap(config.getBackground());
    }

    public GridColorConfig(ColorMap fg, ColorMap bg) {
        this.defaultConfig = false;
        this.fgMap = fg;
        this.bgMap = bg;
    }

    public boolean isDefaultConfig() {
        return defaultConfig;
    }

    public Color getColor(GridColorToken token, boolean isForeground) {
        ColorMap map = isForeground ? getForeground() : getBackground();
        return map.get(token);
    }

    public void setColor(Color color, GridColorToken token, boolean isForeground) {
        ColorMap map = isForeground ? getForeground() : getBackground();
        map.put(token, color);
    }

    public void updateUI() {
        if (! defaultConfig) return;

        Color bgColor = UIManager.getColor("Table.background");
        bgColor = bgColor == null ? Color.WHITE : bgColor;

        Color keyColor = Util.blendColors(new Color(215,255,215), bgColor);
        Color altColor = Util.blendColors(new Color(215,215,255), bgColor);
        Color nullColor = Util.blendColors(new Color(255,45,45), bgColor);


        Color selBgColor = UIManager.getColor("Table.selectionBackground");
        Color defaultBgColor = new Color(145, 79, 206);
        Color selColor = selBgColor == null ? defaultBgColor : selBgColor;

        Color selFgColor = UIManager.getColor("Table.selectionForeground");
        selFgColor = selFgColor == null ? Color.BLACK : selColor;

        Color fgColor = UIManager.getColor("Table.foreground");

        Color markColor = Util.blendColors(new Color(255, 145, 0), bgColor);

//        Color markKeyColor = Util.blendColors(markColor, keyColor);
//        Color markBgColor = Util.blendColors(markColor, bgColor);
        Color markAltColor = Util.blendColors(markColor, altColor);
        Color markSelColor = Util.blendColors(markColor, selColor);

        fgMap = new ColorMap();
        fgMap.put(GridColorToken.NULL, nullColor);
        fgMap.put(GridColorToken.KEY, fgColor);
        fgMap.put(GridColorToken.ODD, fgColor);
        fgMap.put(GridColorToken.EVEN, fgColor);
        fgMap.put(GridColorToken.MARK, fgColor);
        fgMap.put(GridColorToken.SELECTED, selFgColor);
        fgMap.put(GridColorToken.MARK_SELECTED, selFgColor);
        fgMap.freeze();

        bgMap = new ColorMap();
        bgMap.put(GridColorToken.KEY, keyColor);
        bgMap.put(GridColorToken.ODD, bgColor);
        bgMap.put(GridColorToken.EVEN, altColor);
        bgMap.put(GridColorToken.MARK, markAltColor);
        bgMap.put(GridColorToken.SELECTED, selColor);
        bgMap.put(GridColorToken.MARK_SELECTED, markSelColor);
        bgMap.freeze();
    }

    public ColorMap getForeground() {
        if (isDefaultConfig()) updateUI();
        return fgMap;
    }

    public ColorMap getBackground() {
        if (isDefaultConfig()) updateUI();
        return bgMap;
    }

    public boolean isUnmodifiable() {
        return defaultConfig || (fgMap.isUnmodifiable() && bgMap.isUnmodifiable());
    }

    public GridColorConfig cloneConfig() {
        if (isUnmodifiable()) return this;

        GridColorConfig config = new GridColorConfig(fgMap, bgMap);
        config.freeze();
        return config;
    }

    public void freeze() {
        if (isUnmodifiable()) return;
        fgMap = fgMap.cloneMap();
        fgMap.freeze();
        bgMap = bgMap.cloneMap();
        bgMap.freeze();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GridColorConfig)) return false;
        GridColorConfig that = (GridColorConfig) o;
        if (isDefaultConfig() && that.isDefaultConfig()) return true;
        if (isDefaultConfig() || that.isDefaultConfig()) return false;
        return Objects.equals(fgMap, that.fgMap) && Objects.equals(bgMap, that.bgMap);
    }

    @Override
    public int hashCode() {
        if (isDefaultConfig()) return 0;
        return Objects.hash(fgMap, bgMap);
    }
}
