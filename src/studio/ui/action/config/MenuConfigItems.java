package studio.ui.action.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import studio.ui.action.MenuFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuConfigItems {

    private final List<MenuConfig> items = new ArrayList<>();

    public static MenuConfigItems parse(JsonArray array) {
        MenuConfigItems items = new MenuConfigItems();

        for (JsonElement element: array.asList()) {
            items.items.add(MenuConfig.parse(element));
        }

        return items;
    }

    private MenuConfigItems() {
    }

    public List<MenuConfig> getList() {
        return Collections.unmodifiableList(items);
    }

    public JMenu init(JMenu menu, MenuFactory factory) {
        for (MenuConfig menuConfig: items) {
            menuConfig.init(menu, factory);
        }
        return menu;
    }
}
