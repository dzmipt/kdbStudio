package studio.ui.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import studio.ui.Util;
import studio.ui.action.config.ActionConfig;
import studio.ui.action.config.ConfigParser;
import studio.ui.action.config.MenuConfig;

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
        List<ActionConfig> actions = new ConfigParser("testActionConfig.json").getActions();
        assertEquals(6, actions.size());

        ActionConfig serverAction = actions.get(0);
        assertEquals("serverBack", serverAction.getId());
        assertEquals("Back", serverAction.getText());
        assertEquals("left", serverAction.getIconName());
        assertEquals("Previous server", serverAction.getDescription());
        assertEquals(KeyEvent.VK_B, serverAction.getMnemonic());
        assertEquals(KeyStroke.getKeyStroke("meta shift OPEN_BRACKET"), serverAction.getKeyStroke(true));
        assertEquals(KeyStroke.getKeyStroke("ctrl shift OPEN_BRACKET"), serverAction.getKeyStroke(false));
        assertFalse(serverAction.isToggle());

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

        assertTrue(actions.get(4).isToggle());
        assertFalse(actions.get(5).isToggle());
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

    @Test
    public void menuBarConfigTest() {
        List<MenuConfig> menuBarItems = new ConfigParser("testActionConfig.json").getMenuConfig().getList();
        assertEquals(2, menuBarItems.size());

        MenuConfig.Menu menu = (MenuConfig.Menu) menuBarItems.get(0);
        assertEquals("First Menu", menu.getText());

        List<MenuConfig> items = menu.getItems().getList();
        assertEquals(7, items.size());
        assertEquals("action1", ((MenuConfig.Action)items.get(0)).getId());
        assertInstanceOf(MenuConfig.Separator.class, items.get(1));
        assertEquals("action2", ((MenuConfig.Action)items.get(2)).getId());
        assertEquals("testMark", ((MenuConfig.Marker)items.get(3)).getId());

        MenuConfig.Condition condition = (MenuConfig.Condition) items.get(4);
        assertFalse(condition.isMac());
        List<MenuConfig> items1 = condition.getItems().getList();
        assertEquals(2, items1.size());
        assertEquals("a", ((MenuConfig.Action)items1.get(0)).getId());
        assertInstanceOf(MenuConfig.Separator.class, items1.get(1));

        condition = (MenuConfig.Condition) items.get(5);
        assertTrue(condition.isMac());
        items1 = condition.getItems().getList();
        assertEquals(1, items1.size());
        assertInstanceOf(MenuConfig.Separator.class, items1.get(0));

        MenuConfig.Menu subMenu = (MenuConfig.Menu)items.get(6);
        assertEquals("Sub-menu", subMenu.getText());
        items1 = subMenu.getItems().getList();
        assertEquals(0, items1.size());

        menu = (MenuConfig.Menu) menuBarItems.get(1);
        assertEquals("Empty Menu", menu.getText());
        items1 = menu.getItems().getList();
        assertEquals(0, items1.size());

    }
}
