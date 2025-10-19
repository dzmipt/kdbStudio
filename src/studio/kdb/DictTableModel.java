package studio.kdb;

public class DictTableModel extends KTableModel {
    private final int keyCount;
    private final KColumn[] columns;

    public DictTableModel(K.Dict dict) {
        super(dict.count());

        boolean keyFlip = dict.x instanceof K.Flip;
        boolean valueFlip = dict.y instanceof K.Flip;
        keyCount = keyFlip ? ((K.Flip)dict.x).x.count() : 1;
        int valueCount = valueFlip ? ((K.Flip)dict.y).x.count() : 1;

        columns = new KColumn[keyCount + valueCount];
        for (int col = 0; col<columns.length; col++) {
            boolean keyColumn = col < keyCount;

            K.KBase obj = keyColumn ? dict.x : dict.y;
            int index = keyColumn ? col : col - keyCount;

            String name;
            if (obj instanceof K.Flip) {
                K.KSymbolVector v = ((K.Flip) obj).x;
                name = v.at(index).getString();
            } else { //list
                name = keyColumn ? "key" : "value";
            }

            K.KBaseVector<? extends K.KBase> data;
            if (obj instanceof K.Flip) {
                data = (K.KBaseVector<? extends K.KBase>) ((K.Flip)obj).y.at(index);
            } else { //list
                data = (K.KBaseVector<? extends K.KBase>)obj;
            }

            columns[col] = new KColumn(name, data);
        }
    }

    public boolean isKey(int column) {
        return column < keyCount;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public KColumn getColumn(int col) {
        return columns[col];
    }
};
