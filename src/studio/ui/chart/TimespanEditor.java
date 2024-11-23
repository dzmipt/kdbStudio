package studio.ui.chart;

import studio.kdb.K;

import javax.swing.*;
import java.awt.*;
import java.time.temporal.ChronoUnit;

public class TimespanEditor extends DurationEditor {

    private final Class<? extends K.KBase> unitClass;
    private K.KTimespan value = K.KTimespan.NULL;
    private final JComboBox<ChronoUnit> comboUnit =
            new JComboBox<>(K.KTimespan.getSupportedUnits());


    public TimespanEditor(Class<? extends K.KBase> unitClass) {
        this.unitClass = unitClass;

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
        K.KTimespan newValue = K.KTimespan.duration(value, unitClass);
        if (newValue.equals(this.value)) return;

        if (this.value.equals(K.KTimespan.NULL)) {
            unitAutoSelect(newValue);
        }

        this.value = newValue;

        refresh();
        notifyValueChanged();
    }

    public double getValue() {
        return value.toUnitValue(unitClass);
    }

    protected void refresh() {
        txtValue.setText("" + value.toUnitValue((ChronoUnit) comboUnit.getSelectedItem()));
    }

    protected void txtValueChanged(double newValue) {
        value = K.KTimespan.duration(newValue, (ChronoUnit) comboUnit.getSelectedItem());
    }



    public static void main(String[] args) {

        JFrame f = new JFrame("Test");
        TimespanEditor editor = new TimespanEditor(K.Second.class);
        editor.setValue(600);
        editor.addValueChangedListener(e -> System.out.printf("New value: %f\n", e.getValue()) );

        f.setContentPane(editor);

        f.setSize(200,50);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }

}
