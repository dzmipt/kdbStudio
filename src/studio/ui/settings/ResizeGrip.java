package studio.ui.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class ResizeGrip extends JComponent implements MouseListener, MouseMotionListener {
    private static final int SIZE = 16;

    private final JComponent reference;
    private Point dragStart;
    private int prefHeight;

    public ResizeGrip(JComponent reference) {
        this.reference = reference;
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        addMouseListener(this);
        addMouseMotionListener(this);
    }


    @Override
    public void mousePressed(MouseEvent e) {
        dragStart = e.getLocationOnScreen();
        prefHeight = reference.getPreferredSize().height;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int dy = e.getLocationOnScreen().y - dragStart.y;
        Dimension prefSize = reference.getPreferredSize();
        prefSize.height = prefHeight + dy;
        reference.setPreferredSize(prefSize);
        reference.getParent().revalidate();
        reference.getParent().repaint();
    }


    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(UIManager.getColor("ScrollBar.thumbShadow"));
        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < 3; i++) {
            int offset = i * 4;
            g2.drawLine(w - 10 + offset, h - 1, w - 1, h - 10 + offset);
        }
        g2.dispose();
    }
}
