package studio.ui.action.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import studio.ui.Util;
import studio.ui.action.BaseAction;
import studio.ui.action.MenuFactory;

import javax.swing.*;

public abstract class MenuConfig {

    private final static String MARK = "mark";
    private final static String MENU = "menu";
    private final static String OS = "os";
    private final static String ITEMS = "items";
    private final static String MAC = ActionConfig.MAC;
    private final static String OTHER = ActionConfig.OTHER;

    public static MenuConfig parse(JsonElement jsonElement) {
        if (jsonElement.isJsonNull()) return new Separator();
        if (jsonElement.isJsonPrimitive()) return new Action(jsonElement.getAsString());

        JsonObject json = jsonElement.getAsJsonObject();
        if (json.has(MARK)) return new Marker(json.get(MARK).getAsString());

        if (json.has(MENU)) {
            return new Menu(json.get(MENU).getAsString(), MenuConfigItems.parse(json.getAsJsonArray(ITEMS)));
        }

        if (json.has(OS)) {
            String value = json.get(OS).getAsString();
            if (!value.equals(MAC) && !value.equals(OTHER)) throw new IllegalArgumentException("Can't parse os value: " + value);

            boolean isMac = MAC.equals(value);
            return new Condition(isMac, MenuConfigItems.parse(json.getAsJsonArray(ITEMS)));
        }

        throw new IllegalStateException("Can't parse menu config:\n" + jsonElement);
    }

    public abstract JMenu init(JMenu menu, MenuFactory factory);

    public static class Menu extends MenuConfig {
        private final String text;
        private final MenuConfigItems items;
        private Menu(String text, MenuConfigItems items) {
            this.text = text;
            this.items = items;
        }

        @Override
        public JMenu init(JMenu menu, MenuFactory factory) {
            JMenu subMenu = new JMenu(text);
            subMenu.setIcon(Util.BLANK_ICON);
            if (menu != null) menu.add(subMenu);
            return items.init(subMenu, factory);
        }

        public String getText() {
            return text;
        }

        public MenuConfigItems getItems() {
            return items;
        }
    }

    public static class Condition extends MenuConfig {
        private final boolean isMac;
        private final MenuConfigItems items;
        private Condition(boolean isMac, MenuConfigItems items) {
            this.isMac = isMac;
            this.items = items;
        }

        @Override
        public JMenu init(JMenu menu, MenuFactory factory) {
            if (isMac == Util.MAC_OS_X) items.init(menu, factory);
            return menu;
        }

        public boolean isMac() {
            return isMac;
        }

        public MenuConfigItems getItems() {
            return items;
        }
    }

    public static class Marker extends MenuConfig {
        private final String id;
        private Marker(String id) {
            this.id = id;
        }

        @Override
        public JMenu init(JMenu menu, MenuFactory factory) {
            factory.addMarker(menu, id, menu.getMenuComponentCount());
            return menu;
        }

        public String getId() {
            return id;
        }
    }

    public static class Action extends MenuConfig {
        private final String id;
        private Action(String id) {
            this.id = id;
        }

        @Override
        public JMenu init(JMenu menu, MenuFactory factory) {
            BaseAction.addToMenu(factory.getActionMap().get(id), menu);
            return menu;
        }

        public String getId() {
            return id;
        }
    }

    public static class Separator extends MenuConfig {
        @Override
        public JMenu init(JMenu menu, MenuFactory factory) {
            menu.addSeparator();
            return menu;
        }
    }


}
