package studio.ui.colorlist;

import studio.ui.ColorChooser;
import studio.ui.UserAction;
import studio.ui.chart.LegendButton;
import studio.ui.chart.SquareIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

public class ColorListComponent extends JComponent implements DropTargetListener {
    private final int SIZE = 24;
    private final int GAP = 8;

    private List<JLabel> labels  = new ArrayList<>();
    private Map<Point, Integer> hotspots = new HashMap<>();
    private Point selectedHotspot = null;
    private JLabel draggedLabel = null;
    private JLabel selectedLabel = null;
    private final List<ActionListener> listeners = new ArrayList<>();

    private JComponent prefWidthComponent = null;
    private int prefWidthInsets;

    private Action actionEdit;
    private Action actionDelete;
    private Action actionInsert;


    public ColorListComponent() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refresh();
            }
        });
        setLayout(new FlowLayout(FlowLayout.LEFT, GAP, GAP));
        new DropTarget(this, this);

        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        actionEdit = UserAction.create("Edit", this::edit);
        actionInsert = UserAction.create("Insert", this::insert);
        actionDelete = UserAction.create("Delete", this::delete);

        String key = "delete";
        actionMap.put(key, actionDelete);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), key);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), key);

        key = "edit";
        actionMap.put(key, actionEdit);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), key);

        key = "insert";
        actionMap.put(key, actionInsert);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), key);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), key);
        setFocusable(true);
    }

    public void setColors(List<Color> colors) {
        if (Objects.equals(colors, getColors())) return;

        selectLabel(null);
        selectHotspot(null);

        removeAll();
        labels = new ArrayList<>(colors.size());
        for (int index = 0; index < colors.size(); index++) {
            JLabel label = new JLabel(new SquareIcon(colors.get(index), SIZE));
            label.setOpaque(true);
            label.setPreferredSize(new Dimension(SIZE, SIZE));
            labels.add(label);
            add(label);
            new DraggableLabel(label);
            label.addMouseListener(new MouseAdapter() {
                private boolean checkPopup(MouseEvent e) {
                    if (!e.isPopupTrigger()) return false;
                    if (labels.contains(e.getSource())) {
                        selectLabel((JLabel) e.getSource(), true);
                    }
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(actionEdit);
                    popup.add(actionInsert);
                    popup.add(actionDelete);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    popup.show(e.getComponent(), e.getX(), e.getY());
                    return true;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    checkPopup(e);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (checkPopup(e)) return;

                    selectLabel(label);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        selectedLabel = label;
                        edit();
                    }
                }
            });
        }
        actionDelete.setEnabled(labels.size() > 1);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(this::refresh);
        fireActionEvent();
    }

    public List<Color> getColors() {
        List<Color> colors = new ArrayList<>(labels.size());
        for (JLabel label: labels) {
            colors.add( (Color)((SquareIcon) label.getIcon()).getColor());
        }
        return colors;
    }

    public void setPrefWidthComponent(JComponent component, int gap) {
        prefWidthComponent = component;
        prefWidthInsets = gap;
    }

    @Override
    public Dimension getPreferredSize() {
        if (prefWidthComponent == null) return super.getPreferredSize();

        int width = prefWidthComponent.getWidth() - prefWidthInsets;
        if (width <= 0) return super.getPreferredSize();

        int cols = (width - GAP) / (GAP + SIZE);
        if (cols<=0) cols = 1;
        int rows = (labels.size() + cols - 1) / cols;

        return new Dimension(GAP + cols * (SIZE+GAP), GAP + rows * (SIZE+GAP));
    }

    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    private void fireActionEvent() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "");
        listeners.forEach(listener -> listener.actionPerformed(event));
    }

    private void selectLabel(JLabel label) {
        selectLabel(label, false);
    }

    private void selectLabel(JLabel label, boolean forced) {
        requestFocus();
        if (forced) {
            selectedLabel = label;
        } else {
            if (selectedLabel == null && label == null) return;

            if (selectedLabel == label) {
                selectedLabel = null;
            } else {
                selectedLabel = label;
            }
        }
        repaint();
    }

    private void delete() {
        if (selectedLabel == null) return;

        if (labels.size() == 1) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        int index = labels.indexOf(selectedLabel);
        List<Color> colors = getColors();
        colors.remove(index);
        setColors(colors);
    }

    private void edit() {
        if (selectedLabel == null) return;
        Color color = (Color) ((SquareIcon)selectedLabel.getIcon()).getColor();
        Color newColor = ColorChooser.chooseColor(this,"Select color", color);
        if (newColor == null || newColor.equals(color)) return;

        int index = labels.indexOf(selectedLabel);
        List<Color> colors = getColors();
        colors.set(index, newColor);
        setColors(colors);
        selectLabel(labels.get(index));
    }

    private void insert() {
        int newIndex;
        if (selectedLabel == null) {
            newIndex = labels.size();;
        } else {
            newIndex = labels.indexOf(selectedLabel);
        }

        Color newColor = ColorChooser.chooseColor(this,"Select color", Color.BLACK);
        if (newColor == null) return;
        List<Color> colors = getColors();
        colors.add(newIndex, newColor);
        setColors(colors);
    }

    private void refresh() {
        hotspots.clear();

        int maxX = 0;
        int maxY = 0;

        for (int index = 0; index < labels.size(); index++) {
            JLabel label = labels.get(index);
            JLabel prev = index > 0 ? labels.get(index-1) : null;
            JLabel next = index == labels.size()-1 ? null : labels.get(index+1);

            maxX = Math.max(maxX, label.getX() + label.getWidth());
            maxY = Math.max(maxY, label.getY() + label.getHeight());

            int y = label.getY() + label.getHeight() / 2;

            boolean first = prev == null || prev.getX()>=label.getX();
            boolean last = next == null || next.getX()<=label.getX();

            int prevX = first ? 0 : prev.getX()+prev.getWidth();
            hotspots.put(new Point( (label.getX() + prevX)/2, y), index);

            if (last) {
                hotspots.put(new Point(label.getX() + label.getWidth() + GAP/2, y), index+1);
            }

        }

    }

    private void findDropLocation(Point p) {
        Point best = null;
        int bestS = Integer.MAX_VALUE;
        for (Point hs: hotspots.keySet()) {
            int dx = p.x - hs.x;
            int dy = p.y - hs.y;
            int s = dx*dx + dy*dy;
            if (s<bestS) {
                bestS = s;
                best = hs;
            }
        }

        if (bestS > 10*SIZE*SIZE) best = null;
        selectHotspot(best);
    }

    private void selectHotspot(Point point) {
        if (Objects.equals(point, selectedHotspot)) return;
        selectedHotspot = point;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.blue);
        g2.setStroke(new BasicStroke(2));

        if (selectedHotspot != null) {
            int y1 = selectedHotspot.y - SIZE / 2;
            int y2 = selectedHotspot.y + SIZE / 2;
            int x = selectedHotspot.x;

            g2.drawLine(x, y1, x, y2);
        }

        if (selectedLabel != null) {
            int x = selectedLabel.getX() - GAP/2;
            int y = selectedLabel.getY() - GAP/2;
            g2.drawRect(x, y, GAP + selectedLabel.getWidth(), GAP + selectedLabel.getHeight());
        }
    }


    private boolean check(DropTargetDragEvent dtde) {
        if (draggedLabel == null) {
            dtde.rejectDrag();
            return false;
        }
        return true;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        if (!check(dtde)) return;
        findDropLocation(dtde.getLocation());
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        if (draggedLabel == null || selectedHotspot == null) {
            dtde.rejectDrop();
            return;
        }

        int curIndex = labels.indexOf(draggedLabel);
        int newIndex = hotspots.get(selectedHotspot);
        if (newIndex>curIndex) newIndex--;


        add(draggedLabel, newIndex);
        labels.remove(draggedLabel);
        labels.add(newIndex, draggedLabel);

        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
        dtde.dropComplete(true);
        revalidate();
        repaint();
        fireActionEvent();
    }



    private class DraggableLabel extends DragSourceAdapter implements DragGestureListener {
        private DragSource dragSource = new DragSource();
        private JLabel label;

        public DraggableLabel(JLabel label) {
            this.label = label;
            dragSource.createDefaultDragGestureRecognizer(label, DnDConstants.ACTION_MOVE, this);
            dragSource.addDragSourceListener(this);
        }

        @Override
        public void dragGestureRecognized(DragGestureEvent dge) {
            selectLabel(null);
            draggedLabel = label; // Store reference to dragged label

            StringSelection transferable = new StringSelection("");

            BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            label.getIcon().paintIcon(label,g,0,0 );
            g.dispose();

            int x = dge.getDragOrigin().x;
            int y = dge.getDragOrigin().y;
            dragSource.startDrag(dge, DragSource.DefaultMoveDrop, image, new Point(-x,-y), transferable, this);
        }

        @Override
        public void dragDropEnd(DragSourceDropEvent dsde) {
            ColorListComponent.this.selectHotspot(null);
            draggedLabel = null;
        }
    }

    public static void main(String... args) {
        List<Color> list = new ArrayList<>();
        for (Paint p: LegendButton.BASE_COLORS) {
            if (p instanceof Color) list.add((Color)p);
        }

        ColorListComponent comp = new ColorListComponent();
        comp.setColors(list);

        JFrame f = new JFrame("Test");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setContentPane(comp);
        f.setSize(400,200);
        f.show();

    }
}
