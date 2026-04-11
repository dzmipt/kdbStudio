package studio.ui.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.Workspace;
import studio.kdb.config.ColorMap;
import studio.kdb.config.ColorToken;
import studio.kdb.config.EditorColorToken;
import studio.kdb.config.TLSResolutionMode;
import studio.ui.*;
import studio.utils.QConnection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MultipleServerEditor extends JPanel {

    private List<Server> servers;

    private final ServerField<List<String>> folderField = new ServerField<>("Folder", new FolderEditor(), Server::getFolderPath);
    private final ServerField<String> hostField = new ServerField<>("Host", new TextEditor(), Server::getHost);
    private final ServerField<Integer> portField = new ServerField<>("Port", new PortEditor(), Server::getPort);
    private final ServerField<TLSResolutionMode> tlsField = new ServerField<>("Use TLS", new TLSEditor(), Server::getTLSResolutionMode);
    private final ServerField<String> userField = new ServerField<>("Username", new TextEditor(), Server::getUsername);
    private final ServerField<String> passwordField = new ServerField<>("Password", new PasswordEditor(), Server::getPassword);
    private final ServerField<String> authMethodField = new ServerField<>("Auth. Method", new AuthMethodEditor(), Server::getAuthenticationMechanism);
    private final ServerField<Color> bgColorField = new ServerField<>("Background Color", new BgColorEditor(), Server::getBackgroundColor);

    private final ServerField<?>[] fields = {folderField, hostField, portField, tlsField, userField, passwordField, authMethodField, bgColorField};

    private ChangeListener changeListener = null;
    private final static Logger log = LogManager.getLogger();

    public MultipleServerEditor() {
        setServers(new ArrayList<>());


        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);
        GroupLayoutSimple.Stack stackUndo = new GroupLayoutSimple.Stack();
        GroupLayoutSimple.Stack stackLabels = new GroupLayoutSimple.Stack();
        GroupLayoutSimple.Stack stackEditors = new GroupLayoutSimple.Stack();

        List<JLabel> labels = new ArrayList<>();
        for (ServerField<?> f: fields) {
            JLabel label = new JLabel();
            updateLabelText(label, f);
            labels.add(label);

            stackLabels.addLine(label);
            stackEditors.addLine(f.getEditor());

            UndoButton undoButton = new UndoButton();
            undoButton.setEnabled(false);
            f.setChangeListener( e-> {
                undoButton.setEnabled(f.amended());
                updateLabelText(label, f);
                if (changeListener != null) changeListener.stateChanged(e);
            } );
            undoButton.setListener(e-> f.resetToCommonValue());

            stackUndo.addLine(undoButton);
        }

        int width = 0, height = 0;
        for(JLabel label: labels) {
            Dimension prefSize = label.getPreferredSize();
            width = Math.max(width, prefSize.width);
            height = Math.max(height, prefSize.height);
        }
        Dimension prefSize = new Dimension((int) (width*1.2), height);
        for (JLabel label: labels) {
            label.setPreferredSize(prefSize);
        }

        layout.addMaxWidthComponents(
                Arrays.stream(fields).map(ServerField::getEditor).toArray(Component[]::new)
        );
        layout.setStacks(stackUndo, stackLabels, stackEditors);
    }

    private void updateLabelText(JLabel label, ServerField<?> field) {
        String text = field.getLabel();
        if (field.theSame()) {
            text = String.format("<html><b><i>%s</i></b></html>",text);
        }
        label.setText(text);
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public boolean amended() {
        for (ServerField<?> f: fields) {
            if (f.amended) return true;
        }
        return false;
    }

    public void setServers(List<Server> servers) {
        this.servers = servers;
        for (ServerField<?> f : fields) {
            f.initServers(servers);
        }
    }

    public List<Server> getServers() {
        return servers;
    }

    public List<Server> getAmendedServers() {
        ServerTreeNode root = Config.getInstance().getServerConfig().getServerTree();
        List<Server> newServers = new ArrayList<>();
        for (Server server: servers) {
            List<String> folderPath = folderField.getValueForServer(server);
            ServerTreeNode parent = root.findPath(folderPath, true);
            String host = hostField.getValueForServer(server);
            int port = portField.getValueForServer(server);
            TLSResolutionMode tlsResolutionMode = tlsField.getValueForServer(server);
            String username = userField.getValueForServer(server);
            String password = passwordField.getValueForServer(server);
            String authMethod = authMethodField.getValueForServer(server);
            Color bgColor = bgColorField.getValueForServer(server);

            QConnection conn = new QConnection(host, port, username, password, tlsResolutionMode.isUseTLS());
            Server newServer = new Server(server.getName(), conn, authMethod, bgColor, parent, tlsResolutionMode.isFlipTLS());
            newServers.add(newServer);
        }
        return newServers;
    }

    private static class UndoButton extends JLabel {

        private ActionListener listener = null;

        UndoButton() {
            super(Util.UNDO_ICON);
            setDisabledIcon(Util.BLANK_ICON);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    onPress();
                }
            });
        }

        private void onPress() {
            if (listener!=null) listener.actionPerformed(null);
        }

        public void setListener(ActionListener listener) {
            this.listener = listener;
        }
    }

    private static class ServerField<E> implements ChangeListener {
        private boolean amended = false;
        private E commonValue;
        private final String label;
        private final Editor<E> editor;
        private final FieldGetter<E> fieldGetter;
        private ChangeListener changeListener;

        ServerField(String label, Editor<E> editor, FieldGetter<E> fieldGetter) {
            this.label = label;
            this.editor = editor;
            editor.setChangeListener(this);
            this.fieldGetter = fieldGetter;
            this.commonValue = null;
        }

        public void setChangeListener(ChangeListener changeListener) {
            this.changeListener = changeListener;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            amended = ! Objects.equals(commonValue, getValue());
            stateChanged();
        }

        private void stateChanged() {
            if (changeListener != null) changeListener.stateChanged(null);
        }

        String getLabel() {
            return label;
        }

        JComponent getEditor() {
            return editor.getComponent();
        }

        void initServers(List<Server> servers) {
            commonValue = null;

            boolean isFirstServer = true;
            for (Server server: servers) {
                if (isFirstServer) {
                    commonValue = fieldGetter.get(server);
                    isFirstServer = false;
                } else if (commonValue != null) {
                    E value = fieldGetter.get(server);
                    if (!commonValue.equals(value)) {
                        commonValue = null;
                    }
                }

            }

            editor.setValue(commonValue);
            amended = false;
            stateChanged();
        }

        E getValue() {
            return editor.getValue();
        }

        boolean amended() {
            return this.amended;
        }

        boolean theSame() {
            return amended || commonValue != null;
        }
        E getValueForServer(Server server) {
            return amended() ? getValue() : fieldGetter.get(server);
        }

        void resetToCommonValue() {
            editor.setValue(commonValue);
            amended = false;
            stateChanged();
        }

    }

    interface FieldGetter<E> {
        E get(Server server);
    }

    private static abstract class  Editor<E> {
        private ChangeListener changeListener = null;

        public void setChangeListener(ChangeListener changeListener) {
            this.changeListener = changeListener;
        }

        protected void valueChanged() {
            if (changeListener != null) changeListener.stateChanged(null);
        }

        abstract JComponent getComponent();
        abstract void setValue(E value);
        abstract E getValue();
    }

    private static class TextEditor extends Editor<String> {
        private final JTextField txtField = new JTextField();

        public TextEditor() {
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

    private static class PortEditor extends Editor<Integer> {
        private final JTextField txtField = new JTextField();

        public PortEditor() {
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

    private static class PasswordEditor extends Editor<String> {
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

    private static class TLSEditor extends Editor<TLSResolutionMode> {
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

    private static class AuthMethodEditor extends Editor<String> {
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

    private static class BgColorEditor extends Editor<Color> {
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
        JComponent getComponent() {
            return txtField;
        }

        private Color getDefaultBgColor() {
            return Config.getInstance().getEditorColors().get(EditorColorToken.BACKGROUND);
        }

        @Override
        void setValue(Color value) {
            if (value == null) value = getDefaultBgColor();
            txtField.setBackground(value);
            valueChanged();
        }

        @Override
        Color getValue() {
            return txtField.getBackground();
        }
    }

    private static class FolderEditor extends Editor<List<String>> {

        private final JPanel panel = new JPanel(new BorderLayout());
        private final JLabel label = new JLabel();
        private List<String> value = List.of();

        FolderEditor() {
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
        JComponent getComponent() {
            return panel;
        }

        @Override
        void setValue(List<String> value) {
            if (value == null) value = List.of();
            this.value = value;
            label.setText(Server.getFolderName(value));
            valueChanged();
        }

        @Override
        List<String> getValue() {
            return value;
        }
    }


}
