package studio.ui.chart;


import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public class LegendIcon implements Icon {

    private final static int WIDTH = 30;
    private final static int HEIGHT = 20;
    private final static Stroke SHAPE_OUTLINE_STROKE = new BasicStroke(0.3f);
    private final static AffineTransform SHAPE_TRANSFORM;
    private final static Shape LINE;
    private final static Shape[] BAR_SHAPES;
    static {
        double dx = WIDTH / 2.0;
        double dy = HEIGHT / 2.0;
        SHAPE_TRANSFORM = new AffineTransform(2, 0, 0, 2, dx, dy);
        LINE = new Line2D.Double(0, dy, WIDTH, dy);

        // bar rectangles
        double w = 5;
        double d = 2;
        double[] hr = new double[] {0.25, 0.5, 1};
        double h = HEIGHT - 2*d;

        double x0 = dx - 1.5*w - d;
        double y0 = HEIGHT - d;
        BAR_SHAPES = new Shape[3];
        for (int i=0; i<3; i++) {
            BAR_SHAPES[i] = new Rectangle2D.Double(x0, y0 - h*hr[i], w, h*hr[i]);
            x0 += w+d;
        }
    }


    private Paint color;
    private Shape shape;
    private BasicStroke stroke;
    private ChartType chartType;

    public LegendIcon(LegendIcon icon) {
        this.color = icon.color;
        this.shape = icon.shape;
        this.stroke = icon.stroke;
        this.chartType = icon.chartType;
    }

    public LegendIcon(Paint color, Shape shape, BasicStroke stroke) {
        if (color == null) throw new IllegalArgumentException("Color is null");
        this.color = color;
        this.shape = shape;
        this.stroke = stroke;

        chartType = shape == null ?
                        (stroke == null ? ChartType.BAR : ChartType.LINE) :
                        (stroke == null ? ChartType.SHAPE : ChartType.LINE_SHAPE);
    }

    public void setChartType(ChartType chartType) {
        if (chartType.hasLine() && stroke == null)
            throw new IllegalArgumentException("Stroke is null. Can't set chartType type is " + chartType);

        if (chartType.hasShape() && shape == null)
            throw new IllegalArgumentException("Shape is null. Can't set chartType type is " + chartType);


        this.chartType = chartType;
    }

    public ChartType getChartType() {
        return chartType;
    }

    public Paint getColor() {
        return color;
    }

    public void setColor(Paint color) {
        if (color == null) throw new IllegalArgumentException("Color is null");
        this.color = color;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        if (chartType.hasShape() && shape == null)
            throw new IllegalArgumentException("Shape is null. chartType type is " + chartType);
        this.shape = shape;
    }

    public BasicStroke getStroke() {
        return stroke;
    }

    public void setStroke(BasicStroke stroke) {
        if (chartType.hasLine() && stroke == null)
            throw new IllegalArgumentException("Stroke is null. chartType type is " + chartType);
        this.stroke = stroke;
    }

    public XYItemRenderer getChartRenderer() {
        XYItemRenderer renderer;
        if (chartType == ChartType.BAR) {
            renderer = new XYBarRenderer();
            ((XYBarRenderer)renderer).setGradientPaintTransformer(null);
            ((XYBarRenderer)renderer).setShadowVisible(false);

        } else {
            renderer = new XYLineAndShapeRenderer(chartType.hasLine(), chartType.hasShape());
        }
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesShape(0, shape);
        renderer.setSeriesStroke(0, stroke);
        ((AbstractRenderer) renderer).setAutoPopulateSeriesPaint(false);
        ((AbstractRenderer) renderer).setAutoPopulateSeriesShape(false);
        ((AbstractRenderer) renderer).setAutoPopulateSeriesStroke(false);
        return renderer;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(x, y);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Paint paint = g2.getPaint();
        g2.setPaint(color);

        if (chartType == ChartType.BAR) {
            for (Shape shape : BAR_SHAPES) {
                g2.fill(shape);
            }
        } else {
            if (chartType.hasLine()) {
                g2.setStroke(stroke);
                g2.draw(LINE);
            }

            if (chartType.hasShape()) {
                AffineTransform transform = g2.getTransform();
                g2.transform(SHAPE_TRANSFORM);
                g2.fill(shape);

                g2.setPaint(Color.BLACK);
                g2.setStroke(SHAPE_OUTLINE_STROKE);
                g2.draw(shape);

                g2.setTransform(transform);
            }
        }
        g2.setPaint(paint);
        g2.translate(-x, -y);
    }

    @Override
    public int getIconWidth() {
        return WIDTH;
    }

    @Override
    public int getIconHeight() {
        return HEIGHT;
    }
}
