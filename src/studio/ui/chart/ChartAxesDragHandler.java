package studio.ui.chart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

public class ChartAxesDragHandler implements MouseWheelListener, MouseListener, MouseMotionListener {

    private final ChartPanel panel;
    private final XYPlot plot;

    private ValueAxis dragAxis = null;
    private Shape dragAxisArea = null;
    private RectangleEdge dragEdge;

    private Range dragRange;
    private int dragX, dragY;

    private boolean zoomDrag = false;

    private final static Logger log = LogManager.getLogger();

    public ChartAxesDragHandler(ChartPanel panel, XYPlot plot) {
        this.panel = panel;
        this.plot = plot;
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (dragAxis == null) {
            dragRange = null;
            return;
        }

        dragX = e.getX();
        dragY = e.getY();
        dragRange = dragAxis.getRange();
        zoomDrag = (e.getModifiersEx() & panel.getPanMask()) == 0;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragRange = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragRange == null) return;

        Rectangle2D rect = dragAxisArea.getBounds2D();

        Range newRange;
        if (zoomDrag) {
            double xd, x, x1, x2;
            if (RectangleEdge.isTopOrBottom(dragEdge)) {
                xd = dragX;
                x = e.getX();
                x1 = rect.getMinX();
                x2 = rect.getMaxX();
            } else {
                xd = dragY;
                x = e.getY();
                x1 = rect.getMinY();
                x2 = rect.getMaxY();
            }

            double x0 = (x1+x2)/2;

            if (Math.abs(xd-x0)<0.5) xd = x0 + 1;

            if (xd>x0) x = Math.max(x, x0+1);
            else x = Math.min(x, x0-1);

            double p = (xd-x0) / (x-x0);

            double v1 = dragRange.getLowerBound();
            double v2 = dragRange.getUpperBound();
            double v0 = dragRange.getCentralValue();

            newRange = new Range(v0 + p*(v1-v0), v0 + p*(v2-v0));

            log.debug("Dragging: x1={}, x0={}, x2={}", x1, x0, x2);
            log.debug("Dragging: xd={}, x={}, p={}", xd, x, p);
            log.debug("Dragging: v1={}, v0={}, v2={}", v1, v0, v2);
            log.debug("Dragging: newRange={}", newRange);
        } else {
            double len;
            int delta;
            if (RectangleEdge.isTopOrBottom(dragEdge)) {
                delta = dragX - e.getX(); // opposite to Y
                len = rect.getWidth();
            } else {
                delta = e.getY() - dragY;
                len = rect.getHeight();
            }

            double rDelta = dragRange.getLength() * delta / len;

            newRange = Range.shift(dragRange, rDelta, true);
        }
        dragAxis.setRange(newRange);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (dragRange != null)  return;

        ChartEntity entity = panel.getEntityForPoint(e.getX(), e.getY());
        ValueAxis axis = null;
        Shape area = null;
        RectangleEdge edge = null;
        if (entity instanceof AxisEntity && ((AxisEntity)entity).getAxis() instanceof ValueAxis) {
            AxisEntity axisEntity = (AxisEntity) entity;
            axis = (ValueAxis) axisEntity.getAxis();
            area = axisEntity.getArea();

            int count = plot.getDomainAxisCount();
            for(int i=0; i<count; i++) {
                if (plot.getDomainAxis(i) == axis) {
                    edge = plot.getDomainAxisEdge(i);
                    break;
                }
            }

            count = plot.getRangeAxisCount();
            for(int i=0; i<count; i++) {
                if (plot.getRangeAxis(i) == axis) {
                    edge = plot.getRangeAxisEdge(i);
                    break;
                }
            }

            if (edge == null) {
                axis = null;
                area = null;
            }
        }

        if (Objects.equals(dragAxisArea, area)) return;
        dragEdge = edge;

        if (dragAxis != null) {
            Font font = dragAxis.getTickLabelFont();
            dragAxis.setTickLabelFont(font.deriveFont(Font.PLAIN));
        }

        dragAxis = axis;
        dragAxisArea = area;

        if (axis != null) {
            Font font = axis.getTickLabelFont();
            axis.setTickLabelFont(font.deriveFont(Font.BOLD));
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            panel.setCursor(Cursor.getDefaultCursor());
        }

        panel.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (dragAxis == null) return;

        double factor = e.getWheelRotation()<0 ? 0.9 : 1.1;
        dragAxis.resizeRange(factor);
    }
}
