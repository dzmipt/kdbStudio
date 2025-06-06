package studio.kdb;

import javax.swing.table.AbstractTableModel;

public abstract class KTableModel extends AbstractTableModel {

    public abstract boolean isKey(int column);
    public abstract KColumn getColumn(int col);

    @Override
    public final String getColumnName(int column) {
        return getColumn(column).getName();
    }

    public static KTableModel getModel(K.KBase obj) {
        if (obj instanceof K.Flip) {
            return new FlipTableModel((K.Flip) obj);
        }

        if (obj instanceof K.Dict) {
            K.Dict dict = (K.Dict) obj;
            if ( (dict.x instanceof K.KBaseVector || dict.x instanceof K.Flip) &&
                 (dict.y instanceof K.KBaseVector || dict.y instanceof K.Flip) ) {
                return new DictTableModel(dict);
            } else {
                return null;
            }
        }

        if ((obj instanceof K.KBaseVector) && obj.getType() != KType.CharVector && obj.getType() != KType.ByteVector) {
            return new ListModel((K.KBaseVector<? extends K.KBase>)obj);
        }
        return null;
    }

    protected int[] index;
    protected boolean ascSorted;
    protected int sortedByColumn;

    protected KTableModel(int rowCount) {
        index = new int[rowCount];
        ascSorted = true;
        initIndex();
    }

    private void initIndex() {
        int k = ascSorted ? 1 : -1;
        int b = ascSorted ? 0 : index.length - 1;
        for (int i = 0; i< index.length; i++) {
            index[i] = b + k*i;
        }
        sortedByColumn = -1;
    }

    public int[] getIndex() {
        return index;
    }

    public void sort(int col) {
        if (sortedByColumn == col) {
            if (ascSorted) {
                ascSorted = false;
            } else {
                ascSorted = true;
                col = -1;
            }
        } else {
            ascSorted = true;
        }
        if (col == -1) {
            initIndex();
        } else {
            KColumn column = getColumn(col);
            if (sortedByColumn == col) {
                index = Sorter.reverse(column, index);
            } else {
                index = Sorter.sort(column, index);
            }
        }
        sortedByColumn = col;

        fireTableDataChanged();
    }

    public boolean isSortedAsc(int column) {
        return ascSorted && sortedByColumn == column;
    }

    public boolean isSortedDesc(int column) {
        return !ascSorted && sortedByColumn == column;
    }

    public Object getValueAt(int row,int col) {
        return get(row, col);
    }

    public K.KBase get(int row, int col) {
        return getColumn(col).get(row);
    }

    public int getRowCount() {
        return getColumn(0).size();
    }

}
