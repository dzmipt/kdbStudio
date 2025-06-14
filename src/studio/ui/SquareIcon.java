package studio.ui;

import javax.swing.*;
import java.awt.*;

public class SquareIcon implements Icon {

    private final Paint color;
    private final int size;

    public SquareIcon(Paint color, int size) {
        this.color = color;
        this.size = size;
    }

    public Paint getColor() {
        return color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(x, y);
        Paint paint = g2.getPaint();
        g2.setPaint(color);
        g2.fillRect(2,2, size - 4, size - 4);
        g2.setPaint(paint);
        g2.translate(-x, -y);
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
