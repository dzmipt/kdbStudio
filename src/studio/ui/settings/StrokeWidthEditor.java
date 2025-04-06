package studio.ui.settings;

import studio.kdb.Config;

import java.awt.*;
import java.util.List;

public class StrokeWidthEditor extends StrokeEditorComponent {

    public StrokeWidthEditor(int iconWidth, int prefWidth) {
        super(iconWidth, prefWidth);
    }

    @Override
    protected List<BasicStroke> getInitStrokes() {
        return Config.getInstance().getStrokesFromWidth();
    }

    @Override
    protected DndList.ListItem getListItem(BasicStroke stroke) {
        return new DndList.ListItem("x " + stroke.getLineWidth(), getIcon(stroke));
    }

    @Override
    protected String getText(BasicStroke stroke) {
        return Config.widthFromStroke(stroke);
    }

    @Override
    protected BasicStroke getStroke(String text) {
        return Config.strokeFromWidth(text);
    }

}
