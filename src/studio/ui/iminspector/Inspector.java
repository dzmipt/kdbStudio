package studio.ui.iminspector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.ui.DocumentChangeListener;
import studio.ui.EscapeDialog;import studio.ui.GroupLayoutSimple;import studio.ui.Util;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;import java.util.*;
import java.util.List;

public class Inspector extends EscapeDialog {

    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode root;
    private final JCheckBox chbxNativeText = new JCheckBox("Native shortcut text", true);
    private final JTextField txtFilter = new JTextField(20);
    private final KeyStrokeScanner keyStrokeScanner = new KeyStrokeScanner();
    private final JLabel lblPressedKeyStrokeText = new JLabel();
    private final JLabel lblTypedKeyStrokeText = new JLabel();

    private final Map<Class<?>, Set<ComponentNode>> componentNodeMap = new HashMap<>();

    private final static Logger log = LogManager.getLogger();


    public Inspector() {
        super(null, "InputMap inspector");

        root = getRoot();
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);


        chbxNativeText.addItemListener(this::applyNativeText);

        txtFilter.getDocument().addDocumentListener((DocumentChangeListener) e -> applyFilter() );

        JLabel filterLabel = new JLabel("Filter:");

        keyStrokeScanner.setListener(this::keyStrokeEntered);

        JButton btnClear = new JButton("Clear");
        btnClear.addActionListener(this::clearShortcuts);
        
        JSeparator separator = new JSeparator();
        JPanel rightPanel = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(rightPanel);
        layout.addMaxWidthComponents(txtFilter,separator);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(chbxNativeText)
                        .addLine(filterLabel, txtFilter)
                        .addLine(separator)
                        .addLineAndGlue(keyStrokeScanner, btnClear)
                        .addLineAndGlue(new JLabel("Pressed: "), lblPressedKeyStrokeText)
                        .addLineAndGlue(new JLabel("Typed: "), lblTypedKeyStrokeText)

        );

        JPanel content = new JPanel(new BorderLayout());
        content.add(new JScrollPane(tree), BorderLayout.CENTER);
        content.add(rightPanel, BorderLayout.EAST);
        setContentPane(content);
    }

    private void updateKeyStrokeText() {
        KeyStroke pressed = keyStrokeScanner.getPressedKeyStroke();
        KeyStroke typed = keyStrokeScanner.getTypedKeyStroke();
        String pressedText = "";
        String typedText = "";
        if (pressed != null) {
            pressedText = keyStrokeText(pressed, chbxNativeText.isSelected());
        }
        if (typed != null) {
            typedText= keyStrokeText(typed, chbxNativeText.isSelected());
        }

        lblPressedKeyStrokeText.setText(pressedText);
        lblTypedKeyStrokeText.setText(typedText);

    }

    private void keyStrokeEntered(ChangeEvent evt) {
        updateKeyStrokeText();
        applyFilter();
    }

    private void applyNativeText(ItemEvent evt) {
        updateKeyStrokeText();
        boolean nativeText = chbxNativeText.isSelected();
        setNativeText(root, nativeText);
        if (! root.equals(treeModel.getRoot()) ) {
            setNativeText((DefaultMutableTreeNode) treeModel.getRoot(), nativeText);
        }
        tree.repaint();
    }

    private void clearShortcuts(ActionEvent evt) {
        keyStrokeScanner.reset();
    }

    private void setNativeText(DefaultMutableTreeNode node, boolean nativText) {
        Object userObject = node.getUserObject();
        if (userObject instanceof LeafNode leafNode) {
            leafNode.setNativeText(nativText);
            treeModel.nodeChanged(node);
        }

        int count = node.getChildCount();
        for (int index = 0; index < count; index++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(index);
            setNativeText(child, nativText);
        }
    }

    private void applyFilter() {
        List<KeyStroke> keyStrokes = new ArrayList<>();
        KeyStroke pressedKS = keyStrokeScanner.getPressedKeyStroke();
        KeyStroke typedKS = keyStrokeScanner.getTypedKeyStroke();
        if (pressedKS != null) keyStrokes.add(pressedKS);
        if (typedKS != null) keyStrokes.add(typedKS);

        String filterText = txtFilter.getText();
        filterText = filterText.trim();
        if (filterText.isEmpty() && keyStrokes.isEmpty()) {
            treeModel.setRoot(root);
            return;
        }

        String[] words = filterText.toLowerCase().split("\\s+");
        DefaultMutableTreeNode newRoot = applyFilter(root, words, keyStrokes.toArray(new KeyStroke[0]));
        if (newRoot == null) newRoot = new DefaultMutableTreeNode("/");
        treeModel.setRoot(newRoot);
        expandAll();
    }

    private DefaultMutableTreeNode applyFilter(DefaultMutableTreeNode parent, String[] words, KeyStroke[] keyStrokes) {
        Object userObject = parent.getUserObject();

        if (userObject instanceof LeafNode leaf) {
            boolean matched = false;
            for (KeyStroke ks: keyStrokes) {
                if (keyStrokeCompare(ks, leaf.getKeyStroke())) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                keyStrokes = new KeyStroke[0];
            }
        }

        String title = userObject.toString().toLowerCase().trim();

        List<String> list = new ArrayList<>();
        for (String word : words) {
            if (!title.contains(word)) list.add(word);
        }
        words = list.toArray(new String[0]);

        DefaultMutableTreeNode newParent = new DefaultMutableTreeNode(parent.getUserObject());

        int count = parent.getChildCount();
        for (int index = 0; index < count; index++) {
            DefaultMutableTreeNode newChild = applyFilter((DefaultMutableTreeNode) parent.getChildAt(index), words, keyStrokes);
            if (newChild != null) newParent.add(newChild);
        }

        if (newParent.getChildCount() == 0 && (words.length>0 || keyStrokes.length>0) ) return null;

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

    private static String keyStrokeText(KeyStroke keyStroke, boolean nativeText) {
        if (keyStroke.getKeyCode() == KeyEvent.VK_UNDEFINED) { //typed
            return "" + keyStroke.getKeyChar();
        }

        if (nativeText) {
            return Util.getAcceleratorString(keyStroke);
        }

        return keyStroke.toString().replace("pressed ", "")
                .replace("released ", "")
                .replace("typed ", "");
    }

    private static boolean keyStrokeCompare(KeyStroke keyStroke1, KeyStroke keyStroke2) {
        return keyStroke1.getKeyCode() == keyStroke2.getKeyCode()
                && keyStroke1.getModifiers() == keyStroke2.getModifiers()
                && keyStroke1.getKeyChar() == keyStroke2.getKeyChar();
    }

    private static class LeafNode {
        private final KeyStroke keyStroke;
        private final String action;
        private boolean nativeText = true;

        public LeafNode(KeyStroke keyStroke, String action) {
            this.keyStroke = keyStroke;
            this.action = action;
        }

        public KeyStroke getKeyStroke() {
            return keyStroke;
        }

        public void setNativeText(boolean nativeText) {
            this.nativeText = nativeText;
        }

        @Override
        public String toString() {
            return keyStrokeText(keyStroke, nativeText) + " -> " + action;
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
                node.add(new DefaultMutableTreeNode(new LeafNode(key, map.get(key))));
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
