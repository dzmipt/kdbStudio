package studio.ui.rstextarea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Segment;
import java.awt.event.ActionEvent;

public class DeleteWordAction extends RecordableTextAction {

    public static final String deleteNextWordAction = "kdbStudio.deleteNextWordAction";
    public static final String deletePreviousWordAction = "kdbStudio.deletePreviousWordAction";

    private final boolean forward;
    private final static Logger log = LogManager.getLogger();


    public static class Next extends DeleteWordAction {
        public Next() {super(true);}
    }

    public static class Previous extends DeleteWordAction {
        public Previous() {super(false);}
    }

    protected DeleteWordAction(boolean forward) {
        super(forward ? deleteNextWordAction : deletePreviousWordAction);
        this.forward = forward;
    }

    @Override
    public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
        if (!textArea.isEditable() || !textArea.isEnabled()) {
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            return;
        }
        try {
            int start = textArea.getSelectionStart();
            int end = textArea.getSelectionEnd();

            if (start == end) {
                if (forward) {
                    int endOffset = textArea.getLineEndOffsetOfCurrentLine();

                    // the line includes '\n' for all except the last line
                    if (endOffset < textArea.getDocument().getLength() ) endOffset--;

                    Segment segment = new Segment();
                    textArea.getDocument().getText(start, endOffset-start, segment);
                    end = start + getEndOffsetForRemoval(segment) - segment.getBeginIndex();
                } else {
                    int startOffset = textArea.getLineStartOffsetOfCurrentLine();
                    Segment segment = new Segment();
                    textArea.getDocument().getText(startOffset, end-startOffset, segment);
                    start = startOffset + getStartOffsetForRemoval(segment) - segment.getBeginIndex();
                }
            }

            if (start == end) {
                if (!forward && start>0) start--;
                if (forward && end<textArea.getDocument().getLength()) end++;
            }

            if (start < end) {
                textArea.getDocument().remove(start, end - start);
            } else {
                UIManager.getLookAndFeel().provideErrorFeedback(textArea);
            }
        } catch (BadLocationException ex) {
            log.error("Unexpected error", ex);
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
        }
    }

    @Override
    public String getMacroID() {
        return getName();
    }


    protected static int getStartOffsetForRemoval(Segment segment) {
        char cur = segment.last();
        while (Character.isWhitespace(cur) ) cur = segment.previous();

        if (Character.isLetterOrDigit(cur)) {
            while (cur != Segment.DONE && Character.isLetterOrDigit(cur) ) cur = segment.previous();
        } else {
            while (cur != Segment.DONE
                            && !Character.isWhitespace(cur)
                            && !Character.isLetterOrDigit(cur) ) cur = segment.previous();
        }


        if (cur != Segment.DONE) segment.next();

        return segment.getIndex();
    }

    protected static int getEndOffsetForRemoval(Segment segment) {
        char cur = segment.first();

        if (Character.isLetterOrDigit(cur)) {
            while (cur != Segment.DONE && Character.isLetterOrDigit(cur) ) cur = segment.next();
        } else {
            while (cur != Segment.DONE
                    && !Character.isWhitespace(cur)
                    && !Character.isLetterOrDigit(cur) ) cur = segment.next();
        }

        while (Character.isWhitespace(cur) ) cur = segment.next();

        return segment.getIndex();
    }
}
