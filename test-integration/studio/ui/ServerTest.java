package studio.ui;


import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JComboBoxFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Config;
import studio.kdb.Server;

import java.awt.*;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.junit.Assert.assertEquals;

public class ServerTest extends StudioTest {

    private static Server server1, server2;

    private final static String NAME1 = "bgServerTest1";
    private final static String NAME2 = "bgServerTest2";

    @BeforeClass
    public static void initServer() {
        Color color1 = new Color(255, 127, 127);

        Color color2 = new Color(158, 215, 246);

        server1 = new Server(NAME1, "testHost", 1111, "user", "password", color1,
                DefaultAuthenticationMechanism.NAME, false);
        server2 = new Server(NAME2, "testHost", 1111, "user", "password", color2,
                DefaultAuthenticationMechanism.NAME, false);
        Config.getInstance().addServers(false, server1, server2);
    }

    @AfterClass
    public static void resetMock() {
        ColorChooser.mock(null);
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
        JComboBoxFixture serverComboBox = frameFixture.comboBox("serverDropDown");
        serverComboBox.selectItem(server2.getName());

        frameFixture.menuItemWithPath("Server", "Edit").click();

        DialogFixture serverDialog = new DialogFixture(robot(), robot().finder().findByType(EditServerForm.class));
        JTextComponentFixture sample = serverDialog.textBox("sampleTextOnBackground");
        Color bgColor = execute(() -> sample.target().getBackground());
        assertEquals(server2.getBackgroundColor(), bgColor);

        Color newColor = new Color (171, 255, 171);
        ColorChooser.mock(newColor);
        serverDialog.button("editColor").click();
        bgColor = execute(() -> sample.target().getBackground());
        assertEquals(newColor, bgColor);

        serverDialog.button("okButton").click();

        JTextComponentFixture editor = frameFixture.textBox("editor1");
        bgColor = execute(() -> editor.target().getBackground());
        assertEquals(newColor, bgColor);

    }

}
