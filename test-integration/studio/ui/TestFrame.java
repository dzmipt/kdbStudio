package studio.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import studio.ui.rstextarea.RSTextAreaFactory;

import javax.swing.*;
import java.awt.*;

public class TestFrame extends JFrame {

    public TestFrame() {
        super("Frame");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());

        JTextField textField = new JTextField(50);
        textField.setName("textField");

        RSyntaxTextArea textArea = RSTextAreaFactory.newTextArea(true);
        textArea.setName("textArea");

        panel.add(textField, BorderLayout.NORTH);
        panel.add(textArea, BorderLayout.CENTER);

        setContentPane(panel);
        setSize(600,400);
        show();
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()->new TestFrame());
    }
}
