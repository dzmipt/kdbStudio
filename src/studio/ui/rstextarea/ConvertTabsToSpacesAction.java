package studio.ui.rstextarea;

import studio.ui.EditorPane;

import java.awt.event.ActionEvent;

public class ConvertTabsToSpacesAction extends EditorPaneAction {

    public static final String action = "kdbStudio.ConvertTabsToSpacesAction";

    public ConvertTabsToSpacesAction() {
        super(action);
    }

    @Override
    protected void actionPerformed(ActionEvent e, EditorPane pane) {
        pane.getTextArea().convertTabsToSpaces();
    }
}
