package studio.ui.chart;

import studio.kdb.K;
import studio.kdb.KType;

import javax.swing.*;
import java.awt.*;
import java.time.temporal.ChronoUnit;

public class TimespanEditor extends Editor {

    private final KType unitType;
    private K.KTimespan timespan = K.KTimespan.NULL;
    private final JComboBox<ChronoUnit> comboUnit =
            new JComboBox<>(K.KTimespan.getSupportedUnits());


    public TimespanEditor(KType unitType) {
        this.unitType = unitType;

        add(comboUnit, BorderLayout.EAST);
        comboUnit.addActionListener(e -> refresh());
    }

    private void unitAutoSelect(K.KTimespan value) {
        ChronoUnit selectedUnit = null;

        for (int i=comboUnit.getItemCount()-1; i>=0; i--) {
            ChronoUnit unit = comboUnit.getItemAt(i);
            double v = value.toUnitValue(unit);
            if (Math.abs(v) >= 1.0 || i==0) {
                selectedUnit = unit;
                break;
            }
        }

        comboUnit.setSelectedItem(selectedUnit);
    }

    public void setValue(double value) {
        K.KTimespan newValue = K.KTimespan.duration(value, unitType);
        if (newValue.equals(this.timespan)) return;

        if (this.timespan.equals(K.KTimespan.NULL)) {
            unitAutoSelect(newValue);
        }

        this.timespan = newValue;

        refresh();
        notifyValueChanged();
    }

    public double getValue() {
        return timespan.toUnitValue(unitType);
    }

    protected void refresh() {
        txtValue.setText("" + timespan.toUnitValue((ChronoUnit) comboUnit.getSelectedItem()));
    }

    @Override
    protected double parseValue(String text) throws NumberFormatException {
        return Double.parseDouble(text);
    }

    protected void txtValueChanged(double newValue) {
        timespan = K.KTimespan.duration(newValue, (ChronoUnit) comboUnit.getSelectedItem());
    }

}
