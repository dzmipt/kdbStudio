package studio.ui.chart;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.*;
import org.jfree.chart.ui.RectangleEdge;
import studio.ui.chart.patched.CrosshairOverlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

public class ChartPanel extends studio.ui.chart.patched.ChartPanel {

    private final Crosshair xCrosshair;
    private final Crosshair yCrosshair;

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

        String key = "ESCAPE";
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processEsc();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), key);

    }

    private void processEsc() {
        if (zoomRectangle != null) {
            zoomPoint = null;
            zoomRectangle = null;
            repaint();
        } else {
            restoreAutoBounds();
        }
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
        JFreeChart chart = getChart();
        if (chart == null) {
            return;
        }
        Plot plot = chart.getPlot();
        int mods = e.getModifiers();
        if ((mods & this.panMask) == this.panMask) {
            // can we pan this plot?
            if (plot instanceof Pannable) {
                Pannable pannable = (Pannable) plot;
                if (pannable.isDomainPannable() || pannable.isRangePannable()) {
                    Rectangle2D screenDataArea = getScreenDataArea(e.getX(),
                            e.getY());
                    if (screenDataArea != null && screenDataArea.contains(
                            e.getPoint())) {
                        this.panW = screenDataArea.getWidth();
                        this.panH = screenDataArea.getHeight();
                        this.panLast = e.getPoint();
                        setCursor(Cursor.getPredefinedCursor(
                                Cursor.MOVE_CURSOR));
                    }
                }
                // the actual panning occurs later in the mouseDragged()
                // method
            }
        }
        else if (this.zoomRectangle == null) {
            Rectangle2D screenDataArea = getScreenDataArea(e.getX(), e.getY());
            if (screenDataArea != null) {
                this.zoomPoint = getPointInRectangle(e.getX(), e.getY(),
                        screenDataArea);
            }
            else {
                this.zoomPoint = null;
            }
            if (e.isPopupTrigger()) {
                displayPopupMenu(e.getX(), e.getY());
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        JPopupMenu popup = getPopupMenu();
        // if the popup menu has already been triggered, then ignore dragging...
        if (popup != null && popup.isShowing()) {
            return;
        }

        JFreeChart chart = getChart();
        ChartRenderingInfo info = getChartRenderingInfo();
        // handle panning if we have a start point
        if (this.panLast != null) {
            double dx = e.getX() - this.panLast.getX();
            double dy = e.getY() - this.panLast.getY();
            if (dx == 0.0 && dy == 0.0) {
                return;
            }
            double wPercent = -dx / this.panW;
            double hPercent = dy / this.panH;
            boolean old = chart.getPlot().isNotify();
            chart.getPlot().setNotify(false);
            Pannable p = (Pannable) chart.getPlot();
            if (p.getOrientation() == PlotOrientation.VERTICAL) {
                p.panDomainAxes(wPercent, info.getPlotInfo(),
                        this.panLast);
                p.panRangeAxes(hPercent, info.getPlotInfo(),
                        this.panLast);
            }
            else {
                p.panDomainAxes(hPercent, info.getPlotInfo(),
                        this.panLast);
                p.panRangeAxes(wPercent, info.getPlotInfo(),
                        this.panLast);
            }
            this.panLast = e.getPoint();
            chart.getPlot().setNotify(old);
            return;
        }

        // if no initial zoom point was set, ignore dragging...
        if (this.zoomPoint == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) getGraphics();

        // erase the previous zoom rectangle (if any).  We only need to do
        // this is we are using XOR mode, which we do when we're not using
        // the buffer (if there is a buffer, then at the end of this method we
        // just trigger a repaint)
        drawZoomRectangle(g2, true);

        Rectangle2D scaledDataArea = getScreenDataArea(
                (int) this.zoomPoint.getX(), (int) this.zoomPoint.getY());
        double x = Math.min(e.getX(), scaledDataArea.getMaxX());
        x = Math.max(x, scaledDataArea.getMinX());
        double y = Math.min(e.getY(), scaledDataArea.getMaxY());
        y = Math.max(y, scaledDataArea.getMinY());

        double x0 = Math.min(x, zoomPoint.getX());
        double x1 = Math.max(x, zoomPoint.getX());
        double y0 = Math.min(y, zoomPoint.getY());
        double y1 = Math.max(y, zoomPoint.getY());
        this.zoomRectangle = new Rectangle2D.Double(x0, y0, x1-x0, y1-y0);

        // Draw the new zoom rectangle...
        repaint();
        g2.dispose();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // if we've been panning, we need to reset now that the mouse is
        // released...
        if (this.panLast != null) {
            this.panLast = null;
            setCursor(Cursor.getDefaultCursor());
        }

        else if (this.zoomRectangle != null) {
            boolean zoomTrigger1 = Math.abs(e.getX()
                    - this.zoomPoint.getX()) >= this.zoomTriggerDistance;
            boolean zoomTrigger2 = Math.abs(e.getY()
                    - this.zoomPoint.getY()) >= this.zoomTriggerDistance;
            if (zoomTrigger1 || zoomTrigger2) {
                zoom(zoomRectangle);
            }
            else {
                // erase the zoom rectangle
                Graphics2D g2 = (Graphics2D) getGraphics();
                repaint();
                g2.dispose();
            }
            this.zoomPoint = null;
            this.zoomRectangle = null;
        }

        else if (e.isPopupTrigger()) {
            displayPopupMenu(e.getX(), e.getY());
        }

    }

}
