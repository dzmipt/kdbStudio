package studio.kdb.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.kdb.Config;
import studio.kdb.ConfigAllTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigJsonConverterTest {

    private final static Logger log = LogManager.getLogger();

    private static final Properties properties = new Properties();
    private static Path studioConfigPath;
    @BeforeAll
    public static void prepareBeforeConverter() throws IOException {
        try (InputStream inputStream = ConfigAllTest.class.getClassLoader().getResourceAsStream("studio14.properties") ) {
            properties.load(inputStream);
        }
        studioConfigPath = Files.createTempFile("studio", ".json");
        Files.delete(studioConfigPath);
    }

    @AfterAll
    public static void cleanupAfterConverter() throws IOException {
        Files.delete(studioConfigPath);
    }

    @Test
    public void testConverter() {
        ConfigToJsonConverter converter = Config.getConfigToJsonConverter();
        converter.convert(properties, studioConfigPath);

        Properties remaining = converter.getRemainingProperties();
        if (! remaining.isEmpty()) {
            log.error("Remaining properties {}", remaining);
        }

        assertTrue(remaining.isEmpty());
    }

}
