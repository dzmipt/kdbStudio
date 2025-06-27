package studio.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class UserAction extends AbstractAction {

    public final static String TOGGLE_ICON = "ToggleIcon";

    public UserAction(String text,
                      Icon icon,
                      String desc,
                      Integer mnemonic,
                      KeyStroke key) {
        super(text,icon);
        putValue(SHORT_DESCRIPTION,desc);
        putValue(MNEMONIC_KEY,mnemonic);
        putValue(ACCELERATOR_KEY,key);
    }

    public String getText() {
        return (String)getValue(NAME);
    }

    public void setText(String text) {
        putValue(NAME, text);
    }

    public void setIcon(Icon icon) {
        putValue(SMALL_ICON, icon);
    }

    public KeyStroke getKeyStroke() {
        return (KeyStroke)getValue(ACCELERATOR_KEY);
    }

    public String getKeyString() {
        StringBuilder builder = new StringBuilder(getText());
        KeyStroke keyStroke = getKeyStroke();
        if (keyStroke != null) builder.append(" - ").append(keyStroke);
        return builder.toString();
    }

    public UserAction toggleButton(Icon icon) {
        putValue(TOGGLE_ICON, icon);
        return this;
    }

    public boolean isSelected() {
        return getValue(Action.SELECTED_KEY) == Boolean.TRUE;
    }

    public void click() {
        if (getValue(TOGGLE_ICON) != null) {
            putValue(Action.SELECTED_KEY, !isSelected());
        }
        actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click"));
    }

    public UserAction addActionToComponent(JComponent component) {
        String key = getKeyString();
        component.getActionMap().put(key, this);
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(getKeyStroke(), key);
        return this;
    }

    public static UserAction create(String text, Runnable action) {
        return new UserAction(text, null, "", 0, null) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        };
    }

    public static UserAction create(String text, Icon icon,
                               String desc, int mnemonic,
                               KeyStroke key, ActionListener listener) {
        return new UserAction(text, icon, desc, mnemonic, key) {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }
        };
    }

    public void setSelected(boolean value) {
        putValue(SELECTED_KEY, value);
    }

    public static UserAction create(String text,
                             String desc, int mnemonic,
                             KeyStroke key, ActionListener listener) {
        return create(text, null, desc, mnemonic, key, listener);
    }

    public static UserAction create(String text,
                             String desc, int mnemonic,
                             ActionListener listener) {
        return create(text, null, desc, mnemonic, null, listener);
    }

    public static UserAction create(String text, Icon icon, ActionListener listener) {
        return create(text, icon, null, 0, null, listener);
    }

    public static UserAction create(String text, ActionListener listener) {
        return create(text, null, null, 0, null, listener);
    }
}
