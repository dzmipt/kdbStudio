package studio.ui.rstextarea.autocompletion;

import studio.ui.EditorPane;
import studio.ui.rstextarea.EditorPaneAction;

import java.awt.event.ActionEvent;

public abstract class AutoCompletionWindowAction extends EditorPaneAction {

    protected AutoCompletionWindowAction(String name) {
        super(name);
    }

    @Override
    protected boolean actionPerformed(ActionEvent e, EditorPane pane) {
        AutoCompletionWindow autoCompletionWindow = pane.getAutoCompletionWindow();
        if (autoCompletionWindow == null) return false;
        action(autoCompletionWindow);
        return true;
    }

    abstract protected void action(AutoCompletionWindow autoCompletionWindow);


    public static AutoCompletionWindowAction keyUpAction() {
        return new AutoCompletionWindowAction("autoCompletionWindow.up") {
            @Override
            protected void action(AutoCompletionWindow autoCompletionWindow) {
                autoCompletionWindow.keyUp();
            }
        };
    }

    public static AutoCompletionWindowAction keyDownAction() {
        return new AutoCompletionWindowAction("autoCompletionWindow.down") {
            @Override
            protected void action(AutoCompletionWindow autoCompletionWindow) {
                autoCompletionWindow.keyDown();
            }
        };
    }

    public static AutoCompletionWindowAction keyPageUpAction() {
        return new AutoCompletionWindowAction("autoCompletionWindow.pageUp") {
            @Override
            protected void action(AutoCompletionWindow autoCompletionWindow) {
                autoCompletionWindow.keyPageUp();
            }
        };
    }

    public static AutoCompletionWindowAction keyPageDownAction() {
        return new AutoCompletionWindowAction("autoCompletionWindow.pageDown") {
            @Override
            protected void action(AutoCompletionWindow autoCompletionWindow) {
                autoCompletionWindow.keyPageDown();
            }
        };
    }

}
