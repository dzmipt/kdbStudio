package studio.kdb.config;

import org.junit.jupiter.api.Test;
import studio.kdb.FileChooserConfig;

import java.awt.*;

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

    }
}
