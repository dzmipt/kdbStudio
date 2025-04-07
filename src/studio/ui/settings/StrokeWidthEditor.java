package studio.ui.settings;

import studio.kdb.Config;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StrokeWidthEditor extends StrokeEditorComponent {

    public StrokeWidthEditor(int iconWidth, int prefWidth) {
        super(iconWidth, prefWidth);
    }

    @Override
    protected List<BasicStroke> getInitStrokes() {
        List<Double> widths = Config.getInstance().getDoubleArray(Config.CHART_STROKE_WIDTHS);
        List<BasicStroke> strokes = new ArrayList<>(widths.size());
        for (Double width: widths) {
            strokes.add(new BasicStroke(round(width.floatValue())));
        }
        return strokes;
    }

    @Override
    public void saveSettings() {
        List<BasicStroke> strokes = getStrokes();
        List<Double> values = new ArrayList<>(strokes.size());
        for (BasicStroke stroke: strokes) {
            values.add(Math.round(100*stroke.getLineWidth()) / 100.0 );
        }
        Config.getInstance().setDoubleArray(Config.CHART_STROKE_WIDTHS, values);
    }

    @Override
    protected DndList.ListItem getListItem(BasicStroke stroke) {
        return new DndList.ListItem("x " + f2.format(round(stroke.getLineWidth())), getIcon(stroke));
    }

    @Override
    protected String getText(BasicStroke stroke) {
        return f2.format(round(stroke.getLineWidth()));
    }

    @Override
    protected BasicStroke getStroke(String text) {
        try {
            float width = round(Float.parseFloat(text));
            return new BasicStroke(width);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
