package studio.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

abstract public class DocumentChangeListener implements DocumentListener {

    abstract public void documentChanged(DocumentEvent e);

    @Override
    public void insertUpdate(DocumentEvent e) {
        documentChanged(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        documentChanged(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        documentChanged(e);
    }
}
