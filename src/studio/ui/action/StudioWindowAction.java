package studio.ui.action;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import studio.ui.EditorTab;
import studio.ui.ResultTab;
import studio.ui.StudioWindow;
import studio.ui.rstextarea.RSTextAreaFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

public interface StudioWindowAction extends Consumer<StudioWindow> {

    @Override
    default void accept(StudioWindow studioWindow) {
        action(studioWindow);
    }

    void action(StudioWindow studioWindow);

    interface EditorAction extends StudioWindowAction {
        void action(EditorTab editor);

        @Override
        default void action(StudioWindow studioWindow) {
            EditorTab editor = studioWindow.getActiveEditor();
            if (editor != null) action(editor);
        }
    }

    interface ResultTabAction extends StudioWindowAction {
        void action(ResultTab tab);

        @Override
        default void action(StudioWindow studioWindow) {
            ResultTab tab = studioWindow.getSelectedResultTab();
            if (tab != null) action(tab);
        }
    }

    interface StaticAction extends StudioWindowAction {
        void action();

        @Override
        default void action(StudioWindow studioWindow) {
            action();
        }
    }

    class RSTAAction implements EditorAction {

        private final Action action;

        public RSTAAction(String name) {
            action = RSTextAreaFactory.getAction(name);
        }

        @Override
        public void action(EditorTab editor) {
            RSyntaxTextArea textArea = editor.getTextArea();
            String command = (String) action.getValue(Action.NAME);
            action.actionPerformed(new ActionEvent(textArea, ActionEvent.ACTION_PERFORMED, command));
        }
    }

}
