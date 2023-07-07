package studio.ui.search;

import studio.kdb.K;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;

public class TableIterator {

    private final JTable table;
    private final TableModel model;

    private LineIterator rowIterator;
    private LineIterator colIterator;
    private boolean returnFirst;

    public TableIterator(JTable table, boolean forwardDirection) {
        this.table = table;
        model = table.getModel();

        int[] cols = table.getSelectedColumns();
        int[] rows = table.getSelectedRows();

        if (cols.length == 1 && rows.length == 1) {
            rowIterator = new LineIterator(null, table.getRowCount(), forwardDirection);
            colIterator = new LineIterator(null, table.getColumnCount(), forwardDirection);
            rowIterator.setPosition(rows[0]);
            colIterator.setPosition(cols[0]);
            returnFirst = false;
        } else {
            rowIterator = new LineIterator(rows, table.getRowCount(), forwardDirection);
            colIterator = new LineIterator(cols, table.getColumnCount(), forwardDirection);
            returnFirst = true;
        }
    }

    public int getRow() {
        return table.convertRowIndexToModel(rowIterator.get());
    }

    public int getColumn() {
        return table.convertColumnIndexToModel(colIterator.get());
    }

    public boolean hasNext() {
        if (returnFirst) return true;
        if (colIterator.hasNext()) return true;
        if (rowIterator.hasNext()) return true;
        return false;
    }

    public K.KBase next() {
        if (returnFirst) {
            returnFirst = false;
        } else {
            if (colIterator.hasNext()) {
                colIterator.next();
            } else {
                rowIterator.next();
                colIterator.reset();
            }
        }
        return (K.KBase)model.getValueAt(getRow(), getColumn());
    }

    public void scrollTo() {
        int colIndex = colIterator.get();
        int rowIndex = rowIterator.get();

        table.setColumnSelectionInterval(colIndex, colIndex);
        table.setRowSelectionInterval(rowIndex, rowIndex);
        Rectangle rectangle = table.getCellRect(rowIndex, colIndex, false);
        table.scrollRectToVisible(rectangle);
    }


    public static class LineIterator {

        private final int[] array;
        private final int count;
        private final boolean forwardDirection;

        private int pos;


        LineIterator(int[] array, int count, boolean forwardDirection) {
            if (array != null && array.length == 0) array = null;

            this.array = array;
            this.count = (array != null) ? array.length : count;
            this.forwardDirection = forwardDirection;

            reset();
        }

        void reset() {
            pos = forwardDirection ? 0 : count -1;
        }

        boolean hasNext() {

            if (forwardDirection) {
                return pos < count - 1;
            } else {
                return pos > 0;
            }
        }

        void next() {
            if (forwardDirection) {
                pos++;
            } else {
                pos--;
            }
        }

        void setPosition(int pos) {
            this.pos = pos;
        }

        int get() {
            return array == null ? pos : array[pos];
        }
    }

}
