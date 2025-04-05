package studio.kdb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
    private static Path configPath, complexDefaultConfigPath, simpleDefaultConfigPath, errorDefaultConfigPath;
    private static ConfigTypeRegistry registry;
    private final static String KEY = "key";

    @BeforeAll
    public static void prepare() throws IOException {
        registry = new ConfigTypeRegistry();
        registry.add(KEY, ConfigType.BOUNDS, new Rectangle(5, 10, 100, 200));

        configPath = Files.createTempFile("studioConfig",".json");
        complexDefaultConfigPath = Files.createTempFile("defaultStudioConfig",".json");
        errorDefaultConfigPath = Files.createTempFile("errorDefaultStudioConfig",".json");
        simpleDefaultConfigPath = Files.createTempFile("simpleDefaultStudioConfig",".json");
        Files.delete(simpleDefaultConfigPath);

        Files.writeString(complexDefaultConfigPath,
                "{" +
                        "key: {" +
                        "'width':105," +
                        "'height':200" +
                        "}}".replace('\'','"'));
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.delete(configPath);
        Files.delete(complexDefaultConfigPath);
        Files.delete(errorDefaultConfigPath);
    }

    @BeforeEach
    public void prepareConfig() throws IOException {
        Files.writeString(configPath, "{}");
    }

    private StudioConfig getConfig(Path defaultConfigPath) {
        return new StudioConfig(registry, new FileConfig(configPath), new FileConfig(defaultConfigPath));
    }

    private void check(Path defaultConfigPath, Rectangle rect) {
        StudioConfig config = getConfig(defaultConfigPath);
        config.set(KEY, rect);
        config.getFileConfig().saveOnDisk();
        assertEquals(rect, getConfig(defaultConfigPath).get(KEY));
    }

    @Test
    public void test() {
        assertEquals(new Rectangle(5, 10, 105, 200), getConfig(complexDefaultConfigPath).get(KEY));

        check(complexDefaultConfigPath, new Rectangle(5, 10, 100, 200));
        check(complexDefaultConfigPath, new Rectangle(5, 10, 105, 200));
        check(complexDefaultConfigPath, new Rectangle(5, 15, 105, 200));
        check(complexDefaultConfigPath, new Rectangle(5, 15, 100, 200));
        check(complexDefaultConfigPath, new Rectangle(5, 15, 105, 205));
        check(complexDefaultConfigPath, new Rectangle(5, 15, 100, 205));
        check(complexDefaultConfigPath, new Rectangle(0, 0, 0, 0));
    }

    @Test
    public void testSimple() {
        assertEquals(new Rectangle(5, 10, 100, 200), getConfig(simpleDefaultConfigPath).get(KEY));

        check(simpleDefaultConfigPath, new Rectangle(5, 10, 100, 200));
        check(simpleDefaultConfigPath, new Rectangle(5, 10, 105, 200));
        check(simpleDefaultConfigPath, new Rectangle(5, 15, 105, 200));
        check(simpleDefaultConfigPath, new Rectangle(5, 15, 100, 200));
        check(simpleDefaultConfigPath, new Rectangle(5, 15, 105, 205));
        check(simpleDefaultConfigPath, new Rectangle(5, 15, 100, 205));
        check(simpleDefaultConfigPath, new Rectangle(0, 0, 0, 0));
    }

    public void check(String error) throws IOException {
        Files.writeString(errorDefaultConfigPath, error);
        assertEquals(new Rectangle(5, 10, 100, 200), getConfig(errorDefaultConfigPath).get(KEY));
    }

    @Test
    public void testError() throws IOException {
        check("{");

        check("{" +
                "key: {" +
                "'width':105a," +
                "'height':200" +
                "}}".replace('\'','"') );

        check("{" +
                "key: {" +
                "'width':105" +
                "'height':200" +
                "}}".replace('\'','"') );

        check("{" +
                "key: {" +
                "'width':'xxx'," +
                "'height':200" +
                "}}".replace('\'','"') );

        check("{" +
                "key: {" +
                "'width':[11,12]," +
                "'height':200" +
                "}}".replace('\'','"') );

    }
}
