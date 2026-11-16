package studio.mcp.ui;

import studio.kdb.Config;
import studio.kdb.config.McpAccess;
import studio.kdb.config.McpTokenMode;
import studio.mcp.AgentConfigTemplates;
import studio.mcp.McpService;
import studio.ui.DocumentChangeListener;
import studio.ui.EscapeDialog;
import studio.ui.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.net.InetAddress;

/**
 * Shows configuration snippets to add the Studio MCP server to AI clients
 */
public class ConnectAgentDialog extends EscapeDialog {

    private final int port;
    private final boolean remoteAccess;
    private final McpTokenMode tokenMode;
    private final String token;

    private final JComboBox<AgentConfigTemplates.Client> comboClient;
    private final JTextField txtHost;
    private final JTextArea txtInstructions;
    private final JTextArea txtSnippet;

    public static void showDialog(Component owner) {
        Config config = Config.getInstance();
        new ConnectAgentDialog(owner, config.getInt(Config.MCP_PORT),
                config.getEnum(Config.MCP_ACCESS) == McpAccess.REMOTE,
                config.getEnum(Config.MCP_TOKEN_MODE), McpService.getToken()).alignAndShow();
    }

    public ConnectAgentDialog(Component owner, int port, boolean remoteAccess, McpTokenMode tokenMode, String token) {
        this(owner, port, remoteAccess, tokenMode, token, null);
    }

    /**
     * @param warning shown at the top of the dialog (e.g. about unsaved settings); can be null
     */
    public ConnectAgentDialog(Component owner, int port, boolean remoteAccess, McpTokenMode tokenMode, String token,
                              String warning) {
        super(owner, "Connect an AI Agent");
        this.port = port;
        this.remoteAccess = remoteAccess;
        this.tokenMode = tokenMode;
        this.token = token;

        comboClient = new JComboBox<>(AgentConfigTemplates.Client.values());
        comboClient.addActionListener(e -> refresh());

        txtHost = new JTextField(remoteAccess ? localHostName() : "127.0.0.1", 20);
        txtHost.setEnabled(remoteAccess);
        txtHost.setToolTipText(remoteAccess ? "Host name or IP address of this machine as seen from the agent's machine"
                                            : "Only local connections are allowed (see Settings)");
        txtHost.getDocument().addDocumentListener((DocumentChangeListener) (DocumentEvent e) -> refresh());

        txtInstructions = new JTextArea(2, 60);
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setOpaque(false);
        txtInstructions.setBorder(null);

        txtSnippet = new JTextArea(14, 70);
        txtSnippet.setEditable(false);
        txtSnippet.setFont(Config.getInstance().getFont(Config.FONT_EDITOR));

        JButton btnCopy = new JButton("Copy");
        btnCopy.addActionListener(e -> Util.copyTextToClipboard(txtSnippet.getText()));
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        JPanel pnlTop = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 8);
        c.gridx = 0; c.gridy = 0;
        pnlTop.add(new JLabel("AI client:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        pnlTop.add(comboClient, c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        pnlTop.add(new JLabel("Studio host:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        pnlTop.add(txtHost, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        c.insets = new Insets(8, 0, 4, 0);
        pnlTop.add(txtInstructions, c);

        JLabel lblNote = new JLabel("<html>The access token gives full access to the MCP server; keep it private. " +
                "Client configuration formats change over time - check the client's documentation if a snippet doesn't work.</html>");
        lblNote.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.add(btnCopy);
        pnlButtons.add(btnClose);

        JPanel pnlBottom = new JPanel(new BorderLayout());
        pnlBottom.add(lblNote, BorderLayout.CENTER);
        pnlBottom.add(pnlButtons, BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout(0, 5));
        root.setBorder(new EmptyBorder(10, 10, 5, 10));
        if (warning != null) {
            JLabel lblWarning = new JLabel("<html>" + warning + "</html>");
            lblWarning.setForeground(new Color(200, 0, 0));
            lblWarning.setBorder(new EmptyBorder(0, 0, 8, 0));
            JPanel pnlNorth = new JPanel(new BorderLayout());
            pnlNorth.add(lblWarning, BorderLayout.NORTH);
            pnlNorth.add(pnlTop, BorderLayout.CENTER);
            pnlTop = pnlNorth;
        }
        root.add(pnlTop, BorderLayout.NORTH);
        root.add(new JScrollPane(txtSnippet), BorderLayout.CENTER);
        root.add(pnlBottom, BorderLayout.SOUTH);
        setContentPane(root);

        refresh();
    }

    private static String localHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static boolean isLocalHost(String host) {
        String h = host.trim().toLowerCase();
        return h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1") || h.equals("[::1]");
    }

    private void refresh() {
        AgentConfigTemplates.Client client = (AgentConfigTemplates.Client) comboClient.getSelectedItem();
        String host = txtHost.getText().trim();
        if (host.isEmpty()) host = "127.0.0.1";
        boolean remote = remoteAccess && !isLocalHost(host);
        String url = AgentConfigTemplates.url(host, port);
        String tokenToUse = tokenMode.isRequired(remote) ? token : null;
        txtInstructions.setText(client.getInstructions());
        txtSnippet.setText(AgentConfigTemplates.render(client, url, tokenToUse));
        txtSnippet.setCaretPosition(0);
    }
}
