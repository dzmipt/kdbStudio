package studio.ui.iminspector;

import studio.ui.DocumentChangeListener;
import studio.ui.EscapeDialog;import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Inspector extends EscapeDialog {

    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode root;

    private final Map<Class<?>, Set<ComponentNode>> componentNodeMap = new HashMap<>();


    public Inspector() {
        super(null, "InputMap inspector");

        root = getRoot();
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);

        JTextField filter = new JTextField(20);
        filter.getDocument().addDocumentListener((DocumentChangeListener) e -> applyFilter(filter.getText()) );

        JLabel filterLabel = new JLabel("Filter:");

        JPanel rightPanel = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(rightPanel);
        layout.addMaxWidthComponents(filter);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(filterLabel, filter)
        );

        JPanel content = new JPanel(new BorderLayout());
        content.add(new JScrollPane(tree), BorderLayout.CENTER);
        content.add(rightPanel, BorderLayout.EAST);
        setContentPane(content);
    }

    private void applyFilter(String filter) {
        filter = filter.trim();
        if (filter.isEmpty()) {
            treeModel.setRoot(root);
            return;
        }

        String[] words = filter.toLowerCase().split("\\s+");
        DefaultMutableTreeNode newRoot = applyFilter(root, words);
        if (newRoot == null) newRoot = new DefaultMutableTreeNode("/");
        treeModel.setRoot(newRoot);
        expandAll();
    }

    private DefaultMutableTreeNode applyFilter(DefaultMutableTreeNode parent, String[] words) {
        String title = parent.toString().toLowerCase().trim();

        List<String> list = new ArrayList<>();
        for (String word: words) {
            if (! title.contains(word)) list.add(word);
        }
        words = list.toArray(new String[0]);

        DefaultMutableTreeNode newParent = new DefaultMutableTreeNode(parent.getUserObject());

        int count = parent.getChildCount();
        for (int index = 0; index < count; index++) {
            DefaultMutableTreeNode newChild = applyFilter((DefaultMutableTreeNode) parent.getChildAt(index), words);
            if (newChild != null) newParent.add(newChild);
        }

        if (newParent.getChildCount() == 0 && words.length>0) return null;

        return newParent;
    }

    private void expandAll() {
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
    }


    private DefaultMutableTreeNode getRoot() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("/");
        for (Window window : Window.getWindows()) {
            DefaultMutableTreeNode node = scanComponent(window);
            if (node != null) {
                root.add(node);
            }
        }
        return root;
    }

    private DefaultMutableTreeNode scanComponent(Component component) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(component.getClass().getName());

        if (component instanceof JComponent jc) {
            ComponentNode componentNode = new ComponentNode(jc);
            Set<ComponentNode> existingNodes = componentNodeMap.computeIfAbsent(jc.getClass(), k -> new HashSet<>());
            if (!existingNodes.contains(componentNode)) {
                existingNodes.add(componentNode);
                componentNode.addIntoTreeNode(node);
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                DefaultMutableTreeNode childNode = scanComponent(child);
                if (childNode != null) node.add(childNode);
            }
        }

        if (node.getChildCount() == 0) {
            return null;
        }
        return node;
    }

    private static class ComponentNode {

        private final InputMapNode focusedInputMapNode;
        private final InputMapNode ancestorInputMapNode;
        private final InputMapNode windowInputMapNode;

        public ComponentNode(JComponent component) {
            focusedInputMapNode = new InputMapNode(component.getInputMap(JComponent.WHEN_FOCUSED));
            ancestorInputMapNode = new InputMapNode(component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
            windowInputMapNode = new InputMapNode(component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW));
        }

        public void addIntoTreeNode(DefaultMutableTreeNode node) {
            DefaultMutableTreeNode whenFocused = focusedInputMapNode.getTreeNode("WHEN_FOCUSED");
            DefaultMutableTreeNode whenAncestor = ancestorInputMapNode.getTreeNode("WHEN_ANCESTOR_OF_FOCUSED_COMPONENT");
            DefaultMutableTreeNode whenWindow = windowInputMapNode.getTreeNode("WHEN_IN_FOCUSED_WINDOW");

            if (whenFocused.getChildCount()>0) node.add(whenFocused);
            if (whenAncestor.getChildCount()>0) node.add(whenAncestor);
            if (whenWindow.getChildCount()>0) node.add(whenWindow);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ComponentNode)) return false;
            ComponentNode that = (ComponentNode) o;
            return Objects.equals(focusedInputMapNode, that.focusedInputMapNode) && Objects.equals(ancestorInputMapNode, that.ancestorInputMapNode) && Objects.equals(windowInputMapNode, that.windowInputMapNode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(focusedInputMapNode, ancestorInputMapNode, windowInputMapNode);
        }
    }

    private static class InputMapNode {

        private final Map<KeyStroke, String> map = new LinkedHashMap<>();

        public InputMapNode(InputMap inputMap) {
            add(inputMap);
        }

        private void add(InputMap inputMap) {
            KeyStroke[] keys = inputMap.allKeys();
            if (keys != null) {
                for (KeyStroke keyStroke : keys) {
                    if (map.containsKey(keyStroke)) continue;
                    map.put(keyStroke, inputMap.get(keyStroke).toString());
                }
            }
            InputMap parentInputMap = inputMap.getParent();
            if (parentInputMap != null) {
                add(parentInputMap);
            }
        }

        public DefaultMutableTreeNode getTreeNode(String name) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
            for (KeyStroke key: map.keySet()) {
                node.add(new DefaultMutableTreeNode(key + " -> " + map.get(key)));
            }
            return node;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof InputMapNode)) return false;
            InputMapNode that = (InputMapNode) o;
            return Objects.equals(map, that.map);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map);
        }
    }

}
