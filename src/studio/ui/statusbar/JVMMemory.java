package studio.ui.statusbar;

import studio.ui.MinSizeLabel;
import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JVMMemory extends MinSizeLabel {

    private double ratio = 0;

    public JVMMemory() {
        super("");
        refresh(null);
        setOpaque(false);
        setMinimumWidth(format(999_000_000, 99999_000_000L));
        setHorizontalAlignment(JLabel.CENTER);
        new Timer(1000, this::refresh).start();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.gc();
                refresh(null);
                repaint();
            }
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private String getContentText() {
        Runtime rt = Runtime.getRuntime();
        long free = rt.freeMemory();
        long total = rt.totalMemory();
        long used = total - free;
        ratio = (double) used/ total;
        return format(used, total);
    }

    private String format(long used, long total) {
        return String.format(
                " %d of %d MB ",
                used / 1_000_000, total / 1_000_000
        );
    }

    private void refresh(ActionEvent e) {
        setText(getContentText());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        int w = getWidth();
        int h = getHeight();

        // draw used memory bar (light red)
        int usedWidth = (int) (w * ratio);

        Color color = UIManager.getColor("Tree.selectionBackground");
        g2.setColor(Util.blendColors(color, getBackground()));

        g2.fillRect(0, 0, usedWidth, h);

        super.paintComponent(g2);
        g2.dispose();
    }


}
