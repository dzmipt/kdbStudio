package studio.ui.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.Credentials;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.config.server.BgColorRules;
import studio.kdb.config.server.Editor;
import studio.kdb.config.server.FieldGetter;
import studio.ui.GroupLayoutSimple;
import studio.ui.Util;
import studio.utils.QConnection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class MultipleServerEditor extends JPanel {

    private List<Server> servers;

    private final Editor.BgColorEditor bgColorEditor = new Editor.BgColorEditor();
    private final ServerField<String> nameField = new ServerField<>(new Editor.TextEditor(), FieldGetter.NAME);
    private final ServerField<List<String>> folderField = new ServerField<>(new Editor.FolderEditor(), FieldGetter.FOLDER_PATH);
    private final ServerField<String> hostField = new ServerField<>(new Editor.TextEditor(), FieldGetter.HOST);
    private final ServerField<Integer> portField = new ServerField<>(new Editor.PortEditor(), FieldGetter.PORT);
    private final ServerField<Boolean> tlsField = new ServerField<>(new Editor.BooleanEditor(), FieldGetter.TLS);
    private final ServerField<Boolean> defaultAuthField = new ServerField<>(new Editor.BooleanEditor(), FieldGetter.DEFAULT_AUTH);
    private final ServerField<String> authMethodField = new ServerField<>(new Editor.AuthMethodEditor(), FieldGetter.SERVER_AUTH);
    private final ServerField<Boolean> defaultCredentialsField = new ServerField<>(new Editor.BooleanEditor(), FieldGetter.DEFAULT_CREDENTIALS);
    private final ServerField<String> userField = new ServerField<>(new Editor.TextEditor(), FieldGetter.SERVER_USER);
    private final ServerField<String> passwordField = new ServerField<>(new Editor.PasswordEditor(), FieldGetter.SERVER_PASSWORD);
    private final ServerField<Color> bgColorField = new ServerField<>(bgColorEditor, FieldGetter.COLOR);

    private final ServerField<?>[] initFields =
            {nameField, folderField, null, hostField, portField, tlsField, null,
                    defaultAuthField, authMethodField, defaultCredentialsField, userField, passwordField, null, bgColorField};

    private final List<ServerField<?>> fields = new ArrayList<>();
    private final boolean editName;

    private ChangeListener changeListener = null;
    private final static Logger log = LogManager.getLogger();

    public MultipleServerEditor(boolean editName) {
        this.editName = editName;
        setServers(new ArrayList<>());


        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);
        GroupLayoutSimple.Stack stackUndo = new GroupLayoutSimple.Stack();
        GroupLayoutSimple.Stack stackLabels = new GroupLayoutSimple.Stack();
        GroupLayoutSimple.Stack stackEditors = new GroupLayoutSimple.Stack();

        List<JSeparator> separators = new ArrayList<>();
        List<JLabel> labels = new ArrayList<>();
        for (ServerField<?> f: initFields) {
            if (f == null) {
                JSeparator separator = new JSeparator();
                separator.setPreferredSize(new Dimension(470,10));
                separators.add(separator);

                stackUndo.addLine(32, 10).continueLine(separator).continueLine(8,10);
                stackLabels.skipLine();
                stackEditors.skipLine();
                continue;
            }

            if (!editName && f.fieldGetter == FieldGetter.NAME) continue;

            fields.add(f);

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
                refreshAuthFields();
                if (changeListener != null) changeListener.stateChanged(e);
                refreshColorOverride();
            } );
            undoButton.setListener(e-> f.resetToCommonValue());

            stackUndo.addLine(undoButton);

            refreshColorOverride();
        }
        refreshAuthFields();

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
                fields.stream().map(ServerField::getEditor).toArray(Component[]::new)
        );
        layout.addMaxWidthComponents(separators.toArray(Component[]::new));

        layout.setStacks(stackUndo, stackLabels, stackEditors);
    }

    private void refreshColorOverride() {
        if (servers.isEmpty()) {
            bgColorEditor.setColorOverride(null);
            return;
        }

        BgColorRules rules = Config.getInstance().getServerBgColorRules();
        Set<FieldGetter.Names> ruleFields = rules.getFieldGetterSet();

        Set<FieldGetter.Names> knownFields = new HashSet<>();

        for (ServerField<?> f: fields) {
            if (isKnown(f)) {
                knownFields.add(f.fieldGetter.getName());
            }
        }
        refreshKnownAuthFields(knownFields);

        ruleFields.removeAll(knownFields);
        if (knownFields.contains(FieldGetter.Names.folderPath)) {
            ruleFields.remove(FieldGetter.Names.folderName);

            if (knownFields.contains(FieldGetter.Names.name)) {
                ruleFields.remove(FieldGetter.Names.fullName);
            }
        }

        if (! ruleFields.isEmpty()) {
            bgColorEditor.setColorOverride(null);
            return;
        }

        Server amendedServer = amendServer(servers.get(0));
        bgColorEditor.setColorOverride(rules.overrideColor(amendedServer) );

    }

    private boolean isKnown(ServerField<?> field) {
        return field.theSame() || field.amended();
    }

    // Auth. method, user and password are known (the same for all servers) only together with default flags
    private void refreshKnownAuthFields(Set<FieldGetter.Names> knownFields) {
        Server server = servers.get(0);
        boolean knownAuth = false, knownCredentials = false;
        if (isKnown(defaultAuthField)) {
            if (defaultAuthField.getValueForServer(server)) {
                knownAuth = knownCredentials = true;
            } else {
                knownAuth = isKnown(authMethodField);
                if (isKnown(defaultCredentialsField)) {
                    knownCredentials = defaultCredentialsField.getValueForServer(server) ?
                            knownAuth : isKnown(userField) && isKnown(passwordField);
                }
            }
        }

        if (knownAuth) knownFields.add(FieldGetter.Names.auth);
        else knownFields.remove(FieldGetter.Names.auth);

        if (knownCredentials) {
            knownFields.add(FieldGetter.Names.user);
            knownFields.add(FieldGetter.Names.password);
        } else {
            knownFields.remove(FieldGetter.Names.user);
            knownFields.remove(FieldGetter.Names.password);
        }
    }

    // If default auth.method or default credentials are selected, the corresponding fields are disabled
    // and show values from the Settings
    private void refreshAuthFields() {
        Config config = Config.getInstance();
        if (defaultAuthField.getValue()) {
            String auth = config.getDefaultAuthMechanism();
            Credentials credentials = config.getDefaultCredentials(auth);
            authMethodField.override(auth);
            defaultCredentialsField.override(true);
            userField.override(credentials.getUsername());
            passwordField.override(credentials.getPassword());
        } else {
            authMethodField.release();
            defaultCredentialsField.release();
            if (defaultCredentialsField.getValue()) {
                String auth = authMethodField.getValue();
                Credentials credentials = auth == null ? Credentials.DEFAULT : config.getDefaultCredentials(auth);
                userField.override(credentials.getUsername());
                passwordField.override(credentials.getPassword());
            } else {
                userField.release();
                passwordField.release();
            }
        }
    }

    private void updateLabelText(JLabel label, ServerField<?> field) {
        String text = field.getLabel();
        if ((servers.size()>1 && field.theSame()) || field.amended()) {
            text = String.format("<html><b><i>%s</i></b></html>", text);
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

    private Server amendServer(Server server) {
        ServerTreeNode root = Config.getInstance().getServerConfig().getServerTree();
        String name = editName ? nameField.getValueForServer(server) : server.getName();

        List<String> folderPath = folderField.getValueForServer(server);
        ServerTreeNode parent = root.findPath(folderPath, true);
        String host = hostField.getValueForServer(server);
        int port = portField.getValueForServer(server);
        boolean useTLS = tlsField.getValueForServer(server);
        String username = userField.getValueForServer(server);
        String password = passwordField.getValueForServer(server);
        String authMethod = authMethodField.getValueForServer(server);
        boolean defaultAuth = defaultAuthField.getValueForServer(server);
        boolean defaultCredentials = defaultCredentialsField.getValueForServer(server);
        Color bgColor = bgColorField.getValueForServer(server);

        QConnection conn = new QConnection(host, port, username, password, useTLS);
        return new Server(name, conn, authMethod, bgColor, parent, false, defaultAuth, defaultCredentials);
    }

    public List<Server> getAmendedServers() {
        List<Server> newServers = new ArrayList<>();
        for (Server server: servers) {
            newServers.add(amendServer(server));
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
        private final Editor<E> editor;
        private final FieldGetter<E> fieldGetter;
        private ChangeListener changeListener;
        // when overridden, the editor is disabled and shows a value which is not the field value
        private boolean overridden = false;
        private E overriddenValue;
        private boolean ignoreEditorChanges = false;

        ServerField(Editor<E> editor, FieldGetter<E> fieldGetter) {
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
            if (ignoreEditorChanges) return;
            amended = ! Objects.equals(commonValue, getValue());
            stateChanged();
        }

        private void stateChanged() {
            if (changeListener != null) changeListener.stateChanged(null);
        }

        String getLabel() {
            return fieldGetter.getName().toString();
        }

        JComponent getEditor() {
            return editor.getComponent();
        }

        void initServers(List<Server> servers) {
            commonValue = null;

            boolean isFirstServer = true;
            for (Server server: servers) {
                if (isFirstServer) {
                    commonValue = fieldGetter.getValue(server);
                    isFirstServer = false;
                } else if (commonValue != null) {
                    E value = fieldGetter.getValue(server);
                    if (!commonValue.equals(value)) {
                        commonValue = null;
                    }
                }

            }

            setEditorValue(commonValue);
            amended = false;
            stateChanged();
        }

        private void setEditorValue(E value) {
            if (overridden) overriddenValue = value;
            else editor.setValue(value);
        }

        private void setEditorValueSilently(E value) {
            ignoreEditorChanges = true;
            try {
                editor.setValue(value);
            } finally {
                ignoreEditorChanges = false;
            }
        }

        void override(E value) {
            if (! overridden) {
                overriddenValue = getValue();
                overridden = true;
                editor.getComponent().setEnabled(false);
            }
            setEditorValueSilently(value);
        }

        void release() {
            if (! overridden) return;
            overridden = false;
            setEditorValueSilently(overriddenValue);
            editor.getComponent().setEnabled(true);
        }

        E getValue() {
            if (overridden) return overriddenValue;
            try {
                return editor.getValue();
            } catch (RuntimeException e) {
                throw new RuntimeException(
                        String.format("Can't parse field %s. Error: %s", fieldGetter.getName(), e.getMessage()), e);
            }
        }

        boolean amended() {
            return this.amended;
        }

        boolean theSame() {
            return commonValue != null;
        }
        E getValueForServer(Server server) {
            return amended() ? getValue() : fieldGetter.getValue(server);
        }

        void resetToCommonValue() {
            setEditorValue(commonValue);
            amended = false;
            stateChanged();
        }

    }

}
