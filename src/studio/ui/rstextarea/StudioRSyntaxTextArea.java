package studio.ui.rstextarea;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RUndoManager;

import java.awt.*;

public class StudioRSyntaxTextArea extends RSyntaxTextArea {

    private Gutter gutter = null;
    private Runnable actionsUpdateListener = null;

    public StudioRSyntaxTextArea(String text) {
        super(text);
    }

    @Override
    protected void handleReplaceSelection(String content) {
        //Probably this is a dirty hack, but here is we convert character 160 to space
        //which is pasted by Skype for Business and result in 'char error from kdb
        content = content.replace((char)160,' ');
        super.handleReplaceSelection(content);
    }

    public void setGutter(Gutter gutter) {
        this.gutter = gutter;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (gutter != null) gutter.setLineNumberFont(font);
    }

    public void setActionsUpdateListener(Runnable actionUpdateListener) {
        this.actionsUpdateListener = actionUpdateListener;
    }

    @Override
    protected RUndoManager createUndoManager() {
        return new StudioRUndoManager(this);
    }

    private class StudioRUndoManager extends RUndoManager {

        public StudioRUndoManager(RTextArea textArea) {
            super(textArea);
        }

        //It turns out that in the updates from the RSTA.getDocument() DocumentListener, canUndo() and canRedo() returns previous state
        @Override
        public void updateActions() {
            super.updateActions();
            if (actionsUpdateListener != null) actionsUpdateListener.run();
        }
    }
}
