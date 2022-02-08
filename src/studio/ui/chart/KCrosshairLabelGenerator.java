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

    @Override
    public String generateLabel(Crosshair crosshair) {
        XYPlot plot = (XYPlot) chart.getPlot();
        NumberAxis axis = (NumberAxis) (domainAxis ? plot.getDomainAxis() : plot.getRangeAxis());
        NumberFormat format = axis.getNumberFormatOverride();
        return format.format(crosshair.getValue());

    }
}
