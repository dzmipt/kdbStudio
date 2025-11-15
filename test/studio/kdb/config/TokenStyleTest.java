package studio.kdb.config;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TokenStyleTest {

    @Test
    public void testDecoding() {
        TokenStyle tokenStyle = new TokenStyle(new Color(0xabcdef), FontStyle.Plain);
        assertEquals(tokenStyle, TokenStyle.fromString("abcdef"));
        assertEquals(tokenStyle, TokenStyle.fromString("  abcdef "));
        assertEquals(tokenStyle, TokenStyle.fromString("abcdef  : Plain "));
        assertEquals(tokenStyle, TokenStyle.fromString("aBcDef  : Plain "));

        assertEquals("abcdef", tokenStyle.toString());

        tokenStyle = tokenStyle.derive(FontStyle.Underline);
        assertEquals("abcdef:Underline", tokenStyle.toString());

        tokenStyle = tokenStyle.derive(Color.WHITE);
        assertEquals(new Color(0xffffff), tokenStyle.getColor());
        assertEquals(FontStyle.get(false, false, true), tokenStyle.getStyle());
    }
}
