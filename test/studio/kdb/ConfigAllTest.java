package studio.kdb;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.KdbMessageLimitAction;
import studio.ui.Util;
import studio.utils.LineEnding;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        Credentials c = config.getDefaultCredentials("Plain");
        assertEquals("Plain", config.getDefaultAuthMechanism());
        assertEquals("testuser", c.getUsername());
        assertEquals("testpassword", c.getPassword());

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
        assertEquals(Config.ExecAllOption.Ignore, config.getExecAllOption());
        assertEquals("javax.swing.plaf.nimbus.NimbusLookAndFeel", config.getLookAndFeel());
        assertEquals(500000, config.getMaxCharsInResult());
        assertEquals(1024, config.getMaxCharsInTableCell());
        assertArrayEquals(new String[]{"file1", "/tmp/file2"}, config.getMRUFiles());
        assertEquals("94a43c9badd0e2e1c2bdf3e40ded8366", config.getNotesHash());
        assertEquals(25, config.getResultTabsCount());
        //serverHistory
        //version
        assertEquals(10, config.getTableMaxConnectionPopup());

        assertEquals("server, host", config.getConnColWords());
        assertEquals("s,server", config.getHostColWords());
        assertEquals("p,port", config.getPortColWords());
        assertEquals(30, config.getServerHistoryDepth());
        assertEquals("UTF-8", config.getEncoding());

    }

    @Test
    public void testTokenColors() {
        assertEquals(new Color(0x00ff00), config.getColor(Config.COLOR_CHARVECTOR));
        assertEquals(new Color(0xffff00), config.getColor(Config.COLOR_EOLCOMMENT));
        assertEquals(new Color(0xeeee00), config.getColor(Config.COLOR_IDENTIFIER));
        assertEquals(new Color(0x010101), config.getColor(Config.COLOR_OPERATOR));
        assertEquals(new Color(0x0000ff), config.getColor(Config.COLOR_BOOLEAN));
        assertEquals(new Color(0x0000fe), config.getColor(Config.COLOR_BYTE));
        assertEquals(new Color(0x0000fd), config.getColor(Config.COLOR_SHORT));
        assertEquals(new Color(0x0000fc), config.getColor(Config.COLOR_LONG));
        assertEquals(new Color(0x0000fb), config.getColor(Config.COLOR_REAL));
        assertEquals(new Color(0x0000fa), config.getColor(Config.COLOR_INTEGER));
        assertEquals(new Color(0x0000f9), config.getColor(Config.COLOR_FLOAT));
        assertEquals(new Color(0x0000f8), config.getColor(Config.COLOR_TIMESTAMP));
        assertEquals(new Color(0x0000f7), config.getColor(Config.COLOR_TIMESPAN));
        assertEquals(new Color(0x0000f6), config.getColor(Config.COLOR_DATETIME));
        assertEquals(new Color(0x0000f5), config.getColor(Config.COLOR_DATE));
        assertEquals(new Color(0x0000f4), config.getColor(Config.COLOR_MONTH));
        assertEquals(new Color(0x0000f3), config.getColor(Config.COLOR_MINUTE));
        assertEquals(new Color(0x0000f2), config.getColor(Config.COLOR_SECOND));
        assertEquals(new Color(0x0000f1), config.getColor(Config.COLOR_TIME));
        assertEquals(new Color(0x0000f0), config.getColor(Config.COLOR_SYMBOL));
        assertEquals(new Color(0x0000ef), config.getColor(Config.COLOR_KEYWORD));
        assertEquals(new Color(0x0000ee), config.getColor(Config.COLOR_COMMAND));
        assertEquals(new Color(0x0000ed), config.getColor(Config.COLOR_SYSTEM));
        assertEquals(new Color(0x020202), config.getColor(Config.COLOR_WHITESPACE));
        assertEquals(new Color(0x030303), config.getColor(Config.COLOR_DEFAULT));
        assertEquals(new Color(0x040404), config.getColor(Config.COLOR_BRACKET));
        assertEquals(new Color(0xfe0101), config.getColor(Config.COLOR_ERROR));
        assertEquals(new Color(0xfefefe), config.getColor(Config.COLOR_BACKGROUND));
    }
}