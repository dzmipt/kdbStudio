package studio.kdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.utils.LineEnding;
import studio.utils.QConnection;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkspaceFromFileTest {

    private Properties propertiesFromFile;
    private Workspace workspace;

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
        ServerTreeNode root = new ServerTreeNode();
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
                .addServer(new Server("server1", new QConnection("`:tcps://serverHost1:2000:user:password"),
                        "Plain", Color.BLACK, root));

        workspace.getWindows()[0].getLeft()
                .addTab(true)
                .setCaret(7)
                .addContent("oneMore")
                .setLineEnding(LineEnding.Unix)
                .setModified(true)
                .addServer(new Server("server2", new QConnection("`:serverHost2:2000"),
                        "Plain", Color.BLACK, root));

        workspace.getWindows()[0]
                .addRight()
                .addLeft(true, 0.7)
                .addTab(true)
                .setCaret(5)
                .addContent("right")
                .setLineEnding(LineEnding.Windows)
                .setModified(true)
                .addServer(new Server("server3", new QConnection("`:serverHost2:1100"),
                        "dzAuth", Color.BLACK, root));

        workspace.getWindows()[0]
                .getRight()
                .addRight()
                .addTab(true)
                .setCaret(9)
                .addContent("rightDown")
                .setLineEnding(LineEnding.MacOS9)
                .setModified(true)
                .addServer(new Server("", new QConnection("`:serverHost3:1300"),
                        "Plain", Color.BLACK));


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
                .addServer(new Server("server5", new QConnection("`:serverHost4:2100:user1:password1"),
                        "Plain", Color.BLACK, root.add("folder")));
    }

    @Test
    public void testLoad() {
        Workspace workspaceFromFile = new Workspace();
        workspaceFromFile.load(propertiesFromFile);
        assertEquals(workspace, workspaceFromFile);
    }

    @Test
    public void testSave() {
        Properties properties = new Properties();
        workspace.save(properties);
        assertEquals(propertiesFromFile, properties);
    }
}