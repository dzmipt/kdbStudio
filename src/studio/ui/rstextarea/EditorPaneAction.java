package studio.ui.rstextarea;

import studio.ui.EditorPane;

import javax.swing.*;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;

abstract public class EditorPaneAction extends TextAction {

    public EditorPaneAction(String name) {
        super(name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Container container = getTextComponent(e);
        while (container != null && ! (container instanceof EditorPane)) {
            container = container.getParent();
        }
        if (container == null) return;

        actionPerformed(e, (EditorPane) container) ;
    }

    public String getName() {
        return getValue(Action.NAME).toString();
    }

    /**
     *
     * @param e
     * @param pane
     * @return true if the chain for MultiActoin should be stopped
     */
    abstract protected boolean actionPerformed(ActionEvent e, EditorPane pane);
}
