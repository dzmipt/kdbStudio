package studio.mcp.ui;

import studio.kdb.Config;
import studio.kdb.config.McpGrantInterval;
import studio.ui.EscapeDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Asks the user to confirm a query requested by an AI agent. Auto-denies after the timeout.
 */
public class QueryConfirmationDialog extends EscapeDialog {

    public enum Decision {RUN, DENY, ALLOW_FOR_INTERVAL, TIMEOUT}

    private Decision decision = Decision.DENY;
    private final JComboBox<McpGrantInterval> comboInterval;
    private final JLabel lblCountdown;
    private final Timer timer;
    private int secondsLeft;

    public QueryConfirmationDialog(Component owner, String agent, String serverDescription, String target,
                                   String query, int timeoutSeconds) {
        super(owner, "AI Agent Query Confirmation");
        this.secondsLeft = timeoutSeconds;

        JLabel lblHeader = new JLabel("<html><b>" + escape(agent) + "</b> wants to run a query</html>");
        lblHeader.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel pnlInfo = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(1, 0, 1, 8);
        c.gridx = 0; c.gridy = 0;
        pnlInfo.add(new JLabel("Server:"), c);
        c.gridx = 1; c.weightx = 1;
        pnlInfo.add(PlainText.label(serverDescription), c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        pnlInfo.add(new JLabel("Run in:"), c);
        c.gridx = 1; c.weightx = 1;
        pnlInfo.add(PlainText.label(target), c);

        JTextArea txtQuery = new JTextArea(query);
        txtQuery.setEditable(false);
        txtQuery.setFont(Config.getInstance().getFont(Config.FONT_EDITOR));
        txtQuery.setCaretPosition(0);
        int lines = Math.max(3, Math.min(15, query.split("\n", -1).length));
        txtQuery.setRows(lines);
        txtQuery.setColumns(60);
        JScrollPane scrollQuery = new JScrollPane(txtQuery);

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.add(lblHeader, BorderLayout.NORTH);
        pnlTop.add(pnlInfo, BorderLayout.CENTER);

        JButton btnRun = new JButton("Run");
        JButton btnDeny = new JButton("Deny");
        JButton btnAllow = new JButton("Run and allow for");
        comboInterval = new JComboBox<>(McpGrantInterval.values());
        comboInterval.setSelectedItem(Config.getInstance().getEnum(Config.MCP_GRANT_INTERVAL));
        btnAllow.setToolTipText("Run this query and further queries on this server without confirmation");

        btnRun.addActionListener(e -> close(Decision.RUN));
        btnDeny.addActionListener(e -> close(Decision.DENY));
        btnAllow.addActionListener(e -> close(Decision.ALLOW_FOR_INTERVAL));

        lblCountdown = new JLabel();
        refreshCountdown();

        JPanel pnlLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        pnlLeft.add(btnRun);
        pnlLeft.add(btnDeny);
        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        pnlRight.add(btnAllow);
        pnlRight.add(comboInterval);

        JPanel pnlButtons = new JPanel(new BorderLayout());
        pnlButtons.add(pnlLeft, BorderLayout.WEST);
        pnlButtons.add(pnlRight, BorderLayout.EAST);

        JPanel pnlBottom = new JPanel(new BorderLayout(0, 5));
        pnlBottom.add(lblCountdown, BorderLayout.NORTH);
        pnlBottom.add(pnlButtons, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.add(pnlTop, BorderLayout.NORTH);
        root.add(scrollQuery, BorderLayout.CENTER);
        root.add(pnlBottom, BorderLayout.SOUTH);
        setContentPane(root);

        timer = new Timer(1000, e -> {
            secondsLeft--;
            if (secondsLeft <= 0) close(Decision.TIMEOUT);
            else refreshCountdown();
        });
        setAlwaysOnTop(true);
        // safest default for an accidental Enter
        getRootPane().setDefaultButton(btnDeny);
        SwingUtilities.invokeLater(btnDeny::requestFocusInWindow);
    }

    private static String escape(String text) {
        return PlainText.escape(text);
    }

    private void refreshCountdown() {
        lblCountdown.setText(String.format("Auto-deny in %d:%02d", secondsLeft / 60, secondsLeft % 60));
    }

    private void close(Decision decision) {
        this.decision = decision;
        timer.stop();
        dispose();
    }

    @Override
    public void cancel() {
        close(Decision.DENY);
    }

    public McpGrantInterval getInterval() {
        return (McpGrantInterval) comboInterval.getSelectedItem();
    }

    /**
     * Shows the modal dialog; should be called on the EDT
     */
    public Decision showDialog() {
        timer.start();
        alignAndShow();
        timer.stop();
        return decision;
    }
}
