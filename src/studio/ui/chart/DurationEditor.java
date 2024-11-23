package studio.ui.chart;

import studio.kdb.K;
import studio.ui.chart.event.ValueChangedEvent;
import studio.ui.chart.event.ValueChangedListener;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;


public class DurationEditor extends JPanel {

    public static final List<Class<? extends K.KBase>> VALUE_CLASSES =
            List.of(
                    K.KInteger.class,
                    K.KDouble.class,
                    K.KFloat.class,
                    K.KShort.class,
                    K.KLong.class
            );

    public static final List<Class<? extends K.KBase>> TEMPORAL_CLASSES =
            List.of(
                    K.KDate.class,
                    K.KTime.class,
                    K.KTimestamp.class,
                    K.KTimespan.class,
                    K.KDatetime.class,
                    K.Month.class,
                    K.Second.class,
                    K.Minute.class
            );

    private final EventListenerList listenerList = new EventListenerList();

    public static DurationEditor create(Class<? extends K.KBase> unitClass) {
        if (VALUE_CLASSES.contains(unitClass)) return new DurationEditor();
        if (TEMPORAL_CLASSES.contains(unitClass)) return new TimespanEditor(unitClass);

        throw new UnsupportedOperationException("DurationEditor for class " + unitClass + " is not supported");
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

    protected final JTextField txtValue = new JTextField();

    private double value = Double.NaN;

    protected DurationEditor() {
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

    protected void refresh() {
        txtValue.setText("" + value);
    }

    protected void txtValueChanged(double newValue) {
        this.value = newValue;
    }

    private void txtValueChanged(ActionEvent event) {
        boolean error = false;
        try {
            double v = Double.parseDouble(txtValue.getText());
            txtValueChanged(v);
        } catch (NumberFormatException e) {
            error = true;
        }
        refresh();
        if (!error) {
            notifyValueChanged();
        }
    }

}
