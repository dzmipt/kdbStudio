package studio.ui.chart.event;

import studio.ui.chart.LegendIcon;

import java.util.EventObject;

public class LegendChangeEvent extends EventObject {

    private LegendIcon icon;

    public LegendChangeEvent(Object source, LegendIcon icon) {
        super(source);
        this.icon = icon;
    }

    public LegendIcon getIcon() {
        return icon;
    }
}
