package studio.ui.rstextarea;

import studio.ui.EditorPane;

import java.awt.event.ActionEvent;

public class HideAutoCompletionAction extends EditorPaneAction {

    public static final String action = "kdbStudio.EditorPaneAction";

    public HideAutoCompletionAction() {
        super(action);
    }

    @Override
    protected boolean actionPerformed(ActionEvent e, EditorPane pane) {
        if (pane.getAutoCompletionWindow() == null) return false;
        pane.getAutoCompletionWindow().hideWindow();
        pane.removeAutoCompletionWindow();
        return true;
    }

}
