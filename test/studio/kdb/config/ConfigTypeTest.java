package studio.kdb.config;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.kdb.FileChooserConfig;
import studio.kdb.KType;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigTypeTest {

    private void check(ConfigType type, Object value, Object defaultValue) {
        Object parsedValue = type.fromJson(type.toJson(value),defaultValue);
        assertEquals(value, parsedValue);
    }

    @Test
    public void test() {
        check(ConfigType.STRING, "test", "other");
        check(ConfigType.INT, 10, 15);

        check(ConfigType.DOUBLE, 3.14, 2.05);
        check(ConfigType.DOUBLE, -3.14, 2.05);
        check(ConfigType.DOUBLE, 0.0, 2.05);
        check(ConfigType.DOUBLE, 0.01, 2.05);

        check(ConfigType.BOOLEAN, true, false);
        check(ConfigType.BOOLEAN, false, true);

        check(ConfigType.FONT, new Font("Times", Font.BOLD | Font.ITALIC, 15), null);

        check(ConfigType.BOUNDS, new Rectangle(-10, 5, 100, 117), null);

        check(ConfigType.COLOR, Color.CYAN, null);

        check(ConfigType.ENUM, KdbMessageLimitAction.ASK, KdbMessageLimitAction.BLOCK);

        check(ConfigType.SIZE, new Dimension(215, 317), null);

        check(ConfigType.FILE_CHOOSER, new FileChooserConfig("/folder/afile.ext", new Dimension(125, 137)), null);

        check(ConfigType.CREDENTIALS, Credentials.DEFAULT, null);
        check(ConfigType.CREDENTIALS, new Credentials("user",""), null);

        DefaultAuthConfig config = new DefaultAuthConfig();
        check(ConfigType.DEFAULT_AUTH_CONFIG, config, null);

        config.setCredentials("testAuth", new Credentials("dz", "pwd"));
        check(ConfigType.DEFAULT_AUTH_CONFIG, config, null);

        config.setDefaultAuth("something");
        check(ConfigType.DEFAULT_AUTH_CONFIG, config, null);
    }

    @Test
    public void testStringArray() {
        String[] strings = new String[] {"one", "two", "three"};
        JsonElement json = ConfigType.STRING_ARRAY.toJson(strings);
        String[] parsed = (String[]) ConfigType.STRING_ARRAY.fromJson(json, new String[] {"something"});
        assertArrayEquals(strings, parsed);
    }

    @Test
    public void testIntArray() {
        int[] ints = new int[] {11, 22};
        JsonElement json = ConfigType.INT_ARRAY.toJson(ints);
        int[] parsed = (int[]) ConfigType.INT_ARRAY.fromJson(json, new int[] {0});
        assertArrayEquals(ints, parsed);
    }

    @Test
    public void testEnumArray() {
        KType[] types = new KType[] {KType.Int, KType.IntVector, KType.Function};
        JsonElement json = ConfigType.ENUM_ARRAY.toJson(types);
        KType[] parsed = (KType[]) ConfigType.ENUM_ARRAY.fromJson(json, new KType[] {KType.List});
        assertArrayEquals(types, parsed);
    }
}
