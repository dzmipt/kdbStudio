package studio.utils;

import java.util.HashSet;
import java.util.Set;

public class TableMarkers {

    private final Set<Integer> state = new HashSet<>();
    private final int columnCount;

    public TableMarkers(int columnCount) {
        this.columnCount = columnCount;
    }

    public void clear() {
        state.clear();
    }

    private int getIndex(int row, int column) {
        return column + row*columnCount;
    }

    public void mark(int row, int column) {
        state.add(getIndex(row,column));
    }

    public boolean isMarked(int row, int column) {
        return state.contains(getIndex(row, column));
    }

}
