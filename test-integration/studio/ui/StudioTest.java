package studio.ui;

import org.apache.commons.io.FileUtils;
import org.assertj.swing.core.KeyPressInfo;
import org.assertj.swing.core.Robot;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.core.Studio;
import studio.utils.log4j.EnvConfig;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class StudioTest extends AssertJSwingJUnitTestCase {

    private FrameFixture window;

    private static Path tmpConfigFolder;

    @BeforeClass
    public static void prepareConfig() throws IOException {
        tmpConfigFolder = Files.createTempDirectory("tmpKdbStudio");
        EnvConfig.setBaseFolder(tmpConfigFolder.toString());
    }

    @AfterClass
    public static void cleanupConfig() throws IOException {
        FileUtils.deleteDirectory(tmpConfigFolder.toFile());
    }

    @Override
    protected void onSetUp() {
        Robot robot = robot();
        GuiActionRunner.execute( () -> Studio.main(new String[0]) );

        StudioPanel[] panels = GuiActionRunner.execute(() -> StudioPanel.getPanels());

        DialogFixture dialog = WindowFinder.findDialog(HelpDialog.class).using(robot);
        dialog.pressAndReleaseKeys(KeyEvent.VK_ESCAPE);

        Assert.assertEquals("expected to have one panel", 1, panels.length);
        window = new FrameFixture(robot, panels[0].getFrame());

        List<EditorTab> editors = panels[0].getActiveEditor().getEditorsPanel().getAllEditors(false);
        Assert.assertEquals("expected to have one editor", 1, panels.length);
        editors.get(0).getTextArea().setName("qEditor");
    }

    private void type(JTextComponentFixture tb, String text) {
        for (int i = 0; i < text.length(); ++i) {
            int code = text.charAt(i);
            if (code >= 'a' && code <= 'z') {
                tb.pressAndReleaseKey(KeyPressInfo.keyCode(code - 'a' + 'A'));
            } else {
                tb.pressAndReleaseKey(KeyPressInfo.keyCode(code));
            }
        }
    }

    @Test
    public void test() throws Exception {
        JTextComponentFixture tb = window.textBox("qEditor");
        tb.requireEmpty();
        tb.robot().pressAndReleaseKey(KeyEvent.VK_A);
        tb.requireText("a");
        type(tb, "bc\ndef");
        tb.requireText("abc\ndef");
    }

}
