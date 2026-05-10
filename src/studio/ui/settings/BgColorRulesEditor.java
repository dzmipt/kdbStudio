package studio.ui.settings;

import studio.core.Studio;
import studio.kdb.config.server.BgColorRules;
import studio.kdb.config.server.ServerFilterRule;
import studio.ui.GroupLayoutSimple;
import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BgColorRulesEditor extends JPanel implements DropTargetListener {

    public JPanel getEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JButton btnAdd = new JButton("Add new rule");
        btnAdd.addActionListener(this::addAction);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(btnAdd);
        panel.add(this, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }


    private final GroupLayoutSimple layout;

    private BgColorRules rules;
    private final List<ServerFilterRulePanel> panels = new ArrayList<>();

    private int draggedPanelIndex = -1;
    private int droppedPositionY = -1;
    private int droppedPositionIndex = -1;
    private final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    public BgColorRulesEditor(BgColorRules rules) {
        this.rules = rules;
        new DropTarget(this, this);

        layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);

        refresh();
    }

    public BgColorRules getRules() {
        loadRules();
        return rules;
    }

    private void refresh() {
        removeAll();
        panels.clear();
        GroupLayoutSimple.Stack stack = new GroupLayoutSimple.Stack();
        for (int index = 0; index < rules.size(); index++) {
            ServerFilterRule<?> rule = rules.get(index);

            JLabel lbl3dots = new JLabel(Util.THREE_DOTS_ICON);
            lbl3dots.setToolTipText("Drag to change order of rules");
            lbl3dots.setCursor(HAND_CURSOR);

            JLabel lblRemove = new JLabel(Util.CROSS_ICON);
            lblRemove.setToolTipText("Pres to remove the rule");
            lblRemove.setCursor(HAND_CURSOR);

            final int theIndex = index;
            lblRemove.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    removePanel(theIndex);
                }
            });

            ServerFilterRulePanel serverFilterRulePanel = new ServerFilterRulePanel(rule);
            panels.add(serverFilterRulePanel);

            JPanel left = new JPanel(new FlowLayout());
            left.add(lbl3dots);
            left.add(lblRemove);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(left, BorderLayout.WEST);
            panel.add(serverFilterRulePanel, BorderLayout.CENTER);

            new DraggablePanel(lbl3dots, index, panel);

            stack.addLine(panel);
            layout.addMaxWidthComponents(panel);
        }
        layout.setStacks(stack);
        revalidate();
        repaint();

    }

    private void loadRules() {
        rules = panels.stream()
                    .map(ServerFilterRulePanel::getRule)
                    .collect(BgColorRules::new, BgColorRules::add, BgColorRules::addAll);
    }

    private void addAction(ActionEvent evt) {
        loadRules();
        rules.add(ServerFilterRule.newDefault());
        refresh();
    }

    private void removePanel(int index) {
        loadRules();
        rules.remove(index);
        refresh();
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {}
    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {}
    @Override
    public void dragExit(DropTargetEvent dte) {}

    private boolean check(DropTargetDragEvent dtde) {
        if (draggedPanelIndex == -1) {
            dtde.rejectDrag();
            return false;
        }
        return true;
    }


    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (!check(dtde)) return;

        if (panels.size() == 0) {
            setDroppedPosition(-1, -1);
            return;
        }

        int newPositionY = -1;
        int newPositionIndex = -1;
        int y = dtde.getLocation().y;

        int min = Integer.MAX_VALUE;
        for (int index = 0; index <= panels.size(); index++) { // last iteration - check the bottom
            int thisValue = index < panels.size() ?
                                 getComponent(index).getY() :
                                (int)getComponent(panels.size()-1).getBounds().getMaxY();

            int dist = Math.abs(y - thisValue);
            if (dist < min) {
                min = dist;
                newPositionY = thisValue;
                newPositionIndex = index;
            }
        }
        setDroppedPosition(newPositionY, newPositionIndex);
    }

    private void setDroppedPosition(int y, int index) {
        if (droppedPositionY == y && draggedPanelIndex == index) return;
        droppedPositionY = y;
        droppedPositionIndex = index;
        repaint();
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        if (droppedPositionIndex == -1 || draggedPanelIndex == -1) {
            dtde.rejectDrop();
            return;
        }

        loadRules();

        if (droppedPositionIndex> draggedPanelIndex) droppedPositionIndex--;
        ServerFilterRule<?> rule = rules.remove(draggedPanelIndex);
        rules.add(droppedPositionIndex, rule);

        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
        dtde.dropComplete(true);

        refresh();
    }

    private class DraggablePanel extends DragSourceAdapter implements DragGestureListener {

        private final DragSource dragSource = new DragSource();
        private final int index;
        private final JPanel panel;

        public DraggablePanel(JLabel dragLabel, int index, JPanel panel) {
            this.panel = panel;
            this.index = index;
            dragSource.createDefaultDragGestureRecognizer(dragLabel, DnDConstants.ACTION_MOVE, this);
            dragSource.addDragSourceListener(this);
        }

        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            draggedPanelIndex = index;
            StringSelection transferable = new StringSelection("");

            BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            panel.paintAll(g);
            g.dispose();

            int x = dge.getDragOrigin().x;
            int y = dge.getDragOrigin().y;
            dragSource.startDrag(dge, DragSource.DefaultMoveDrop, image, new Point(-x,-y), transferable, this);
        }

        @Override
        public void dragDropEnd(DragSourceDropEvent dsde) {
            setDroppedPosition(-1, -1);
        }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);

        if (droppedPositionY == -1) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.blue);
        g2.setStroke(new BasicStroke(2));

        g2.drawLine(0, droppedPositionY, getWidth(), droppedPositionY);
    }

    public static void main(String... args) {
        Studio.initLF();

        BgColorRulesEditor editor = new BgColorRulesEditor(new BgColorRules());

        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(editor.getEditorPanel());
        frame.setSize(700, 600);
        frame.setVisible(true);
    }
}
