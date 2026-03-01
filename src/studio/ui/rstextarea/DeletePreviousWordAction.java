package studio.ui.rstextarea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import java.awt.event.ActionEvent;
import java.text.CharacterIterator;

public class DeletePreviousWordAction extends RecordableTextAction {

    public static final String deletePreviousWordAction = "kdbStudio.DeletePreviousWordAction";
    private final static Logger log = LogManager.getLogger();

    public DeletePreviousWordAction() {
        super(deletePreviousWordAction);
    }

    @Override
    public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
        if (!textArea.isEditable() || !textArea.isEnabled()) {
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            return;
        }
        try {
            int end = textArea.getSelectionEnd();
            int start = getStartOffsetRemoval(textArea, end);

            if (end > start) {
                textArea.getDocument().remove(start, end - start);
            } else {
                UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            }
        } catch (BadLocationException ex) {
            log.error("Unexpected error", ex);
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
        }
    }

    private int getStartOffsetRemoval(RTextArea textArea, int offset) throws BadLocationException {
        int startSelection = textArea.getSelectionStart();
        if (startSelection < offset) {
            return startSelection;
        }
        int startOffset = textArea.getLineStartOffsetOfCurrentLine();
        if (startOffset == offset) {
            return Math.max(0, offset-1);
        }

        Segment segment = new Segment();
        textArea.getDocument().getText(startOffset, offset-startOffset, segment);

        char ch = segment.last();
        if (Character.isWhitespace(ch)) {
            do {
                ch = segment.previous();
            } while ( ch != CharacterIterator.DONE && Character.isWhitespace(ch));
        } else if (Character.isLetterOrDigit(ch)) {
            do {
                ch = segment.previous();
            } while ( ch != CharacterIterator.DONE && Character.isLetterOrDigit(ch));
        } else {
            segment.previous();
        }
        return offset + segment.getIndex() - segment.getEndIndex();
    }

    @Override
    public String getMacroID() {
        return getName();
    }
}
