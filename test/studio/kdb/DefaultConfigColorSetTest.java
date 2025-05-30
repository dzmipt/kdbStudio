package studio.kdb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.kdb.config.ColorSets;
import studio.kdb.config.ConfigType;
import studio.kdb.config.ConfigTypeRegistry;
import studio.kdb.config.StudioConfig;
import studio.utils.FileConfig;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultConfigColorSetTest {

    private static Path configPath, defaultConfigPath;
    private static ConfigTypeRegistry registry;

    @BeforeAll
    public static void prepare() throws IOException {
        configPath = Files.createTempFile("studioConfig",".json");
        Files.delete(configPath);

        defaultConfigPath = Files.createTempFile("defaultStudioConfig",".json");
        Files.delete(defaultConfigPath);

        Files.writeString(configPath, "{}");

        Files.writeString(defaultConfigPath,
                "{ chartColorSets: {" +
                            "'default':'custom'," +
                            "'set': {" +
                                "'custom':{" +
                                    "'colors':['ff0000','00ff00'], " +
                                    "'background':'ff00ff'," +
                                    "'grid':'ffffff' }}" +
                        "}}".replace('\'','"'));

        registry = new ConfigTypeRegistry();
        registry.add("chartColorSets", ConfigType.CHART_COLOR_SETS, ColorSets.DEFAULT);
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Files.delete(configPath);
        Files.delete(defaultConfigPath);
    }

    @Test
    public void testOverrideColorSchema() throws IOException {
        StudioConfig config = new StudioConfig(registry, new FileConfig(configPath), new FileConfig(defaultConfigPath));

        ColorSets colorSets = (ColorSets) config.get("chartColorSets");

        assertEquals(2, colorSets.getNames().size());
        assertEquals("custom", colorSets.getDefaultName());
        assertEquals(List.of(Color.RED, Color.GREEN), colorSets.getColorSchema("custom").getColors());
        assertTrue(colorSets.getColorSchema("Default").getColors().size()>0);

    }

}
