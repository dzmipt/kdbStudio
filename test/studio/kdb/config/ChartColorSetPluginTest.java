package studio.kdb.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.kdb.Config;
import studio.utils.MockConfig;
import studio.utils.log4j.EnvConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChartColorSetPluginTest {

    private final static String pluginConfig = "{ chartColorSets: {" +
            "'default':'custom'," +
            "'set': {" +
            "'custom':{" +
            "'colors':['ff0000','00ff00'], " +
            "'background':'ff00ff'," +
            "'grid':'ffffff' }}" +
            "}}".replace('\'','"');

    @BeforeAll
    public static void prepare() throws IOException {
        Path basePath = MockConfig.createTempDir();
        try (InputStream inputStream = ChartColorSetPluginTest.class.getClassLoader().getResourceAsStream("studio14.properties") ) {
            Files.copy(inputStream, basePath.resolve(Config.OLD_CONFIG_FILENAME));
        }

        Path pluginPath = Files.createDirectories(basePath.resolve("plugins"));
        EnvConfig.setPluginFolder(pluginPath);
        Files.writeString(pluginPath.resolve(Config.CONFIG_FILENAME), pluginConfig);

        MockConfig.mock(basePath);
    }

    @Test
    public void test() {
        ColorSets colorSets = Config.getInstance().getChartColorSets();
        assertEquals(2, colorSets.getNames().size());
        assertEquals("custom", colorSets.getDefaultName());
        assertEquals(2, colorSets.getColorSchema("custom").getColors().size());
    }
}

