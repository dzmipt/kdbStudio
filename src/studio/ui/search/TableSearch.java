package studio.ui.search;

import studio.kdb.K;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

public class TableSearch {

    private final JTable table;
    private final TableModel model;
    private int[] cols;
    private int[] rows;
    private int col, nextCol;
    private int row, nextRow;
    private boolean nextCalculated = false;

    private final boolean direction;

    public TableSearch(JTable table, boolean direction) {
        this.table = table;
        this.direction = direction;
        cols = table.getSelectedColumns();
        rows = table.getSelectedRows();

        col = 0;
        row = 0;
        if (cols.length == 0 && rows.length == 0) {
            nextCalculated = true;
        }

        if (cols.length == 1 && rows.length == 1) {
            col = cols[0];
            row = rows[0];
            cols = new int[0];
            rows = new int[0];
        }

        model = table.getModel();
    }

    public boolean hasNext() {
        if (nextCalculated) return true;

        boolean toNextRow = false;
        nextRow = row;
        nextCol = col+1;
        if (cols.length>0) {
            if (nextCol == cols.length) toNextRow = true;
        } else {
            if (model.getColumnCount() == nextCol) toNextRow = true;
        }

        if (toNextRow) {
            nextCol = 0;
            nextRow++;

            boolean finish = false;
            if (rows.length>0) {
                if (nextRow == rows.length) finish = true;
            } else {
                if (model.getRowCount() == nextRow) finish = true;
            }

            if (finish) return false;
        }

        nextCalculated = true;
        return true;
    }

    public K.KBase next() {
        if (!nextCalculated) {
            boolean success = hasNext();
            if (! success) throw new IllegalStateException("TableSearch reaches to the end");
        }

        col = nextCol;
        row = nextRow;
        nextCalculated = false;

        int colIndex = cols.length > 0 ? cols[col] : col;
        int rowIndex = rows.length > 0 ? rows[row] : row;
        return (K.KBase)model.getValueAt(table.convertRowIndexToModel(rowIndex), table.convertColumnIndexToModel(colIndex));
    }

    public void scrollTo() {
        int colIndex = cols.length > 0 ? cols[col] : col;
        int rowIndex = rows.length > 0 ? rows[row] : row;

        table.setColumnSelectionInterval(colIndex, colIndex);
        table.setRowSelectionInterval(rowIndex, rowIndex);
        Rectangle rectangle = table.getCellRect(rowIndex, colIndex, false);
        table.scrollRectToVisible(rectangle);
    }

}
