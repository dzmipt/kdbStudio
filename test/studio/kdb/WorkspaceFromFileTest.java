package studio.kdb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.kdb.config.ColorMap;
import studio.kdb.config.EditorColorToken;
import studio.kdb.config.WorkspaceToJsonConverter;
import studio.ui.action.WorkspaceSaver;
import studio.utils.LineEnding;
import studio.utils.MockConfig;
import studio.utils.QConnection;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class WorkspaceFromFileTest {

    private Properties propertiesFromFile;
    private Workspace workspace;
    private static Server server1, server2, server3, server5;

    private static Color bgColor = new Color(14,15,16);
    private static Color bgColor2 = new Color(24,25,26);

    @BeforeAll
    public static void initConfig() throws IOException {
        MockConfig.mock();
        ColorMap colorMap = new ColorMap(Config.getInstance().getEditorColors());
        colorMap.put(EditorColorToken.BACKGROUND, bgColor);
        Config.getInstance().setEditorColors(colorMap);

        ServerTreeNode root = Config.getInstance().getServerTree();;

        server1 = new Server("server1", QConnection.get("`:tcps://serverHost1:2000:user:password"),
                "Plain", bgColor2, root);
        server2 = new Server("server2", QConnection.get("`:serverHost2:2000"),
                "Plain", bgColor2, root);
        server3 = new Server("server3", QConnection.get("`:serverHost2:1100"),
                "dzAuth", bgColor2, root);
        server5 = new Server("server5", QConnection.get("`:serverHost4:2100:user1:password1"),
                "Plain", bgColor2, root.add("folder"));
        Config.getInstance().getServerConfig().addServers(false, server1, server2, server3, server5);
    }

    @BeforeEach
    public void setTLSOptions() {
        Config.getInstance().setBoolean(Config.TRY_TLS_CONNECTION_FIRST, false);
        Config.getInstance().setBoolean(Config.FAILOVER_BETWEEN_TLS_AND_TCP_CONNECTIONS, false);
    }

    @BeforeEach
    public void load() throws IOException {
        try (InputStream inputStream =
                     WorkspaceFromFileTest.class.getClassLoader().getResourceAsStream("workspace.properties")) {

            propertiesFromFile = new Properties();
            propertiesFromFile.load(inputStream);
        }
    }

    @BeforeEach
    public void init() {
        workspace = new Workspace();
        workspace
                .addWindow(false)
                .setLocation(new Rectangle(-1913, 234, 1792, 881))
                .setServerListBounds(new Rectangle(970, 430, 300, 400))
                .setResultDividerLocation(0.9)
                .addLeft(false, 0.5)
                .addTab(false)
                .setCaret(14)
                .addContent("some test\nnew line")
                .setLineEnding(LineEnding.MacOS9)
                .setModified(true)
                .addFilename("/folder/file.q")
                .addServer(server1);

        workspace.getWindows()[0].getLeft()
                .addTab(true)
                .setCaret(7)
                .addContent("oneMore")
                .setLineEnding(LineEnding.Unix)
                .setModified(true)
                .addServer(server2);

        workspace.getWindows()[0]
                .addRight()
                .addLeft(true, 0.7)
                .addTab(true)
                .setCaret(5)
                .addContent("right")
                .setLineEnding(LineEnding.Windows)
                .setModified(true)
                .addServer(server3);

        workspace.getWindows()[0]
                .getRight()
                .addRight()
                .addTab(true)
                .setCaret(9)
                .addContent("rightDown")
                .setLineEnding(LineEnding.MacOS9)
                .setModified(true)
                .addServer(new Server("", QConnection.get("`:serverHost3:1300"),
                        "Plain", bgColor));


        workspace
                .addWindow(true)
                .setLocation(new Rectangle(224, 126, 1100, 1008))
                .setServerListBounds(new Rectangle(97, 43, 320, 410))
                .setResultDividerLocation(0.4)
                .addTab(true)
                .setCaret(9)
                .addContent("newWindow")
                .setLineEnding(LineEnding.MacOS9)
                .setModified(false)
                .addServer(server5);
    }

    private void assertWorkspace(Workspace actualWorkspace) {
        assertEquals(workspace, actualWorkspace);

        assertSame(server1, actualWorkspace.getWindows()[0].getLeft().getAllTabs()[0].getServer());
        assertSame(server2, actualWorkspace.getWindows()[0].getLeft().getAllTabs()[1].getServer());
        assertSame(server3, actualWorkspace.getWindows()[0].getRight().getLeft().getAllTabs()[0].getServer());
        assertSame(server5, actualWorkspace.getWindows()[1].getAllTabs()[0].getServer());

        assertEquals(bgColor,
                actualWorkspace.getWindows()[0].getRight().getRight().getAllTabs()[0].getServer().getBackgroundColor());
    }

    @Test
    public void testWorkspaceToJsonConverter() {
        WorkspaceToJsonConverter converter = new WorkspaceToJsonConverter(propertiesFromFile);
        Workspace workspaceFromConverter = converter.load();
        assertWorkspace(workspaceFromConverter);
    }

    @Test
    public void testWorkspaceSaverJsonLoad() throws IOException {
        try (InputStream inputStream =
                     WorkspaceFromFileTest.class.getClassLoader().getResourceAsStream("workspace.json")) {

            JsonObject json = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
            Workspace workspaceFromJson = WorkspaceSaver.fromJson(json);
            assertWorkspace(workspaceFromJson);
        }
    }

    @Test
    public void testWorkspaceSaverToJson() {
        JsonObject json = WorkspaceSaver.toJson(workspace);
        Workspace workspaceConverted = WorkspaceSaver.fromJson(json);
        assertWorkspace(workspaceConverted);
    }

    @Test
    public void testLoadTCPConnection() {
        Workspace workspace = new Workspace();
        workspace.addWindow(true)
                .addTab(true)
                    .addServer("", "`:test:12", "Plain");
        JsonObject json = WorkspaceSaver.toJson(workspace);

        Config.getInstance().setBoolean(Config.TRY_TLS_CONNECTION_FIRST, true);
        Config.getInstance().setBoolean(Config.FAILOVER_BETWEEN_TLS_AND_TCP_CONNECTIONS, true);
        workspace = WorkspaceSaver.fromJson(json);

        Server server = workspace.getWindows()[0].getAllTabs()[0].getServer();
        assertEquals("`:test:12", server.getConnectionStringWithPwd());
        assertTrue(server.isFlipTLS());

    }

}