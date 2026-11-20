package studio.mcp.ui;

import javax.swing.*;

/**
 * Components for text which comes from AI agents (client names, host names, queries).
 * Swing renders any text starting with &lt;html&gt; as HTML; these components never do.
 */
public class PlainText {

    private static final String HTML_DISABLE = "html.disable";

    public static JLabel label(String text) {
        JLabel label = new JLabel();
        label.putClientProperty(HTML_DISABLE, Boolean.TRUE);
        label.setText(text);
        return label;
    }

    public static JMenuItem menuItem(String text) {
        JMenuItem item = new JMenuItem();
        item.putClientProperty(HTML_DISABLE, Boolean.TRUE);
        item.setText(text);
        return item;
    }

    // Escapes text to be inserted into an HTML fragment
    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
