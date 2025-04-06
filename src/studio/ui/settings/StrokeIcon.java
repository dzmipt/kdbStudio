package studio.ui.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

public class StrokeIcon implements Icon {

    private final BasicStroke stroke;
    private final Color color;
    private final int width;
    private final int height;

    public StrokeIcon(BasicStroke stroke, Color color, int width, int height) {
        this.stroke = stroke;
        this.color = color;
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(x, y);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(color);
        g2.setStroke(stroke);

        double dy = height / 2.0;
        Shape line = new Line2D.Double(0, dy, width, dy);
        g2.draw(line);

        g2.translate(-x, -y);
    }

    public BasicStroke getStroke() {
        return stroke;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
