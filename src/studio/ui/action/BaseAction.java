package studio.ui.action;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class BaseAction<S> extends AbstractAction {

    public final static String TOGGLE_ICON = "ToggleIcon";

    private S source = null;
    private Consumer<S> baseAction = null;

    public void setSource(S source) {
        this.source = source;
    }

    public void setBaseAction(Consumer<S> baseAction) {
        this.baseAction = baseAction;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (baseAction == null) throw new IllegalStateException("baseAction is null");
        if (source == null) throw new IllegalStateException("source is null");
        baseAction.accept(source);
    }

    public boolean isSelected() {
        return getValue(Action.SELECTED_KEY) == Boolean.TRUE;
    }

    public void click() {
        if (getValue(TOGGLE_ICON) != null) {
            putValue(Action.SELECTED_KEY, !isSelected());
        }
        actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click"));
    }

    public void setSelected(boolean value) {
        putValue(SELECTED_KEY, value);
    }


}
