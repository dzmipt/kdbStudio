package studio.ui.chart;

import org.jfree.chart.plot.Crosshair;

import java.awt.*;

public class MultipleValueCrosshair extends Crosshair {

    private double[] values = null;

    public MultipleValueCrosshair(double value, Paint paint, Stroke stroke) {
        super(value, paint, stroke);
    }

    public void setValues(double[] values) {
        this.values = values;
    }

    public double[] getValues() {
        return values;
    }
}
