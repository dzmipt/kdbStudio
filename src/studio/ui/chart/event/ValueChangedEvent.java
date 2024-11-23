package studio.ui.chart.event;

import java.util.EventObject;

public class ValueChangedEvent extends EventObject {

    private double value;

    public ValueChangedEvent(Object source, double value) {
        super(source);
        this.value = value;
    }

    public double getValue() {
        return value;
    }

}
