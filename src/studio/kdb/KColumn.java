package studio.kdb;

public class KColumn {

    private final String name;
    private final K.KBaseVector<? extends K.KBase> data;

    public KColumn(String name, K.KBaseVector<? extends K.KBase> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public KType getElementType() {
        return data.getType().getElementType();
    }

    public K.KBase get(int index) {
        return data.at(index);
    }

    public int size() {
        return data.count();
    }

}
