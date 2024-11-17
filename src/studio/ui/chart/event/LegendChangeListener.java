package studio.ui.chart.event;

import java.util.EventListener;

public interface LegendChangeListener extends EventListener {

    void legendChanged(LegendChangeEvent event);
    void changeAllStrokes(LegendChangeEvent event);
    void changeAllShapes(LegendChangeEvent event);
}
