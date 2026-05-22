package studio.ui.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.Studio;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.config.ServerConfig;
import studio.ui.DocumentChangeListener;
import studio.ui.EscapeDialog;
import studio.ui.StudioOptionPane;
import studio.ui.WindowFactory;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BulkEditServerDialog extends EscapeDialog implements DocumentChangeListener {

    private final JTree serverTree;
    private final JTextField txtFilter;
    private final MultipleServerEditor serverEditor;
    private final JLabel selectionLabel;
    private final JButton btnOK, btnCancel, btnApply;

    private boolean refreshing = false;

    private final static Logger log = LogManager.getLogger();

    public BulkEditServerDialog(Component windowOwner) {
        super(windowOwner, "Bulk Server Modification");

        txtFilter = new JTextField();
        txtFilter.getDocument().addDocumentListener(this);

        ServerTreeNode root = Config.getInstance().getServerTree();
        DefaultTreeModel treeModel = new DefaultTreeModel(root, true);
        serverTree = new JTree(treeModel) {
            @Override
            public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                ServerTreeNode node = (ServerTreeNode) value;
                return node.isFolder() ? node.getFolder() : node.getServer().getDescription(false);
            }

            @Override
            public void makeVisible(TreePath path) {
                // do not expand on selection
            }

            @Override
            public void updateUI() {
                super.updateUI();
                DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) getCellRenderer();
                renderer.setOpenIcon(renderer.getDefaultOpenIcon());
                renderer.setClosedIcon(renderer.getDefaultClosedIcon());
                renderer.setLeafIcon(renderer.getDefaultLeafIcon());

            }
        };
        serverTree.setRootVisible(true);
        serverTree.setEditable(false);
        serverTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        serverTree.addTreeSelectionListener(e-> refresh());

        selectionLabel = new JLabel();
        btnOK = new JButton("OK");
        btnCancel =new JButton("Cancel");
        btnApply = new JButton("Apply");

        serverEditor = new MultipleServerEditor(false);
        serverEditor.setBorder(
                new CompoundBorder(BorderFactory.createEtchedBorder(),
                                    new EmptyBorder(0,5,0,5)  ) );
        serverEditor.setChangeListener(e-> {
            btnApply.setEnabled(serverEditor.amended());
        });

        btnCancel.addActionListener(e-> cancel());
        btnApply.addActionListener(e-> applyChange());
        btnOK.addActionListener(e-> accept());

        JPanel leftTopPanel = new JPanel(new BorderLayout(5,0));
        leftTopPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        leftTopPanel.add(txtFilter, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        leftPanel.setBorder(new EmptyBorder(5,5,5,5));
        leftPanel.add(leftTopPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(serverTree), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(btnOK);
        bottomPanel.add(btnCancel);
        bottomPanel.add(btnApply);

        JPanel rightPanel = new JPanel(new BorderLayout(5,5));
        rightPanel.setPreferredSize(new Dimension(440, 310));
        rightPanel.add(selectionLabel, BorderLayout.NORTH);

        rightPanel.add(serverEditor, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        setContentPane(splitPane);

        refresh();
    }

    // Return total number of servers
    private int scan(boolean selected, TreePath path, List<TreePath> toAdd, List<Server> servers) {
        boolean isModelSelected = serverTree.isPathSelected(path);

        ServerTreeNode node = (ServerTreeNode) path.getLastPathComponent();
        if (node.isFolder()) {
            if (selected && ! isModelSelected) toAdd.add(path);

            int total = 0;
            for (ServerTreeNode child: node.childNodes()) {
                total += scan(selected || isModelSelected, path.pathByAddingChild(child), toAdd, servers);
            }
            return total;
        } else {
            if (selected || isModelSelected) {
                servers.add(node.getServer());
                if (! isModelSelected) {
                    toAdd.add(path);
                }
            }
            return 1;
        }
    }

    private void refresh() {
       if (refreshing) return;
       try {
           refreshing = true;
           List<TreePath> toAdd = new ArrayList<>();
           List<Server> servers = new ArrayList<>();
           TreePath rootPath = new TreePath(serverTree.getModel().getRoot());

           int total = scan(serverTree.isPathSelected(rootPath), rootPath, toAdd, servers);

           if (! toAdd.isEmpty()) serverTree.addSelectionPaths(toAdd.toArray(new TreePath[0]));

           serverEditor.setServers(servers);
           selectionLabel.setText(String.format("<html><b>Selected servers:</b> %d of %d</html>", servers.size(), total));

           boolean enable = ! servers.isEmpty();
           serverEditor.setVisible(enable);
           btnApply.setEnabled(enable && serverEditor.amended());
       } finally {
           refreshing = false;
       }
    }

    @Override
    public void accept() {
        if (applyChange()) {
            super.accept();
        }
    }

    private boolean applyChange() {
        if (!serverEditor.amended()) return true;

        try {
            List<Server> servers = serverEditor.getServers();
            List<Server> newServers = serverEditor.getAmendedServers();
            ServerConfig serverConfig = Config.getInstance().getServerConfig();
            serverConfig.replaceServers(servers, newServers);

            documentChanged(null);
            return true;
        } catch (RuntimeException e) {
            StudioOptionPane.showError(this, e.getMessage(), "Error");
            return false;
        }
    }

    @Override
    public void documentChanged(DocumentEvent e) {
        ServerTreeNode root = Config.getInstance().getServerTree();
        root = root.filter(txtFilter.getText());
        serverTree.setModel(new DefaultTreeModel(root, true));
        refresh();
    }

    public static void run() {
        BulkEditServerDialog dialog = new BulkEditServerDialog(WindowFactory.getActiveWindow());
        dialog.alignAndShow();
    }

    public static void main(String... args) {
        Studio.initLF();
        run();
    }

}
