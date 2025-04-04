package studio.kdb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.kdb.config.ConfigType;
import studio.kdb.config.ConfigTypeRegistry;
import studio.kdb.config.StudioConfig;
import studio.utils.FileConfig;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StudioConfigComplexDefaultsTest {
    private static Path configPath, defaultConfigPath;
    private static ConfigTypeRegistry registry;
    private final static String KEY = "key";

    @BeforeAll
    public static void prepare() throws IOException {
        registry = new ConfigTypeRegistry();
        registry.add(KEY, ConfigType.BOUNDS, new Rectangle(5, 10, 100, 200));

        configPath = Files.createTempFile("studioConfig",".json");
        Files.delete(configPath);

        defaultConfigPath = Files.createTempFile("defaultStudioConfig",".json");
        Files.delete(defaultConfigPath);

        Files.writeString(configPath, "{}");

        Files.writeString(defaultConfigPath,
                "{" +
                        "key: {" +
                        "'width':105," +
                        "'height':200" +
                        "}}".replace('\'','"'));
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.delete(configPath);
        Files.delete(defaultConfigPath);
    }

    private StudioConfig getConfig() {
        return new StudioConfig(registry, new FileConfig(configPath), new FileConfig(defaultConfigPath));
    }

    private void check(Rectangle rect) {
        StudioConfig config = getConfig();
        config.set(KEY, rect);
        config.getFileConfig().saveOnDisk();
        assertEquals(rect, getConfig().get(KEY));
    }

    @Test
    public void test() {
        assertEquals(new Rectangle(5, 10, 105, 200), getConfig().get(KEY));

        check(new Rectangle(5, 10, 100, 200));
        check(new Rectangle(5, 10, 105, 200));
        check(new Rectangle(5, 15, 105, 200));
        check(new Rectangle(5, 15, 100, 200));
        check(new Rectangle(5, 15, 105, 205));
        check(new Rectangle(5, 15, 100, 205));
        check(new Rectangle(0, 0, 0, 0));
    }

}
