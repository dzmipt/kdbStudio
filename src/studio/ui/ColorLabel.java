package studio.ui;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ColorLabel extends JLabel {
    public final static int DEFAULT_SIZE = 24;

    private final List<ChangeListener> listeners = new ArrayList<>();

    private boolean singleClick = false;

    public ColorLabel(Color color, int size) {
        super(new SquareIcon(color, size));
        ((SquareIcon)getIcon()).setBorder(true);
        setOpaque(true);
        setPreferredSize(new Dimension(size, size));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ( !singleClick && e.getClickCount() >= 2 ) {
                    selectColor();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (singleClick) {
                    selectColor();
                }
            }
        });
    }

    public ColorLabel() {
        this(Color.BLACK);
    }

    public ColorLabel(Color color) {
        this(color, DEFAULT_SIZE);
    }

    public boolean isSingleClick() {
        return singleClick;
    }

    public void setSingleClick(boolean singleClick) {
        this.singleClick = singleClick;
    }

    public Color getColor() {
        return (Color) ((SquareIcon)getIcon()).getColor();
    }

    public int getIconSize() {
        return getIcon().getIconWidth();
    }

    public void setColor(Color newColor) {
        if (getColor().equals(newColor)) return;
        SquareIcon icon = new SquareIcon(newColor, getIconSize());
        icon.setBorder(true);
        setIcon(icon);
        fireEvent();
    }

    public void selectColor() {
        Color color = getColor();
        Color newColor = ColorChooser.chooseColor(ColorLabel.this,"Select color", color);
        if (newColor == null || newColor.equals(color)) return;

        setColor(newColor);
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private void fireEvent() {
        ChangeEvent event = new ChangeEvent(this);
        listeners.forEach(l -> l.stateChanged(event));
    }
}
