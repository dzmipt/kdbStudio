package studio.ui.server;

import studio.kdb.Server;
import studio.ui.EscapeDialog;
import studio.ui.StudioOptionPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class EditServerDialog extends EscapeDialog {
    private final MultipleServerEditor serverEditor;

    private Server server;

    public EditServerDialog(Component windowOwner, String title, Server server) {
        super(windowOwner, title);
        this.server = server;

        serverEditor = new MultipleServerEditor(true);
        serverEditor.setServers(List.of(server));

        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(this::onOK);
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(this::onCancel);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(btnOK);
        bottom.add(btnCancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(serverEditor, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

    }

    public Server getServer() {
        return server;
    }

    private void onOK(ActionEvent evt) {
        try {
            server = serverEditor.getAmendedServers().get(0);
            if (server.getName().isBlank()) {
                throw new RuntimeException("Server name can't be blank");
            }
            accept();
        } catch (RuntimeException e) {
            StudioOptionPane.showError(this, e.getMessage(), "Error");
        }
    }

    private void onCancel(ActionEvent evt) {
        cancel();
    }
}
