package studio.ui;

import org.apache.commons.io.FileUtils;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.EmergencyAbortListener;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import studio.core.Studio;
import studio.kdb.Server;
import studio.utils.LogErrors;
import studio.utils.Lookup;
import studio.utils.MockConfig;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
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
import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.util.Platform.controlOrCommandKey;

@GUITest
@RunWith(GUITestRunner.class)
public class StudioTest extends AssertJSwingJUnitTestCase {

    protected FrameFixture frameFixture;
    protected StudioWindow studioWindow;

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
        studioWindow = execute( () -> new StudioWindow(Server.NO_SERVER, null) );
        frameFixture = new FrameFixture(robot(), studioWindow);

        Assert.assertEquals("expected to have one editor", 1, getEditors().size());
    }

    @Override
    protected void onTearDown() throws Exception {
        super.onTearDown();

        String[] errors = LogErrors.get();
        if (errors.length > 0) {
            fail("Error logs were detected:\n" + String.join("\n", errors));
        }
    }

    protected List<RSyntaxTextArea> getEditors() {
        return getEditors(studioWindow);
    }

    protected List<RSyntaxTextArea> getEditors(StudioWindow studioWindow) {
        return Lookup.getChildren(studioWindow, RSyntaxTextArea.class);
    }

    protected Rectangle getScreenBound(Component component) {
        return execute(() -> new Rectangle(component.getLocationOnScreen(), component.getSize()) );
    }


    protected void addTab(String editorName) {
        JTextComponentFixture editorFixture = frameFixture.textBox(editorName);

        JTabbedPane tabbedPane = Lookup.getParent(editorFixture.target(), JTabbedPane.class);
        JTabbedPaneFixture tabbedFixture = new JTabbedPaneFixture(robot(), tabbedPane);

        int count = getEditors().size();
        int tabCount = execute(() -> tabbedPane.getTabCount());

        editorFixture.focus();
        editorFixture.pressAndReleaseKey(keyCode(VK_N).modifiers(controlOrCommandKey()));

        int newCount = getEditors().size();
        int newTabCount = execute(() -> tabbedPane.getTabCount());

        assertEquals("Number of tabs should increase by 1", tabCount + 1, newTabCount);
        assertEquals("Number of editors should increase by 1", count + 1, newCount);
    }

    protected void split(String editorName, boolean verticallySplit) {
        int count = getEditors().size();
        frameFixture.textBox(editorName).focus();
        String menuName = verticallySplit ? "Split down" : "Split right";
        frameFixture.menuItem(menuName).click();

        int newCount = getEditors().size();
        assertEquals("Number of editor should increase after split", count + 1, newCount);
    }


}
