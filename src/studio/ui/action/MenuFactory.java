package studio.ui.action;


import studio.ui.action.config.MenuConfig;
import studio.ui.action.config.MenuConfigItems;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class MenuFactory {

    public final static String openMRU_MARK = "openMRU";
    public final static String clone_MARK = "clone";
    public final static String windows_MARK = "windows";

    private final MenuConfigItems items;
    private final StudioActionMap actionMap;

    private final Map<String, JMenu> markerMenus = new HashMap<>();
    private final Map<String, Integer> markerPos = new HashMap<>();
    private final Map<String, Integer> markerItemCounts = new HashMap<>();


    public MenuFactory(MenuConfigItems items, StudioActionMap actionMap) {
        this.items = items;
        this.actionMap = actionMap;
    }

    public StudioActionMap getActionMap() {
        return actionMap;
    }

    public JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        for (MenuConfig menuConfig: items.getList()) {
            menuBar.add(menuConfig.init(null, this));
        }
        return menuBar;
    }

    public void addMarker(JMenu menu, String id, int pos) {
        if (markerMenus.containsKey(id)) throw new IllegalArgumentException("Repeat marker: " + id);

        markerMenus.put(id, menu);
        markerPos.put(id, pos);
        markerItemCounts.put(id, 0);
    }

    private void shiftMarker(String id, int shift) {
        JMenu menu = markerMenus.get(id);
        int pos = markerPos.get(id);
        int count = markerItemCounts.get(id);
        markerItemCounts.put(id, count + shift);

        for(String otherId: markerMenus.keySet()) {
            if ( markerMenus.get(otherId) != menu ) continue;

            int otherPos = markerPos.get(otherId);
            if (otherPos<=pos) continue;

            markerPos.put(otherId, otherPos + shift);
        }

    }

    public void cleanAtMarker(String id) {
        JMenu menu = markerMenus.get(id);
        if (menu == null) throw new IllegalArgumentException("Unknown marker: " + id);

        int pos = markerPos.get(id);
        int count = markerItemCounts.get(id);
        for (int i=0; i<count; i++) {
            menu.remove(pos);
        }
        shiftMarker(id, -count);
    }

    public void addAtMarker(String id, BaseAction<?> action) {
        JMenu menu = markerMenus.get(id);
        if (menu == null) throw new IllegalArgumentException("Unknown marker: " + id);

        BaseAction.insertToMenu(action, menu, markerPos.get(id) + markerItemCounts.get(id));
        shiftMarker(id, 1);
    }

}
