package studio.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import studio.kdb.Lm;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;

public class HelpDialog extends EscapeDialog {
    public HelpDialog(JFrame parent) {
        super(parent, "Studio for kdb+");
        String env = EnvConfig.getEnvironment();
        String htmlText = "<html><head><style>" +
                "ul { margin: 0 0 20px 5px; padding: 0 0 0 15px;}" +
                "ul ul { margin-bottom: 0px;}" +
                "h1 {margin: 10px 0 10px 0;}" +
                "h2 {font-size: 110%; margin: 0; padding-left: 5px;}" +
                "code {font-size: 130%;}" +
                "p {margin: 0 0 10px 0;}" +
                "</style><title>Studio for kdb+</title></head><body><h1>Studio for kdb+</h1>"
                + "<p>"
                + (env == null ? "" : "Environment: " + env + "<br/>")
                + "Version: " + Lm.version + " (" + Lm.date + ")"
                + "<br/>Build date: " + Lm.build
                + "<br/>JVM Version: " + System.getProperty("java.version")
                + "</p><p>License: <a href=\"https://github.com/dzmipt/kdbStudio/blob/master/license.md\">Apache 2</a>"
                + "<br/>N.B. Some components have their own license terms, see this project on github for details."
                + "<br/>Source available from <a href=\"https://github.com/dzmipt/kdbStudio\">Github</a>"
                + "<br/>The repository was forked from <a href=\"https://github.com/CharlesSkelton/studio\">Github</a>"
                + "<br/>Contributions and corrections welcome."
                + "<br/><br/><hr/>"
                + "<h1>Release Notes</h1>"
                + Lm.notes
                + "</p></body></html>";

        JEditorPane jep = new JEditorPane("text/html", htmlText);

        jep.setEditable(false);
        jep.setOpaque(true);
        jep.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType()))
                    Util.openURL(hle.getURL().toString());
            }
        });
        jep.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        jep.setCaretPosition(0);
        getContentPane().add(new JScrollPane(jep));
        JPanel buttonPane = new JPanel();
        JButton button = new JButton("Close");
        buttonPane.add(button);
        button.addActionListener(e->accept());
        getContentPane().add(buttonPane, BorderLayout.PAGE_END);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(800,450));
    }

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        HelpDialog help = new HelpDialog(null);
        help.alignAndShow();
    }

}
