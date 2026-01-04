package studio.kdb;

public class ListModel extends KTableModel {
    private final KColumn column;

    public ListModel(K.KBaseVector<? extends K.KBase> list) {
        super(list.count());
        column = new KColumn("value", list, list.count());
    }
    @Override
    public boolean isKey(int column) {
        return false;
    }

    @Override
    public KColumn getColumn(int col) {
        return column;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }
}
