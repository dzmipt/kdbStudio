package studio.kdb;

public class FlipTableModel extends KTableModel {
    private K.Flip flip;

    public static boolean isTable(Object obj) {
        if (obj instanceof K.Flip)
            return true;
        else if (obj instanceof K.Dict) {
            K.Dict d = (K.Dict) obj;

            if ((d.x instanceof K.Flip) && (d.y instanceof K.Flip))
                return true;
        }

        return false;
    }

    public FlipTableModel(K.Flip obj) {
        flip = obj;
    }

    public boolean isKey(int column) {
        return false;
    }

    public int getColumnCount() {
        return flip.x.getLength();
    }

    public String getColumnName(int i) {
        return flip.x.at(i).toString(false);
    }

    public K.KBaseVector getColumn(int col) {
        return (K.KBaseVector) flip.y.at(col);
    }
};
