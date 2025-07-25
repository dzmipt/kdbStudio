package studio.ui;

import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.ui.servertree.NodeSelectedListener;
import studio.ui.servertree.ServerTree;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ServerList extends EscapeDialog implements NodeSelectedListener {

    private JPanel contentPane;
    private JTabbedPane tabbedPane;
    private JList<String> serverHistoryList;
    private List<Server> serverHistory;
    private ServerTree tree;
    private JTextField filter;
    private JToggleButton tglBtnBoxTree;

   private Server selectedServer;

    public static final int DEFAULT_WIDTH = 300;
    public static final int DEFAULT_HEIGHT = 400;

    private static final String JTABBED_TREE_LABEL = "Servers - tree";
    private static final String JTABBED_LIST_LABEL = "Servers - list";

    private final KeyStroke TREE_VIEW_KEYSTROKE = Util.getMenuShortcut(KeyEvent.VK_T);

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

        updateServerHistory(serverHistory);
        selectHistoryTab(selectHistoryTab);
        tree.setActiveNode(activeNode);
        setVisible(true);

        if (getResult() == DialogResult.CANCELLED) return activeServer;

        return selectedServer;
    }

    public ServerTreeNode showFolders(ServerTreeNode activeNode) {
        tree.setRootVisible(true);
        tree.setActiveNode(activeNode, false);
        selectHistoryTab(false);
        tabbedPane.setEnabledAt(1, false);
        setVisible(true);

        if (getResult() == DialogResult.CANCELLED) return activeNode;


        return Config.getInstance().getServerTree().findPath(tree.getSelectedNode().getPath(), true);
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

    @Override
    public void nodeSelected(ServerTreeNode node) {
        selectedServer = node.isFolder() ? Server.NO_SERVER : node.getServer();
        accept();
    }

    private void selectServerFromHistory() {
        int index = serverHistoryList.getSelectedIndex();
        if (index == -1) return;
        selectedServer = serverHistory.get(index);
        accept();
    }

    private void toggleTreeListView() {
        tglBtnBoxTree.setSelected(!tglBtnBoxTree.isSelected());
        actionToggleButton();
    }

    private void actionToggleButton() {
        boolean listView = tglBtnBoxTree.isSelected();
        tree.setListView(listView);
        tabbedPane.setTitleAt(0, listView ? JTABBED_LIST_LABEL : JTABBED_TREE_LABEL);
    }

    private void initComponents() {
        tree = new ServerTree(this);
        filter = new JTextField();
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                tree.setFilter(filter.getText());
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

        serverHistoryList = new JList<>();
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
    }

    private void initActions() {

        UserAction toggleAction = UserAction.create("toggle", e-> toggleTreeListView());
        UserAction focusTreeAction = UserAction.create("focus tree", e-> tree.requestFocusInWindow());
        UserAction selectServerFromHistory = UserAction.create("select from history", e -> selectServerFromHistory());

        contentPane.getActionMap().put(toggleAction.getText(), toggleAction);
        filter.getActionMap().put(focusTreeAction.getText(), focusTreeAction);
        serverHistoryList.getActionMap().put(selectServerFromHistory.getText(), selectServerFromHistory);

        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(TREE_VIEW_KEYSTROKE, toggleAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), focusTreeAction.getText());
        filter.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), focusTreeAction.getText());
        serverHistoryList.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), selectServerFromHistory.getText());
    }


}
