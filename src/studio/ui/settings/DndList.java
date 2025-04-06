package studio.ui.settings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.ui.UserAction;
import studio.ui.chart.LegendIcon;
import studio.utils.BasicDataFlavor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class DndList extends JList<DndList.ListItem> {
    private final ListItem defaultItem;
    private final DefaultListModel<DndList.ListItem> model;
    private final Action actionDelete;
    private final Action actionInsert;

    private int prefWidth = -1;

    private static final BasicDataFlavor listItemFlavor = new BasicDataFlavor(ListItem.class);
    private static final Logger log = LogManager.getLogger();

    public DndList(ListItem defaultItem) {
        model = new DefaultListModel<>();
        setModel(model);
        this.defaultItem = defaultItem;
        setCellRenderer(new IconListRenderer());
        setDragEnabled(true);
        setDropMode(DropMode.INSERT);
        setTransferHandler(new ListItemTransferHandler());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        actionInsert = UserAction.create("Insert", this::insert);
        actionDelete = UserAction.create("Delete", this::delete);

        String key = "delete";
        actionMap.put(key, actionDelete);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), key);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), key);

        key = "insert";
        actionMap.put(key, actionInsert);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HELP, 0), key);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), key);

        addMouseListener(new MouseAdapter() {
            private boolean checkPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return false;
                int index = locationToIndex(e.getPoint());
                if (index != -1) setSelectedIndex(index);

                JPopupMenu popup = new JPopupMenu();
                popup.add(actionInsert);
                popup.add(actionDelete);
                popup.show(e.getComponent(), e.getX(), e.getY());
                popup.show(e.getComponent(), e.getX(), e.getY());
                return true;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }
        });

    }

    public void setPrefWidth(int prefWidth) {
        this.prefWidth = prefWidth;
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension prefSize = super.getPreferredSize();
        if (prefWidth == -1) return prefSize;
        return new Dimension(prefWidth, prefSize.height);
    }

    public void add(ListItem item) {
        model.addElement(item);
    }

    public void add(Icon icon, String text) {
        add(new ListItem(text, icon));
    }

    public Icon getSelectedIcon() {
        int index = getSelectedIndex();
        if (index == -1) return null;
        return getSelectedValue().icon;
    }

    public void setSelected(ListItem item) {
        int index = getSelectedIndex();
        if (index == -1) return;
        model.set(index, item);
    }

    public void insert() {
        int index = getSelectedIndex();
        if (index == -1) {
            index = model.size();
        } else {
            index++;
        }
        model.add(index, defaultItem);
        actionDelete.setEnabled(true);
    }

    public void delete() {
        if (model.size() == 1) return;

        int selIndex = getSelectedIndex();
        if (selIndex == -1) return;

        model.removeElementAt(selIndex);
        actionDelete.setEnabled(model.size()>1);
    }

    private static final Color selectionColor = new Color(36, 4, 94);
    private static final Border noBorder = BorderFactory.createEmptyBorder(3,3,3,3);
    private static final Border selectedBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(selectionColor),
            BorderFactory.createEmptyBorder(2,2,2,2) );

    // Custom renderer to show icon + text
    static class IconListRenderer extends JLabel implements ListCellRenderer<ListItem> {
        public Component getListCellRendererComponent(JList<? extends ListItem> list, ListItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(value.text);
            setIcon(value.icon);
            setOpaque(true);
            setBorder(new EmptyBorder(5, 5, 5, 5));
            setForeground(list.getForeground());
            setBorder(isSelected ? selectedBorder : noBorder);

            return this;
        }
    }

    // TransferHandler to handle drag & drop
    private class ListItemTransferHandler extends TransferHandler {
        private int dragSourceIndex = -1;

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<?> list = (JList<?>) c;
            dragSourceIndex = list.getSelectedIndex();
            return listItemFlavor.getTransferable(model.get(dragSourceIndex));
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return support.isDataFlavorSupported(listItemFlavor);
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            try {
                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
                int dropIndex = dropLocation.getIndex();

                Transferable t = support.getTransferable();
                ListItem item = (ListItem) t.getTransferData(listItemFlavor);

                if (dragSourceIndex != -1 && dragSourceIndex < dropIndex) {
                    dropIndex--; // Adjust drop index if moving down in list
                }

                model.remove(dragSourceIndex);
                model.add(dropIndex, item);

                return true;
            } catch (UnsupportedFlavorException | IOException e) {
                log.error("Can't import data", e);
            }
            return false;
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            dragSourceIndex = -1;
        }
    }

    // Item class that holds text and an icon
    public static class ListItem {
        private final String text;
        private final Icon icon;

        public ListItem(String text, Icon icon) {
            this.text = text;
            this.icon = icon;
        }

        public String toString() {
            return text;
        }

        public Icon getIcon() {
            return icon;
        }
    }


    public static void main(String[] args) {
        BasicStroke[] strokes = new BasicStroke[] {
                new BasicStroke(1f),
                new BasicStroke(1f,BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,1f,new float[] {10,10},0f
                ),
                new BasicStroke(1f,BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,1f,new float[] {10,5},0f
                ),
                new BasicStroke(1f,BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,1f,new float[] {5,5},0f
                ),
                new BasicStroke(1f,BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,1f,new float[] {1.5f,3},0f
                ),
                new BasicStroke(1f,BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_BEVEL,1f,new float[] {10,3,3,3},0f
                ),
        };

        DndList list = new DndList(new ListItem("Default", new LegendIcon(Color.BLACK, null, strokes[0])));

        for (int index = 0; index<strokes.length; index++) {
            list.add(new LegendIcon(Color.BLACK,null, strokes[index]), "Label " + index);
        }

        BasicStroke s1 = new BasicStroke(1f,BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,1f,null,0f);
        BasicStroke s2 = new BasicStroke(1f,BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,1f,new float[]{1},0f);
        BasicStroke s3 = new BasicStroke(1f,BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,1f,new float[]{1,1},0f);
        BasicStroke s4 = new BasicStroke(1f,BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,1f,new float[]{10},0f);
        BasicStroke s5 = new BasicStroke(1f,BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL,1f,new float[]{10,0},0f);

        list.add(new LegendIcon(Color.BLACK,null, s1), "null");
        list.add(new LegendIcon(Color.BLACK,null, s2), "1");
        list.add(new LegendIcon(Color.BLACK,null, s3), "1,1");
        list.add(new LegendIcon(Color.BLACK,null, s4), "10");
        list.add(new LegendIcon(Color.BLACK,null, s5), "10, 0");

        JFrame f = new JFrame("Test");
        f.setContentPane(list);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(400, 300);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}