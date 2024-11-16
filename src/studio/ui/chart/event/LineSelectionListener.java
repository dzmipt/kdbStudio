package studio.ui.chart.event;

import java.util.EventListener;

public interface LineSelectionListener extends EventListener {

    void lineSelected(LineSelectionEvent event);
}
