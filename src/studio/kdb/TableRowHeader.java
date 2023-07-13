package studio.kdb;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;

public class TableRowHeader extends JList {
    private JTable table;

    public void recalcWidth() {
        Insets i = new RowHeaderRenderer().getInsets();
        int w = i.left + i.right;
        int width = SwingUtilities.computeStringWidth(table.getFontMetrics(table.getFont()),
                                                      (table.getRowCount() < 9999 ? "9999" : "" + (table.getRowCount() - 1)));
        // used to be rowcount - 1 as 0 based index
        setFixedCellWidth(w + width);
    }

    public TableRowHeader(final JTable table) {
        this.table = table;
        setAutoscrolls(false);
        setCellRenderer(new RowHeaderRenderer());
        setFixedCellHeight(table.getRowHeight());
        setFont(table.getFont());
        recalcWidth();

        setFocusable(false);
        setModel(new TableListModel());
        setOpaque(false);
        setSelectionModel(table.getSelectionModel());
        if (table.getRowCount() > 0) {
            MouseInputAdapter mia = new MouseInputAdapter() {
                int startIndex = 0;

                public void mousePressed(MouseEvent e) {
                    startIndex = locationToIndex(e.getPoint());
                    select(e);
                }

                public void mouseReleased(MouseEvent e) {
                    select(e);
                }

                public void mouseDragged(MouseEvent e) {
                    select(e);
                }

                private void select(MouseEvent e) {
                    int index = locationToIndex(e.getPoint());
                    table.setColumnSelectionInterval(0,table.getColumnCount() - 1);
                    table.setRowSelectionInterval(startIndex,index);
                    table.requestFocus();
                }
            };
            addMouseListener(mia);
            addMouseMotionListener(mia);
        }
    }

    class TableListModel extends AbstractListModel {
        public int getSize() {
            return table.getRowCount();
        }

        public Object getElementAt(int index) {
            int value = ((KTableModel)table.getModel()).getIndex()[index];
            return String.valueOf(value);
        }
    }

    class RowHeaderRenderer extends JLabel implements ListCellRenderer {
        RowHeaderRenderer() {
            super();
            setHorizontalAlignment(RIGHT);
            setVerticalAlignment(CENTER);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                        UIManager.getBorder("TableHeader.cellBorder"),
                        BorderFactory.createEmptyBorder(0,0,0,5)
                      ));
            setFont(table.getFont());
            setBackground(UIManager.getColor("TableHeader.background"));
            setForeground(UIManager.getColor("TableHeader.foreground"));
        }

        public void setFont(Font font) {
            super.setFont(font);
        }
        public Component getListCellRendererComponent(JList list,Object value,int index,boolean isSelected,boolean cellHasFocus) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
}
