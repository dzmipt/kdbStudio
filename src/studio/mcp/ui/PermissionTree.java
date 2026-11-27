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
 * Server tree with check boxes to select servers and folders permitted for AI agents, and the filter.
 * A checked folder permits all servers inside it (including servers added later) which match the filter.
 * The filter is saved together with the selection.
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
                if (bounds != null && e.getX() < bounds.x + checkBoxWidth()) toggle((ServerTreeNode) path.getLastPathComponent());
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

        txtFilter = new JTextField(permissions.getFilter());
        txtFilter.setToolTipText("<html>Words separated by spaces; every word should be found in a folder name or " +
                "in the server name or host:port.<br/>The filter is saved: only servers matching it are permitted, " +
                "including servers added later.</html>");
        txtFilter.getDocument().addDocumentListener((studio.ui.DocumentChangeListener) e -> applyFilter());

        JPanel pnlFilter = new JPanel(new BorderLayout(5, 0));
        pnlFilter.add(new JLabel("Filter (applies to permitted servers):"), BorderLayout.WEST);
        pnlFilter.add(txtFilter, BorderLayout.CENTER);

        lblSummary = new JLabel();

        add(pnlFilter, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(450, 260));
        add(scroll, BorderLayout.CENTER);
        add(lblSummary, BorderLayout.SOUTH);

        applyFilter();
    }

    public McpPermissions getPermissions() {
        return current();
    }

    private static int checkBoxWidth() {
        Icon icon = UIManager.getIcon("CheckBox.icon");
        return (icon == null ? 16 : icon.getIconWidth()) + 2;
    }

    private McpPermissions current() {
        return new McpPermissions(entries, txtFilter.getText());
    }

    // The node is selected via a checked ancestor folder, so it can't be unchecked by itself
    private boolean isImplied(ServerTreeNode node) {
        if (entries.contains(McpPermissions.entry(node))) return false;
        ServerTreeNode parent = (ServerTreeNode) node.getParent();
        return parent != null && current().isAllowed(parent);
    }

    private void toggle(ServerTreeNode node) {
        if (isImplied(node)) return;
        String entry = McpPermissions.entry(node);
        if (!entries.remove(entry)) entries.add(entry);
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
        refreshSummary();
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
        String filterNote = txtFilter.getText().trim().isEmpty() ? "" : " matching the filter";
        lblSummary.setText(count == 0 ? "No servers are permitted" :
                count + (count == 1 ? " server is" : " servers are") + " permitted. Checked folders also permit servers " +
                        "added later" + filterNote + ".");
    }

    // A plain label renderer: the check box is painted as a part of the icon, so the label is never squeezed
    private class CheckRenderer extends DefaultTreeCellRenderer {
        private final JCheckBox checkBox = new JCheckBox();
        private boolean checked, enabled;

        private final Icon checkIcon = new Icon() {
            private Icon boxIcon() {
                Icon icon = UIManager.getIcon("CheckBox.icon");
                return icon != null ? icon : new javax.swing.plaf.metal.MetalCheckBoxIcon();
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                checkBox.setSelected(checked);
                checkBox.setEnabled(enabled);
                Icon box = boxIcon();
                box.paintIcon(checkBox, g, x, y + (getIconHeight() - box.getIconHeight()) / 2);
            }

            @Override
            public int getIconWidth() {
                return boxIcon().getIconWidth();
            }

            @Override
            public int getIconHeight() {
                return boxIcon().getIconHeight();
            }
        };

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            ServerTreeNode node = (ServerTreeNode) value;
            checked = current().isAllowed(node);
            boolean implied = isImplied(node);
            enabled = !implied;
            setIcon(new CompositeIcon(checkIcon, getIcon()));
            setToolTipText(implied ? "Permitted via a parent folder" : null);
            return this;
        }
    }

    // Two icons side by side
    private static class CompositeIcon implements Icon {
        private static final int GAP = 4;
        private final Icon first, second;

        CompositeIcon(Icon first, Icon second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            first.paintIcon(c, g, x, y + (getIconHeight() - first.getIconHeight()) / 2);
            if (second != null) {
                second.paintIcon(c, g, x + first.getIconWidth() + GAP, y + (getIconHeight() - second.getIconHeight()) / 2);
            }
        }

        @Override
        public int getIconWidth() {
            return first.getIconWidth() + (second == null ? 0 : GAP + second.getIconWidth());
        }

        @Override
        public int getIconHeight() {
            return Math.max(first.getIconHeight(), second == null ? 0 : second.getIconHeight());
        }
    }
}
