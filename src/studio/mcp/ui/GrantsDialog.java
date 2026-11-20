package studio.mcp.ui;

import studio.mcp.McpGrants;
import studio.mcp.McpLog;
import studio.mcp.McpService;
import studio.ui.EscapeDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Non-modal window with active time-limited grants for AI agent queries
 */
public class GrantsDialog extends EscapeDialog implements McpGrants.Listener {

    private static GrantsDialog instance = null;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JPanel pnlGrants;
    private final JButton btnRevokeAll;
    private final Timer timer;

    public static void showDialog(Component owner) {
        if (instance == null) {
            instance = new GrantsDialog(owner);
            instance.align();
        }
        instance.setVisible(true);
        instance.toFront();
    }

    private GrantsDialog(Component owner) {
        super(owner, "AI Agent Query Permissions", ModalityType.MODELESS);
        setRemoveContentOnDispose(false);

        pnlGrants = new JPanel(new GridBagLayout());
        pnlGrants.setBorder(new EmptyBorder(5, 5, 5, 5));

        btnRevokeAll = new JButton("Revoke all");
        btnRevokeAll.addActionListener(e -> {
            McpLog.info("All query grants are revoked by the user");
            McpService.getInstance().getGrants().revokeAll();
        });
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.add(btnRevokeAll);
        pnlButtons.add(btnClose);

        JLabel lblInfo = new JLabel("Queries on these servers run without confirmation until the permission expires.");
        lblInfo.setBorder(new EmptyBorder(8, 8, 0, 8));

        JPanel root = new JPanel(new BorderLayout());
        root.add(lblInfo, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(pnlGrants);
        scroll.setPreferredSize(new Dimension(680, 220));
        root.add(scroll, BorderLayout.CENTER);
        root.add(pnlButtons, BorderLayout.SOUTH);
        setContentPane(root);

        timer = new Timer(1000, e -> rebuild());
        McpService.getInstance().getGrants().addListener(this);
        rebuild();
        timer.start();
    }

    @Override
    public void grantsChanged() {
        SwingUtilities.invokeLater(this::rebuild);
    }

    private static String formatTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    private static String formatRemaining(Duration duration) {
        long seconds = Math.max(0, duration.getSeconds());
        if (seconds >= 3600) return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void addCell(Component component, int row, int col, double weight) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = col;
        c.gridy = row;
        c.weightx = weight;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 4, 2, 8);
        pnlGrants.add(component, c);
    }

    private JLabel header(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private void rebuild() {
        McpGrants grants = McpService.getInstance().getGrants();
        List<McpGrants.Grant> active = grants.getActive();
        Instant now = grants.now();

        pnlGrants.removeAll();
        String[] headers = {"Server", "Granted at", "Expires", "Remaining", "Queries run", ""};
        for (int col = 0; col < headers.length; col++) {
            addCell(header(headers[col]), 0, col, col == 0 ? 1 : 0);
        }

        int row = 1;
        for (McpGrants.Grant grant: active) {
            addCell(PlainText.label(grant.getLabel()), row, 0, 1);
            addCell(new JLabel(formatTime(grant.getGrantedAt())), row, 1, 0);
            Instant expires = grant.getExpiresAt();
            addCell(new JLabel(expires == null ? "until Studio exits" : formatTime(expires)), row, 2, 0);
            addCell(new JLabel(expires == null ? "-" : formatRemaining(Duration.between(now, expires))), row, 3, 0);
            addCell(new JLabel(String.valueOf(grant.getQueryCount())), row, 4, 0);
            JButton btnRevoke = new JButton("Revoke");
            String key = grant.getKey();
            btnRevoke.addActionListener(e -> {
                McpLog.info("Query grant for {} is revoked by the user", grant.getLabel());
                grants.revoke(key);
            });
            addCell(btnRevoke, row, 5, 0);
            row++;
        }
        if (active.isEmpty()) {
            JLabel lbl = new JLabel("No active permissions. Every query requires confirmation.");
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0; c.gridy = row; c.gridwidth = headers.length; c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(8, 4, 2, 4);
            pnlGrants.add(lbl, c);
            row++;
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        pnlGrants.add(Box.createGlue(), filler);

        btnRevokeAll.setEnabled(!active.isEmpty());
        pnlGrants.revalidate();
        pnlGrants.repaint();
    }

    @Override
    public void dispose() {
        timer.stop();
        McpService.getInstance().getGrants().removeListener(this);
        instance = null;
        super.dispose();
    }
}
