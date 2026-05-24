package studio.kdb.config.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import studio.kdb.Server;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BgColorRules extends ArrayList<ServerFilterRule<?>> {

    public Color overrideColor(Server server) {
        for (ServerFilterRule<?> rule: this) {
            Color color = rule.getServerColor(server);
            if (color != null) return color;
        }
        return null;
    }

    public Set<FieldGetter.Names> getFieldGetterSet() {
        HashSet<FieldGetter.Names> set = new HashSet<>();
        for (ServerFilterRule<?> rule: this) {
            set.add(rule.getFieldName());
        }
        return set;
    }

    public static BgColorRules fromJson(JsonArray jsonArray) {
        BgColorRules rules = new BgColorRules();
        for (JsonElement jsonElement: jsonArray) {
            rules.add(ServerFilterRule.fromJson(jsonElement.getAsJsonObject()));
        }

        return rules;
    }

    public JsonArray toJson() {
        JsonArray jsonArray = new JsonArray(size());
        for (ServerFilterRule<?> rule: this) {
            jsonArray.add(rule.toJson());
        }
        return jsonArray;
    }

    public BgColorRules copy() {
        BgColorRules rules = new BgColorRules();
        rules.addAll(this);
        return rules;
    }

}
