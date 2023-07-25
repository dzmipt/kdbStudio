package studio.ui.search;

import javax.swing.*;

public class TableIterator {

    private final LineIterator rowIterator;
    private final LineIterator colIterator;
    private boolean returnFirst;

    public TableIterator(JTable table, boolean forwardDirection, Position startPos) {
        int[] cols = table.getSelectedColumns();
        int[] rows = table.getSelectedRows();

        returnFirst = true;
        if (cols.length == 1 && rows.length == 1) {
            rowIterator = new LineIterator(null, table.getRowCount(), forwardDirection);
            colIterator = new LineIterator(null, table.getColumnCount(), forwardDirection);
        } else {
            rowIterator = new LineIterator(rows, table.getRowCount(), forwardDirection);
            colIterator = new LineIterator(cols, table.getColumnCount(), forwardDirection);
        }

        if (startPos != null) {
            rowIterator.setPosition(startPos.getRow());
            colIterator.setPosition(startPos.getColumn());
            returnFirst = false;
        }
    }

    public boolean hasNext() {
        if (returnFirst) return true;
        if (colIterator.hasNext()) return true;
        if (rowIterator.hasNext()) return true;
        return false;
    }

    public Position next() {
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
        return new Position(rowIterator.get(), colIterator.get());
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

        void setPosition(int value) {
            if (array == null) pos = value;
            else {
                for(pos = 0; pos<array.length; pos++) {
                    if (array[pos] == value) break;
                }
                if (pos == array.length) pos = 0;
            }
        }

        int get() {
            return array == null ? pos : array[pos];
        }
    }

}
