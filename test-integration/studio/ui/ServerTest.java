package studio.ui;


import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Pause;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.kdb.config.ServerConfig;
import studio.utils.QConnection;

import java.awt.*;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.junit.Assert.assertEquals;

public class ServerTest extends StudioTest {

    private static Server server1, server2;

    private final static String NAME1 = "bgServerTest1";
    private final static String NAME2 = "bgServerTest2";

    @Before
    public void initServers() {
        Color color1 = new Color(255, 127, 127);

        Color color2 = new Color(158, 215, 246);

        ServerConfig serverConfig = StudioWindow.CONFIG.getServerConfig();
        serverConfig.setRoot(new ServerTreeNode());
        server1 = new Server(NAME1, "testHost", 1111, "user", "password", color1,
                DefaultAuthenticationMechanism.NAME, false);
        server2 = new Server(NAME2, "testHost", 1111, "user", "password", color2,
                DefaultAuthenticationMechanism.NAME, false);
        serverConfig.addServers(false, server1, server2);

        GuiActionRunner.execute(StudioWindow::refreshAll);
    }

    @After
    public void removeServers() {
        StudioWindow.CONFIG.getServerConfig().setRoot(new ServerTreeNode());
    }

    @AfterClass
    public static void resetMock() {
        ColorChooser.mock(null);
        ServerEditor.resetMock();
    }

    @Test
    public void testGetQConnection() {
        QConnection conn = server1.getConnection();
        assertEquals("`:testHost:1111:user:password",  conn.toString());
        assertEquals("`:testHost:1111",  conn.toString(false));

        conn = conn.changeTLS(true);
        assertEquals("`:tcps://testHost:1111:user:password",  conn.toString());
        assertEquals("`:tcps://testHost:1111",  conn.toString(false));

        conn = conn.changeUserPassword("u","p");
        assertEquals("`:tcps://testHost:1111:u:p",  conn.toString());
        assertEquals("`:tcps://testHost:1111",  conn.toString(false));
    }

    @Test
    public void testBackgroundColor() {
        JComboBoxFixture serverComboBox = frameFixture.comboBox("serverDropDown");
        serverComboBox.selectItem(server1.getName());

        JTextComponentFixture editor = frameFixture.textBox("editor1");

        Color bgColor = execute(() -> editor.target().getBackground());
        assertEquals(server1.getBackgroundColor(), bgColor);
    }

    @Test
    public void testChangeBackgroundColor() {
        Color newColor = new Color (171, 255, 171);
        Server newServer = new Server(server2.getName(),
                server2.getConnection(), server2.getAuthenticationMechanism(),
                newColor, server2.getParent(), server2.isFlipTLS());

        JComboBoxFixture serverComboBox = frameFixture.comboBox("serverDropDown");
        serverComboBox.selectItem(server2.getName());


        ServerEditor.mock(newServer);
        frameFixture.menuItemWithPath("Server", "Edit").click();

        JTextComponentFixture editor = frameFixture.textBox("editor1");
        Color newBgColor = execute(() -> editor.target().getBackground());
        assertEquals(newColor, newBgColor);
    }

    @Test
    public void testAddNewServer() {
        String name = "testNewServer";
        Server newServer = new Server("testNewServer", "testHost", 1111, "aUser", "pwd",
                new Color(170,200,100), DefaultAuthenticationMechanism.NAME, false );

        ServerEditor.mock(newServer);
        frameFixture.menuItemWithPath("Server", "Add...").click();


        Pause.pause(2000);
        Server serverInConfig = GuiActionRunner.execute(()-> StudioWindow.CONFIG.getServerConfig().getServer(name));
        assertEquals(newServer, serverInConfig);


    }

}
