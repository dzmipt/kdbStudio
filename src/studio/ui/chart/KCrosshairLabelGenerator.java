package studio.ui.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CrosshairLabelGenerator;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;

import java.text.NumberFormat;

public class KCrosshairLabelGenerator implements CrosshairLabelGenerator {

    private JFreeChart chart;
    private boolean domainAxis;

    public KCrosshairLabelGenerator(JFreeChart chart, boolean domainAxis) {
        this.chart = chart;
        this.domainAxis = domainAxis;
    }

    private String format(NumberAxis axis, double value) {
        NumberFormat format = axis.getNumberFormatOverride();
        return format.format(value);
    }

    @Override
    public String generateLabel(Crosshair crosshair) {
        XYPlot plot = (XYPlot) chart.getPlot();

        if (domainAxis) {
            return format((NumberAxis) plot.getDomainAxis(), crosshair.getValue());
        }

        MultipleValueCrosshair mCrosshair = (MultipleValueCrosshair) crosshair;
        double[] values = mCrosshair.getValues();

        if (values == null) {
            return format((NumberAxis) plot.getRangeAxis(), crosshair.getValue());
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index<values.length; index++) {
            if (index>0) builder.append('\n');
            NumberAxis axis = (NumberAxis) plot.getRangeAxis(index);

            builder.append(axis.getLabel())
                    .append(": ")
                    .append(format(axis,  values[index]));
        }

        return builder.toString();
    }
}
