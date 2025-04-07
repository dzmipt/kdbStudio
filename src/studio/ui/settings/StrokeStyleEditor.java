package studio.ui.settings;

import studio.kdb.Config;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StrokeStyleEditor extends StrokeEditorComponent {

    public StrokeStyleEditor(int iconWidth, int prefWidth) {
        super(iconWidth, prefWidth);
    }

    @Override
    protected List<BasicStroke> getInitStrokes() {
        return Config.getInstance().getStyleStrokes();
    }

    @Override
    public void saveSettings() {
        List<BasicStroke> strokes = getStrokes();
        List<String> texts = new ArrayList<>(strokes.size());
        for (BasicStroke stroke: strokes) {
            texts.add(getText(stroke));
        }
        Config.getInstance().setStringArray(Config.CHART_STROKE_STYLES, texts);
    }

    @Override
    protected DndList.ListItem getListItem(BasicStroke stroke) {
        return new DndList.ListItem("", getIcon(stroke));
    }

    @Override
    protected String getText(BasicStroke stroke) {
        float[] dashArray = stroke.getDashArray();
        if (dashArray == null) return "";
        StringBuffer str = new StringBuffer();
        for (int i = 0; i < dashArray.length; i++) {
            if (i > 0) str.append(", ");
            str.append(f2.format(round(dashArray[i])));
        }
        return str.toString();
    }

    @Override
    protected BasicStroke getStroke(String text) {
        return parseDashArray(text);
    }

    public static BasicStroke parseDashArray(String text) {
        try {
            String str = text.trim();
            if (str.isEmpty()) {
                return new BasicStroke(1);
            }
            String[] words = str.split(",");
            float[] dashArray = new float[words.length];
            for (int i = 0; i < words.length; i++) {
                dashArray[i] = round(Float.parseFloat(words[i]));
            }

            return new BasicStroke(1f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_BEVEL, 1f, dashArray, 0f);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
