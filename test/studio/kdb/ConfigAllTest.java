package studio.kdb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.*;
import studio.ui.Util;
import studio.utils.LineEnding;
import studio.utils.MockConfig;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigAllTest {

    private static InstrumentedMockConfig lastConfig;
    private static Path lastStudioConfigPath;

    @BeforeAll
    public static void prepare() throws IOException {
        Util.setMockFitToScreen(true);

        Path configPath = MockConfig.createTempDir();
        lastStudioConfigPath = configPath.resolve(Config.CONFIG_FILENAME);
        try (InputStream inputStream = ConfigAllTest.class.getClassLoader().getResourceAsStream("studio_last.json") ) {
            Files.copy(inputStream, lastStudioConfigPath);
        }
        lastConfig = new InstrumentedMockConfig(configPath);
    }

    @AfterAll
    public static void cleanup() throws IOException {
        Util.setMockFitToScreen(false);
    }

    private Config getConfig(String name, boolean oldProperties) throws IOException {
        Path configPath = MockConfig.createTempDir();
        Path path = configPath.resolve(oldProperties ? Config.OLD_CONFIG_FILENAME : Config.CONFIG_FILENAME);
        try (InputStream inputStream = ConfigAllTest.class.getClassLoader().getResourceAsStream(name) ) {
            Files.copy(inputStream, path);
        }
        return new Config(configPath);
    }

    private Config getConfig(String name) throws IOException {
        return getConfig(name, false);
    }

    @Test
    public void testConverted() throws IOException {
        Config config = getConfig("studio14.properties", true);
        testAll_1_4(config);
    }

    @Test
    public void test21() throws IOException {
        Config config = getConfig("studio21.json");
        testAll_1_4(config);
        testAlL_2_1(config);
    }

    @Test
    public void testAllKeysDefined() throws IOException {
        try (Reader reader = Files.newBufferedReader(lastStudioConfigPath)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            Set<String> keysInTest = json.keySet();
            Set<String> definedTypes = new HashSet<>(lastConfig.getConfigTypeRegistry().keySet());
            definedTypes.add(StudioConfig.COMMENT);

            Set<String> set = new HashSet<>(definedTypes);
            set.removeAll(keysInTest);
            System.out.println("Keys missed in the test: " + set);

            set = new HashSet<>(keysInTest);
            set.removeAll(definedTypes);
            System.out.println("Extra keys the test: " + set);

            assertEquals(definedTypes, keysInTest);

            String strVersion = json.get(Config.CONFIG_VERSION).getAsString();
            ConfigVersion version = ConfigVersion.valueOf(strVersion);
            assertEquals(ConfigVersion.LAST, version);
        }
    }

    @Test
    public void testLastConfig() {
        lastConfig.resetAccessedKeys();

        testLast(lastConfig);

        Set<String> accessedKeys = lastConfig.getAccessedKeys();
        Set<String> definedTypes = new HashSet<>(lastConfig.getConfigTypeRegistry().keySet());

        Set<String> set = new HashSet<>(definedTypes);
        set.removeAll(accessedKeys);
        System.out.println("Not accessed keys: " + set);

        set = new HashSet<>(accessedKeys);
        set.removeAll(definedTypes);
        System.out.println("Access not defined keys (how?): " + set);

        assertEquals(definedTypes, accessedKeys);
    }

    private void testLast(Config config) {
        testAll_1_4(config);
        testAlL_2_1(config);
        testAlL_2_2(config);
    }

    private void testAlL_2_2(Config config) {
        ColorMap colors = config.getEditorColors();
        assertEquals(new Color(0xfefefe), colors.get(EditorColorToken.BACKGROUND));
        assertEquals(new Color(0xabcdef), colors.get(EditorColorToken.SELECTED));
        assertEquals(new Color(0x987654), colors.get(EditorColorToken.CURRENT_LINE_HIGHLIGHT));
        assertEquals(false, config.getBoolean(Config.ALIGN_RIGHT_NUMBERS_IN_RESULT));
    }

    private BasicStroke getStroke(float... dashArray) {
        return new BasicStroke(1f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 1f, dashArray, 0f);
    }

    private Color[] getColors(int... rgbs) {
        return IntStream.of(rgbs).mapToObj(Color::new).toArray(Color[]::new);
    }

    private ColorMap getColorMap(GridColorToken[] keys, Color[] colors) {
        if (keys.length != colors.length) throw new IllegalArgumentException("Length is different");

        ColorMap colorMap = new ColorMap();
        for (int i=0; i<keys.length; i++) {
            colorMap.put(keys[i], colors[i]);
        }
        return colorMap;
    }

    private void testAlL_2_1(Config config) {
        assertTrue(config.getBoolean(Config.LOG_DEBUG));
        assertFalse(config.getBoolean(Config.SERVER_FROM_RESULT_IN_CURRENT));
        assertFalse(config.getBoolean(Config.INSPECT_RESULT_IN_CURRENT));
        assertEquals(List.of(1.0, 0.5, 1.5, 2.0),
                                config.getDoubleArray(Config.CHART_STROKE_WIDTHS));

        assertEquals(List.of(
                            getStroke(0.5f, 1.5f, 1),
                            new BasicStroke(1),
                            getStroke(1),
                            getStroke(2,2)
                        ), config.getStyleStrokes());


        GridColorConfig gridColorConfig = new GridColorConfig(
                getColorMap(new GridColorToken[] {
                            GridColorToken.NULL, GridColorToken.KEY, GridColorToken.ODD, GridColorToken.EVEN, GridColorToken.MARK,
                            GridColorToken.SELECTED, GridColorToken.MARK_SELECTED
                        },
                        getColors(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07)),
                getColorMap(new GridColorToken[] {
                                GridColorToken.KEY, GridColorToken.ODD, GridColorToken.EVEN, GridColorToken.MARK,
                                GridColorToken.SELECTED, GridColorToken.MARK_SELECTED
                        },
                        getColors(0x08, 0x09, 0x10, 0x11, 0x12, 0x13))
        );

        assertEquals(gridColorConfig, config.getGridColorConfig());


        ColorSchema dzSchema = new ColorSchema(
                new Color(0x333333), new Color(0xffff00),
                List.of(getColors(0xff0000, 0x0000ff, 0x00ff00)) );

        String name = "DZ Custom";

        ColorSets colorSets = new ColorSets(name,
                Map.of(name, dzSchema, ColorSets.DEFAULT_NAME, ColorSchema.DEFAULT)
        );

        assertEquals(colorSets, config.getChartColorSets());

    }

    private void testAll_1_4(Config config) {
        test(config);
        testTokenColors(config);
        testCustom(config);
    }

    private void test(Config config) {
        assertEquals(ActionOnExit.NOTHING, config.getEnum(Config.ACTION_ON_EXIT));

        assertEquals("Plain", DefaultAuthenticationMechanism.NAME);
        assertEquals("testplugin", config.getDefaultAuthMechanism());

        Credentials c = config.getDefaultCredentials("Plain");
        assertEquals("testuser", c.getUsername());
        assertEquals("testpassword", c.getPassword());

        c = config.getDefaultCredentials("testplugin");
        assertEquals("uu", c.getUsername());
        assertEquals("pwd", c.getPassword());

        assertEquals(true, config.getBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN));
        assertEquals(250, config.getInt(Config.CELL_MAX_WIDTH));
        assertEquals(0.9, config.getDouble(Config.CELL_RIGHT_PADDING));

        assertEquals(new Rectangle(261,95,1332,803), config.getBounds(Config.CHART_BOUNDS));

        assertEquals(LineEnding.Windows, config.getEnum(Config.DEFAULT_LINE_ENDING));
        assertEquals(true, config.getBoolean(Config.EDITOR_TAB_EMULATED));
        assertEquals(8, config.getInt(Config.EDITOR_TAB_SIZE));
        assertEquals(1500, config.getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT));
        assertEquals(new FileChooserConfig("/tmp/excelExport.xls", new Dimension(505, 306)), config.getFileChooserConfig(Config.EXPORT_FILE_CHOOSER));

        assertEquals(new Font("Times New Roman", Font.ITALIC | Font.BOLD, 16), config.getFont(Config.FONT_EDITOR) );
        assertEquals(new Font("Andale Mono", Font.BOLD, 17), config.getFont(Config.FONT_TABLE) );

        assertEquals(true,config.getBoolean(Config.AUTO_SAVE));
        assertEquals(KdbMessageLimitAction.BLOCK,config.getEnum(Config.KDB_MESSAGE_SIZE_LIMIT_ACTION));
        assertEquals(1,config.getInt(Config.KDB_MESSAGE_SIZE_LIMIT_MB));
        assertEquals(9, config.getInt(Config.MAX_FRACTION_DIGITS));

        assertEquals(new FileChooserConfig("/tmp/file1.q", new Dimension(505, 306)), config.getFileChooserConfig(Config.OPEN_FILE_CHOOSER));
        assertEquals(false, config.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));
        assertEquals(false, config.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));
        assertEquals(false, config.getBoolean(Config.RSTA_INSERT_PAIRED_CHAR));
        assertEquals(true, config.getBoolean(Config.RSTA_WORD_WRAP));

        assertEquals(new FileChooserConfig("/tmp/file9.q", new Dimension(505, 306)), config.getFileChooserConfig(Config.SAVE_FILE_CHOOSER));

        assertEquals(true, config.getBoolean(Config.SESSION_INVALIDATION_ENABLED));
        assertEquals(11, config.getInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS));
        assertEquals(false, config.getBoolean(Config.SESSION_REUSE));
        assertEquals(false, config.getBoolean(Config.SHOW_SERVER_COMBOBOX));

    }

    private void testCustom(Config config) {
        assertEquals(ExecAllOption.Ignore, config.getEnum(Config.EXEC_ALL));
        assertEquals("javax.swing.plaf.nimbus.NimbusLookAndFeel", config.getString(Config.LOOK_AND_FEEL));
        assertEquals(500000, config.getInt(Config.MAX_CHARS_IN_RESULT));
        assertEquals(1024, config.getInt(Config.MAX_CHARS_IN_TABLE_CELL));
        assertEquals(List.of("file1", "/tmp/file2"), config.getStringArray(Config.MRU_FILES));
        assertEquals("94a43c9badd0e2e1c2bdf3e40ded8366", config.getString(Config.NOTES_HASH));
        assertEquals(25, config.getInt(Config.RESULT_TAB_COUNTS));

        
        ServerHistoryConfig serverHistoryConfig = new ServerHistoryConfig(30, List.of(
                "",
                "The.Last.Server",
                "server1",
                "Root2/innerServer2",
                "Folder/Sub-Folder/innerServer",
                "server1",
                "qqq/dd5",
                "dd2",
                "Root2/innerServer2",
                "Root2/innerServer2",
                "Folder/Sub-Folder/innerServer",
                "Folder/Sub-Folder/innerServer",
                "Root2/innerServer2",
                "Folder/Sub-Folder/innerServer"
                ));
        assertEquals(serverHistoryConfig, config.getServerHistoryConfig());

        TableConnExtractor extractor = config.getTableConnExtractor();
        assertEquals(10, extractor.getMaxConn());

        assertEquals(List.of("server", " host"), extractor.getConnWords());
        assertEquals(List.of("s", "server"), extractor.getHostWords());
        assertEquals(List.of("p", "port"), extractor.getPortWords());

        // Removed encoding from the config
        // assertEquals("UTF-8", config.getEncoding());

        assertEquals(ConfigVersion.LAST, config.getEnum(Config.CONFIG_VERSION));

    }

    private void testTokenColors(Config config) {
        assertEquals(new Color(0xfefefe), config.getEditorColors().get(EditorColorToken.BACKGROUND));

        TokenStyleMap colorTokenConfig = config.getTokenStyleConfig();;
        assertEquals(new Color(0x00ff00), colorTokenConfig.getColor(ColorToken.CHARVECTOR));
        assertEquals(new Color(0xffff00), colorTokenConfig.getColor(ColorToken.EOLCOMMENT));
        assertEquals(new Color(0xeeee00), colorTokenConfig.getColor(ColorToken.IDENTIFIER));
//        assertEquals(new Color(0x010101), colorTokenConfig.get(ColorToken.OPERATOR));
        assertEquals(new Color(0x0000ff), colorTokenConfig.getColor(ColorToken.BOOLEAN));
        assertEquals(new Color(0x0000fe), colorTokenConfig.getColor(ColorToken.BYTE));
        assertEquals(new Color(0x0000fd), colorTokenConfig.getColor(ColorToken.SHORT));
        assertEquals(new Color(0x0000fc), colorTokenConfig.getColor(ColorToken.LONG));
        assertEquals(new Color(0x0000fb), colorTokenConfig.getColor(ColorToken.REAL));
        assertEquals(new Color(0x0000fa), colorTokenConfig.getColor(ColorToken.INTEGER));
        assertEquals(new Color(0x0000f9), colorTokenConfig.getColor(ColorToken.FLOAT));
        assertEquals(new Color(0x0000f8), colorTokenConfig.getColor(ColorToken.TIMESTAMP));
        assertEquals(new Color(0x0000f7), colorTokenConfig.getColor(ColorToken.TIMESPAN));
        assertEquals(new Color(0x0000f6), colorTokenConfig.getColor(ColorToken.DATETIME));
        assertEquals(new Color(0x0000f5), colorTokenConfig.getColor(ColorToken.DATE));
        assertEquals(new Color(0x0000f4), colorTokenConfig.getColor(ColorToken.MONTH));
        assertEquals(new Color(0x0000f3), colorTokenConfig.getColor(ColorToken.MINUTE));
        assertEquals(new Color(0x0000f2), colorTokenConfig.getColor(ColorToken.SECOND));
        assertEquals(new Color(0x0000f1), colorTokenConfig.getColor(ColorToken.TIME));
        assertEquals(new Color(0x0000f0), colorTokenConfig.getColor(ColorToken.SYMBOL));
        assertEquals(new Color(0x0000ef), colorTokenConfig.getColor(ColorToken.KEYWORD));
        assertEquals(new Color(0x0000ee), colorTokenConfig.getColor(ColorToken.COMMAND));
//        assertEquals(new Color(0x0000ed), colorTokenConfig.get(ColorToken.SYSTEM));
//        assertEquals(new Color(0x020202), colorTokenConfig.get(ColorToken.WHITESPACE));
        assertEquals(new Color(0x030303), colorTokenConfig.getColor(ColorToken.DEFAULT));
        assertEquals(new Color(0x040404), colorTokenConfig.getColor(ColorToken.BRACKET));
        assertEquals(new Color(0xfe0101), colorTokenConfig.getColor(ColorToken.ERROR));
    }

}