package studio.ui.chart;

import studio.kdb.K;
import studio.kdb.KType;

import javax.swing.*;
import java.awt.*;
import java.time.temporal.ChronoUnit;

public class TimespanEditor extends Editor {

    private K.KTimespan value = K.KTimespan.NULL;
    private final JComboBox<ChronoUnit> comboUnit =
            new JComboBox<>(K.KTimespan.getSupportedUnits());


    public TimespanEditor(KType unitType) {
        super(unitType, false);

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
        if (newValue.equals(this.value)) return;

        if (this.value.equals(K.KTimespan.NULL)) {
            unitAutoSelect(newValue);
        }

        this.value = newValue;

        refresh();
        notifyValueChanged();
    }

    public double getValue() {
        return value.toUnitValue(unitType);
    }

    protected void refresh() {
        txtValue.setText("" + value.toUnitValue((ChronoUnit) comboUnit.getSelectedItem()));
    }

    protected void txtValueChanged(double newValue) {
        value = K.KTimespan.duration(newValue, (ChronoUnit) comboUnit.getSelectedItem());
    }

}
