package studio.ui.chart;

import org.jfree.chart.annotations.AbstractAnnotation;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class Line extends AbstractAnnotation implements XYAnnotation {

    private final ChartPanel chartPanel;
    private Point2D.Double p0, p1;
    private LegendIcon icon;
    private String title = "";

    private Point screenP0, screenP1;
    private boolean selected = false;
    private boolean off = false;
    private boolean init = false;
    private boolean visible = true;

    public Line(ChartPanel chartPanel, Point2D.Double p0) {
        this.chartPanel = chartPanel;
        this.p0 = p0;
        icon = new LegendIcon(Color.BLACK, null, LegendButton.getDefaultStroke());
        XYPlot plot = chartPanel.getChart().getXYPlot();
        plot.getDomainAxis().addChangeListener(e -> refresh());
        plot.getRangeAxis().addChangeListener(e -> refresh());
    }

    public void addPoint(Point2D.Double p1) {
        if (init) throw new IllegalStateException("The line is already initialized");
        this.p1 = p1;
        init = true;
        refresh();
    }

    private void refresh() {
        if (!init) return;
        XYPlot plot = chartPanel.getChart().getXYPlot();
        double xMin = plot.getDomainAxis().getLowerBound();
        double xMax = plot.getDomainAxis().getUpperBound();

        double yMin = plot.getRangeAxis().getLowerBound();
        double yMax = plot.getRangeAxis().getUpperBound();

        Line2D.Double line = new Line2D.Double(p0.x, p0.y, p1.x, p1.y);
        double xLow = intersectHorizontal(line, yMin);
        double xUp = intersectHorizontal(line, yMax);
        double yLow = intersectVertical(line, xMin);
        double yUp = intersectVertical(line, xMax);

        List<Point2D.Double> points = new ArrayList<>(2);
        if (within(xLow, xMin, xMax)) points.add(new Point2D.Double(xLow, yMin));
        if (within(xUp, xMin, xMax)) points.add(new Point2D.Double(xUp, yMax));
        if (within(yLow, yMin, yMax)) points.add(new Point2D.Double(xMin, yLow));
        if (within(yUp, yMin, yMax)) points.add(new Point2D.Double(xMax, yUp));

        if (points.size() == 2) {
            Point2D.Double p0 = points.get(0);
            Point2D.Double p1 = points.get(1);
            screenP0 = chartPanel.fromPlot(p0);
            screenP1 = chartPanel.fromPlot(p1);
            off = true;
        } else {
            off = false;
            selected = false;
        }
    }

    private static double intersectVertical(Line2D.Double line, double x) {
        return line.y1 + (x-line.x1)*(line.y2 - line.y1) / (line.x2 - line.x1);
    }

    private static double intersectHorizontal(Line2D.Double line, double y) {
        return line.x1 + (y-line.y1)*(line.x2 - line.x1) / (line.y2 - line.y1);
    }

    private static boolean within(double d0, double d1, double d2) {
        return (d0 >= d1) && (d0 <= d2);
    }

    public double distanceSqr(int x, int y) {
        if (!init || !off || !visible) return Double.POSITIVE_INFINITY;

        double s2 = x*(screenP0.y-screenP1.y) + screenP0.x*(screenP1.y-y) + screenP1.x*(y-screenP0.y);
        double l2 = screenDist(screenP0, screenP1);
        return s2*s2/l2;
    }

    public static double screenDist(Point p1, Point p2) {
        return (p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y);
    }

    public void moveTo(Point2D.Double p) {
        if (!init) return;
        double x0 = p.x - (p1.x - p0.x);
        double y0 = p.y - (p1.y - p0.y);
        p0 = new Point2D.Double(x0, y0);
        p1 = p;
        refresh();
        fireAnnotationChanged();
    }

    public void dragTo(Point p) {
        if (!init) return;
        double d0 = screenDist(p, screenP0);
        double d1 = screenDist(p, screenP1);

        if (d0<d1) {
            screenP0 = p;
        } else {
            screenP1 = p;
        }

        p0 = chartPanel.toPlot(screenP0);
        p1 = chartPanel.toPlot(screenP1);
        refresh();
        fireAnnotationChanged();
    }

    public double getDX(double dy) {
        return dy * (p1.x - p0.x) / (p1.y - p0.y);
    }

    public double getDY(double dx) {
        return dx * (p1.y - p0.y) / (p1.x - p0.x);
    }

    public double getX(double y) {
        return intersectHorizontal(new Line2D.Double(p0, p1), y);
    }

    public double getY(double x) {
        return intersectVertical(new Line2D.Double(p0, p1), x);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LegendIcon getIcon() {
        return icon;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void draw(Graphics2D g2, XYPlot plot, Rectangle2D dataArea, ValueAxis domainAxis, ValueAxis rangeAxis, int rendererIndex, PlotRenderingInfo info) {
        if (!init) {
            Point p = chartPanel.fromPlot(p0);
            Shape shape = new Ellipse2D.Double(p.x-2, p.y-2, 4,4);

            g2.setPaint(icon.getColor());
            g2.fill(shape);
            return;
        }

        if (!off || !visible) return;

        BasicStroke stroke = icon.getStroke();
        if (selected) {
            float width = 2 * stroke.getLineWidth();
            float[] dash = stroke.getDashArray();
            if (dash != null) {
                for (int i=0; i<dash.length; i++) dash[i] = 2*dash[i];
            }

            stroke = new BasicStroke(width, stroke.getEndCap(), stroke.getLineJoin(),
                    stroke.getMiterLimit(), dash, stroke.getDashPhase());
        }

        g2.setPaint(icon.getColor());
        g2.setStroke(stroke);
        g2.drawLine(screenP0.x, screenP0.y, screenP1.x, screenP1.y);
    }

}
