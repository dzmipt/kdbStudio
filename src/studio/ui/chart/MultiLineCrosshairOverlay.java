package studio.ui.chart;

import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.text.TextBlock;
import org.jfree.chart.text.TextBlockAnchor;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.Size2D;
import studio.ui.chart.patched.CrosshairOverlay;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class MultiLineCrosshairOverlay extends CrosshairOverlay {

    @Override
    protected void drawHorizontalCrosshair(Graphics2D g2, Rectangle2D dataArea, double y, Crosshair crosshair) {
        if (y >= dataArea.getMinY() && y <= dataArea.getMaxY()) {
            Line2D line = new Line2D.Double(dataArea.getMinX(), y,
                    dataArea.getMaxX(), y);
            Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            g2.setPaint(crosshair.getPaint());
            g2.setStroke(crosshair.getStroke());
            g2.draw(line);
            if (crosshair.isLabelVisible()) {
                String label = crosshair.getLabelGenerator().generateLabel(
                        crosshair);
                if (label != null && !label.isEmpty()) {
                    Font savedFont = g2.getFont();
                    g2.setFont(crosshair.getLabelFont());

                    TextBlock text = TextUtils.createTextBlock(label, crosshair.getLabelFont(), crosshair.getLabelPaint());
                    text.setLineAlignment(HorizontalAlignment.LEFT);
                    Size2D size = text.calculateDimensions(g2);

                    RectangleAnchor anchor = crosshair.getLabelAnchor();
                    Point2D pt = calculateLabelPoint(line, anchor, crosshair.getLabelXOffset(), crosshair.getLabelYOffset());
                    Rectangle2D hotspot = RectangleAnchor.createRectangle(size, pt.getX(), pt.getY(), flipAnchorV(anchor));

                    if (!dataArea.contains(hotspot.getBounds2D())) {
                        anchor = flipAnchorV(anchor);
                        pt = calculateLabelPoint(line, anchor, crosshair.getLabelXOffset(), crosshair.getLabelYOffset());
                        hotspot = RectangleAnchor.createRectangle(size, pt.getX(), pt.getY(), flipAnchorV(anchor));
                    }

                    g2.setPaint(crosshair.getLabelBackgroundPaint());
                    g2.fill(hotspot);
                    if (crosshair.isLabelOutlineVisible()) {
                        g2.setPaint(crosshair.getLabelOutlinePaint());
                        g2.setStroke(crosshair.getLabelOutlineStroke());
                        g2.draw(hotspot);
                    }
                    g2.setPaint(crosshair.getLabelPaint());
                    text.draw(g2,(float)hotspot.getMinX(), (float)hotspot.getMinY(), TextBlockAnchor.TOP_LEFT);
                    g2.setFont(savedFont);
                }
            }
            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
        }
    }

    @Override
    protected void drawVerticalCrosshair(Graphics2D g2, Rectangle2D dataArea, double x, Crosshair crosshair) {
        if (x >= dataArea.getMinX() && x <= dataArea.getMaxX()) {
            Line2D line = new Line2D.Double(x, dataArea.getMinY(), x,
                    dataArea.getMaxY());
            Paint savedPaint = g2.getPaint();
            Stroke savedStroke = g2.getStroke();
            g2.setPaint(crosshair.getPaint());
            g2.setStroke(crosshair.getStroke());
            g2.draw(line);
            if (crosshair.isLabelVisible()) {
                String label = crosshair.getLabelGenerator().generateLabel(
                        crosshair);
                if (label != null && !label.isEmpty()) {
                    Font savedFont = g2.getFont();
                    g2.setFont(crosshair.getLabelFont());

                    TextBlock text = TextUtils.createTextBlock(label, crosshair.getLabelFont(), crosshair.getLabelPaint());
                    Size2D size = text.calculateDimensions(g2);

                    RectangleAnchor anchor = crosshair.getLabelAnchor();
                    Point2D pt = calculateLabelPoint(line, anchor, crosshair.getLabelXOffset(), crosshair.getLabelYOffset());
                    Rectangle2D hotspot = RectangleAnchor.createRectangle(size, pt.getX(), pt.getY(), flipAnchorH(anchor));

                    if (!dataArea.contains(hotspot.getBounds2D())) {
                        anchor = flipAnchorH(anchor);
                        pt = calculateLabelPoint(line, anchor, crosshair.getLabelXOffset(), crosshair.getLabelYOffset());
                        hotspot = RectangleAnchor.createRectangle(size, pt.getX(), pt.getY(), flipAnchorH(anchor));
                    }

                    g2.setPaint(crosshair.getLabelBackgroundPaint());
                    g2.fill(hotspot);
                    if (crosshair.isLabelOutlineVisible()) {
                        g2.setPaint(crosshair.getLabelOutlinePaint());
                        g2.setStroke(crosshair.getLabelOutlineStroke());
                        g2.draw(hotspot);
                    }
                    g2.setPaint(crosshair.getLabelPaint());
                    text.draw(g2, (float)hotspot.getMinX(), (float)hotspot.getMinY(), TextBlockAnchor.TOP_LEFT);
                    g2.setFont(savedFont);
                }
            }
            g2.setPaint(savedPaint);
            g2.setStroke(savedStroke);
        }
    }
}
