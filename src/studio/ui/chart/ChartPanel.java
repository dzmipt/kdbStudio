package studio.ui.chart;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import studio.ui.chart.patched.CrosshairOverlay;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class ChartPanel extends studio.ui.chart.patched.ChartPanel {

    private Crosshair xCrosshair;
    private Crosshair yCrosshair;

    public ChartPanel(JFreeChart chart) {
        super(chart);
        CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
        xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0.5f));
        xCrosshair.setLabelVisible(true);
        xCrosshair.setLabelGenerator(new KCrosshairLabelGenerator(chart, true));
        yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0.5f));
        yCrosshair.setLabelVisible(true);
        yCrosshair.setLabelGenerator(new KCrosshairLabelGenerator(chart, false));

        crosshairOverlay.addDomainCrosshair(xCrosshair);
        crosshairOverlay.addRangeCrosshair(yCrosshair);
        addOverlay(crosshairOverlay);
        setPreferredSize(new java.awt.Dimension(500, 270));
        setMouseWheelEnabled(true);
        setMouseZoomable(true, true);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        Rectangle2D dataArea = getScreenDataArea();
        JFreeChart chart = getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        double x = xAxis.java2DToValue(e.getX(), dataArea,
                RectangleEdge.BOTTOM);
        double y = plot.getRangeAxis().java2DToValue(e.getY(), dataArea, RectangleEdge.LEFT);
        xCrosshair.setValue(x);
        yCrosshair.setValue(y);

    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        xCrosshair.setVisible(false);
        yCrosshair.setVisible(false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        xCrosshair.setVisible(true);
        yCrosshair.setVisible(true);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
    }

}
