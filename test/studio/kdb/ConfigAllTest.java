package studio.kdb;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.*;
import studio.ui.Util;
import studio.utils.LineEnding;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConfigAllTest {

    private static Path configPath;
    private static Config config;

    @BeforeAll
    public static void prepare() throws IOException {
        Util.setMockFitToScreen(true);
        configPath = Files.createTempDirectory("kdbStudioConfig");
        Path path = configPath.resolve("studio.properties");
        try (InputStream inputStream = ConfigAllTest.class.getClassLoader().getResourceAsStream("studio14.properties") ) {
            Files.copy(inputStream, path);
        }
        config = new Config(path);
    }

    @AfterAll
    public static void cleanup() throws IOException {
        FileUtils.deleteDirectory(configPath.toFile());
        Util.setMockFitToScreen(false);
    }

    @Test
    public void test() {
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

        assertEquals(new Font("Times New Roman", Font.PLAIN, 16), config.getFont(Config.FONT_EDITOR) );
        assertEquals(new Font("Andale Mono", Font.PLAIN, 17), config.getFont(Config.FONT_TABLE) );

        assertEquals(false,config.getBoolean(Config.AUTO_SAVE));
        assertEquals(KdbMessageLimitAction.ASK,config.getEnum(Config.KDB_MESSAGE_SIZE_LIMIT_ACTION));
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
        assertEquals(true, config.getBoolean(Config.SESSION_REUSE));
        assertEquals(false, config.getBoolean(Config.SHOW_SERVER_COMBOBOX));

    }

    @Test
    public void testCustom() {
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

        assertEquals(ConfigVersion.V2_0, config.getEnum(Config.CONFIG_VERSION));

    }

    @Test
    public void testTokenColors() {
        assertEquals(new Color(0xfefefe), config.getColor(Config.COLOR_BACKGROUND));

        ColorTokenConfig colorTokenConfig = config.getColorTokenConfig();;
        assertEquals(new Color(0x00ff00), colorTokenConfig.getColor(ColorToken.CHARVECTOR));
        assertEquals(new Color(0xffff00), colorTokenConfig.getColor(ColorToken.EOLCOMMENT));
        assertEquals(new Color(0xeeee00), colorTokenConfig.getColor(ColorToken.IDENTIFIER));
        assertEquals(new Color(0x010101), colorTokenConfig.getColor(ColorToken.OPERATOR));
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
        assertEquals(new Color(0x0000ed), colorTokenConfig.getColor(ColorToken.SYSTEM));
        assertEquals(new Color(0x020202), colorTokenConfig.getColor(ColorToken.WHITESPACE));
        assertEquals(new Color(0x030303), colorTokenConfig.getColor(ColorToken.DEFAULT));
        assertEquals(new Color(0x040404), colorTokenConfig.getColor(ColorToken.BRACKET));
        assertEquals(new Color(0xfe0101), colorTokenConfig.getColor(ColorToken.ERROR));
    }

}