package studio.ui;


import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Condition;
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

//    @Ignore("I think AssertJ-Swing doesn't work well with modal dialogs")
    @Test
    public void testChangeBackgroundColor() {
        JComboBoxFixture serverComboBox = frameFixture.comboBox("serverDropDown");
        serverComboBox.selectItem(server2.getName());

        frameFixture.menuItemWithPath("Server", "Edit").click();

        DialogFixture serverDialog = new DialogFixture(robot(), robot().finder().findByType(EditServerForm.class));
        JTextComponentFixture sample = serverDialog.textBox("sampleTextOnBackground");
        final Color bgColor = execute(() -> sample.target().getBackground());
        assertEquals(server2.getBackgroundColor(), bgColor);

        Color newColor = new Color (171, 255, 171);
        ColorChooser.mock(newColor);
        serverDialog.button("editColor").click();
        Pause.pause(new Condition("wait until sample background color is changed") {
            @Override
            public boolean test() {
                return ! sample.target().getBackground().equals(bgColor);
            }
        }, 1000 );

        Color newBgColor = execute(() -> sample.target().getBackground());
        assertEquals(newColor, newBgColor);

        serverDialog.button("okButton").click();

        JTextComponentFixture editor = frameFixture.textBox("editor1");
        Pause.pause(new Condition("wait until edito background color is changed") {
            @Override
            public boolean test() {
                return ! editor.target().getBackground().equals(bgColor);
            }
        }, 1000 );


        newBgColor = execute(() -> editor.target().getBackground());
        assertEquals(newColor, newBgColor);

    }

}
