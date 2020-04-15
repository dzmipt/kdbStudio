package studio.ui;

import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

public class ServerList extends EscapeDialog implements TreeExpansionListener  {

    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTextField filter;
    private boolean ignoreExpansionListener = false;
    private java.util.Set<TreePath> expandedPath = new HashSet<>();
    private java.util.Set<TreePath> collapsedPath = new HashSet<>();

    private Server selectedServer;
    private Server activeServer;
    private ServerTreeNode serverTree;

    public final static int DEFAULT_WIDTH = 300;
    public final static int DEFAULT_HEIGHT = 400;

    public ServerList(JFrame parent) {
        super(parent, "Server List");
        initComponents();
    }

    public void updateServerTree(ServerTreeNode serverTree, Server activeServer) {
        this.serverTree = serverTree;
        this.activeServer = activeServer;
        selectedServer = activeServer;
        refreshServers();
    }

    //Split filter text by spaces
    private java.util.List<String> getFilters() {
        java.util.List<String> filters = new ArrayList<>();
        filters.clear();
        StringTokenizer st = new StringTokenizer(filter.getText()," \t");
        while (st.hasMoreTokens()) {
            String word = st.nextToken().trim();
            if (word.length()>0) filters.add(word.toLowerCase());
        }
        return filters;
    }

    //Reload server tree
    private void refreshServers() {
        java.util.List<String> filters = getFilters();
        if (filters.size() > 0) {
            treeModel.setRoot(filter(serverTree, filters));
            treeModel.reload();
            expandAll(); // expand all if we apply any filters
        } else {
            ignoreExpansionListener = true;
            treeModel.setRoot(serverTree);
            treeModel.reload();
            //restore expanded state which was the last time (potentially was changed during filtering)
            for (TreePath path: expandedPath) {
                tree.expandPath(path);
            }
            for (TreePath path: collapsedPath) {
                tree.collapsePath(path);
            }
            //Make sure that active server is expanded and visible
            ServerTreeNode folder = activeServer.getFolder();
            if (folder != null) {
                ServerTreeNode node = folder.findServerNode(activeServer);
                if (node != null) {
                    TreePath path = new TreePath(node.getPath());
                    tree.expandPath(path.getParentPath());
                    //validate before scrollPathToVisible is needed to layout all nodes
                    tree.validate();
                    tree.scrollPathToVisible(path);
                }
            }
            ignoreExpansionListener = false;
        }
        tree.invalidate();
    }


    private void expandAll() {
        ServerTreeNode root = (ServerTreeNode)treeModel.getRoot();
        if (root == null) return;
        expandAll(root, new TreePath(root));
    }

    private void expandAll(ServerTreeNode parent, TreePath path) {
        for(ServerTreeNode child:parent.childNodes() ) {
            expandAll(child, path.pathByAddingChild(child));
        }
        tree.expandPath(path);
    }

    private ServerTreeNode filter(ServerTreeNode parent, java.util.List<String> filters) {
        String value = parent.isFolder() ? parent.getFolder() : parent.getServer().getName();
        value = value.toLowerCase();
        java.util.List<String> left = new ArrayList<>();
        for(String filter:filters) {
            if (! value.contains(filter)) {
                left.add(filter);
            }
        }

        if (left.size() ==0) return parent.copy();

        java.util.List<ServerTreeNode> children = new ArrayList<>();
        for (ServerTreeNode child: parent.childNodes()) {
            ServerTreeNode childFiltered = filter(child, left);
            if (childFiltered != null) {
                children.add(childFiltered);
            }
        }

        if (children.size() == 0) return null;

        ServerTreeNode result = new ServerTreeNode(parent.getFolder());
        for (ServerTreeNode child: children) {
            result.add(child);
        }
        return result;
    }

    public Server getSelectedServer() {
        return selectedServer;
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        if (ignoreExpansionListener) return;
        if (filter.getText().trim().length() > 0) return;

        TreePath path = event.getPath();
        collapsedPath.remove(path);
        expandedPath.add(path);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        if (ignoreExpansionListener) return;
        if (filter.getText().trim().length() > 0) return;

        TreePath path = event.getPath();
        expandedPath.remove(path);
        collapsedPath.add(path);
    }

    private void action() {
        ServerTreeNode node  = (ServerTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return; // no selection
        if (node.isFolder()) return;
        selectedServer = node.getServer();
        dispose();
    }

    private void initComponents() {
        treeModel = new DefaultTreeModel(new ServerTreeNode(), true);
        tree = new JTree(treeModel) {
            @Override
            public String convertValueToText(Object nodeObj, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                ServerTreeNode node = (ServerTreeNode) nodeObj;
                String value;
                if (node.isFolder()) {
                    value = node.getFolder();
                } else {
                    value = node.getServer().getDescription();
                }
                if (!node.isFolder() && node.getServer().equals(activeServer)) {
                    value = "<html><b>" + value + "</b></html>";
                }
                return value;
            }
        };
        tree.setRootVisible(false);
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeExpansionListener(this);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                action();
            }
        });
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    action();
                }
            }
        });
        add(new JScrollPane(tree), BorderLayout.CENTER);
        filter = new JTextField();
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshServers();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshServers();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshServers();
            }
        });
        add(filter, BorderLayout.NORTH);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
    }

}
