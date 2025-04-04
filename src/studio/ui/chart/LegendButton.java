package studio.ui.chart;

import org.jfree.chart.plot.DefaultDrawingSupplier;
import studio.ui.ColorChooser;
import studio.ui.chart.event.LegendChangeEvent;
import studio.ui.chart.event.LegendChangeListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class LegendButton extends JLabel implements MouseListener {

    private final EventListenerList listenerList = new EventListenerList();
    private final List<ChartType> chartTypeList = new ArrayList<>(4);

    private Action[] additionalActions = null;

    private final static Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2,2,2,2);
    private final static Border SELECTED_BORDER = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

    public static final Paint[] BASE_COLORS = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE;
    public static final Shape[] SHAPES = DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE;
    public static final BasicStroke[] BASE_STROKES = new BasicStroke[] {
            new BasicStroke(1f),
            new BasicStroke(1f,BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,1f,new float[] {10,10},0f
            ),
            new BasicStroke(1f,BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,1f,new float[] {10,5},0f
            ),
            new BasicStroke(1f,BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,1f,new float[] {5,5},0f
            ),
            new BasicStroke(1f,BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,1f,new float[] {1.5f,3},0f
            ),
            new BasicStroke(1f,BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL,1f,new float[] {10,3,3,3},0f
            ),
    };

    private static class StrokeWidth {
        String title;
        float width;
        StrokeWidth(String title, float width) {
            this.title = title;
            this.width = width;
        }
    }

    private static StrokeWidth[] STROKE_WIDTHS = new StrokeWidth[] {
            new StrokeWidth("x 1", 1),
            new StrokeWidth("x 1.5", 1.5f),
            new StrokeWidth("x 2", 2),
            new StrokeWidth("x 3", 3),
    };

    //@TODO: May be it is better to have a cache of all possible strokes to avoid unnecessary garbage ?
    public static BasicStroke strokeWithWidth(BasicStroke stroke, float width) {
        if (stroke.getLineWidth() == width) return stroke;

        return new BasicStroke(width, stroke.getEndCap(), stroke.getLineJoin(),
                stroke.getMiterLimit(), stroke.getDashArray(), stroke.getDashPhase());
    }

    public static BasicStroke getDefaultStroke() {
        return strokeWithWidth(BASE_STROKES[0], STROKE_WIDTHS[2].width);// 2x width by default
    }

    public LegendButton(LegendIcon icon, boolean line, boolean shape, boolean bar) {
        super(icon);

        if (line) chartTypeList.add(ChartType.LINE);
        if (shape) chartTypeList.add(ChartType.SHAPE);
        if (line && shape) chartTypeList.add(ChartType.LINE_SHAPE);
        if (bar) chartTypeList.add(ChartType.BAR);

        setBorder(EMPTY_BORDER);
        addMouseListener(this);
    }

    public void setAdditionalActions(Action ... actions) {
        this.additionalActions = actions;
    }

    public LegendIcon getLegendIcon() {
        return (LegendIcon) getIcon();
    }

    public void addChangeListener(LegendChangeListener listener) {
        listenerList.add(LegendChangeListener.class, listener);
    }

    public void removeChangeListener(LegendChangeListener listener) {
        listenerList.remove(LegendChangeListener.class, listener);
    }

    private void notifyChange() {
        LegendChangeEvent event = new LegendChangeEvent(this, getLegendIcon());
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == LegendChangeListener.class) {
                ((LegendChangeListener) listeners[i + 1]).legendChanged(event);
            }
        }
    }

    private void notifyChangeAllStrokes() {
        LegendChangeEvent event = new LegendChangeEvent(this, getLegendIcon());
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == LegendChangeListener.class) {
                ((LegendChangeListener) listeners[i + 1]).changeAllStrokes(event);
            }
        }
    }

    private void notifyChangeAllShapes() {
        LegendChangeEvent event = new LegendChangeEvent(this, getLegendIcon());
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == LegendChangeListener.class) {
                ((LegendChangeListener) listeners[i + 1]).changeAllShapes(event);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {
        if (!isEnabled()) return;
        setBorder(SELECTED_BORDER);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!isEnabled()) return;
        setBorder(EMPTY_BORDER);
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (!isEnabled()) return;

        LegendIcon icon = getLegendIcon();
        Paint color = icon.getColor();
        Shape shape = icon.getShape();
        BasicStroke stroke = icon.getStroke();
        ChartType chartType = icon.getChartType();

        JPopupMenu popup = new JPopupMenu();

        JMenuItem menu = new JMenuItem("Change color", new SquareIcon(color, 15));
        menu.addActionListener(this::showChangeColor);
        popup.add(menu);

        if (chartTypeList.size() > 1) {
            JMenu subMenu = new JMenu("Change type");
            for (ChartType menuCharType: chartTypeList) {
                LegendIcon menuIcon = new LegendIcon(color, shape, stroke);
                menuIcon.setChartType(menuCharType);

                JCheckBoxMenuItem item = new JCheckBoxMenuItem(menuCharType.toString(), menuIcon, chartType == menuCharType);
                item.addActionListener(e -> {
                    icon.setChartType(menuCharType);
                    notifyChange();
                });
                subMenu.add(item);
            }
            popup.add(subMenu);
        }

        if (chartType.hasShape()) {
            JMenu subMenu = new JMenu("Change shape");
            for (Shape menuShape: SHAPES) {
                LegendIcon menuIcon = new LegendIcon(color, menuShape, stroke);
                menuIcon.setChartType(chartType);

                JCheckBoxMenuItem item = new JCheckBoxMenuItem("", menuIcon, shape == menuShape);
                item.addActionListener(e -> {
                    icon.setShape(menuShape);
                    notifyChange();
                });
                subMenu.add(item);
            }
            popup.add(subMenu);

            menu = new JMenuItem("Set this shape to all");
            menu.addActionListener(e -> notifyChangeAllShapes() );
            popup.add(menu);
        }

        if (chartType.hasLine()) {
            float width = stroke.getLineWidth();
            JMenu subMenu = new JMenu("Change stroke");
            for (BasicStroke baseStroke: BASE_STROKES) {
                BasicStroke menuStroke = strokeWithWidth(baseStroke, width);
                LegendIcon menuIcon = new LegendIcon(color, null, menuStroke);
                JCheckBoxMenuItem item = new JCheckBoxMenuItem("", menuIcon, menuStroke.equals(stroke));
                item.addActionListener(e -> {
                    icon.setStroke(menuStroke);
                    notifyChange();
                });
                subMenu.add(item);
            }
            subMenu.addSeparator();

            for (StrokeWidth strokeWidth: STROKE_WIDTHS) {
                boolean selected = width == strokeWidth.width;
                BasicStroke menuStroke = strokeWithWidth(stroke, strokeWidth.width);
                LegendIcon menuIcon = new LegendIcon(color, null, menuStroke);

                JCheckBoxMenuItem item = new JCheckBoxMenuItem(strokeWidth.title, menuIcon, selected);
                item.addActionListener(e -> {
                    icon.setStroke(menuStroke);
                    notifyChange();
                });
                subMenu.add(item);
            }
            popup.add(subMenu);

            menu = new JMenuItem("Set this stroke to all");
            menu.addActionListener(e -> notifyChangeAllStrokes() );
            popup.add(menu);
        }

        if (additionalActions != null) {
            for (Action action: additionalActions) {
                popup.add(action);
            }
        }

        popup.show(event.getComponent(), event.getX(), event.getY());

    }

    private void showChangeColor(ActionEvent e) {
        LegendIcon icon = getLegendIcon();
        Paint paint = icon.getColor();
        Color color = Color.BLACK;
        if (paint instanceof Color) {
            color = (Color)paint;
        }
        LegendIcon colorChoosePreviewIcon = new LegendIcon(color, icon.getShape(), icon.getStroke());
        colorChoosePreviewIcon.setChartType(icon.getChartType());
        Color result = ColorChooser.chooseColor(this, "Choose color", color,
                new JLabel(colorChoosePreviewIcon, SwingConstants.CENTER),
                colorChoosePreviewIcon::setColor
        );

        if (result != null) {
            icon.setColor(result);
            notifyChange();
        }
    }

}
