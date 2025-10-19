package studio.kdb;

public class FlipTableModel extends KTableModel {

    private final KColumn[] columns;

    public FlipTableModel(K.Flip flip) {
        super(flip.count());

        int count = flip.x.count();
        columns = new KColumn[count];
        for (int index=0; index<count; index++) {
            columns[index] = new KColumn(flip.x.at(index).getString(), (K.KBaseVector<? extends K.KBase>) flip.y.at(index) );
        }
    }

    public boolean isKey(int column) {
        return false;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public KColumn getColumn(int col) {
        return columns[col];
    }
};
