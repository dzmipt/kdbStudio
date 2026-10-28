package studio.kdb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.kdb.config.*;
import studio.utils.FileConfig;
import studio.utils.MockConfig;
import studio.utils.log4j.EnvConfig;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StyleSchemaMigrationTest {

    private final static String DARCULA_LAF = "com.formdev.flatlaf.FlatDarculaLaf";
    private final static String DARK_LAF = "com.formdev.flatlaf.FlatDarkLaf";

    private final static String SERVERS = ("{'name': '', 'children': [" +
            "{'name': 'same', 'handle': '`:host:1', 'bgColor': 'fefefe', 'authMethod': 'Plain'}," +
            "{'name': 'white', 'handle': '`:host:2', 'bgColor': 'ffffff', 'authMethod': 'Plain'}," +
            "{'name': 'custom', 'handle': '`:host:3', 'bgColor': '123456', 'authMethod': 'Plain'}," +
            "{'name': 'folder', 'children': [" +
                "{'name': 'inner', 'handle': '`:host:4', 'bgColor': 'fefefe', 'authMethod': 'Plain'}" +
            "]}" +
            "]}").replace('\'', '"');

    private Path basePath;
    private Path pluginFolder;

    @BeforeEach
    public void init() throws IOException {
        basePath = MockConfig.createTempDir();
        Files.writeString(basePath.resolve(Config.SERVERCONFIG_FILENAME), SERVERS);
        pluginFolder = EnvConfig.getPluginFolder();
    }

    @AfterEach
    public void restore() {
        EnvConfig.setPluginFolder(pluginFolder);
    }

    private void writeStudioConfig(String json) throws IOException {
        Files.writeString(basePath.resolve(Config.CONFIG_FILENAME), json.replace('\'', '"'));
    }

    private Config load() {
        Config config = new Config(basePath);
        FileConfig.saveAllOnDisk();
        return config;
    }

    private JsonObject readStudioJson() throws IOException {
        return JsonParser.parseString(Files.readString(basePath.resolve(Config.CONFIG_FILENAME))).getAsJsonObject();
    }

    @Test
    public void testCustomizedStyle() throws IOException {
        writeStudioConfig("{'version': 'V2_3', 'lookandfeel': '" + DARCULA_LAF + "'," +
                "'editorColors': {'background': 'fefefe'}," +
                "'tokens': {'keyword': '0000fe:Bold'}" +
                "}");

        Config config = load();

        StyleSchemas schemas = config.getStyleSchemas();
        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK, ConfigConverter.CUSTOM_STYLE_SCHEMA), schemas.getNames());
        assertEquals(ConfigConverter.CUSTOM_STYLE_SCHEMA, config.getStyleSchemaName());
        assertEquals(Map.of(DARCULA_LAF, ConfigConverter.CUSTOM_STYLE_SCHEMA), schemas.getLookAndFeelSchemas());
        // other dark Look and Feels use the predefined Dark schema
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(DARK_LAF));

        StyleSchema custom = schemas.get(ConfigConverter.CUSTOM_STYLE_SCHEMA);
        assertEquals(StyleSchemas.LIGHT, custom.getBasedOn());
        assertEquals(new Color(0xfefefe), custom.getBackgroundColor());
        assertEquals(ColorMap.DEFAULT_EDITOR_COLORS.get(EditorColorToken.SELECTED), custom.getEditorColors().get(EditorColorToken.SELECTED));
        assertEquals(new TokenStyle(new Color(0x0000fe), FontStyle.Bold), custom.getTokens().get(ColorToken.KEYWORD));
        // not customized tokens are taken from the old defaults
        assertEquals(ColorToken.SYMBOL.getDefaultStyle(), custom.getTokens().get(ColorToken.SYMBOL));
        // grid colors were not customized - they are still derived from Look and Feel
        assertTrue(custom.getGridColors().isDefaultConfig());

        JsonObject json = readStudioJson();
        assertEquals("V2_4", json.get(Config.CONFIG_VERSION).getAsString());
        assertFalse(json.has("editorColors"));
        assertFalse(json.has("tokens"));
        assertFalse(json.has("gridColors"));
        assertTrue(json.getAsJsonObject(Config.STYLE_SCHEMAS).has(ConfigConverter.CUSTOM_STYLE_SCHEMA));

        ServerConfig serverConfig = config.getServerConfig();
        assertNull(serverConfig.getServer("same").getBackgroundColor());
        assertNull(serverConfig.getServer("folder/inner").getBackgroundColor());
        assertEquals(Color.WHITE, serverConfig.getServer("white").getBackgroundColor());
        assertEquals(new Color(0x123456), serverConfig.getServer("custom").getBackgroundColor());

        assertEquals(new Color(0xfefefe), config.getServerBgColor(serverConfig.getServer("same")));

        String servers = Files.readString(basePath.resolve(Config.SERVERCONFIG_FILENAME));
        assertFalse(servers.contains("fefefe"));
        assertTrue(servers.contains("123456"));

        // loading once again doesn't change anything
        Config config2 = load();
        assertEquals(schemas, config2.getStyleSchemas());
    }

    @Test
    public void testNotCustomizedStyle() throws IOException {
        writeStudioConfig("{'version': 'V2_3', 'lookandfeel': '" + DARK_LAF + "'," +
                "'editorColors': {'background': 'ffffff'}," +
                "'tokens': {}" +
                "}");

        Config config = load();
        StyleSchemas schemas = config.getStyleSchemas();
        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK), schemas.getNames());
        assertTrue(schemas.getLookAndFeelSchemas().isEmpty());
        assertEquals(StyleSchemas.DARK, config.getStyleSchemaName());
        assertEquals(StyleSchemas.getBuiltIn().get(StyleSchemas.DARK), config.getStyleSchema());

        ServerConfig serverConfig = config.getServerConfig();
        assertNull(serverConfig.getServer("white").getBackgroundColor());
        assertEquals(new Color(0xfefefe), serverConfig.getServer("same").getBackgroundColor());
        assertEquals(config.getBackgroundColor(), config.getServerBgColor(serverConfig.getServer("white")));
    }

    @Test
    public void testPluginSchemas() throws IOException {
        Path plugins = Files.createDirectories(basePath.resolve("plugins"));
        EnvConfig.setPluginFolder(plugins);
        Files.writeString(plugins.resolve(Config.CONFIG_FILENAME), ("{" +
                "'editorColors': {'background': '000001'}," +
                "'styleSchemas': {'Corp': {'basedOn': 'Dark', 'editorColors': {'background': '000002'}}}," +
                "'lookAndFeelSchemas': {'" + DARK_LAF + "': 'Corp'}" +
                "}").replace('\'', '"'));

        writeStudioConfig("{'version': 'V2_4', 'lookandfeel': '" + DARK_LAF + "'," +
                "'styleSchemas': {'Mine': {'basedOn': 'Light', 'editorColors': {'background': '000003'}}}" +
                "}");

        Config config = load();
        StyleSchemas schemas = config.getStyleSchemas();
        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK, "Corp", "Mine"), schemas.getNames());
        assertTrue(schemas.isPredefined("Corp"));
        assertFalse(schemas.isPredefined("Mine"));
        assertEquals("Corp", config.getStyleSchemaName());
        // old style keys in the plugin config are ignored
        assertEquals(new Color(0x000002), config.getBackgroundColor());

        schemas = schemas.duplicate("Corp", "Corp copy").select(DARK_LAF, "Corp copy");
        config.setStyleSchemas(schemas);
        FileConfig.saveAllOnDisk();

        JsonObject json = readStudioJson().getAsJsonObject(Config.STYLE_SCHEMAS);
        assertFalse(json.has("Corp"));
        assertTrue(json.has("Mine"));
        assertEquals("Dark", json.getAsJsonObject("Corp copy").get(StyleSchema.BASED_ON).getAsString());

        Config config2 = load();
        assertEquals("Corp copy", config2.getStyleSchemaName());
        assertEquals(new Color(0x000002), config2.getBackgroundColor());
        assertTrue(config2.getStyleSchemas().isPredefined("Corp"));
    }

}
