package studio.ui.chart;

public class NumericEditor extends Editor {

    protected void refresh() {
        txtValue.setText("" + getValue());
    }

    @Override
    protected double parseValue(String text) throws NumberFormatException {
        return Double.parseDouble(text);
    }
}
