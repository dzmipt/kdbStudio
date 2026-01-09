package studio.ui.action;

import studio.ui.StudioWindow;
import studio.ui.action.config.ActionConfig;
import studio.ui.action.config.ConfigParser;
import studio.ui.action.config.MenuConfigItems;
import studio.ui.action.config.StudioWindowAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ActionRegistry {

    private final static String CONFIG_FILENAME = "config.json";

    private final static Map<String, ActionConfig> actionConfigs = new HashMap<>();
    private final static Map<String, StudioWindowAction> studioWindowActions = Actions.studioWindowActions;

    private final static MenuConfigItems menuConfigItems;

    static {
        ConfigParser parser = new ConfigParser(CONFIG_FILENAME);
        for (ActionConfig config: parser.getActions() ) actionConfigs.put(config.getId(), config);

        menuConfigItems = parser.getMenuConfig();
    }

    private static StudioAction getStudioAction(StudioWindow studioWindow, String id) {
        if (! actionConfigs.containsKey(id)) throw new IllegalArgumentException("ActionConfig is not found for action " + id);
        if (! studioWindowActions.containsKey(id)) throw new IllegalArgumentException("No action binds with id " + id);

        StudioAction studioAction = new StudioAction();
        studioAction.setSource(studioWindow);
        studioAction.setBaseAction(studioWindowActions.get(id));

        return (StudioAction) actionConfigs.get(id).apply(studioAction);
    }

    public static StudioActionMap getStudioActionMap(StudioWindow studioWindow) {
        StudioActionMap map = new StudioActionMap();
        for(String id: studioWindowActions.keySet()) {
            map.put(id, getStudioAction(studioWindow, id));
        }
        return map;
    }


    public static Set<String> getActionIDs() {
        return actionConfigs.keySet();
    }

    public static MenuFactory getMenuFactory(StudioActionMap actionMap) {
        return new MenuFactory(menuConfigItems, actionMap);
    }

}
