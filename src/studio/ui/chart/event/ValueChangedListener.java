package studio.ui.chart.event;

import java.util.EventListener;

public interface ValueChangedListener extends EventListener {

    void valueChanged(ValueChangedEvent event);
}
