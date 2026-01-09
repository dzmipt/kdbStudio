package studio.ui.action;

import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public class BaseAction<S> extends AbstractAction {

    public final static String TOGGLE_ICON = "ToggleIcon";

    private S source = null;
    private Consumer<S> baseAction = null;

    public static <S> BaseAction<S> build(String text, Icon icon, S source, Consumer<S> action) {
        BaseAction<S> newAction = new BaseAction<>();
        newAction.putValue(Action.NAME, text);
        newAction.putValue(Action.SMALL_ICON, icon);
        newAction.setSource(source);
        newAction.setBaseAction(action);
        return newAction;
    }

    public static <S> BaseAction<S> build(String text, S source, Consumer<S> action) {
        return build(text, Util.BLANK_ICON, source, action);
    }

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

    public boolean isToggle() {
        return getValue(Action.SELECTED_KEY) != null;
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


    private static Component getMenuItem(BaseAction<?> action) {
        if (action == null) return new JPopupMenu.Separator();

        if (action.isToggle()) return new JCheckBoxMenuItem(action);

        return new JMenuItem(action);
    }

    public static void addToMenu(BaseAction<?> action, JMenu menu) {
        menu.add(getMenuItem(action));
    }

    public static void insertToMenu(BaseAction<?> action, JMenu menu, int pos) {
        if (pos == menu.getMenuComponentCount()) addToMenu(action, menu);
        else {
            if (action == null) menu.insertSeparator(pos);
            else menu.insert( (JMenuItem) getMenuItem(action), pos);
        }
    }

}
