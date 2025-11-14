package studio.kdb.config;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FontStyleTest {

    private final static String NAME = "Times New Roman";

    @Test
    public void testStyles() {
        assertEquals(FontStyle.Plain, FontStyle.fromFont(new Font(NAME, Font.PLAIN, 15)));
        assertEquals(FontStyle.Italic, FontStyle.fromFont(new Font(NAME, Font.ITALIC, 15)));
        assertEquals(FontStyle.Bold, FontStyle.fromFont(new Font(NAME, Font.BOLD, 15)));
        assertEquals(FontStyle.ItalicAndBold, FontStyle.fromFont(new Font(NAME, Font.BOLD | Font.ITALIC, 15)));
    }

    @Test
    public void testUnderline() {
        Font font = FontStyle.Underline.getFont(NAME, 15);
        assertEquals(Font.PLAIN, font.getStyle());

        assertEquals(FontStyle.Underline, FontStyle.fromFont(font));


        font = FontStyle.UnderlineAndBold.getFont(NAME, 15);
        assertEquals(Font.BOLD, font.getStyle());
        assertEquals(FontStyle.UnderlineAndBold, FontStyle.fromFont(font));
    }

    @Test
    public void testApplyFont() {
        Font font = new Font(NAME, Font.PLAIN, 15);

        Font derivedFont = FontStyle.UnderlineAndItalic.applyStyle(font);
        assertEquals(Font.ITALIC, derivedFont.getStyle());

        assertEquals(FontStyle.UnderlineAndItalic, FontStyle.fromFont(derivedFont));
    }
}
