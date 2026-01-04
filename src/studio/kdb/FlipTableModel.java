package studio.kdb;

public class FlipTableModel extends KTableModel {

    private final KColumn[] columns;

    public FlipTableModel(K.Flip flip) {
        super(flip.count());

        int len = flip.count();
        int count = flip.x.count();
        columns = new KColumn[count];

        for (int index=0; index<count; index++) {
            String name = flip.x.at(index).getString();
            K.KBase kBase = flip.y.at(index);
            columns[index] = new KColumn(name, kBase, len);
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
