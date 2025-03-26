package studio.kdb.config;

import java.awt.*;

public enum FontStyle {
    Plain(Font.PLAIN), Bold(Font.BOLD), Italic(Font.ITALIC), ItalicAndBold(Font.BOLD | Font.ITALIC);
    private int style;

    FontStyle(int style) {
        this.style = style;
    }

    public int getStyle() {
        return style;
    }
}
