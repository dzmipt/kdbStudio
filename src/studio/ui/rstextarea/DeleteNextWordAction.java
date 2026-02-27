package studio.ui.rstextarea;

import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.text.CharacterIterator;

public class DeleteNextWordAction extends RecordableTextAction {

    public static final String deleteNextWordAction = "kdbStudio.deleteNextWordAction";

    public DeleteNextWordAction() {
        super(deleteNextWordAction);
    }

    @Override
    public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
        if (!textArea.isEditable() || !textArea.isEnabled()) {
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            return;
        }
        try {
            int start = textArea.getSelectionStart();
            int end = getEndOffsetForRemoval(textArea, start);
            if (end > start) {
                textArea.getDocument().remove(start, end - start);
            } else {
                UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            }
        } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
        }
    }

    private int getEndOffsetForRemoval(RTextArea textArea, int offset) throws BadLocationException {
        int endSelection = textArea.getSelectionEnd();
        if (endSelection < offset) {
            return endSelection;
        }
        int endOffset = textArea.getLineEndOffsetOfCurrentLine();
        if (endOffset <= offset) {
            return offset;
        }

        Segment segment = new Segment();
        textArea.getDocument().getText(offset, endOffset-offset, segment);

        char ch = segment.first();
        if (Character.isWhitespace(ch)) {
            do {
                ch = segment.next();
            } while ( ch != CharacterIterator.DONE && Character.isWhitespace(ch));
        } else if (Character.isLetterOrDigit(ch)) {
            do {
                ch = segment.next();
            } while ( ch != CharacterIterator.DONE && Character.isLetterOrDigit(ch));
        } else {
            segment.next();
        }
        return offset + segment.getIndex() - segment.getBeginIndex();
    }

    @Override
    public String getMacroID() {
        return getName();
    }
}
