package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class ServerList extends EscapeDialog implements TreeExpansionListener  {

    private static final Logger log = LogManager.getLogger();

    private JPanel contentPane;
    private JTabbedPane tabbedPane;
    private JList<String> serverHistoryList;
    private List<Server> serverHistory;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTextField filter;
    private JToggleButton tglBtnBoxTree;

    private boolean ignoreExpansionListener = false;
    private java.util.Set<TreePath> expandedPath = new HashSet<>();
    private java.util.Set<TreePath> collapsedPath = new HashSet<>();

    private Server selectedServer;
    private ServerTreeNode activeNode;
    private ServerTreeNode root;

    private boolean showServerNodes = true;

    private JPopupMenu popupMenu;
    private UserAction selectAction, removeAction,
                        insertFolderAction, insertServerAction,
                        addServerBeforeAction, addServerAfterAction,
                        addFolderBeforeAction, addFolderAfterAction;

    public static final int DEFAULT_WIDTH = 300;
    public static final int DEFAULT_HEIGHT = 400;

    private static final String JTABBED_TREE_LABEL = "Servers - tree";
    private static final String JTABBED_LIST_LABEL = "Servers - list";

    private static final int menuShortcutKeyMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private final KeyStroke TREE_VIEW_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_T, menuShortcutKeyMask);

    public ServerList(Window parent, Rectangle bounds) {
        super(parent, "Server List");
        initComponents();

        if (bounds != null && Util.fitToScreen(bounds)) {
            setBounds(bounds);
        } else {
            setBounds(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
            Util.centerChildOnParent(this, parent);
        }
    }


    public Server showServerTree(Server activeServer, List<Server> serverHistory, boolean selectHistoryTab) {
        ServerTreeNode activeNode = null;
        if (activeServer != null) {
            ServerTreeNode folder = Config.getInstance().getServerTree().findPath(activeServer.getFolderPath(), false);
            if (folder != null) {
                activeNode = folder.findServerNode(activeServer);
            }
        }

        updateServerTree(activeNode);
        updateServerHistory(serverHistory);
        selectHistoryTab(selectHistoryTab);
        setVisible(true);

        if (getResult() == DialogResult.CANCELLED) return activeServer;

        return selectedServer;
    }

    public ServerTreeNode showFolders(ServerTreeNode activeNode) {
        tree.setRootVisible(true);
        showServerNodes = false;
        updateServerTree(activeNode);
        selectHistoryTab(false);
        tabbedPane.setEnabledAt(1, false);
        setVisible(true);

        if (getResult() == DialogResult.CANCELLED) return activeNode;


        return Config.getInstance().getServerTree().findPath(getSelectedNode().getPath(), true);
    }

    public void updateServerTree(ServerTreeNode activeNode) {
        this.activeNode = activeNode;
        refreshServers();
    }

    private void updateServerHistory(final List<Server> list) {
        this.serverHistory = list;
        serverHistoryList.setModel(new AbstractListModel<String>() {
                                       @Override
                                       public int getSize() {
                                           return list.size();
                                       }
                                       @Override
                                       public String getElementAt(int index) {
                                           return list.get(index).getDescription(true);
                                       }
                                   }
        );
    }

    private void selectHistoryTab(boolean value) {
        tabbedPane.setSelectedIndex(value ? 1 : 0);
    }

    //Split filter text by spaces
    private List<String> getFilters() {
        List<String> filters = new ArrayList<>();
        filters.clear();
        StringTokenizer st = new StringTokenizer(filter.getText()," \t");
        while (st.hasMoreTokens()) {
            String word = st.nextToken().trim();
            if (word.length()>0) filters.add(word.toLowerCase());
        }
        return filters;
    }

    private void setRoot(ServerTreeNode newRoot) {
        if (newRoot == null) {
            root = new ServerTreeNode();
        } else if (isListView()) {
            root = new ServerTreeNode();
            for (Enumeration e = newRoot.depthFirstEnumeration(); e.hasMoreElements(); ) {
                ServerTreeNode node = (ServerTreeNode) e.nextElement();
                if (node.isFolder()) continue;
                root.add(node.getServer());
            }
        } else {
            root = newRoot;
        }
        treeModel.setRoot(root);
        treeModel.reload();
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

    //Reload server tree
    private void refreshServers() {
        java.util.List<String> filters = getFilters();

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
                tree.expandPath(path);
            }
            for (TreePath path: collapsedPath) {
                tree.collapsePath(path);
            }
            //Make sure that active server is expanded and visible
            if (activeNode != null) {
                TreePath path = new TreePath(activeNode.getPath());
                tree.expandPath(path.getParentPath());
                //validate before scrollPathToVisible is needed to layout all nodes
                tree.validate();
                tree.scrollPathToVisible(path);
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

    public ServerTreeNode getSelectedNode() {
        return (ServerTreeNode) tree.getLastSelectedPathComponent();
    }

    private void selectTreeNode() {
        ServerTreeNode node = getSelectedNode();
        if (node == null) return; // no selection

        if (node.isFolder()) {
            if (!showServerNodes) {
                accept();
            }
        } else {
            selectedServer = node.getServer();
            accept();
        }
    }

    private void selectServerFromHistory() {
        int index = serverHistoryList.getSelectedIndex();
        if (index == -1) return;
        selectedServer = serverHistory.get(index);
        accept();
    }

    private boolean isListView() {
        return tglBtnBoxTree.isSelected();
    }

    private void toggleTreeListView() {
        tglBtnBoxTree.setSelected(!tglBtnBoxTree.isSelected());
        actionToggleButton();
    }

    private void actionToggleButton() {
        refreshServers();
        tabbedPane.setTitleAt(0, isListView() ? JTABBED_LIST_LABEL : JTABBED_TREE_LABEL);
    }

    private void initComponents() {
        treeModel = new DefaultTreeModel(new ServerTreeNode(), true);
        tree = new JTree(treeModel) {

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
                    value = node.getServer().getDescription( isListView() );
                }
                if (activeNode != null && node.fullPath().equals(activeNode.fullPath()) ) {
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
        tglBtnBoxTree = new JToggleButton(Util.TEXT_TREE_ICON);
        tglBtnBoxTree.setToolTipText(Util.getTooltipWithAccelerator("Toggle tree/list", TREE_VIEW_KEYSTROKE));
        tglBtnBoxTree.setSelectedIcon(Util.TEXT_ICON);
        tglBtnBoxTree.setFocusable(false);
        tglBtnBoxTree.addActionListener(e->actionToggleButton());
        JToolBar toolbar = new JToolBar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setFloatable(false);
        toolbar.add(tglBtnBoxTree);
        toolbar.addSeparator();
        toolbar.add(new JLabel("Filter: "));
        toolbar.add(filter);
        filter.requestFocus();

        serverHistoryList = new JList();
        serverHistoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverHistoryList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectServerFromHistory();
                }
            }
        });

        //An extra panel to avoid selecting last item when clicking below the list
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(serverHistoryList, BorderLayout.NORTH);
        panel.setBackground(serverHistoryList.getBackground());

        tabbedPane = new JTabbedPane();
        tabbedPane.add(JTABBED_TREE_LABEL, new JScrollPane(tree));
        tabbedPane.add("Recent", new JScrollPane(panel));
        tabbedPane.setFocusable(false);

        contentPane = new JPanel(new BorderLayout());
        contentPane.add(toolbar, BorderLayout.NORTH);
        contentPane.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(contentPane);

        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        initActions();
        initPopupMenu();
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

        UserAction toggleAction = UserAction.create("toggle", e-> toggleTreeListView());
        UserAction focusTreeAction = UserAction.create("focus tree", e-> tree.requestFocusInWindow());
        UserAction selectServerFromHistory = UserAction.create("select from history", e -> selectServerFromHistory());

        contentPane.getActionMap().put(toggleAction.getText(), toggleAction);
        tree.getActionMap().put(selectAction.getText(), selectAction);
        filter.getActionMap().put(focusTreeAction.getText(), focusTreeAction);
        serverHistoryList.getActionMap().put(selectServerFromHistory.getText(), selectServerFromHistory);

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(TREE_VIEW_KEYSTROKE, toggleAction.getText());
        tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), focusTreeAction.getText());
        serverHistoryList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectServerFromHistory.getText());
    }

    private void handlePopup(MouseEvent e) {
        boolean empty = root.getChildCount() == 0;
        int x = e.getX();
        int y = e.getY();

        TreePath path = empty ? new TreePath(root) : tree.getPathForLocation(x, y);
        if (path == null) {
            return;
        }

        tree.setSelectionPath(path);

        if (isListView()) {
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

        popupMenu.show(tree, x, y);
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
        ServerTreeNode selNode  = (ServerTreeNode) tree.getLastSelectedPathComponent();
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
        tree.scrollPathToVisible(treePath);
        tree.setSelectionPath(treePath);
    }

    private enum AddNodeLocation {INSERT, BEFORE, AFTER};

    private void addNode(boolean folder, AddNodeLocation location) {
        ServerTreeNode selNode  = (ServerTreeNode) tree.getLastSelectedPathComponent();
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
            if (serverForm.getResult() == DialogResult.CANCELLED) return;

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
            tree.scrollPathToVisible(treePath);
            tree.setSelectionPath(treePath);
        }
    }

}
