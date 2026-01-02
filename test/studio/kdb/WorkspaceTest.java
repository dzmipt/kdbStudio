package studio.kdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.core.DefaultAuthenticationMechanism;
import studio.utils.LineEnding;

import java.awt.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class WorkspaceTest {

    private Workspace workspace;

    @BeforeEach
    public void setup() {
        ServerTreeNode parent = new ServerTreeNode("").add("testFolder");
        Server server = new Server("testName","someHost",1111, "", "",
                                    Color.red, "auth", false, parent);

        workspace = new Workspace();
        Workspace.Window w1 = workspace.addWindow(false);
        Workspace.Window w2 = workspace.addWindow(true);


        w1.addTab(true)
                .addServer(Config.getInstance().getServerByConnectionString("`:server.com:12345:user:password"))
                .addFilename("test.q");

        w2.addTab(false)
                .addServer(server)
                .addContent("Test content");

        w2.addTab(true)
                .addServer(server)
                .addFilename("test1.q")
                .addContent("Test content");

        w2.addTab(false)
                .addContent("another content");


        Workspace.Window w3 = workspace.addWindow(false);
        Workspace.Window left = w3.addLeft();
        Workspace.Window right = w3.addRight();
        w3.setVerticalSplit(true);

        left.addTab(false)
                .addServer(server)
                .addContent("left1");
        left.addTab(true)
                .addServer(server)
                .addContent("left2");

        Workspace.Window right1 = right.addLeft();
        Workspace.Window right2 = right.addRight();
        right1.setVerticalSplit(false);

        right1.addTab(true).addServer(server).addContent("right1");
        right2.addTab(true).addServer(server).addContent("right2");

        Workspace.Window w4 = workspace.addWindow(false);
        w4.addTab(false)
                .setLineEnding(LineEnding.Unix);
        w4.addTab(false)
                .setLineEnding(LineEnding.Windows);
        w4.addTab(true)
                .setLineEnding(LineEnding.MacOS9);

    }

    private void checkWorkspace(Workspace workspace) {
        assertEquals(1, workspace.getSelectedWindow());
        Workspace.Window[] windows = workspace.getWindows();
        assertEquals(4, windows.length);

        assertEquals(0, windows[0].getSelectedTab());
        assertEquals(1, windows[1].getSelectedTab());

        Workspace.Tab[] tabs = windows[0].getAllTabs();
        assertEquals(1, tabs.length);
        assertEquals("", tabs[0].getContent());
        assertEquals("test.q", tabs[0].getFilename());
        assertNull(tabs[0].getServerFullName());
        assertNotNull(tabs[0].getServerConnection());
        assertEquals("", tabs[0].getContent());
        assertEquals(DefaultAuthenticationMechanism.NAME, tabs[0].getServerAuth());
        assertFalse(tabs[0].isModified());

        tabs = windows[1].getAllTabs();
        assertEquals(3, tabs.length);

        assertNotNull(tabs[0].getContent());
        assertNotEquals("", tabs[0].getContent());
        assertNull(tabs[0].getFilename());
        assertEquals("testFolder/testName", tabs[0].getServerFullName());
        assertEquals("auth", tabs[0].getServerAuth());
        assertTrue(tabs[0].isModified());

        assertNotNull(tabs[2].getContent());
        assertNull(tabs[2].getFilename());
        assertNull(tabs[2].getServerFullName());
        assertNull(tabs[2].getServerConnection());
        assertTrue(tabs[2].isModified());


        assertFalse(windows[0].isSplit());
        assertFalse(windows[1].isSplit());
        assertTrue(windows[2].isSplit());
        assertTrue(windows[2].isVerticalSplit());

        Workspace.Window left = windows[2].getLeft();
        Workspace.Window right = windows[2].getRight();

        assertFalse(left.isSplit());
        assertTrue(right.isSplit());

        Workspace.Window right1 = right.getLeft();
        Workspace.Window right2 = right.getRight();

        assertFalse(right1.isSplit());
        assertFalse(right2.isSplit());

        tabs = windows[3].getAllTabs();
        assertEquals(LineEnding.Unix, tabs[0].getLineEnding());
        assertEquals(LineEnding.Windows, tabs[1].getLineEnding());
        assertEquals(LineEnding.MacOS9, tabs[2].getLineEnding());
    }

    @Test
    public void testGetter() {
        checkWorkspace(workspace);
    }

    @Test
    public void testSave() {
        Properties p = new Properties();
        workspace.save(p);

        Workspace workspace = new Workspace();
        workspace.load(p);
        checkWorkspace(workspace);
    }
}
