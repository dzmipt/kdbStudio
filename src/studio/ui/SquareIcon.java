package studio.ui;

import javax.swing.*;
import java.awt.*;

public class SquareIcon implements Icon {

    private final Paint color;
    private final int size;
    private boolean border = false;
    private final static Stroke BORDER_STROKE = new BasicStroke(0.5f);

    public SquareIcon(Paint color, int size) {
        this.color = color;
        this.size = size;
    }

    public Paint getColor() {
        return color;
    }

    public void setBorder(boolean border) {
        this.border = border;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(x, y);
        Paint paint = g2.getPaint();
        g2.setPaint(color);
        g2.fillRect(2,2, size - 4, size - 4);

        if (border) {
            Stroke stroke = g2.getStroke();
            g2.setStroke(BORDER_STROKE);
            g2.setPaint(Color.BLACK);
            g2.drawLine(3, size - 4, size - 4, size - 4);
            g2.drawLine(size - 4, 3, size - 4, size - 4);

            g2.setPaint(Color.WHITE);
            g2.drawLine(2, 2, size - 3, 2);
            g2.drawLine(2, 2, 2, size - 3);

            g2.setStroke(stroke);
        }

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
