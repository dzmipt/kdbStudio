package studio.ui.chart.event;

import studio.ui.chart.Line;

import java.util.EventObject;

public class LineSelectionEvent extends EventObject {

    private final Line line;

    public LineSelectionEvent(Object source, Line line) {
        super(source);
        this.line = line;
    }

    public Line getLine() {
        return line;
    }
}
