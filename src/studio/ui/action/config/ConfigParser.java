package studio.ui.action.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class ConfigParser {

    private final static String ACTIONS = "actions";
    private final static String MENU_BAR = "menuBar";

    private final JsonObject jsonRoot;

    public ConfigParser(String filename) {
        try (InputStream inputStream = ConfigParser.class.getClassLoader().getResourceAsStream(filename) ) {
            jsonRoot = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Error on parsing " + filename, e);
        }
    }

    public List<ActionConfig> getActions() {
        List<ActionConfig> actions = new ArrayList<>();
        JsonObject jsonActions = jsonRoot.getAsJsonObject(ACTIONS);
        for (String id: jsonActions.keySet()) {
            try {
                actions.add(ActionConfig.parse(id, jsonActions.getAsJsonObject(id)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Error parsing action " + id, e);
            }
        }
        return actions;
    }

    public MenuConfigItems getMenuConfig() {
        return MenuConfigItems.parse(jsonRoot.getAsJsonArray(MENU_BAR));
    }

}
