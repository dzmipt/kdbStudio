package studio.ui.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import studio.ui.Util;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ActionConfigTest {

    private final static Logger log = LogManager.getLogger();
    @Test
    public void getMnemonicTest() {
        assertEquals(KeyEvent.VK_B, ActionConfig.getMnemonic("B"));
        assertEquals(KeyEvent.VK_B, ActionConfig.getMnemonic("b"));
        assertEquals(KeyEvent.VK_B, ActionConfig.getMnemonic("  b  "));

        assertEquals(KeyEvent.VK_BACK_SLASH, ActionConfig.getMnemonic("back slash"));
        assertEquals(KeyEvent.VK_OPEN_BRACKET, ActionConfig.getMnemonic("open bracket"));
        assertEquals(KeyEvent.VK_F5, ActionConfig.getMnemonic("f5"));

        assertThrowsExactly(IllegalArgumentException.class, () -> ActionConfig.getMnemonic("FF2"));
    }

    @Test
    public void getKeyStrokeTest() {
        assertEquals(KeyStroke.getKeyStroke("ctrl X"), ActionConfig.getKeyStroke(true, "ctrl x"));
        assertEquals(KeyStroke.getKeyStroke("control X"), ActionConfig.getKeyStroke(true, "ctrl x"));
        assertEquals(KeyStroke.getKeyStroke("meta X"), ActionConfig.getKeyStroke(true, "meta x"));
        assertEquals(KeyStroke.getKeyStroke("alt X"), ActionConfig.getKeyStroke(true, "alt x"));
        assertEquals(KeyStroke.getKeyStroke("altGraph X"), ActionConfig.getKeyStroke(true, "altgraph x"));

        assertEquals(KeyStroke.getKeyStroke("shift X"), ActionConfig.getKeyStroke(true, "shift x"));
        assertEquals(KeyStroke.getKeyStroke("shift X"), ActionConfig.getKeyStroke(false, "shift x"));

        assertEquals(KeyStroke.getKeyStroke("ctrl OPEN_BRACKET"), ActionConfig.getKeyStroke(true, "ctrl open bracket"));
        assertEquals(KeyStroke.getKeyStroke("ctrl OPEN_BRACKET"), ActionConfig.getKeyStroke(false, "ctrl open bracket"));

        assertEquals(KeyStroke.getKeyStroke("meta shift RIGHT_PARENTHESIS"), ActionConfig.getKeyStroke(true, "menu shift right  parenthesis     "));
        assertEquals(KeyStroke.getKeyStroke("ctrl shift RIGHT_PARENTHESIS"), ActionConfig.getKeyStroke(false, "menu shift right  parenthesis     "));


        assertThrowsExactly(IllegalArgumentException.class, () -> ActionConfig.getKeyStroke(true, "ctr x"));
        assertThrowsExactly(IllegalArgumentException.class, () -> ActionConfig.getKeyStroke(true, "alt ff"));
    }

    @Test
    public void parseActionTest() {
        List<ActionConfig> actions = new ActionConfigParser("testActionConfig.json").getActions();
        assertEquals(4, actions.size());

        ActionConfig serverAction = actions.get(0);
        assertEquals("serverBack", serverAction.getId());
        assertEquals("Back", serverAction.getText());
        assertEquals("left", serverAction.getIconName());
        assertEquals("Previous server", serverAction.getDescription());
        assertEquals(KeyEvent.VK_B, serverAction.getMnemonic());
        assertEquals(KeyStroke.getKeyStroke("meta shift OPEN_BRACKET"), serverAction.getKeyStroke(true));
        assertEquals(KeyStroke.getKeyStroke("ctrl shift OPEN_BRACKET"), serverAction.getKeyStroke(false));

        ActionConfig osTestAction = actions.get(1);
        assertEquals("osTestAction", osTestAction.getId());
        assertEquals("test", osTestAction.getText());
        assertEquals("copy", osTestAction.getIconName());
        assertEquals("Test Description", osTestAction.getDescription());
        assertEquals(KeyEvent.VK_M, osTestAction.getMnemonic(true));
        assertEquals(KeyEvent.VK_W, osTestAction.getMnemonic(false));
        assertEquals(KeyStroke.getKeyStroke("meta TAB"), osTestAction.getKeyStroke(true));
        assertEquals(KeyStroke.getKeyStroke("ctrl shift TAB"), osTestAction.getKeyStroke(false));

        ActionConfig emptyAction = actions.get(2);
        assertEquals("emptyAction", emptyAction.getId());
        assertEquals("", emptyAction.getText());
        assertNull(emptyAction.getIconName());
        assertNull(emptyAction.getDescription());
        assertEquals(-1, emptyAction.getMnemonic());
        assertNull(emptyAction.getKeyStroke());
        assertEquals(Util.BLANK_ICON, emptyAction.getIcon());

        ActionConfig nullIconAction = actions.get(3);
        assertNull(nullIconAction.getIcon());
    }

    @Test
    public void allActionDefinedTest() {
        Set<String> ids = ActionRegistry.getActionIDs();
        Set<String> actionIds = Actions.studioWindowActions.keySet();

        Set<String> definedActionConfigs = new HashSet<>(ids);
        definedActionConfigs.removeAll(actionIds);
        if (definedActionConfigs.size()>0) {
            log.error("The following action configs from the config.json are not bind with actions {} ", definedActionConfigs);
        }

        Set<String> bindActions = new HashSet<>(actionIds);
        bindActions.removeAll(ids);
        if (bindActions.size()>0) {
            log.error("The following bindings are not definned in the config.json {} ", bindActions);
        }

        assertTrue(ids.equals(actionIds));
    }

}
