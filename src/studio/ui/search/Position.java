package studio.ui.search;

public class Position {

    private final int column, row;

    public Position(int row, int column) {
        this.column = column;
        this.row = row;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public int hashCode() {
        return row*1013 + column;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Position) ) return false;

        Position p = (Position) obj;
        return column == p.getColumn() && row == p.getRow();
    }

    @Override
    public String toString() {
        return "[" + row + ":" + column + "]";
    }
}
