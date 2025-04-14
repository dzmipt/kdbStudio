package studio.ui.rstextarea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import java.awt.event.ActionEvent;

public class CommentAction extends TextAction {

    public static final String action = "kdbStudio.CommentAction";

    private final static Logger log = LogManager.getLogger();

    public CommentAction() {
        super(action);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            JTextComponent textComponent = getTextComponent(event);
            if (!(textComponent instanceof RSyntaxTextArea)) return;

            RSyntaxTextArea textArea = (RSyntaxTextArea) textComponent;

            Caret c = textArea.getCaret();
            int dot = c.getDot();
            int mark = c.getMark();
            int line1 = textArea.getLineOfOffset(dot);
            int line2 = textArea.getLineOfOffset(mark);
            int start = Math.min(line1, line2);
            int end = Math.max(line1, line2);

            textArea.beginAtomicEdit();
            try {
                for (int line = start; line <= end; line++) {
                    int startOff = textArea.getLineStartOffset(line);
                    int endOff = textArea.getLineEndOffset(line);

                    String text = textArea.getDocument().getText(startOff, endOff - startOff);
                    int pos = text.indexOf('/');
                    boolean comment = false;
                    if (pos != -1) {
                        comment = text.substring(0, pos).trim().isEmpty();
                    }
                    if (comment) {
                        int len = 1;
                        if (pos + 1 < text.length() && text.charAt(pos + 1) == '/') len = 2;
                        textArea.getDocument().remove(startOff + pos, len);
                    } else {
                        String insertText = text.trim().isEmpty() ? "//" : "/";
                        textArea.getDocument().insertString(startOff, insertText, null);
                    }
                }

                if (dot == mark) {
                    int count = textArea.getLineCount();
                    if (start+1 < count) {
                        int pos = dot - textArea.getLineStartOffset(start);
                        int off = textArea.getLineStartOffset(start+1);
                        int endOff = textArea.getLineEndOffset(start+1);
                        int newDot = Math.min (off+pos, endOff-1);
                        textArea.getCaret().setDot(newDot);
                    }
                }
            } finally {
                textArea.endAtomicEdit();
            }
        } catch (BadLocationException e) {
            log.error("Unexpected exception", e);
        }
    }
}
