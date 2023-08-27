package studio.ui;

import org.apache.commons.io.FileUtils;
import org.assertj.swing.core.EmergencyAbortListener;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import studio.core.Studio;
import studio.kdb.Server;
import studio.utils.LogErrors;
import studio.utils.MockConfig;
import studio.utils.log4j.EnvConfig;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static java.awt.event.KeyEvent.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.assertj.swing.core.KeyPressInfo.keyCode;

public class StudioTest extends AssertJSwingJUnitTestCase {

    protected FrameFixture frameFixture;
    protected StudioPanel panel;

    private static Path tmpConfigFolder;

    @BeforeClass
    public static void initLogErrors() {
        Studio.initLogger();
        LogErrors.init();
    }

    @BeforeClass
    public static void prepareConfig() throws IOException {
        tmpConfigFolder = Files.createTempDirectory("tmpKdbStudio");
        FileUtils.deleteDirectory(tmpConfigFolder.toFile());
        Files.createDirectories(tmpConfigFolder);
        EnvConfig.setBaseFolder(tmpConfigFolder.toString());
        System.out.println("Setup temporary folder for configs: " + tmpConfigFolder.toString());

        MockConfig.init();
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
        LogErrors.reset();

        studio.ui.I18n.setLocale(Locale.getDefault());
        panel = GuiActionRunner.execute( () -> new StudioPanel(Server.NO_SERVER, null) );
        frameFixture = new FrameFixture(robot(), panel.getFrame());

        List<EditorTab> editors = panel.getRootEditorsPanel().getAllEditors(false);
        Assert.assertEquals("expected to have one editor", 1, editors.size());
    }

    @Override
    protected void onTearDown() throws Exception {
        super.onTearDown();

        String[] errors = LogErrors.get();
        if (errors.length > 0) {
            fail("Error logs were detected:\n" + String.join("\n", errors));
        }
    }

    protected Rectangle getScreenBound(Component component) {
        return GuiActionRunner.execute(() -> new Rectangle(component.getLocationOnScreen(), component.getSize()) );
    }


    protected void split(String editorName, boolean verticallySplit) {
        int count = panel.getRootEditorsPanel().getAllEditors(false).size();
        JTextComponentFixture editor = frameFixture.textBox(editorName);
        editor.focus();
        String menuName = verticallySplit ? "Split down" : "Split right";
        frameFixture.menuItem(menuName).click();

        int newCount = panel.getRootEditorsPanel().getAllEditors(false).size();
        assertEquals("Number of editor should increase after split", count + 1, newCount);
    }


}
