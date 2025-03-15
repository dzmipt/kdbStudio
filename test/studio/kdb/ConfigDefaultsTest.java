package studio.kdb;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.core.ProxyBlockedClassLoader;
import studio.kdb.config.AbstractConfig;
import studio.utils.log4j.EnvConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigDefaultsTest {

    private static Path configPath;
    private static Path oldBaseConfig;
    private static String home;
    @BeforeAll
    public static void prepare() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        configPath = Files.createTempDirectory("kdbStudioConfig");
        Files.createDirectory(configPath.resolve("plugins"));

        home = System.getProperty("user.dir");
        oldBaseConfig = EnvConfig.getBaseFolder(EnvConfig.getEnvironment());

        EnvConfig.setBaseFolder(configPath);
        System.setProperty("user.dir", configPath.toString());
    }

    private static void setProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @AfterAll
    public static void cleanup() throws IOException {
        FileUtils.deleteDirectory(configPath.toFile());
        EnvConfig.setBaseFolder(oldBaseConfig);
        setProperty("user.dir", home);
    }

    private static AbstractConfig newConfig() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> clazz = ProxyBlockedClassLoader.newClass(Config.class);
        return (AbstractConfig) clazz.getDeclaredMethod("getInstance").invoke(null);
    }

    @Test
    public void test() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        String key = Config.KDB_MESSAGE_SIZE_LIMIT_MB;

        Path mainConfig = configPath.resolve("studio.properties");
        AbstractConfig config = newConfig();
        assertEquals(10, config.getInt(key), "10 is a default value");

        config.setInt(key, 100);
        config.saveToDisk();
        assertEquals(100, newConfig().getInt(key), "100 is saved");

        Path defaultPath = configPath.resolve("plugins").resolve("studio.properties");
        AbstractConfig defaults = new AbstractConfig(defaultPath);
        defaults.setInt(key, 20);
        defaults.saveToDisk();

        assertEquals(100, newConfig().getInt(key), "The value is still 100 despite of default");

        config.setInt(key, 10);
        config.saveToDisk();
        assertEquals(10, newConfig().getInt(key), "The value is 10 as it is written to main config");

        System.out.printf("Deleting %s. File exist: %b%n", mainConfig, Files.exists(mainConfig));
        Files.delete(mainConfig);
        assertEquals(20, newConfig().getInt(key), "The value should be 20 from default");

    }
}
