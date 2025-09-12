package studio.ui.rstextarea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.ui.EditorPane;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.stream.Stream;

public class MultiAction extends EditorPaneAction {

    private final EditorPaneAction[] actions;

    private final static Logger log = LogManager.getLogger();

    public static String newAction(ActionMap actionMap, String... names) {
        EditorPaneAction[] actions = Stream.of(names)
                                .map(n -> (EditorPaneAction) actionMap.get(n))
                                .toArray(EditorPaneAction[]::new);

        MultiAction action = new MultiAction(actions);
        actionMap.put(action.getName(), action);
        return action.getName();
    }

    public MultiAction(EditorPaneAction... actions) {
        super(getNameFromActions(actions));
        this.actions = actions;
    }


    @Override
    protected boolean actionPerformed(ActionEvent e, EditorPane pane) {
        RuntimeException exception = null;
        boolean complete = false;
        for(EditorPaneAction action: actions) {
            try {
                complete = action.actionPerformed(e, pane);
                if (complete) break;
            } catch (RuntimeException exc) {
                log.error("RuntimeException during action execution", exc);
                exception = exc;
            }
        }

        if (exception != null) throw exception;
        return complete;
    }

    private static String getNameFromActions(EditorPaneAction... actions) {
        StringBuilder builder = new StringBuilder("multiAction");
        for (EditorPaneAction action: actions) {
            builder.append('.');
            builder.append(action.getName());
        }
        return builder.toString();
    }
}
