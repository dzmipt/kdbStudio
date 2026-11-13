package studio.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilJsonTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private void checkExclude(String expected, String base, String minus) {
        JsonObject jBase = json(base);
        JsonObject jMinus = json(minus);
        JsonObject excluded = Util.deepExclude(jBase, jMinus);
        assertEquals(json(expected), excluded);
        // merging back gives the same as merging the original
        assertEquals(Util.deepMerge(jMinus, jBase), Util.deepMerge(jMinus, excluded));
    }

    @Test
    public void testDeepExclude() {
        checkExclude("{}", "{'a': 1}", "{'a': 1}");
        checkExclude("{'a': 2}", "{'a': 2}", "{'a': 1}");
        checkExclude("{'b': 1}", "{'b': 1}", "{'a': 1}");
        checkExclude("{'o': {'y': 3}}", "{'o': {'x': 1, 'y': 3}}", "{'o': {'x': 1, 'y': 2}}");
    }

    @Test
    public void testDeepExcludeDropsNestedObjectsEqualToDefault() {
        checkExclude("{}", "{'o': {'x': 1, 'p': {'y': 2}}}", "{'o': {'x': 1, 'p': {'y': 2}}}");
        checkExclude("{'o': {'z': 3}}", "{'o': {'x': 1, 'p': {'y': 2}, 'z': 3}}", "{'o': {'x': 1, 'p': {'y': 2}}}");
        // a nested object equal to the default together with extra default values
        checkExclude("{}", "{'o': {'x': 1}}", "{'o': {'x': 1, 'y': 2}}");
    }

    @Test
    public void testDeepExcludeKeepsEmptyObjects() {
        checkExclude("{'o': {}}", "{'o': {}}", "{'o': {}}");
        checkExclude("{'o': {}}", "{'o': {}}", "{'o': {'x': 1}}");
    }
}
