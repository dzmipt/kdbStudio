package studio.ui.iminspector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.ui.settings.FocusDecorator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class KeyStrokeScanner extends JButton implements FocusListener, KeyEventDispatcher {

    private KeyStroke lastPressedKeyStroke;
    private KeyStroke lastTypedKeyStroke;

    private ChangeListener listener = null;
    private final static Logger log = LogManager.getLogger();

    public KeyStrokeScanner() {
        super("Enter shortcut");
        FocusDecorator.add(this);
        addFocusListener(this);
        addActionListener(e -> requestFocusInWindow() );
    }

    public void reset() {
        if (lastTypedKeyStroke == null && lastPressedKeyStroke == null) return;
        lastTypedKeyStroke = null;
        lastPressedKeyStroke = null;
        fireEvent();
    }

    public void setListener(ChangeListener listener) {
        this.listener = listener;
    }


    public KeyStroke getPressedKeyStroke() {
        return lastPressedKeyStroke;
    }

    public KeyStroke getTypedKeyStroke() {
        return lastTypedKeyStroke;
    }

    private void fireEvent() {
        if (listener == null) return;

        listener.stateChanged(new ChangeEvent(this));
    }

    @Override
    public void focusGained(FocusEvent e) {
        lastPressedKeyStroke = null;
        lastTypedKeyStroke = null;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
    }

    @Override
    public void focusLost(FocusEvent e) {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);

        log.debug("id: {}, keyStroke: {}, event: {}", e.getID(), keyStroke, e);

        if (e.getID() == KeyEvent.KEY_PRESSED) {
            if (! Objects.equals(lastPressedKeyStroke, keyStroke)) {
                lastPressedKeyStroke = keyStroke;
                fireEvent();
            }
        } else if (e.getID() == KeyEvent.KEY_TYPED) {
            if (!Objects.equals(lastTypedKeyStroke, keyStroke)) {
                lastTypedKeyStroke = keyStroke;
                fireEvent();
            }
        }

        e.consume();
        return true;
    }
}
