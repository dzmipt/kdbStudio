package studio.ui.rstextarea;

import studio.ui.EditorPane;

import java.awt.event.ActionEvent;

public class ShowAutoCompletionAction extends EditorPaneAction {

    public static final String action = "kdbStudio.ShowAutoCompletionWindow";

    public ShowAutoCompletionAction() {
        super(action);
    }

    @Override
    protected boolean actionPerformed(ActionEvent e, EditorPane pane) {
        if (pane.getAutoCompletionWindow() != null) return false;
        pane.newAutoCompletionWindow().showWindow();
        return true;
    }

}
