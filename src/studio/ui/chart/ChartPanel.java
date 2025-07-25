package studio.ui.chart;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AnnotationChangeEvent;
import org.jfree.chart.plot.*;
import org.jfree.chart.ui.RectangleEdge;
import studio.ui.UserAction;
import studio.ui.Util;
import studio.ui.chart.event.LineSelectionEvent;
import studio.ui.chart.event.LineSelectionListener;
import studio.ui.chart.event.NewLineEvent;
import studio.ui.chart.event.NewLineListener;
import studio.ui.chart.patched.CrosshairOverlay;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class ChartPanel extends studio.ui.chart.patched.ChartPanel {

    private final Crosshair xCrosshair;
    private final Crosshair yCrosshair;

    private final Action lineAction;
    private final Action copyAction;
    private final Action saveAction;
//    private final Action propertiesAction;
//    private final Action resetZoomAction;
//    private final Action zoomInAction;
//    private final Action zoomOutAction;

    private final EventListenerList listenerList = new EventListenerList();
    private boolean addingLine = false;
    private Line newLine = null;
    private Line selectedLine = null;

    private static final int LINE_SELECTION_SENSITIVITY = 100;

    public ChartPanel(JFreeChart chart) {
        super(chart);
        CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
        xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0.5f));
        xCrosshair.setLabelVisible(true);
        xCrosshair.setLabelGenerator(new KCrosshairLabelGenerator(chart, true));
        yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0.5f));
        yCrosshair.setLabelVisible(true);
        yCrosshair.setLabelGenerator(new KCrosshairLabelGenerator(chart, false));

        crosshairOverlay.addDomainCrosshair(xCrosshair);
        crosshairOverlay.addRangeCrosshair(yCrosshair);
        addOverlay(crosshairOverlay);
        setPreferredSize(new java.awt.Dimension(500, 270));
        setMouseWheelEnabled(true);
        setMouseZoomable(true, true);

        copyAction = addAccelerator(copyItem, Util.getMenuShortcut(KeyEvent.VK_C));
        copyAction.putValue(Action.SMALL_ICON, Util.COPY_ICON);
        copyAction.putValue(Action.SHORT_DESCRIPTION, "Copy the chart");

        saveAction = addAccelerator(pngItem, Util.getMenuShortcut(KeyEvent.VK_S));
        saveAction.putValue(Action.SMALL_ICON, Util.DISKS_ICON);
        saveAction.putValue(Action.SHORT_DESCRIPTION, "Save the chart");

        /*propertiesAction = */ addAccelerator(propertiesItem, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK));
        /*zoomInAction = */ addAccelerator(zoomInBothMenuItem, Util.getMenuShortcut(KeyEvent.VK_PLUS),
                                                          Util.getMenuShortcut(KeyEvent.VK_EQUALS),
                                                          Util.getMenuShortcut(KeyEvent.VK_ADD));
        /*zoomOutAction = */ addAccelerator(zoomOutBothMenuItem, Util.getMenuShortcut(KeyEvent.VK_MINUS),
                                                            Util.getMenuShortcut(KeyEvent.VK_SUBTRACT));
        /*resetZoomAction = */ addAccelerator(zoomResetBothMenuItem, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

        lineAction = UserAction.create("Add line", Util.LINE_ICON, "Add a line", KeyEvent.VK_L,null,
                                        e -> addLineAction() );
//                        .addActionToComponent(this); // didn't figure out why keyStroke doesn't trigger the action
    }

    private Action addAccelerator(JMenuItem menuItem, KeyStroke... keyStrokes) {
        menuItem.setAccelerator(keyStrokes[0]);
        UserAction action = UserAction.create(menuItem.getText(), menuItem.getToolTipText(), 0, keyStrokes[0], this);
        action.putValue(Action.ACTION_COMMAND_KEY, menuItem.getActionCommand());

        for (KeyStroke keyStroke: keyStrokes) {
            String key = menuItem.getText() + " - " + keyStroke;
            getActionMap().put(key, action);
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(keyStroke, key);
        }
        return action;
    }

    public Action getLineAction() {
        return lineAction;
    }

    public Action getCopyAction() {
        return copyAction;
    }

    public Action getSaveAction() {
        return saveAction;
    }

    public void addNewLineListener(NewLineListener listener) {
        listenerList.add(NewLineListener.class, listener);
    }

    public void removeNewLineListener(NewLineListener listener) {
        listenerList.remove(NewLineListener.class, listener);
    }

    public void addLineSelectionListener(LineSelectionListener listener) {
        listenerList.add(LineSelectionListener.class, listener);
    }

    public void removeLineSelectionListener(LineSelectionListener listener) {
        listenerList.remove(LineSelectionListener.class, listener);
    }

    private void notifyListeners(NewLineEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == NewLineListener.class) {
                ((NewLineListener) listeners[i + 1]).lineAdded(event);
            }
        }
    }

    private void notifyListeners(LineSelectionEvent event) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == LineSelectionListener.class) {
                ((LineSelectionListener) listeners[i + 1]).lineSelected(event);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(ZOOM_RESET_BOTH_COMMAND)) {
            JPopupMenu popup = getPopupMenu();
            if (popup != null && popup.isShowing()) {
                return;
            }
            if (zoomRectangle != null) {
                zoomPoint = null;
                zoomRectangle = null;
                repaint();
                return;
            }
        }

        super.actionPerformed(event);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        checkLineSelection(e.getPoint());
        super.mouseMoved(e);
        Rectangle2D dataArea = getScreenDataArea();
        JFreeChart chart = getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        double x = xAxis.java2DToValue(e.getX(), dataArea,
                RectangleEdge.BOTTOM);
        double y = plot.getRangeAxis().java2DToValue(e.getY(), dataArea, RectangleEdge.LEFT);
        xCrosshair.setValue(x);
        yCrosshair.setValue(y);

    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        xCrosshair.setVisible(false);
        yCrosshair.setVisible(false);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
        xCrosshair.setVisible(true);
        yCrosshair.setVisible(true);
    }

    @SuppressWarnings("deprecation")
    private final static int CTRL_MASK = InputEvent.CTRL_MASK;
    @SuppressWarnings("deprecation")
    private final static int ALT_MASK = InputEvent.ALT_MASK;

    private int getPanMask() {
        if (this.panMask == CTRL_MASK) return InputEvent.CTRL_DOWN_MASK;
        if (this.panMask == ALT_MASK) return InputEvent.ALT_DOWN_MASK;
        return this.panMask;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        JFreeChart chart = getChart();
        if (chart == null) {
            return;
        }
        if (selectedLine != null) return;

        if (addingLine) {
            addLine(e);
            return;
        }
        Plot plot = chart.getPlot();
        if ((e.getModifiersEx() & getPanMask()) != 0) {
            // can we pan this plot?
            if (plot instanceof Pannable) {
                Pannable pannable = (Pannable) plot;
                if (pannable.isDomainPannable() || pannable.isRangePannable()) {
                    Rectangle2D screenDataArea = getScreenDataArea(e.getX(),
                            e.getY());
                    if (screenDataArea != null && screenDataArea.contains(
                            e.getPoint())) {
                        this.panW = screenDataArea.getWidth();
                        this.panH = screenDataArea.getHeight();
                        this.panLast = e.getPoint();
                        setCursor(Cursor.getPredefinedCursor(
                                Cursor.MOVE_CURSOR));
                    }
                }
                // the actual panning occurs later in the mouseDragged()
                // method
            }
        }
        else if (this.zoomRectangle == null) {
            Rectangle2D screenDataArea = getScreenDataArea(e.getX(), e.getY());
            if (screenDataArea != null) {
                this.zoomPoint = getPointInRectangle(e.getX(), e.getY(),
                        screenDataArea);
            }
            else {
                this.zoomPoint = null;
            }
            if (e.isPopupTrigger()) {
                displayPopupMenu(e.getX(), e.getY());
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        JPopupMenu popup = getPopupMenu();
        // if the popup menu has already been triggered, then ignore dragging...
        if (popup != null && popup.isShowing()) {
            return;
        }

        if (checkLineDrag(e)) return;

        JFreeChart chart = getChart();
        ChartRenderingInfo info = getChartRenderingInfo();
        // handle panning if we have a start point
        if (this.panLast != null) {
            double dx = e.getX() - this.panLast.getX();
            double dy = e.getY() - this.panLast.getY();
            if (dx == 0.0 && dy == 0.0) {
                return;
            }
            double wPercent = -dx / this.panW;
            double hPercent = dy / this.panH;
            boolean old = chart.getPlot().isNotify();
            chart.getPlot().setNotify(false);
            Pannable p = (Pannable) chart.getPlot();
            if (p.getOrientation() == PlotOrientation.VERTICAL) {
                p.panDomainAxes(wPercent, info.getPlotInfo(),
                        this.panLast);
                p.panRangeAxes(hPercent, info.getPlotInfo(),
                        this.panLast);
            }
            else {
                p.panDomainAxes(hPercent, info.getPlotInfo(),
                        this.panLast);
                p.panRangeAxes(wPercent, info.getPlotInfo(),
                        this.panLast);
            }
            this.panLast = e.getPoint();
            chart.getPlot().setNotify(old);
            return;
        }

        // if no initial zoom point was set, ignore dragging...
        if (this.zoomPoint == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) getGraphics();

        // erase the previous zoom rectangle (if any).  We only need to do
        // this is we are using XOR mode, which we do when we're not using
        // the buffer (if there is a buffer, then at the end of this method we
        // just trigger a repaint)
        drawZoomRectangle(g2, true);

        Rectangle2D scaledDataArea = getScreenDataArea(
                (int) this.zoomPoint.getX(), (int) this.zoomPoint.getY());
        double x = Math.min(e.getX(), scaledDataArea.getMaxX());
        x = Math.max(x, scaledDataArea.getMinX());
        double y = Math.min(e.getY(), scaledDataArea.getMaxY());
        y = Math.max(y, scaledDataArea.getMinY());

        double x0 = Math.min(x, zoomPoint.getX());
        double x1 = Math.max(x, zoomPoint.getX());
        double y0 = Math.min(y, zoomPoint.getY());
        double y1 = Math.max(y, zoomPoint.getY());
        this.zoomRectangle = new Rectangle2D.Double(x0, y0, x1-x0, y1-y0);

        // Draw the new zoom rectangle...
        repaint();
        g2.dispose();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // if we've been panning, we need to reset now that the mouse is
        // released...
        if (this.panLast != null) {
            this.panLast = null;
            setCursor(Cursor.getDefaultCursor());
        }

        else if (this.zoomRectangle != null) {
            boolean zoomTrigger1 = Math.abs(e.getX()
                    - this.zoomPoint.getX()) >= this.zoomTriggerDistance;
            boolean zoomTrigger2 = Math.abs(e.getY()
                    - this.zoomPoint.getY()) >= this.zoomTriggerDistance;
            if (zoomTrigger1 || zoomTrigger2) {
                zoom(zoomRectangle);
            }
            else {
                // erase the zoom rectangle
                Graphics2D g2 = (Graphics2D) getGraphics();
                repaint();
                g2.dispose();
            }
            this.zoomPoint = null;
            this.zoomRectangle = null;
        }

        else if (e.isPopupTrigger()) {
            displayPopupMenu(e.getX(), e.getY());
        }

    }

    private void addLineAction() {
        addingLine = true;
    }

    private boolean checkLineDrag(MouseEvent event) {
        if (selectedLine == null) return false;


        if ((event.getModifiersEx() & Util.menuShortcutKeyMask) == Util.menuShortcutKeyMask) {
            selectedLine.dragTo(event.getPoint());
        } else {
            selectedLine.moveTo(toPlot(event.getPoint()));
        }

        chartChanged(new AnnotationChangeEvent(this, selectedLine));
        return true;
    }

    private void checkLineSelection(Point p) {
        XYPlot plot = getChart().getXYPlot();
        int selectedIndex = -1;
        int newSelectedIndex = -1;
        double shortestDist = Double.POSITIVE_INFINITY;

        @SuppressWarnings("rawtypes")
        List list = plot.getAnnotations();
        for(int index = 0; index<list.size(); index++) {
            Line line = (Line) list.get(index);
            if (line.isSelected()) selectedIndex = index;
            double d = line.distanceSqr(p.x, p.y);
            if (d < LINE_SELECTION_SENSITIVITY && d < shortestDist) {
                shortestDist = d;
                newSelectedIndex = index;
            }
        }

        Line line = null;
        if (selectedIndex != -1) {
            line = (Line)list.get(selectedIndex);
            line.setSelected(false);
        }
        if (newSelectedIndex != -1) {
            line = selectedLine = (Line) list.get(newSelectedIndex);
            selectedLine.setSelected(true);
        } else {
            selectedLine = null;
        }

        if (selectedIndex != newSelectedIndex) {
            notifyListeners(new LineSelectionEvent(this, selectedLine));
            chartChanged(new AnnotationChangeEvent(this, line));
        }
    }

    private void addLine(MouseEvent e) {
        XYPlot plot = getChart().getXYPlot();
        Point2D.Double p = toPlot(e.getPoint());
        if (newLine == null) {
            newLine = new Line(this, p);
            plot.addAnnotation(newLine);
        } else {
            if (! newLine.addPoint(p) ) return;
            chartChanged(new AnnotationChangeEvent(this, newLine));
            notifyListeners(new NewLineEvent(this, newLine));
            newLine = null;
            addingLine = false;
        }
    }

    public Point fromPlot(Point2D.Double p) {
        XYPlot plot = getChart().getXYPlot();
        Rectangle2D dataArea = getScreenDataArea();
        double x = plot.getDomainAxis().valueToJava2D(p.x, dataArea, plot.getDomainAxisEdge());
        double y = plot.getRangeAxis().valueToJava2D(p.y, dataArea, plot.getRangeAxisEdge());
        return new Point((int)x, (int)y);

    }

    public Point2D.Double toPlot(Point p) {
        XYPlot plot = getChart().getXYPlot();
        Rectangle2D dataArea = getScreenDataArea();
        double x = plot.getDomainAxis().java2DToValue(p.x, dataArea, plot.getDomainAxisEdge());
        double y = plot.getRangeAxis().java2DToValue(p.y, dataArea, plot.getRangeAxisEdge());
        return new Point2D.Double(x, y);
    }

}
