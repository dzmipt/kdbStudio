package studio.kdb;

import org.junit.jupiter.api.Test;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.ExecAllOption;
import studio.utils.LineEnding;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigUpgradeTest {

    @Test
    public void testLoadingConfig13() throws IOException, URISyntaxException {
        byte[] content = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("studio13.properties").toURI()));

        File tmpFile = File.createTempFile("studioforkdb", ".properties");
        tmpFile.deleteOnExit();
        Files.write(tmpFile.toPath(), content);
        Config config = new Config(tmpFile.toPath());

        assertEquals(ActionOnExit.NOTHING, config.getEnum(Config.ACTION_ON_EXIT));
        assertEquals("defU", config.getDefaultCredentials(DefaultAuthenticationMechanism.NAME).getUsername());
        assertEquals("defP", config.getDefaultCredentials(DefaultAuthenticationMechanism.NAME).getPassword());
        assertEquals(DefaultAuthenticationMechanism.NAME, config.getDefaultAuthMechanism());
        assertTrue(config.getBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN));
        assertEquals(250, config.getInt(Config.CELL_MAX_WIDTH));
        assertEquals(0.9, config.getDouble(Config.CELL_RIGHT_PADDING));
        assertEquals(LineEnding.Windows, config.getEnum(Config.DEFAULT_LINE_ENDING));
        assertTrue(config.getBoolean(Config.EDITOR_TAB_EMULATED));
        assertEquals(15, config.getInt(Config.EDITOR_TAB_SIZE));
        assertEquals(1500, config.getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT));
        assertEquals(ExecAllOption.Ignore, config.getEnum(Config.EXEC_ALL));
        assertEquals(new Font("Times New Roman", Font.PLAIN, 18),config.getFont(Config.FONT_EDITOR));
        assertEquals(new Font("Arial", Font.PLAIN, 14),config.getFont(Config.FONT_TABLE));
        assertTrue(config.getBoolean(Config.AUTO_SAVE));
        assertEquals("javax.swing.plaf.nimbus.NimbusLookAndFeel",config.getString(Config.LOOK_AND_FEEL));
        assertEquals(500000, config.getInt(Config.MAX_CHARS_IN_RESULT));
        assertEquals(1024, config.getInt(Config.MAX_CHARS_IN_TABLE_CELL));
        assertEquals(9, config.getInt(Config.MAX_FRACTION_DIGITS));
        assertEquals(20, config.getInt(Config.RESULT_TAB_COUNTS));
        assertFalse(config.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));
        assertFalse(config.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));
        assertFalse(config.getBoolean(Config.RSTA_INSERT_PAIRED_CHAR));
        assertTrue(config.getBoolean(Config.RSTA_WORD_WRAP));
        assertTrue(config.getBoolean(Config.SESSION_INVALIDATION_ENABLED));
        assertEquals(10, config.getInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS));
        assertFalse(config.getBoolean(Config.SESSION_REUSE));
        assertFalse(config.getBoolean(Config.SHOW_SERVER_COMBOBOX));


        ServerTreeNode serverTree = config.getServerTree();
        assertEquals(4, serverTree.getChildCount());

        ServerTreeNode folder = serverTree.getChild(1);
        assertEquals("Folder", folder.getFolder());
        assertEquals(1, folder.getChildCount());
        ServerTreeNode subFolder = folder.getChild(0);
        assertEquals("Sub-Folder", subFolder.getFolder());
        assertEquals(1, subFolder.getChildCount());
        ServerTreeNode root2 = serverTree.getChild(2);
        assertEquals("Root2", root2.getFolder());
        assertEquals(1, root2.getChildCount());

        Server server1 = serverTree.getChild(0).getServer();
        Server inner = subFolder.getChild(0).getServer();
        Server inner2 = root2.getChild(0).getServer();
        Server last = serverTree.getChild(3).getServer();

        String defAuth = DefaultAuthenticationMechanism.NAME;
        assertEquals(new Server("server1", "serverHost", 2000,
                    "user", "password", new Color(0x99ffff), defAuth, true), server1);
        assertEquals(new Color(0x99ffff), server1.getBackgroundColor());

        assertEquals(new Server("innerServer", "anotherHost", 3000,
                        "", "", Color.WHITE, defAuth, false),
                     inner);

        assertEquals(new Server("innerServer2", "host2", 4000,
                        "", "", Color.WHITE, defAuth, false),
                inner2);

        assertEquals(new Server("The.Last.Server", "lastServer", 5000,
                        "", "", Color.WHITE, "customAuth", false),
                last);

    }
}
