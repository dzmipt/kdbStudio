package studio.ui.chart;

import java.text.DecimalFormat;

public class NumericEditor extends Editor {
    private static final DecimalFormat df7 = new DecimalFormat("#.#######");

    protected void refresh() {
        txtValue.setText(df7.format(getValue()));
    }

    @Override
    protected double parseValue(String text) throws NumberFormatException {
        return Double.parseDouble(text);
    }
}
