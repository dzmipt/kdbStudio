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
import studio.kdb.Server;
import studio.utils.log4j.EnvConfig;

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
    private static boolean first = true;


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
//        robot.settings().delayBetweenEvents(500);


//        if (first) {
//            GuiActionRunner.execute( () -> Studio.main(new String[0]) );
//            DialogFixture dialog = WindowFinder.findDialog(HelpDialog.class).using(robot);
//            dialog.pressAndReleaseKeys(KeyEvent.VK_ESCAPE);
//            first = false;
//            StudioPanel[] panels = GuiActionRunner.execute(() -> StudioPanel.getPanels());
//            Assert.assertEquals("expected to have one panel", 1, panels.length);
//
//            FrameFixture initWindow = new FrameFixture(robot, panel.getFrame());
//            initWindow.pressAndReleaseKey(keyCode(VK_N).modifiers(controlOrCommandMask(), SHIFT_MASK));
//            panels = GuiActionRunner.execute(() -> StudioPanel.getPanels());
//            Assert.assertEquals("expected to have one panel", 2, panels.length);
//            panel = panels[1];
//        }

        studio.ui.I18n.setLocale(Locale.getDefault());
        panel = GuiActionRunner.execute( () -> new StudioPanel(Server.NO_SERVER, null) );
        window = new FrameFixture(robot, panel.getFrame());

        List<EditorTab> editors = panel.getRootEditorsPanel().getAllEditors(false);
        Assert.assertEquals("expected to have one editor", 1, editors.size());
        editors.get(0).getTextArea().setName("qEditor");
    }

//    @Test
    public void test() {
        JTextComponentFixture tb = window.textBox("qEditor");
        tb.requireEmpty();
//        tb.pressAndReleaseKey(KeyEvent.VK_A);
        tb.enterText("a");
        tb.requireText("a");
        tb.enterText("bc\ndef");
        tb.requireText("abc\ndef");
    }

//    @Test
    public void splitTest() {
        window.menuItem("splitRight").click();
        List<EditorTab> editors = panel.getRootEditorsPanel().getAllEditors(false);
        Assert.assertEquals("expected to have one editor", 2, editors.size());
    }

}
