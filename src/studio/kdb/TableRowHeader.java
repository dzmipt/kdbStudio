package studio.kdb;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;

public class TableRowHeader extends JList<String> {
    private final JTable table;

    public void recalcWidth() {
        Insets i = new RowHeaderRenderer().getInsets();
        int w = i.left + i.right;
        int width = SwingUtilities.computeStringWidth(table.getFontMetrics(getFont()),
                                                      (table.getRowCount() < 9999 ? "9999" : "" + (table.getRowCount() - 1)));
        // used to be rowcount - 1 as 0 based index
        setFixedCellWidth(w + width);
    }

    public TableRowHeader(final JTable table) {
        this.table = table;
        table.addPropertyChangeListener(new PropertyChangeListener() {
                                        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                                            if ("zoom".equals(propertyChangeEvent.getPropertyName())) {
                                                setFont(table.getFont());
                                                recalcWidth();
                                                setCellRenderer(new RowHeaderRenderer());
                                            }
                                        }
                                    });
        setAutoscrolls(false);
        setCellRenderer(new RowHeaderRenderer());
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
                    int index = locationToIndex(e.getPoint());
                    startIndex = index;
                    table.setColumnSelectionInterval(0,table.getColumnCount() - 1);
                    table.setRowSelectionInterval(index,index);
                    table.requestFocus();
                }

                public void mouseReleased(MouseEvent e) {
                    int index = locationToIndex(e.getPoint());
                    table.setColumnSelectionInterval(0,table.getColumnCount() - 1);
                    table.setRowSelectionInterval(startIndex,index);
                    table.requestFocus();
                }

                public void mouseDragged(MouseEvent e) {
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

    class TableListModel extends AbstractListModel<String> {
        @Override
        public int getSize() {
            return table.getRowCount();
        }

        @Override
        public String getElementAt(int index) {
            int value = ((KTableModel)table.getModel()).getIndex()[index];
            return String.valueOf(value);
        }
    }

    class RowHeaderRenderer extends JLabel implements ListCellRenderer<String> {
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
            setPreferredSize(new java.awt.Dimension(0, table.getRowHeight()));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                boolean isSelected, boolean cellHasFocus) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
}
