package studio.kdb.config;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public enum FontStyle {
    Plain(false, false, false),
    Bold(true, false, false),
    Italic(false, true, false),
    ItalicAndBold(true, true, false),
    Underline(false, false, true),
    UnderlineAndBold(true, false, true),
    UnderlineAndItalic(false, true, true),
    UnderlineAndBoldAndItalic(true, true, true);

    private final boolean bold;
    private final boolean italic;
    private final boolean underline;

    FontStyle(boolean bold, boolean italic, boolean underline) {
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
    }

    public boolean isBold() {
        return bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public FontStyle setBold(boolean bold) {
        return get(bold, this.italic, this.underline);
    }

    public FontStyle setItalic(boolean italic) {
        return get(this.bold, italic, this.underline);
    }

    public FontStyle setUnderline(boolean underline) {
        return get(this.bold, this.italic, underline);
    }

    public Font getFont(String name, int size) {
        Font font = new Font(name, Font.PLAIN, size);
        if (this == FontStyle.Plain) return font;
        return applyStyle(font);
    }

    public Font applyStyle(Font font) {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        if (isBold()) {
            attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
        }
        if (isItalic()) {
            attrs.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
        }
        if (isUnderline()) {
            attrs.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }

        return font.deriveFont(attrs);
    }

    public static FontStyle fromFont(Font font) {
        Map<TextAttribute, ?> attrs = font.getAttributes();
        boolean b = false;
        boolean i = false;
        boolean u = false;
        Object value = attrs.get(TextAttribute.WEIGHT);
        if (value instanceof Float && ((Float)value)>0f) b = true;

        value = attrs.get(TextAttribute.POSTURE);
        if (value instanceof Float && ((Float)value)>0f) i = true;

        value = attrs.get(TextAttribute.UNDERLINE);
        if (value instanceof Integer && ((Integer)value)>=0) u = true;

        return get(b, i, u);
    }

    public static FontStyle get(boolean bold, boolean italic, boolean underline) {
        for (FontStyle style:values()) {
            if (style.isBold() == bold &&
                    style.isItalic() == italic &&
                    style.isUnderline() == underline) return style;
        }

        return FontStyle.Plain;
    }
}
