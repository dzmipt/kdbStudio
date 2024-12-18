package studio.ui.chart;

import studio.kdb.KFormat;
import studio.kdb.KType;
import studio.kdb.Parser;


public class KEditor extends Editor {

    private final KType unitType;

    public KEditor(KType unitType) {
        this.unitType = unitType;
    }

    protected void refresh() {
        txtValue.setText(KFormat.format(unitType, getValue()));
    }

    @Override
    protected double parseValue(String text) throws NumberFormatException {
        double v = Parser.parse(unitType, text);
        if (Double.isNaN(v)) throw new NumberFormatException();
        return v;
    }
}
