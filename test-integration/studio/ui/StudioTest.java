package studio.ui;

import org.apache.commons.io.FileUtils;
import org.assertj.swing.core.EmergencyAbortListener;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.kdb.Server;
import studio.utils.log4j.EnvConfig;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static java.awt.event.KeyEvent.*;
import static org.assertj.swing.core.KeyPressInfo.keyCode;

public class StudioTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;
    protected StudioPanel panel;

    private static Path tmpConfigFolder;


    @BeforeClass
    public static void prepareConfig() throws IOException {
        tmpConfigFolder = Files.createTempDirectory("tmpKdbStudio");
        FileUtils.deleteDirectory(tmpConfigFolder.toFile());
        Files.createDirectories(tmpConfigFolder);
        EnvConfig.setBaseFolder(tmpConfigFolder.toString());
        System.out.println("Setup temporary folder for configs: " + tmpConfigFolder.toString());
    }

    @AfterClass
    public static void cleanupConfig() throws IOException {
        FileUtils.deleteDirectory(tmpConfigFolder.toFile());
    }

    //Emergency key is Ctrl+Shift+Q
    @BeforeClass
    public static void setupEmergency() {
        EmergencyAbortListener.registerInToolkit()
                .keyCombination(keyCode(VK_Q).modifiers(CTRL_MASK, SHIFT_MASK));

    }

    @Override
    protected void onSetUp() {
        Robot robot = robot();

        studio.ui.I18n.setLocale(Locale.getDefault());
        panel = GuiActionRunner.execute( () -> new StudioPanel(Server.NO_SERVER, null) );
        window = new FrameFixture(robot, panel.getFrame());

        List<EditorTab> editors = panel.getRootEditorsPanel().getAllEditors(false);
        Assert.assertEquals("expected to have one editor", 1, editors.size());
    }

    @Test
    public void test() {
        JTextComponentFixture tb = window.textBox("editor1");
        tb.requireEmpty();
        tb.enterText("a");
        tb.requireText("a");
        tb.enterText("bc\ndef");
        tb.requireText("abc\ndef");
    }

    private Rectangle getScreenBound(Component component) {
        return new Rectangle(component.getLocationOnScreen(), component.getSize());
    }

    @Test
    public void splitTest() {
        JTextComponentFixture editor1 = window.textBox("editor1");
        Rectangle bound = GuiActionRunner.execute(() -> getScreenBound(editor1.target()));

        window.menuItem("Split right").click();
        List<EditorTab> editors = panel.getRootEditorsPanel().getAllEditors(false);
        Assert.assertEquals("expected to have one editor", 2, editors.size());

        JTextComponentFixture editor2 = window.textBox("editor2");

        Rectangle bound1 = GuiActionRunner.execute(() -> getScreenBound(editor1.target()));
        Rectangle bound2 = GuiActionRunner.execute(() -> getScreenBound(editor2.target()));

        Assert.assertTrue(bound1.x + bound1.width < bound2.x);
        Assert.assertTrue(bound1.y == bound2.y );
        Assert.assertTrue(bound1.height == bound2.height );
        Assert.assertTrue(Math.abs(bound.y - bound1.y) < 10 );
        Assert.assertTrue(Math.abs(bound1.x - bound.x) < 10 );
        Assert.assertTrue(bound1.width + bound2.width < bound.width);

    }

}
