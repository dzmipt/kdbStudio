package studio.ui;

import studio.kdb.Config;
import studio.kdb.KTableModel;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class WidthAdjuster extends MouseAdapter {

    private final JTable table;
    private final JScrollPane scrollPane;
    private int gap;
    private int cellMaxWidth;
    private int resizeIndex = -1;

    private static final int EPSILON = 5;   //boundary sensitivity
    private final boolean[] limitWidthState;

    public WidthAdjuster(JTable table, JScrollPane scrollPane) {
        this.table = table;
        this.scrollPane = scrollPane;
        table.getTableHeader().addMouseListener(this);
        int colCount = table.getColumnCount();
        limitWidthState = new boolean[colCount];
        Arrays.fill(limitWidthState, true);
        revalidate();
    }

    public void revalidate() {
        int charWidth = SwingUtilities.computeStringWidth(table.getFontMetrics(table.getFont()), "x");
        gap =  (int) Math.round(charWidth * Config.getInstance().getDouble(Config.CELL_RIGHT_PADDING));
        cellMaxWidth = charWidth * Config.getInstance().getInt(Config.CELL_MAX_WIDTH);

        for (int i = 0;i < table.getColumnCount();i++)
            resize(i, limitWidthState[i]);
    }

    @Override
    public void mousePressed(MouseEvent evt) {
        if (evt.getClickCount() > 1 && usingResizeCursor()) {
            if ((table.getSelectedRowCount() == table.getRowCount()) && (table.getSelectedColumnCount() == table.getColumnCount()))
                resizeAllColumns(false);
            else {
                int col = getLeftColumn(evt.getPoint());
                if (col == -1) return;
                limitWidthState[col] = !limitWidthState[col];
                resize(getLeftColumn(evt.getPoint()), limitWidthState[col]);
            }
        }
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        if ((e.getModifiers() & Event.ALT_MASK) > 0) {
            if ( (resizeIndex == -1) || (e.getModifiers() & Event.SHIFT_MASK) == 0 ) {
                resizeIndex = getViewColumn(e);
            }
            select(e);
        } else {
            if (!usingResizeCursor()) {
                int column = getModelColumn(e);
                if (column >= 0) {
                    KTableModel ktm = (KTableModel) table.getModel();
                    ktm.sort(column);
                    scrollPane.repaint();
                }
            }
        }
    }

    private int getViewColumn(MouseEvent e) {
        JTableHeader h = (JTableHeader) e.getSource();
        TableColumnModel columnModel = h.getColumnModel();
        return columnModel.getColumnIndexAtX(e.getX());
    }

    private int getModelColumn(MouseEvent e) {
        int viewColumn = getViewColumn(e);
        if (viewColumn < 0) return -1;

        JTableHeader h = (JTableHeader) e.getSource();
        TableColumnModel columnModel = h.getColumnModel();

        return columnModel.getColumn(viewColumn).getModelIndex();
    }

    private void select(MouseEvent e) {
        if ((e.getModifiers() & Event.ALT_MASK) == 0) return;

        int index = getViewColumn(e);
        table.setRowSelectionInterval(0,table.getRowCount() - 1);
        table.setColumnSelectionInterval(resizeIndex, index);
    }

    private JTableHeader getTableHeader() {
        return table.getTableHeader();
    }

    private boolean usingResizeCursor() {
        Cursor cursor = getTableHeader().getCursor();
        return cursor.equals(EAST) || cursor.equals(WEST);
    }
    private static final Cursor EAST = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    private static final Cursor WEST = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
    //if near the boundary, will choose left column
    private int getLeftColumn(Point pt) {
        pt.x -= EPSILON;
        return getTableHeader().columnAtPoint(pt);
    }

    public void resizeAllColumns(boolean limitWidth) {
        for (int i = 0;i < table.getColumnCount();i++)
            resize(i, limitWidth);
    }

    private void resize(int col, boolean limitWidth) {
        TableColumnModel tcm = table.getColumnModel();
        TableColumn tc = tcm.getColumn(col);
        TableCellRenderer tcr = tc.getHeaderRenderer();
        if (tcr == null)
            tcr = table.getTableHeader().getDefaultRenderer();

        Component comp = tcr.getTableCellRendererComponent(table,tc.getHeaderValue(),false,false,0,col);
        int maxWidth = comp.getPreferredSize().width;

        int ub = table.getRowCount();

        int stepSize = ub / 1000;

        if (stepSize == 0)
            stepSize = 1;

        for (int i = 0;i < ub;i += stepSize) {
            tcr = table.getCellRenderer(i,col);
            Object obj = table.getValueAt(i,col);
            comp = tcr.getTableCellRendererComponent(table,obj,false,false,i,col);
            maxWidth = Math.max(maxWidth, 2 + gap + comp.getPreferredSize().width); // we need to add a gap for lines between cells
        }
        if (limitWidth) {
            maxWidth = Math.min(maxWidth, cellMaxWidth);
        }

        tc.setPreferredWidth(maxWidth); //remembers the value
        tc.setWidth(maxWidth);          //forces layout, repaint
    }
}
