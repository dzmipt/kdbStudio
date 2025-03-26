package studio.kdb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.kdb.config.ConfigType;
import studio.kdb.config.ConfigTypeRegistry;
import studio.kdb.config.StudioConfig;
import studio.utils.FileConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StudioConfigDefaultsTest {

    private static Path configPath;
    private static Path defaultConfigPath;
    private static ConfigTypeRegistry registry;
    private static String KEY = "key";
    @BeforeAll
    public static void prepare() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        configPath = Files.createTempFile("studioConfig",".json");
        Files.delete(configPath);
        defaultConfigPath = Files.createTempFile("defaultStudioConfig",".json");
        Files.delete(defaultConfigPath);

        registry = new ConfigTypeRegistry();
        registry.add(KEY, ConfigType.INT, 10);
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.delete(configPath);
        Files.delete(defaultConfigPath);
    }

    private StudioConfig newConfig() {
        return new StudioConfig(registry, new FileConfig(configPath), new FileConfig(defaultConfigPath));
    }

    @Test
    public void test() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        assertEquals(10, newConfig().getInt(KEY), "10 is a default value");

        StudioConfig config = new StudioConfig(registry, new FileConfig(configPath));
        config.setInt(KEY, 100);
        config.getFileConfig().saveOnDisk();
        assertEquals(100, newConfig().getInt(KEY), "100 is saved");

        StudioConfig defaults = new StudioConfig(registry, new FileConfig(defaultConfigPath));
        defaults.setInt(KEY, 20);
        defaults.getFileConfig().saveOnDisk();

        assertEquals(100, newConfig().getInt(KEY), "The value is still 100 despite of default");

        config.setInt(KEY, 10);
        config.getFileConfig().saveOnDisk();
        assertEquals(20, newConfig().getInt(KEY), "The value should be 20 from default");
    }
}
