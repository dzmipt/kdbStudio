package studio.ui.chart;

import studio.kdb.KType;
import studio.ui.chart.event.ValueChangedEvent;
import studio.ui.chart.event.ValueChangedListener;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;


public abstract class Editor extends JPanel {

    public static final List<KType> VALUE_CLASSES =
            List.of(
                    KType.Int,
                    KType.Double,
                    KType.Float,
                    KType.Short,
                    KType.Long
            );

    public static final List<KType> TEMPORAL_CLASSES =
            List.of(
                    KType.Date,
                    KType.Time,
                    KType.Timestamp,
                    KType.Timespan,
                    KType.Datetime,
                    KType.Month,
                    KType.Second,
                    KType.Minute
            );

    private final EventListenerList listenerList = new EventListenerList();

    public static Editor createDurationEditor(KType unitType) {
        if (VALUE_CLASSES.contains(unitType)) return new NumericEditor();
        if (TEMPORAL_CLASSES.contains(unitType)) return new TimespanEditor(unitType);

        throw new UnsupportedOperationException("Editor for type " + unitType + " is not supported");
    }

    public static Editor createEditor(KType unitType) {
        if (VALUE_CLASSES.contains(unitType)) return new NumericEditor();
        if (TEMPORAL_CLASSES.contains(unitType)) return new KEditor(unitType);

        throw new UnsupportedOperationException("Editor for type " + unitType + " is not supported");
    }

    public void addValueChangedListener(ValueChangedListener listener) {
        listenerList.add(ValueChangedListener.class, listener);
    }

    public void removeValueChangedListener(ValueChangedListener listener) {
        listenerList.remove(ValueChangedListener.class, listener);
    }

    protected void notifyValueChanged() {
        ValueChangedEvent event = new ValueChangedEvent(this, getValue());
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ValueChangedListener.class) {
                ((ValueChangedListener) listeners[i + 1]).valueChanged(event);
            }
        }
    }

    protected final JTextField txtValue = new JTextField(15);

    private double value = Double.NaN;

    protected Editor() {
        super(new BorderLayout());
        add(txtValue, BorderLayout.CENTER);

        txtValue.addActionListener(this::txtValueChanged);
        txtValue.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                txtValueChanged(null);
            }
        });

    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        if (value == this.value) return;
        this.value = value;
        refresh();
        notifyValueChanged();
    }

    abstract protected void refresh();

    abstract protected double parseValue(String text) throws NumberFormatException;

    protected void txtValueChanged(double newValue) {
        this.value = newValue;
    }

    private void txtValueChanged(ActionEvent event) {
        try {
            double v = parseValue(txtValue.getText());
            txtValueChanged(v);
            refresh();
            notifyValueChanged();
        } catch (NumberFormatException e) {
            refresh();
        }
    }

}
