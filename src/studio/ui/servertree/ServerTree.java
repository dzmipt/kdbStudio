package studio.ui.servertree;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.ui.EditServerForm;
import studio.ui.EscapeDialog;
import studio.ui.StudioOptionPane;
import studio.ui.UserAction;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

public class ServerTree extends JTree implements TreeExpansionListener {

    private static final Logger log = LogManager.getLogger();

    private final NodeSelectedListener nodeSelectedListener;
    private ServerTreeNode activeNode;
    private ServerTreeNode root;
    private DefaultTreeModel treeModel;
    private boolean showServerNodes = true;
    private List<String> filters = new ArrayList<>();
    private boolean listView = false;

    private boolean ignoreExpansionListener = false;
    private java.util.Set<TreePath> expandedPath = new HashSet<>();
    private java.util.Set<TreePath> collapsedPath = new HashSet<>();

    private JPopupMenu popupMenu;
    private UserAction selectAction, removeAction,
            insertFolderAction, insertServerAction,
            addServerBeforeAction, addServerAfterAction,
            addFolderBeforeAction, addFolderAfterAction;

    private enum AddNodeLocation {INSERT, BEFORE, AFTER};

    public ServerTree(NodeSelectedListener nodeSelectedListener) {
        super(new DefaultTreeModel(new ServerTreeNode(), true));
        treeModel = (DefaultTreeModel)getModel();
        this.nodeSelectedListener = nodeSelectedListener;
        initActions();
        initPopupMenu();
        setDropMode(DropMode.ON_OR_INSERT);
//        setTransferHandler(new ServerList.NodesTransferHander());
        setRootVisible(false);
        setEditable(false);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        addTreeExpansionListener(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                } else if (e.getClickCount() == 2) {
                    selectTreeNode();
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) handlePopup(e);
            }
        });
    }

    private void initActions() {
        selectAction = UserAction.create("Select", "Select the node",
                KeyEvent.VK_S, e -> selectTreeNode());
        removeAction = UserAction.create("Remove", "Remove the node",
                KeyEvent.VK_DELETE, e -> removeNode());
        insertServerAction = UserAction.create("Insert Server", "Insert server into the folder",
                KeyEvent.VK_N, e -> addNode(false, AddNodeLocation.INSERT));
        insertFolderAction = UserAction.create("Insert Folder", "Insert folder into the folder",
                KeyEvent.VK_I, e -> addNode(true, AddNodeLocation.INSERT));
        addServerBeforeAction = UserAction.create("Add Server Before", "Add Server before selected node",
                KeyEvent.VK_R, e -> addNode(false, AddNodeLocation.BEFORE));
        addServerAfterAction = UserAction.create("Add Server After", "Add Server after selected node",
                KeyEvent.VK_E, e -> addNode(false, AddNodeLocation.AFTER));
        addFolderBeforeAction = UserAction.create("Add Folder Before", "Add Folder before selected node",
                KeyEvent.VK_B, e -> addNode(true, AddNodeLocation.BEFORE));
        addFolderAfterAction = UserAction.create("Add Folder After", "Add Folder after selected node",
                KeyEvent.VK_A, e -> addNode(true, AddNodeLocation.AFTER));

        getActionMap().put(selectAction.getText(), selectAction);
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectAction.getText());
    }

    private void selectTreeNode() {
        ServerTreeNode node = getSelectedNode();
        if (node == null) return; // no selection

        if (showServerNodes && node.isFolder()) return;
        nodeSelectedListener.nodeSelected(node);
    }
    private void handlePopup(MouseEvent e) {
        boolean empty = root.getChildCount() == 0;
        int x = e.getX();
        int y = e.getY();

        TreePath path = empty ? new TreePath(root) : getPathForLocation(x, y);
        if (path == null) {
            return;
        }

        setSelectionPath(path);

        if (listView) {
            if (empty) return;
            selectAction.setEnabled(true);
            insertFolderAction.setEnabled(false);
            addFolderBeforeAction.setEnabled(false);
            addFolderAfterAction.setEnabled(false);
            insertServerAction.setEnabled(false);
            addServerBeforeAction.setEnabled(false);
            addServerAfterAction.setEnabled(false);
            removeAction.setEnabled(false);
        } else {
            boolean isFolder = ((ServerTreeNode) path.getLastPathComponent()).isFolder();

            selectAction.setEnabled(!isFolder);
            insertServerAction.setEnabled(isFolder);
            insertFolderAction.setEnabled(isFolder);

            addFolderBeforeAction.setEnabled(!empty);
            addFolderAfterAction.setEnabled(!empty);
            addServerBeforeAction.setEnabled(!empty);
            addServerAfterAction.setEnabled(!empty);
            removeAction.setEnabled(!empty);
        }

        popupMenu.show(this, x, y);
    }

    private void initPopupMenu() {
        popupMenu = new JPopupMenu();
        popupMenu.add(selectAction);
        popupMenu.add(new JSeparator());
        popupMenu.add(insertFolderAction);
        popupMenu.add(addFolderBeforeAction);
        popupMenu.add(addFolderAfterAction);
        popupMenu.add(new JSeparator());
        popupMenu.add(insertServerAction);
        popupMenu.add(addServerBeforeAction);
        popupMenu.add(addServerAfterAction);
        popupMenu.add(new JSeparator());
        popupMenu.add(removeAction);
    }

    private void removeNode() {
        ServerTreeNode selNode  = (ServerTreeNode) getLastSelectedPathComponent();
        if (selNode == null) return;

        ServerTreeNode serverTree = Config.getInstance().getServerTree();
        ServerTreeNode node = serverTree.findPath(selNode.getPath());
        if (node == null) {
            log.error("Oops... Something goes wrong");
            return;
        }

        String message = "Are you sure you want to remove " + (node.isRoot() ? "folder" : "server") + ": " + node.fullPath();
        int result = StudioOptionPane.showYesNoDialog(this, message, "Remove?");
        if (result != JOptionPane.YES_OPTION) return;

        TreeNode[] path = ((ServerTreeNode)selNode.getParent()).getPath();
        node.removeFromParent();
        Config.getInstance().setServerTree(serverTree);
        refreshServers();

        TreePath treePath = new TreePath(path);
        scrollPathToVisible(treePath);
        setSelectionPath(treePath);
    }

    private void addNode(boolean folder, AddNodeLocation location) {
        ServerTreeNode selNode  = (ServerTreeNode) getLastSelectedPathComponent();
        if (selNode == null) return;

        ServerTreeNode serverTree = Config.getInstance().getServerTree();
        ServerTreeNode node = serverTree.findPath(selNode.getPath());
        if (node == null) {
            log.error("Oops... Something goes wrong");
            return;
        }
        ServerTreeNode parent = location == AddNodeLocation.INSERT ? node : (ServerTreeNode)node.getParent();

        ServerTreeNode newNode;
        if (folder) {
            String name = StudioOptionPane.showInputDialog(this, "Enter folder name", "Folder Name");
            if (name == null || name.trim().length() == 0) return;
            newNode = new ServerTreeNode(name);
        } else {
            Server server = Server.newServer();
            server = server.newFolder(parent);

            EditServerForm serverForm = new EditServerForm(this, server);
            serverForm.alignAndShow();
            if (serverForm.getResult() == EscapeDialog.DialogResult.CANCELLED) return;

            server = serverForm.getServer();
            newNode = new ServerTreeNode(server);

            if (! server.getFolderPath().equals(parent.getFolderPath())) {
                node = parent = serverTree.findPath(server.getFolderPath(), true);
                location = AddNodeLocation.INSERT;
            }
        }

        int index;
        if (location == AddNodeLocation.INSERT) {
            index = node.getChildCount();
        } else {
            index = parent.getIndex(node);
            if (location == AddNodeLocation.AFTER) index++;
        }

        parent.insert(newNode, index);
        try {
            Config.getInstance().setServerTree(serverTree);
        } catch (IllegalArgumentException exception) {
            log.error("Error adding new node", exception);
            StudioOptionPane.showError(this, "Error adding new node:\n" + exception, "Error");
        }
        refreshServers();

        selNode = root.findPath(newNode.getPath());
        if (selNode != null) {
            TreePath treePath = new TreePath(selNode.getPath());
            scrollPathToVisible(treePath);
            setSelectionPath(treePath);
        }
    }

    /* Override the default behaviour. This is to workaround a strange bug which I met in MacOS with
big tree (few thousands nodes overall). From time to time, the whole UI is frozen for minutes.
 */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJTree() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.FILLER;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    public String convertValueToText(Object nodeObj, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        ServerTreeNode node = (ServerTreeNode) nodeObj;
        String value;
        if (node.isFolder()) {
            value = node.getFolder();
        } else {
            value = node.getServer().getDescription( listView );
        }
        if (activeNode != null && node.fullPath().equals(activeNode.fullPath()) ) {
            value = "<html><b>" + value + "</b></html>";
        }
        return value;

    }

    public ServerTreeNode getSelectedNode() {
        return (ServerTreeNode) getLastSelectedPathComponent();
    }

    private void setRoot(ServerTreeNode newRoot) {
        if (newRoot == null) {
            root = new ServerTreeNode();
        } else if (listView) {
            root = new ServerTreeNode();
            for (Enumeration e = newRoot.depthFirstEnumeration(); e.hasMoreElements(); ) {
                ServerTreeNode node = (ServerTreeNode) e.nextElement();
                if (node.isFolder()) continue;
                root.add(node.getServer());
            }
        } else {
            root = newRoot;
        }
        setDragEnabled(!listView);
        treeModel.setRoot(root);
        treeModel.reload();
    }

    public void setListView(boolean listView) {
        this.listView = listView;
        refreshServers();
    }

    public void setActiveNode(ServerTreeNode activeNode, boolean showServerNodes) {
        this.activeNode = activeNode;
        this.showServerNodes = showServerNodes;
        refreshServers();
    }

    public void setActiveNode(ServerTreeNode activeNode) {
        setActiveNode(activeNode, showServerNodes);
    }

    public void setFilter(String filter) {
        filters.clear();
        StringTokenizer st = new StringTokenizer(filter," \t");
        while (st.hasMoreTokens()) {
            String word = st.nextToken().trim();
            if (word.length()>0) filters.add(word.toLowerCase());
        }
        refreshServers();
    }

    private ServerTreeNode filterOutServers(ServerTreeNode node) {
        ServerTreeNode result = new ServerTreeNode(node.getFolder());
        for (ServerTreeNode child: node.childNodes() ) {
            if (child.isFolder()) {
                result.add(filterOutServers(child));
            }
        }
        return result;
    }

    private ServerTreeNode filter(ServerTreeNode parent, java.util.List<String> filters) {
        String value = parent.isFolder() ? parent.getFolder() : parent.getServer().getDescription(false);
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

    private void refreshServers() {
        ServerTreeNode serverTree = Config.getInstance().getServerTree();
        if (!showServerNodes) {
            serverTree = filterOutServers(serverTree);
        }

        if (filters.size() > 0) {
            setRoot(filter(serverTree, filters));
            expandAll(); // expand all if we apply any filters
        } else {
            ignoreExpansionListener = true;
            setRoot(serverTree);
            //restore expanded state which was the last time (potentially was changed during filtering)
            for (TreePath path: expandedPath) {
                expandPath(path);
            }
            for (TreePath path: collapsedPath) {
                collapsePath(path);
            }
            //Make sure that active server is expanded and visible
            if (activeNode != null) {
                TreePath path = new TreePath(activeNode.getPath());
                expandPath(path.getParentPath());
                //validate before scrollPathToVisible is needed to layout all nodes
                validate();
                scrollPathToVisible(path);
            }
            ignoreExpansionListener = false;
        }
        invalidate();
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
        expandPath(path);
    }

    @Override
    public void treeExpanded(TreeExpansionEvent event) {
        if (ignoreExpansionListener) return;
        if (filters.size() > 0) return;

        TreePath path = event.getPath();
        collapsedPath.remove(path);
        expandedPath.add(path);
    }

    @Override
    public void treeCollapsed(TreeExpansionEvent event) {
        if (ignoreExpansionListener) return;
        if (filters.size() > 0) return;

        TreePath path = event.getPath();
        expandedPath.remove(path);
        collapsedPath.add(path);
    }

}
