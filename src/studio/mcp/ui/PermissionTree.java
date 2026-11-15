package studio.mcp.ui;

import studio.kdb.ServerTreeNode;
import studio.mcp.McpPermissions;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Server tree with check boxes to select servers and folders permitted for AI agents.
 * A checked folder permits all servers inside it (including servers added later).
 */
public class PermissionTree extends JPanel {

    private final ServerTreeNode root;
    private final JTree tree;
    private final JTextField txtFilter;
    private final JLabel lblSummary;
    private final Set<String> entries = new LinkedHashSet<>();

    public PermissionTree(ServerTreeNode root, McpPermissions permissions) {
        super(new BorderLayout(0, 5));
        this.root = root;
        entries.addAll(permissions.cleanup(root).getEntries());

        tree = new JTree(new DefaultTreeModel(root, true)) {
            @Override
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                ServerTreeNode node = (ServerTreeNode) value;
                if (node.isRoot()) return "All servers";
                return node.isFolder() ? node.getFolder() : node.getServer().getDescription(false);
            }
        };
        tree.setRootVisible(true);
        tree.setCellRenderer(new CheckRenderer());
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) return;
                Rectangle bounds = tree.getPathBounds(path);
                // toggle when clicking on the check box area
                if (bounds != null && e.getX() < bounds.x + 22) toggle((ServerTreeNode) path.getLastPathComponent());
            }
        });
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_SPACE) return;
                TreePath path = tree.getSelectionPath();
                if (path != null) toggle((ServerTreeNode) path.getLastPathComponent());
            }
        });

        txtFilter = new JTextField();
        txtFilter.getDocument().addDocumentListener((studio.ui.DocumentChangeListener) e -> applyFilter());

        JPanel pnlFilter = new JPanel(new BorderLayout(5, 0));
        pnlFilter.add(new JLabel("Filter:"), BorderLayout.WEST);
        pnlFilter.add(txtFilter, BorderLayout.CENTER);

        lblSummary = new JLabel();

        add(pnlFilter, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(500, 260));
        add(scroll, BorderLayout.CENTER);
        add(lblSummary, BorderLayout.SOUTH);

        expandChecked();
        refreshSummary();
    }

    public McpPermissions getPermissions() {
        return new McpPermissions(entries);
    }

    private McpPermissions current() {
        return new McpPermissions(entries);
    }

    private void toggle(ServerTreeNode node) {
        String entry = McpPermissions.entry(node);
        if (entries.contains(entry)) {
            entries.remove(entry);
        } else {
            McpPermissions permissions = current();
            if (permissions.isAllowed(node)) return; // already permitted by an ancestor folder
            entries.add(entry);
        }
        tree.repaint();
        refreshSummary();
    }

    private void applyFilter() {
        String filter = txtFilter.getText().trim();
        ServerTreeNode filtered = filter.isEmpty() ? root : root.filter(filter);
        if (filtered == null) filtered = new ServerTreeNode();
        tree.setModel(new DefaultTreeModel(filtered, true));
        if (filter.isEmpty()) {
            expandChecked();
        } else {
            for (int row = 0; row < tree.getRowCount(); row++) tree.expandRow(row);
        }
    }

    private void expandChecked() {
        McpPermissions permissions = current();
        ServerTreeNode modelRoot = (ServerTreeNode) tree.getModel().getRoot();
        for (Enumeration<TreeNode> e = modelRoot.preorderEnumeration(); e.hasMoreElements();) {
            ServerTreeNode node = (ServerTreeNode) e.nextElement();
            if (permissions.contains(node) && node.getParent() != null) {
                tree.expandPath(new TreePath(((ServerTreeNode) node.getParent()).getPath()));
            }
        }
        tree.expandRow(0);
    }

    private void refreshSummary() {
        int count = current().allowedServers(root).size();
        lblSummary.setText(count == 0 ? "No servers are permitted" :
                count + (count == 1 ? " server is" : " servers are") + " permitted. Checked folders also permit servers added later.");
    }

    private class CheckRenderer extends DefaultTreeCellRenderer {
        private final JCheckBox checkBox = new JCheckBox();
        private final JPanel panel = new JPanel(new BorderLayout());

        CheckRenderer() {
            checkBox.setOpaque(false);
            panel.setOpaque(false);
            panel.add(checkBox, BorderLayout.WEST);
            panel.add(this, BorderLayout.CENTER);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            ServerTreeNode node = (ServerTreeNode) value;
            McpPermissions permissions = current();
            boolean direct = permissions.contains(node);
            boolean allowed = permissions.isAllowed(node);
            checkBox.setSelected(allowed);
            checkBox.setEnabled(direct || !allowed);
            panel.setToolTipText(allowed && !direct ? "Permitted via a parent folder" : null);
            return panel;
        }
    }
}
