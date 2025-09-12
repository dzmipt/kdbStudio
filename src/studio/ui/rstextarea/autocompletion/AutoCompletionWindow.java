package studio.ui.rstextarea.autocompletion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import studio.kdb.Server;
import studio.kdb.query.Schema;
import studio.ui.DocumentChangeListener;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class AutoCompletionWindow extends JWindow implements CaretListener, DocumentChangeListener {

    private final static Action NO_ACTION = new NoAction();
    private final RSyntaxTextArea textArea;
    private final JList<String> list;

    private final static Map<Server, Schema> cache = new ConcurrentHashMap<>();

    private final static List<KeyStroke> proxyKeyStrokes = List.of(
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
            KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0),
            KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0)
    );

    private final Map<KeyStroke, Object> origTextAreaInputMap = new HashMap<>();

    private final static Logger log = LogManager.getLogger();

    public AutoCompletionWindow(RSyntaxTextArea textArea) {
        super(SwingUtilities.getWindowAncestor(textArea));
        setFocusableWindowState(false);
        this.textArea = textArea;

        textArea.addCaretListener(this);
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hideWindow();
            }
        });
        textArea.getDocument().addDocumentListener(this);

        String[] values = Stream.of(textArea.getActionMap().allKeys()).map(Object::toString).toArray(String[]::new);
        list = new JList<>(values);
        list.setFont(textArea.getFont());

        JScrollPane scrollPane = new JScrollPane(list,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        setContentPane(scrollPane);

        setVisible(false);
        setSize(400,300);
    }

    public void keyUp() {

    }
    public void keyDown() {

    }

    public void keyPageUp() {

    }

    public void keyPageDown() {

    }

    public boolean showWindow() {
        if (isVisible()) return false;

        origTextAreaInputMap.clear();
        ActionMap listActionMap = list.getActionMap();
        InputMap listInputMap = list.getInputMap();
        ActionMap textAreaActionMap = textArea.getActionMap();
        InputMap textAreaInputMap = textArea.getInputMap();
        for (KeyStroke ks: proxyKeyStrokes) {
            origTextAreaInputMap.put(ks, textAreaInputMap.get(ks));

            Object action = listInputMap.get(ks);
            String newActionKey;
            Action newAction;
            if (action == null) {
                newActionKey = "JList " + ks.toString();
                newAction = NO_ACTION;
            } else {
                newActionKey = "JList " + action.toString();
                newAction = new ProxyListAction(listActionMap.get(action));
            }
            textAreaActionMap.put(newActionKey, newAction);
            textAreaInputMap.put(ks, newActionKey);

        }

        updateLocation();
        setVisible(true);
        return true;
    }

    public boolean hideWindow() {
        if (! isVisible()) return false;

        ActionMap textAreaActionMap = textArea.getActionMap();
        InputMap textAreaInputMap = textArea.getInputMap();
        for (KeyStroke ks: proxyKeyStrokes) {
            Object action = textAreaInputMap.get(ks);
            if (action != null) {
                textAreaActionMap.remove(action);
            }
            textAreaInputMap.put(ks, origTextAreaInputMap.get(ks));
        }

        setVisible(false);
        return true;
    }

    private void updateLocation() {
        try {
            Rectangle2D r = textArea.modelToView2D(textArea.getCaretPosition());
            Point p = new Point((int)r.getMaxX(), 1 + (int) r.getMaxY());
            SwingUtilities.convertPointToScreen(p, textArea);
            setLocation(p);
        } catch (BadLocationException e) {
            log.error("Unexpected error", e);
        }
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        if (!isVisible()) return;
        updateLocation();
    }

    @Override
    public void documentChanged(DocumentEvent e) {

    }

    private class ProxyListAction extends AbstractAction {

        private final Action proxyAction;
        ProxyListAction(Action proxyAction) {
            this.proxyAction = proxyAction;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            proxyAction.actionPerformed(
                new ActionEvent(list, e.getID(), e.getActionCommand(), e.getWhen(), e.getModifiers())
            );
        }
    }

    private static class NoAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

}
