
package studio.ui;

import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.Workspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static javax.swing.GroupLayout.Alignment.*;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

public class ServerForm extends EscapeDialog {
    private Server server;

    public ServerForm(Component windowOwner, String title, Server server) {
        super(windowOwner, title);
        this.server = server;
        initComponents();

        txtName.setText(this.server.getName());

        updateFolderLabel();
        txtHostname.setText(this.server.getHost());
        txtUsername.setText(this.server.getUsername());
        txtPort.setText("" + this.server.getPort());
        txtPassword.setText(this.server.getPassword());
        chkBoxUseTLS.setSelected(this.server.getUseTLS());
        DefaultComboBoxModel<String> dcbm = (DefaultComboBoxModel<String>) authenticationMechanism.getModel();
        String[] am;
        am = AuthenticationManager.getInstance().getAuthenticationMechanisms();
        boolean foundAuth = false;
        for (int i = 0; i < am.length; i++) {
            dcbm.addElement(am[i]);
            if (this.server.getAuthenticationMechanism().equals(am[i])) {
                dcbm.setSelectedItem(am[i]);
                foundAuth = true;
            }
        }

        if (!foundAuth) {
            String authMethod = server.getAuthenticationMechanism();
            dcbm.setSelectedItem(authMethod);
        }

        authenticationMechanism.addItemListener(e -> {
            String auth = authenticationMechanism.getSelectedItem().toString();
            Credentials credentials = Config.getInstance().getDefaultCredentials(auth);
            txtUsername.setText(credentials.getUsername());
            txtPassword.setText(credentials.getPassword());
        });

        txtName.setToolTipText("The logical name for the server");
        txtHostname.setToolTipText("The hostname or ip address for the server");
        txtPort.setToolTipText("The port for the server");
        chkBoxUseTLS.setToolTipText("Use TLS for a secure connection");
        txtUsername.setToolTipText("The username used to connect to the server");
        txtPassword.setToolTipText("The password used to connect to the server");
        authenticationMechanism.setToolTipText("The authentication mechanism to use");

        sampleTextOnBackgroundTextField.setBackground(this.server.getBackgroundColor());
        sampleTextOnBackgroundTextField.setEditable(false);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                txtName.requestFocus();
            }
        });
        getRootPane().setDefaultButton(okButton);
    }

    private void updateFolderLabel() {
        String folderName = server.getFolderName();
        if (folderName.equals("")) {
            folderName = "/";
        }
        folderTextLabel.setText(folderName);
    }

    private void updateFolder() {
        ServerList serverList = new ServerList(this, Workspace.DEFAULT_BOUNDS);

        ServerTreeNode folder = Config.getInstance().getServerTree().findPath(server.getFolderPath(), false);

        ServerTreeNode newFolder = serverList.showFolders(folder);
        if (newFolder != null && newFolder != folder) {
            server = server.newFolder(newFolder);
            updateFolderLabel();
        }
    }

    private void initComponents() {

        folderLabel = new JLabel("Folder");
        folderTextLabel = new JLabel();
        folderChangeButton = new JButton("...");
        folderChangeButton.addActionListener(e -> updateFolder());

        logicalNameLabel = new javax.swing.JLabel();
        txtName = new javax.swing.JTextField();
        hostnameLabel = new javax.swing.JLabel();
        txtHostname = new javax.swing.JTextField();
        portLabel = new javax.swing.JLabel();
        txtPort = new javax.swing.JTextField();
        usernameLabel = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        passwordLabel = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        okButton.setName("okButton");
        cancelButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        txtPassword = new javax.swing.JPasswordField();
        colorLabel = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        btnEditColor = new javax.swing.JButton();
        btnEditColor.setName("editColor");
        sampleTextOnBackgroundTextField = new javax.swing.JTextField();
        sampleTextOnBackgroundTextField.setName("sampleTextOnBackground");
        authenticationMechanism = new javax.swing.JComboBox();
        authMethodLabel = new javax.swing.JLabel();
        chkBoxUseTLS = new javax.swing.JCheckBox();
        tlsLabel = new javax.swing.JLabel();

        logicalNameLabel.setText("Name");

        hostnameLabel.setText("Host");

        portLabel.setText("Port");

        usernameLabel.setText("Username");

        passwordLabel.setText("Password");

        okButton.setText("Ok");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onOk(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onCancel(evt);
            }
        });

        colorLabel.setText("Color");

        btnEditColor.setText("Edit Color");
        btnEditColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onColor(evt);
            }
        });

        sampleTextOnBackgroundTextField.setText("Sample text on background");


        authMethodLabel.setText("Auth. Method");

        tlsLabel.setText("Use TLS");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(logicalNameLabel)
                                        .addComponent(folderLabel)
                                        .addComponent(hostnameLabel)
                                        .addComponent(portLabel)
                                        .addComponent(tlsLabel)
                                        .addComponent(usernameLabel)
                                        .addComponent(passwordLabel)
                                        .addComponent(authMethodLabel)
                                        .addComponent(colorLabel))
                                .addPreferredGap(RELATED, 21, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(btnEditColor)
                                                .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(cancelButton)
                                                .addPreferredGap(RELATED)
                                                .addComponent(okButton)
                                                .addGap(6, 6, 6))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(folderTextLabel)
                                                .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(folderChangeButton)
                                                .addGap(6, 6, 6))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(LEADING)
                                                        .addComponent(sampleTextOnBackgroundTextField, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(authenticationMechanism, 0, 418, Short.MAX_VALUE)
                                                        .addComponent(txtPassword, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(txtUsername, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(chkBoxUseTLS)
                                                        .addComponent(txtPort, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(txtHostname, DEFAULT_SIZE, 418, Short.MAX_VALUE)
                                                        .addComponent(txtName, DEFAULT_SIZE, 418, Short.MAX_VALUE))
                                                .addContainerGap())))
                        .addGroup(TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addGroup(TRAILING, layout.createSequentialGroup()
                                                .addContainerGap(45, Short.MAX_VALUE)
                                                .addComponent(jSeparator2, DEFAULT_SIZE, 471, Short.MAX_VALUE))
                                        .addGroup(TRAILING, layout.createSequentialGroup()
                                                .addContainerGap(45, Short.MAX_VALUE)
                                                .addComponent(jSeparator3, DEFAULT_SIZE, 471, Short.MAX_VALUE))
                                        .addGroup(TRAILING, layout.createSequentialGroup()
                                                .addContainerGap(45, Short.MAX_VALUE)
                                                .addComponent(jSeparator1, DEFAULT_SIZE, 471, Short.MAX_VALUE)))
                                .addGap(20, 20, 20))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(logicalNameLabel)
                                        .addComponent(txtName, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(folderLabel)
                                        .addComponent(folderTextLabel)
                                        .addComponent(folderChangeButton)
                                )
                                .addPreferredGap(RELATED)
                                .addComponent(jSeparator1, PREFERRED_SIZE, 10, PREFERRED_SIZE)
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(hostnameLabel)
                                        .addComponent(txtHostname, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(portLabel)
                                        .addComponent(txtPort, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(LEADING)
                                        .addComponent(tlsLabel, TRAILING)
                                        .addComponent(chkBoxUseTLS, TRAILING))
                                .addPreferredGap(RELATED)
                                .addComponent(jSeparator2, PREFERRED_SIZE, 10, PREFERRED_SIZE)
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(usernameLabel)
                                        .addComponent(txtUsername, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE, false)
                                        .addComponent(passwordLabel)
                                        .addComponent(txtPassword, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(authenticationMechanism, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
                                        .addComponent(authMethodLabel))
                                .addPreferredGap(RELATED)
                                .addComponent(jSeparator3, PREFERRED_SIZE, 10, PREFERRED_SIZE)
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(colorLabel)
                                        .addComponent(sampleTextOnBackgroundTextField, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE))
                                .addPreferredGap(RELATED)
                                .addGroup(layout.createParallelGroup(BASELINE)
                                        .addComponent(okButton)
                                        .addComponent(cancelButton)
                                        .addComponent(btnEditColor))
                                .addContainerGap())
        );
    }

    private void onOk(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onOk
        txtName.setText(txtName.getText().trim());
        txtHostname.setText(txtHostname.getText().trim());
        txtUsername.setText(txtUsername.getText().trim());
        txtPort.setText(txtPort.getText().trim());
        txtPassword.setText(new String(txtPassword.getPassword()).trim());

        if (txtName.getText().length() == 0) {
            StudioOptionPane.showError(this, "The server's name cannot be empty", "Studio for kdb+");
            txtName.requestFocus();
            return;
        }

        int port = txtPort.getText().trim().length() == 0 ? 0 : Integer.parseInt(txtPort.getText());
        server = new Server(txtName.getText().trim(),
                txtHostname.getText().trim(),
                port,
                txtUsername.getText().trim(),
                new String(txtPassword.getPassword()),
                sampleTextOnBackgroundTextField.getBackground(),
                (String) authenticationMechanism.getSelectedItem(),
                chkBoxUseTLS.isSelected(),
                Config.getInstance().getServerTree().findPath(server.getFolderPath(), true)
                );

        accept();
    }


    public Server getServer() {
        return server;
    }

    private void onCancel(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onCancel
        cancel();
    }

    private void onColor(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onColor
        Color result = ColorChooser.chooseColor(this, "Select background color for editor",
                sampleTextOnBackgroundTextField.getBackground());

        if (result != null) {
            sampleTextOnBackgroundTextField.setBackground(result);
        }
    }

    private javax.swing.JButton btnEditColor;
    private javax.swing.JTextField sampleTextOnBackgroundTextField;
    private javax.swing.JComboBox<String> authenticationMechanism;
    private javax.swing.JButton cancelButton;
    private javax.swing.JTextField txtHostname;
    private javax.swing.JLabel hostnameLabel;
    private javax.swing.JCheckBox chkBoxUseTLS;
    private javax.swing.JLabel colorLabel;
    private javax.swing.JLabel tlsLabel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JTextField txtName;
    private javax.swing.JLabel logicalNameLabel;
    private javax.swing.JButton okButton;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JLabel authMethodLabel;
    private javax.swing.JTextField txtPort;
    private javax.swing.JLabel portLabel;
    private javax.swing.JTextField txtUsername;
    private javax.swing.JLabel usernameLabel;

    private JLabel folderLabel;
    private JLabel folderTextLabel;
    private JButton folderChangeButton;
}
