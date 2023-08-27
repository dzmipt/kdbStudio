package studio.ui;

import org.fife.ui.rtextarea.RTextScrollPane;
import studio.ui.rstextarea.RSTextAreaFactory;
import studio.ui.rstextarea.StudioRSyntaxTextArea;

import javax.swing.*;
import java.awt.*;

public class RSTextAreaFrameForTest extends JFrame {

    public RSTextAreaFrameForTest() {
        super("Frame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());

        JTextField textField = new JTextField(50);
        textField.setName("textField");

        StudioRSyntaxTextArea textArea = RSTextAreaFactory.newTextArea(true);
        textArea.setName("textArea");

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        textArea.setGutter(scrollPane.getGutter());

        panel.add(textField, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        setContentPane(panel);
        setSize(600,400);
        show();
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()->new RSTextAreaFrameForTest());
    }
}
