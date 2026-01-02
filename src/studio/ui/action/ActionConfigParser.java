package studio.ui.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class ActionConfigParser {

    private final JsonObject jsonRoot;

    public ActionConfigParser(String filename) {
        try (InputStream inputStream = ActionConfigParser.class.getClassLoader().getResourceAsStream(filename) ) {
            jsonRoot = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Error on parsing " + filename, e);
        }
    }

    public List<ActionConfig> getActions() {
        List<ActionConfig> actions = new ArrayList<>();
        JsonObject jsonActions = jsonRoot.getAsJsonObject("actions");
        for (String id: jsonActions.keySet()) {
            try {
                actions.add(ActionConfig.parse(id, jsonActions.getAsJsonObject(id)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Error parsing action " + id, e);
            }
        }
        return actions;
    }

}
