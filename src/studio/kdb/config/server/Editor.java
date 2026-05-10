package studio.kdb.config.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.Workspace;
import studio.kdb.config.ColorMap;
import studio.kdb.config.ColorToken;
import studio.kdb.config.TLSResolutionMode;
import studio.ui.ColorChooser;
import studio.ui.DocumentChangeListener;
import studio.ui.ServerTreeDialog;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public abstract class Editor<E> {
    private ChangeListener changeListener = null;

    private static final Logger log = LogManager.getLogger();

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    protected void valueChanged() {
        try {
            if (changeListener != null) changeListener.stateChanged(null);
            getComponent().putClientProperty("JComponent.outline", null);
        } catch (RuntimeException e) {
            log.warn("Error in changing value: {}", e.getMessage());
            getComponent().putClientProperty("JComponent.outline", "error");
        }
    }

    public abstract JComponent getComponent();
    public abstract void setValue(E value);
    public abstract E getValue();


    public static class TextEditor extends Editor<String> {
        private final JTextField txtField = new JTextField();

        public TextEditor() {
            txtField.getDocument().addDocumentListener((DocumentChangeListener) e -> valueChanged() );
        }

        @Override
        public JComponent getComponent() {
            return txtField;
        }

        @Override
        public void setValue(String value) {
            if (value == null) value = "";
            txtField.setText(value);
        }

        @Override
        public String getValue() {
            return txtField.getText();
        }
    }

    public static class IntEditor extends Editor<Integer> {
        private final JTextField txtField = new JTextField();

        public IntEditor() {
            txtField.getDocument().addDocumentListener((DocumentChangeListener)e -> valueChanged() );
        }

        @Override
        public JComponent getComponent() {
            return txtField;
        }

        @Override
        public void setValue(Integer value) {
            if (value == null) txtField.setText("");
            else txtField.setText("" + value);
        }

        @Override
        public Integer getValue() {
            try {
                return Integer.parseInt(txtField.getText());
            } catch (NumberFormatException ignore) {}
            return null;
        }
    }

    public static class PasswordEditor extends Editor<String> {
        private final JPasswordField txtField = new JPasswordField();

        public PasswordEditor() {
            txtField.getDocument().addDocumentListener((DocumentChangeListener)e -> valueChanged() );
        }

        @Override
        public JComponent getComponent() {
            return txtField;
        }

        @Override
        public void setValue(String value) {
            if (value == null) value = "";
            txtField.setText(value);
        }

        @Override
        public String getValue() {
            return txtField.getText();
        }
    }

    public static class TLSEditor extends Editor<TLSResolutionMode> {
        private final JComboBox<TLSResolutionMode> comboBox = new JComboBox<>(TLSResolutionMode.values());

        public TLSEditor() {
            comboBox.addActionListener(e-> valueChanged());
        }

        @Override
        public JComponent getComponent() {
            return comboBox;
        }

        @Override
        public void setValue(TLSResolutionMode value) {
            comboBox.setSelectedItem(value);
        }

        @Override
        public TLSResolutionMode getValue() {
            return (TLSResolutionMode)comboBox.getSelectedItem();
        }
    }

    public static class AuthMethodEditor extends Editor<String> {
        private final JComboBox<String> comboBox = new JComboBox<>(AuthenticationManager.getInstance().getAuthenticationMechanisms());

        public AuthMethodEditor() {
            comboBox.addActionListener(e-> valueChanged());
        }

        @Override
        public JComponent getComponent() {
            return comboBox;
        }

        @Override
        public void setValue(String value) {
            comboBox.setSelectedItem(value);
        }

        @Override
        public String getValue() {
            return (String)comboBox.getSelectedItem();
        }
    }

    public static class BgColorEditor extends Editor<Color> {
        private final JTextField txtField = new JTextField("Sample text on background");

        public BgColorEditor() {
            ColorMap colorMap = Config.getInstance().getEditorColors();
            txtField.setEditable(false);
            txtField.setForeground(colorMap.get(ColorToken.DEFAULT));
            txtField.setBackground(getDefaultBgColor());

            txtField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            txtField.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    onColorChange();
                }
            });
        }

        private void onColorChange() {
            Color result = ColorChooser.chooseColor(txtField, "Select background color for editor",
                    getValue());

            if (result != null) {
                setValue(result);
            }
        }

        @Override
        public JComponent getComponent() {
            return txtField;
        }

        private Color getDefaultBgColor() {
            return Config.getInstance().getBackgroundColor();
        }

        @Override
        public void setValue(Color value) {
            if (value == null) value = getDefaultBgColor();
            txtField.setBackground(value);
            valueChanged();
        }

        @Override
        public Color getValue() {
            return txtField.getBackground();
        }
    }

    public static class FolderEditor extends Editor<java.util.List<String>> {

        private final JPanel panel = new JPanel(new BorderLayout());
        private final JLabel label = new JLabel();
        private java.util.List<String> value = java.util.List.of();

        public FolderEditor() {
            JButton button = new JButton("...");
            panel.add(label, BorderLayout.CENTER);
            panel.add(button, BorderLayout.EAST);

            button.addActionListener(this::onButtonClick);
        }

        private void onButtonClick(ActionEvent e) {
            ServerTreeDialog serverTreeDialog = new ServerTreeDialog(SwingUtilities.getWindowAncestor(panel), Workspace.DEFAULT_BOUNDS);

            ServerTreeNode folder = Config.getInstance().getServerTree().findPath(getValue(), false);

            ServerTreeNode newFolder = serverTreeDialog.showFolders(folder);
            if (newFolder != null) setValue(newFolder.getFolderPath());
        }

        @Override
        public JComponent getComponent() {
            return panel;
        }

        @Override
        public void setValue(java.util.List<String> value) {
            if (value == null) value = java.util.List.of();
            this.value = value;
            label.setText(Server.getFolderName(value));
            valueChanged();
        }

        @Override
        public List<String> getValue() {
            return value;
        }
    }

}
