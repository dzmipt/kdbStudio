package studio.ui.rstextarea;

import java.awt.event.ActionEvent;
import java.text.BreakIterator;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Utilities;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;

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
            int end = Utilities.getNextWord(textArea, start);
            if (end > start) {
                textArea.getDocument().remove(start, end - start);
            }
        } catch (BadLocationException ex) {
            try {
                if ("No more words".equals(ex.getMessage())) {
                    Document document = textArea.getDocument();
                    int caretPosition = textArea.getCaretPosition();
                    Element map = document.getDefaultRootElement();
                    int currentLineNum = map.getElementIndex(caretPosition);
                    Element currentLineElement = map.getElement(currentLineNum);
                    int currentLineEnd = currentLineElement.getEndOffset() - 1;
                    if (caretPosition < currentLineEnd) {
                        document.remove(caretPosition, currentLineEnd - caretPosition);
                        return;
                    }
                }
            } catch (Exception ignore) {
            }
            UIManager.getLookAndFeel().provideErrorFeedback(textArea);
        }
    }

    @Override
    public String getMacroID() {
        return getName();
    }
}
