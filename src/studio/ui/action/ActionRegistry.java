package studio.ui.action;

import studio.ui.StudioWindow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class ActionRegistry {

    private final static Map<String, ActionConfig> actionConfigs = new HashMap<>();
    private final static Map<String, StudioWindowAction> studioWindowActions = Actions.studioWindowActions;


    static {
        ActionConfigParser parser = new ActionConfigParser("config.json");
        for (ActionConfig config: parser.getActions() ) actionConfigs.put(config.getId(), config);
    }

    public static StudioAction getStudioAction(StudioWindow studioWindow, String id) {
        if (! actionConfigs.containsKey(id)) throw new IllegalArgumentException("ActionConfig is not found for action " + id);
        if (! studioWindowActions.containsKey(id)) throw new IllegalArgumentException("No action binds with id " + id);

        StudioAction studioAction = new StudioAction();
        studioAction.setSource(studioWindow);
        studioAction.setBaseAction(studioWindowActions.get(id));

        return (StudioAction) actionConfigs.get(id).apply(studioAction);
    }

    public static Set<String> getActionIDs() {
        return actionConfigs.keySet();
    }

}
