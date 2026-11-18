package studio.ui.settings;

import studio.kdb.Config;
import studio.kdb.config.McpAccess;
import studio.kdb.config.McpTokenMode;
import studio.mcp.McpService;
import studio.mcp.ui.ConnectAgentDialog;
import studio.mcp.ui.PermissionTree;
import studio.ui.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;
import java.awt.*;

/**
 * Settings of the MCP server which lets AI agents work with Studio
 */
public class SettingsMcpTab extends SettingsTab {

    private final JCheckBox chkEnabled;
    private final JFormattedTextField txtPort;
    private final JComboBox<McpAccess> comboAccess;
    private final JComboBox<McpTokenMode> comboTokenMode;
    private final JTextField txtToken;
    private final JLabel lblTokenNote = new JLabel();
    private final JCheckBox chkAllowAdHoc;
    private final JCheckBox chkShowAgentWindow;
    private final JFormattedTextField txtMaxRows;
    private final JFormattedTextField txtMaxChars;
    private final JFormattedTextField txtTimeout;
    private final PermissionTree permissionTree;

    private static final Config CONFIG = Config.getInstance();

    private static JFormattedTextField intField(int min, int max, int value) {
        NumberFormatter formatter = new NumberFormatter(new java.text.DecimalFormat("#"));
        formatter.setValueClass(Integer.class);
        formatter.setMinimum(min);
        formatter.setMaximum(max);
        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(7);
        field.setValue(value);
        return field;
    }

    private static JPanel line(Component... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        for (Component c: components) panel.add(c);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    public SettingsMcpTab() {
        chkEnabled = new JCheckBox("Enable MCP server (lets AI agents such as Claude Code or Copilot work with Studio)");
        chkEnabled.setSelected(CONFIG.getBoolean(Config.MCP_ENABLED));

        txtPort = intField(1, 65535, CONFIG.getInt(Config.MCP_PORT));
        comboAccess = new JComboBox<>(McpAccess.values());
        comboAccess.setSelectedItem(CONFIG.getEnum(Config.MCP_ACCESS));

        comboTokenMode = new JComboBox<>(McpTokenMode.values());
        comboTokenMode.setSelectedItem(CONFIG.getEnum(Config.MCP_TOKEN_MODE));
        txtToken = new JTextField(McpService.getToken(), 24);
        txtToken.setEditable(false);
        JButton btnCopyToken = new JButton("Copy");
        btnCopyToken.addActionListener(e -> Util.copyTextToClipboard(txtToken.getText()));
        JButton btnRegenerate = new JButton("Regenerate");
        btnRegenerate.addActionListener(e -> {
            txtToken.setText(McpService.generateToken());
            lblTokenNote.setText("New token - press Apply, then update the configuration of your AI clients");
        });

        lblTokenNote.setForeground(new Color(200, 0, 0));

        chkAllowAdHoc = new JCheckBox("Allow ad-hoc connections (any host:port with credentials provided by the agent)");
        chkAllowAdHoc.setSelected(CONFIG.getBoolean(Config.MCP_ALLOW_ADHOC));

        chkShowAgentWindow = new JCheckBox("Show queries which agents run on background connections in a separate window");
        chkShowAgentWindow.setSelected(CONFIG.getBoolean(Config.MCP_SHOW_AGENT_WINDOW));

        txtMaxRows = intField(1, 1_000_000, CONFIG.getInt(Config.MCP_MAX_ROWS));
        txtMaxChars = intField(1000, 10_000_000, CONFIG.getInt(Config.MCP_MAX_CHARS));
        txtTimeout = intField(10, 3600, CONFIG.getInt(Config.MCP_CONFIRMATION_TIMEOUT_SEC));

        JButton btnConnect = new JButton("Connect an agent...");
        btnConnect.addActionListener(e -> new ConnectAgentDialog(this, getPort(),
                comboAccess.getSelectedItem() == McpAccess.REMOTE,
                (McpTokenMode) comboTokenMode.getSelectedItem(), txtToken.getText(),
                isServerSettingsModified() ? "MCP settings are changed but not applied yet. " +
                        "Press Apply or OK in the Settings, otherwise the running server will reject this configuration." : null
        ).alignAndShow());

        permissionTree = new PermissionTree(CONFIG.getServerTree(), McpService.getPermissions());
        permissionTree.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lblServers = new JLabel("Servers and folders AI agents may use (every query is confirmed in Studio):");
        lblServers.setBorder(new EmptyBorder(10, 5, 2, 0));
        lblServers.setAlignmentX(LEFT_ALIGNMENT);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(5, 5, 5, 5));
        add(line(chkEnabled));
        add(line(new JLabel("Port:"), txtPort, new JLabel("   Accept connections from:"), comboAccess));
        add(line(new JLabel("Access token:"), comboTokenMode));
        add(line(new JLabel("Token:"), txtToken, btnCopyToken, btnRegenerate));
        add(line(lblTokenNote));
        add(line(chkAllowAdHoc));
        add(line(chkShowAgentWindow));
        add(line(new JLabel("Max rows returned:"), txtMaxRows, new JLabel("   Max characters returned:"), txtMaxChars));
        add(line(new JLabel("Query confirmation timeout:"), txtTimeout, new JLabel("seconds (unanswered queries are denied)")));
        add(line(btnConnect));
        add(lblServers);
        add(permissionTree);
    }

    private static int value(JFormattedTextField field) {
        try {
            field.commitEdit();
        } catch (java.text.ParseException ignored) {
            // keep the last valid value
        }
        return (Integer) field.getValue();
    }

    private int getPort() {
        return value(txtPort);
    }

    // Are settings affecting how agents connect different from the saved (running) ones
    private boolean isServerSettingsModified() {
        return chkEnabled.isSelected() != CONFIG.getBoolean(Config.MCP_ENABLED)
                || getPort() != CONFIG.getInt(Config.MCP_PORT)
                || comboAccess.getSelectedItem() != CONFIG.getEnum(Config.MCP_ACCESS)
                || comboTokenMode.getSelectedItem() != CONFIG.getEnum(Config.MCP_TOKEN_MODE)
                || !txtToken.getText().equals(CONFIG.getString(Config.MCP_TOKEN));
    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        CONFIG.setBoolean(Config.MCP_ENABLED, chkEnabled.isSelected());
        CONFIG.setInt(Config.MCP_PORT, getPort());
        CONFIG.setEnum(Config.MCP_ACCESS, (McpAccess) comboAccess.getSelectedItem());
        CONFIG.setEnum(Config.MCP_TOKEN_MODE, (McpTokenMode) comboTokenMode.getSelectedItem());
        CONFIG.setString(Config.MCP_TOKEN, txtToken.getText());
        CONFIG.setBoolean(Config.MCP_ALLOW_ADHOC, chkAllowAdHoc.isSelected());
        CONFIG.setBoolean(Config.MCP_SHOW_AGENT_WINDOW, chkShowAgentWindow.isSelected());
        CONFIG.setInt(Config.MCP_MAX_ROWS, value(txtMaxRows));
        CONFIG.setInt(Config.MCP_MAX_CHARS, value(txtMaxChars));
        CONFIG.setInt(Config.MCP_CONFIRMATION_TIMEOUT_SEC, value(txtTimeout));
        McpService.setPermissions(permissionTree.getPermissions());
        McpService.getInstance().applyConfig();
        lblTokenNote.setText("");
    }
}
