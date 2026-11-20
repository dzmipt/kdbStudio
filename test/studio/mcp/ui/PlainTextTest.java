package studio.mcp.ui;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;

import static org.junit.jupiter.api.Assertions.*;

public class PlainTextTest {

    private static final String HTML = "<html><b>prod-readonly</b><img src='http://example.com/x.png'></html>";

    @Test
    public void htmlIsNotRendered() {
        JLabel label = PlainText.label(HTML);
        assertNull(label.getClientProperty(BasicHTML.propertyKey));
        assertEquals(HTML, label.getText());

        JMenuItem item = PlainText.menuItem(HTML);
        assertNull(item.getClientProperty(BasicHTML.propertyKey));

        // a regular label would render it
        assertNotNull(new JLabel(HTML).getClientProperty(BasicHTML.propertyKey));
    }

    @Test
    public void escape() {
        assertEquals("&lt;html&gt;a &amp; &quot;b&quot;", PlainText.escape("<html>a & \"b\""));
        assertEquals("", PlainText.escape(null));
    }
}
