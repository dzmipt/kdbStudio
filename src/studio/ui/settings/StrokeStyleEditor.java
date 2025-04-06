package studio.ui.settings;

import studio.kdb.Config;

import java.awt.*;
import java.util.List;

public class StrokeStyleEditor extends StrokeEditorComponent {

    public StrokeStyleEditor(int iconWidth, int prefWidth) {
        super(iconWidth, prefWidth);
    }

    @Override
    protected List<BasicStroke> getInitStrokes() {
        return Config.getInstance().getStrokesFromStyle();
    }

    @Override
    protected DndList.ListItem getListItem(BasicStroke stroke) {
        return new DndList.ListItem("", getIcon(stroke));
    }

    @Override
    protected String getText(BasicStroke stroke) {
        return Config.styleFromStroke(stroke);
    }

    @Override
    protected BasicStroke getStroke(String text) {
        return Config.strokeFromStyle(text);
    }
}
