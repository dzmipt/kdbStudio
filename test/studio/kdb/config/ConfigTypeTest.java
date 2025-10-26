package studio.kdb.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.kdb.FileChooserConfig;
import studio.kdb.KType;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ConfigTypeTest {

    private void check(ConfigType type, Object value) {
        check(type, value, null);
    }

    private void check(ConfigType type, Object value, Object defaultValue) {
        Object parsedValue = type.fromJson(type.toJson(value),defaultValue);
        assertEquals(value, parsedValue);
    }

    @Test
    public void testSimple() {
        check(ConfigType.STRING, "test", "other");
        check(ConfigType.INT, 10, 15);

        check(ConfigType.DOUBLE, 3.14, 2.05);
        check(ConfigType.DOUBLE, -3.14, 2.05);
        check(ConfigType.DOUBLE, 0.0, 2.05);
        check(ConfigType.DOUBLE, 0.01, 2.05);

        check(ConfigType.BOOLEAN, true, false);
        check(ConfigType.BOOLEAN, false, true);

        check(ConfigType.FONT, new Font("Times", Font.BOLD | Font.ITALIC, 15));

        check(ConfigType.BOUNDS, new Rectangle(-10, 5, 100, 117));

        check(ConfigType.COLOR, Color.CYAN);

        check(ConfigType.ENUM, KdbMessageLimitAction.ASK, KdbMessageLimitAction.BLOCK);

        check(ConfigType.SIZE, new Dimension(215, 317));

        check(ConfigType.FILE_CHOOSER, new FileChooserConfig("/folder/afile.ext", new Dimension(125, 137)));
    }

    @Test
    public void testDefaultAuthConfig() {
        check(ConfigType.CREDENTIALS, Credentials.DEFAULT);
        check(ConfigType.CREDENTIALS, new Credentials("user",""));

        DefaultAuthConfig config = DefaultAuthConfig.DEFAULT;
        check(ConfigType.DEFAULT_AUTH_CONFIG, config);

        config = new DefaultAuthConfig(config, "testAuth", new Credentials("dz", "pwd"));
        check(ConfigType.DEFAULT_AUTH_CONFIG, config);

        config = new DefaultAuthConfig(config, "something");
        check(ConfigType.DEFAULT_AUTH_CONFIG, config);
    }

    @Test
    public void testTableConnExtractor() {
        check(ConfigType.TABLE_CONN_EXTRACTOR, TableConnExtractor.DEFAULT);

        TableConnExtractor extractor = new TableConnExtractor(10,
                List.of("server", "host", "connection", "handle"),
                List.of("server", "host"),
                List.of("port") );
        check(ConfigType.TABLE_CONN_EXTRACTOR, extractor);

        extractor = new TableConnExtractor(10,
            List.of("a", "b", "c"),
            List.of("1", "2"),
            List.of("aa1", "bb2", "cc3") );
        check(ConfigType.TABLE_CONN_EXTRACTOR, extractor);

    }

    @Test
    public void testColorTokenConfig() {
        check(ConfigType.COLOR_TOKEN_CONFIG, ColorTokenConfig.DEFAULT);

        ColorMap map = new ColorMap();
        map.put(ColorToken.DATE, new Color(11,22,33));
        check(ConfigType.COLOR_TOKEN_CONFIG, new ColorTokenConfig(map));

        map.put(ColorToken.ERROR, Color.RED);
        check(ConfigType.COLOR_TOKEN_CONFIG, new ColorTokenConfig(map));

        map.put(ColorToken.ERROR, new Color(10, 20, 30));
        check(ConfigType.COLOR_TOKEN_CONFIG, new ColorTokenConfig(map));
    }

    @Test
    public void testColorTokenConfigOneColorChange() {
        Color newDefault = new Color(0x112233);
        assertNotEquals(newDefault, ColorToken.DEFAULT.getColor());

        String jsonText = "{\"DEFAULT\":\"112233\",\"SYSTEM\":\"F0B400\"}";
        JsonElement json = JsonParser.parseString(jsonText);
        ColorTokenConfig config = (ColorTokenConfig)ConfigType.COLOR_TOKEN_CONFIG.fromJson(json, null);

        assertEquals(newDefault, config.getColor(ColorToken.DEFAULT));
        assertEquals(ColorToken.SYMBOL.getColor(), config.getColor(ColorToken.SYMBOL));
    }


        @Test
    public void testServerHistory() {
        ServerHistoryConfig config = new ServerHistoryConfig(20, List.of());
        check(ConfigType.SERVER_HISTORY, config);

        config = new ServerHistoryConfig(20, List.of("name", "folder/name"));
        check(ConfigType.SERVER_HISTORY, config);

        config = new ServerHistoryConfig(10, List.of("name", "folder/name"));
        check(ConfigType.SERVER_HISTORY, config);

        List<String> values = new ArrayList<>();
        for (int i=0; i<30; i++) values.add("value" + i);

        config = new ServerHistoryConfig(10, values);
        check(ConfigType.SERVER_HISTORY, config);
    }

    @Test
    public void testStringArray() {
        List<String> strings = List.of("one", "two", "three");
        check(ConfigType.STRING_ARRAY, strings, List.of("something"));

        check(ConfigType.STRING_ARRAY, List.of(), List.of());
    }

    @Test
    public void testIntArray() {
        List<Integer> ints = List.of(11, 22);
        check(ConfigType.INT_ARRAY, ints, List.of());

        check(ConfigType.INT_ARRAY, List.of(), List.of());
    }

    @Test
    public void testEnumArray() {
        List<KType> types = List.of(KType.Int, KType.IntVector, KType.Function);
        check(ConfigType.ENUM_ARRAY, types, List.of(KType.List));

        types = List.of();
        check(ConfigType.ENUM_ARRAY, types, List.of(KType.List));

        types = List.of();
        check(ConfigType.ENUM_ARRAY, types, List.of());

    }

    @Test
    public void testColorArray() {
        List<Color> colors = List.of(Color.ORANGE, new Color(1,2,3));
        check(ConfigType.COLOR_ARRAY, colors);

        check(ConfigType.COLOR_ARRAY, List.of());
    }

    @Test
    public void testColorSets() {
        ColorSets colorSets = ColorSets.DEFAULT;
        check(ConfigType.CHART_COLOR_SETS, colorSets);

        colorSets = colorSets.setColorSchema("add", new ColorSchema());
        assertEquals(2, colorSets.getNames().size());
        check(ConfigType.CHART_COLOR_SETS, colorSets);

        colorSets = colorSets.newSelected("add");
        assertEquals("add", colorSets.getDefaultName());
        check(ConfigType.CHART_COLOR_SETS, colorSets);

        colorSets = colorSets.deleteColorSchema("Default");
        assertEquals(1, colorSets.getNames().size());
        check(ConfigType.CHART_COLOR_SETS, colorSets);
    }

    @Test
    public void testColorSchema() {
        check(ConfigType.CHART_COLOR_SCHEMA, new ColorSchema());

        ColorSchema colorSchema = ColorSchema.DEFAULT;
        check(ConfigType.CHART_COLOR_SCHEMA, colorSchema);

        colorSchema = colorSchema.newBackgroundColor(Color.RED);
        assertEquals(Color.RED, colorSchema.getBackground());
        check(ConfigType.CHART_COLOR_SCHEMA, colorSchema);

        colorSchema = colorSchema.newGridColor(Color.GREEN);
        assertEquals(Color.GREEN, colorSchema.getGrid());
        check(ConfigType.CHART_COLOR_SCHEMA, colorSchema);

        colorSchema = colorSchema.newColors(List.of(Color.BLACK, Color.CYAN, Color.DARK_GRAY));
        assertEquals(List.of(Color.BLACK, Color.CYAN, Color.DARK_GRAY), colorSchema.getColors());
        check(ConfigType.CHART_COLOR_SCHEMA, colorSchema);

    }
}
