package studio.ui.action.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import studio.ui.StudioIcon;
import studio.ui.Util;
import studio.ui.action.BaseAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.TreeMap;

public class ActionConfig {
    private final String id;
    private final String text;
    private String iconName = null;
    private String description = null;

    private int macMnemonic = -1;
    private int otherMnemonic = -1;

    private KeyStroke macKeyStroke = null;
    private KeyStroke otherKeyStroke = null;

    private boolean toggle = false;

    private static final String TEXT = "text";
    private static final String ICON = "icon";
    private static final String DESCRIPTION = "description";
    private static final String KEY = "key";
    private static final String STROKE = "stroke";
    public static final String MAC = "mac";
    public static final String OTHER = "other";
    private static final String TOGGLE = "toggle";

    private static final Map<String, Integer> MODIFIERS = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        MODIFIERS.put("ctrl", InputEvent.CTRL_DOWN_MASK);
        MODIFIERS.put("control", InputEvent.CTRL_DOWN_MASK);
        MODIFIERS.put("shift", InputEvent.SHIFT_DOWN_MASK);
        MODIFIERS.put("alt", InputEvent.ALT_DOWN_MASK);
        MODIFIERS.put("altGraph", InputEvent.ALT_GRAPH_DOWN_MASK);
        MODIFIERS.put("meta", InputEvent.META_DOWN_MASK);
    }
    private static final String MENU = "menu";

    public static ActionConfig parse(String name, JsonObject json) {
        ActionConfig config = new ActionConfig(name, json.get(TEXT).getAsString());
        if (json.has(ICON)) config.setIconName(json.get(ICON).getAsString());
        if (json.has(DESCRIPTION)) config.setDescription(json.get(DESCRIPTION).getAsString());
        if (json.has(KEY)) {
            JsonElement j = json.get(KEY);
            if (j.isJsonObject()) {
                JsonObject jsonMnemonic = j.getAsJsonObject();
                config.setMacMnemonic(getMnemonic(jsonMnemonic.get(MAC).getAsString()));
                config.setOtherMnemonic(getMnemonic(jsonMnemonic.get(OTHER).getAsString()));
            } else {
                int mnemonic = getMnemonic(j.getAsString());
                config.setMacMnemonic(mnemonic);
                config.setOtherMnemonic(mnemonic);
            }
        }
        if (json.has(STROKE)) {
            JsonElement j = json.get(STROKE);
            if (j.isJsonObject()) {
                JsonObject jsonKeyStroke = j.getAsJsonObject();
                config.setMacKeyStroke(getKeyStroke(true, jsonKeyStroke.get(MAC).getAsString()));
                config.setOtherKeyStroke(getKeyStroke(false, jsonKeyStroke.get(OTHER).getAsString()));
            } else {
                config.setMacKeyStroke(getKeyStroke(true, j.getAsString()));
                config.setOtherKeyStroke(getKeyStroke(false, j.getAsString()));
            }
        }
        if (json.has(TOGGLE)) config.setToggle(json.get(TOGGLE).getAsBoolean());
        return config;
    }

    public static int getMnemonic(String value) {
        try {
            value = value.trim().toUpperCase().replace(' ', '_');
            return AWTKeyStroke.getAWTKeyStroke(value).getKeyCode();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Error parsing mnemonic " + value, e);
        }
    }


    public static KeyStroke getKeyStroke(boolean isMac, String value) {
        try {
            int modifiers = 0;
            StringBuilder keyMnemonic = new StringBuilder();
            for (String word : value.split("\\s+")) {
                if (word.equalsIgnoreCase(MENU)) {
                    if (isMac) modifiers |= KeyEvent.META_DOWN_MASK;
                    else modifiers |= KeyEvent.CTRL_DOWN_MASK;
                } else {
                    Integer modifier = MODIFIERS.get(word);
                    if (modifier != null) modifiers |= modifier;
                    else {
                        if (keyMnemonic.length() > 0) keyMnemonic.append('_');
                        keyMnemonic.append(word);
                    }
                }
            }
            return KeyStroke.getKeyStroke(getMnemonic(keyMnemonic.toString()), modifiers);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(String.format("Error parsing keyStroke %s; isMac=%b", value, isMac), e);
        }
    }

    public <S> BaseAction<S> apply(BaseAction<S> action) {
        action.putValue(Action.NAME, getText());
        action.putValue(Action.SMALL_ICON, getIcon());
        action.putValue(Action.SHORT_DESCRIPTION, description);
        action.putValue(Action.MNEMONIC_KEY, getMnemonic());
        action.putValue(Action.ACCELERATOR_KEY, getKeyStroke());
        if (isToggle()) action.setSelected(false);
        return action;
    }

    private ActionConfig(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getIconName() {
        return iconName;
    }

    public Icon getIcon() {
        if (iconName == null) return Util.BLANK_ICON;
        if (iconName.equalsIgnoreCase("null")) return null;
        return StudioIcon.getIcon(iconName);
    }

    public String getDescription() {
        return description;
    }

    public int getMnemonic(boolean isMacOS) {
        return  isMacOS ? macMnemonic : otherMnemonic;
    }

    public int getMnemonic() {
        return getMnemonic(Util.MAC_OS_X);
    }

    public KeyStroke getKeyStroke(boolean isMacOS) {
        return isMacOS ? macKeyStroke : otherKeyStroke;
    }

    public KeyStroke getKeyStroke() {
        return getKeyStroke(Util.MAC_OS_X);
    }

    public boolean isToggle() {
        return toggle;
    }

    private void setIconName(String iconName) {
        this.iconName = iconName;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    private void setMacMnemonic(int macMnemonic) {
        this.macMnemonic = macMnemonic;
    }

    private void setOtherMnemonic(int otherMnemonic) {
        this.otherMnemonic = otherMnemonic;
    }

    private void setMacKeyStroke(KeyStroke macKeyStroke) {
        this.macKeyStroke = macKeyStroke;
    }

    private void setOtherKeyStroke(KeyStroke otherKeyStroke) {
        this.otherKeyStroke = otherKeyStroke;
    }

    private void setToggle(boolean toggle) {
        this.toggle = toggle;
    }
}
