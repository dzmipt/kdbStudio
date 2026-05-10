package studio.ui.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.config.TLSResolutionMode;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MultipleServerEditor extends JPanel {

    private List<Server> servers;

    private final ServerField<List<String>> folderField = new ServerField<>(new Editor.FolderEditor(), FieldGetter.FOLDER_PATH);
    private final ServerField<String> hostField = new ServerField<>(new Editor.TextEditor(), FieldGetter.HOST);
    private final ServerField<Integer> portField = new ServerField<>(new Editor.IntEditor(), FieldGetter.PORT);
    private final ServerField<TLSResolutionMode> tlsField = new ServerField<>(new Editor.TLSEditor(), FieldGetter.TLS);
    private final ServerField<String> userField = new ServerField<>(new Editor.TextEditor(), FieldGetter.USER);
    private final ServerField<String> passwordField = new ServerField<>(new Editor.PasswordEditor(), FieldGetter.PASSWORD);
    private final ServerField<String> authMethodField = new ServerField<>(new Editor.AuthMethodEditor(), FieldGetter.AUTH);
    private final ServerField<Color> bgColorField = new ServerField<>(new Editor.BgColorEditor(), FieldGetter.COLOR);

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
        private final Editor<E> editor;
        private final FieldGetter<E> fieldGetter;
        private ChangeListener changeListener;

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
            return amended() ? getValue() : fieldGetter.getValue(server);
        }

        void resetToCommonValue() {
            editor.setValue(commonValue);
            amended = false;
            stateChanged();
        }

    }

}
