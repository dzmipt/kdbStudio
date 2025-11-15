package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.colorchooser.ColorChooserComponentFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ColorChooser {

    private final static Logger log = LogManager.getLogger();

    private static JColorChooser colorChooser = null;

    private static Color mockColor = null; // null means not mocked

    public static synchronized void mock(Color returnColor) {
        mockColor = returnColor;
        if (returnColor == null) log.info("Reset mock status");
        else log.info("Mock ColorChooser to return {}", returnColor);
    }

    public static Color chooseColor(Component parent, String title, Color initColor) {
        return chooseColor(parent, title, initColor, null, null);
    }

    public static synchronized Color chooseColor(Component parent, String title, Color initColor,
                                    JComponent previewPanel, ColorChangeListener listener) {

        if (mockColor != null) return mockColor;

        if (colorChooser == null) colorChooser = new JColorChooser();

        if (initColor != null) colorChooser.setColor(initColor);
        if (previewPanel != null) colorChooser.setPreviewPanel(previewPanel);
        else colorChooser.setPreviewPanel(ColorChooserComponentFactory.getPreviewPanel());

        ChangeListener listenerWrapper = null;
        if (listener != null) {
            listenerWrapper = new ChangeListenerWrapper(listener);
            colorChooser.getSelectionModel().addChangeListener(listenerWrapper);
        }

        Color initialColor = colorChooser.getColor();
        EscapeDialog dialog = new EscapeDialog(parent, title);

        JButton btnOK = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");
        JButton btnReset = new JButton("Reset");
        btnOK.addActionListener(e-> dialog.accept());
        btnCancel.addActionListener(e-> dialog.cancel());
        btnReset.addActionListener(e-> colorChooser.setColor(initialColor));

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelButtons.add(btnOK);
        panelButtons.add(btnCancel);
        panelButtons.add(btnReset);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(colorChooser, BorderLayout.CENTER);
        contentPane.add(panelButtons, BorderLayout.SOUTH);
        dialog.setContentPane(contentPane);

        boolean accepted = dialog.alignAndShow();
        if (listenerWrapper != null) colorChooser.getSelectionModel().removeChangeListener(listenerWrapper);

        return accepted ? colorChooser.getColor() : null;
    }

    public interface ColorChangeListener {
        void colorChanged(Color color);
    }

    public static class ChangeListenerWrapper implements ChangeListener {

        private ColorChangeListener listener;

        ChangeListenerWrapper(ColorChangeListener listener) {
            this.listener = listener;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            listener.colorChanged(colorChooser.getColor());
        }
    }

}
