package studio.ui;

import org.apache.commons.io.FileUtils;
import org.assertj.swing.annotation.GUITest;
import org.assertj.swing.core.EmergencyAbortListener;
import org.assertj.swing.core.MouseButton;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.runner.GUITestRunner;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Condition;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import studio.core.Studio;
import studio.kdb.Server;
import studio.ui.dndtabbedpane.DraggableTabbedPane;
import studio.utils.LogErrors;
import studio.utils.Lookup;
import studio.utils.MockConfig;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.awt.event.KeyEvent.*;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.assertj.swing.core.KeyPressInfo.keyCode;
import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;
import static org.assertj.swing.util.Platform.controlOrCommandMask;
import static org.junit.Assert.assertNotEquals;

@GUITest
@RunWith(GUITestRunner.class)
abstract public class StudioTest extends AssertJSwingJUnitTestCase {

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

    protected List<DraggableTabbedPane> getEditorsPanes() {
        return Lookup.getChildren(studioWindow, DraggableTabbedPane.class, dtp -> dtp.getDragID().equals("Editor"));
    }

    protected Rectangle getScreenBound(Component component) {
        return execute(() -> new Rectangle(component.getLocationOnScreen(), component.getSize()) );
    }


    protected int getTabCount(JTabbedPane tabbedPane) {
        return execute(() -> tabbedPane.getTabCount());
    }

    protected void addTab(String editorName) {
        JTextComponentFixture editorFixture = frameFixture.textBox(editorName);

        JTabbedPane tabbedPane = Lookup.getParent(editorFixture.target(), JTabbedPane.class);

        int count = getEditors().size();
        int tabCount = getTabCount(tabbedPane);

        editorFixture.focus();
        editorFixture.pressAndReleaseKey(keyCode(VK_N).modifiers(controlOrCommandMask()));

        int newCount = getEditors().size();
        int newTabCount = getTabCount(tabbedPane);

        assertEquals("Number of tabs should increase by 1", tabCount + 1, newTabCount);
        assertEquals("Number of editors should increase by 1", count + 1, newCount);
    }

    protected void clickTab(String editorName, MouseButton button) {
        RSyntaxTextArea textArea = Lookup.getChild(studioWindow, RSyntaxTextArea.class, Lookup.byName(editorName));
        JTabbedPane tabbedPane = Lookup.getParent(textArea, JTabbedPane.class);

        Rectangle bounds = execute(() -> {
            int count = tabbedPane.getTabCount();
            for (int index=0; index<count; index++) {
                int num = Lookup.getChildren(tabbedPane.getComponentAt(index), RSyntaxTextArea.class, Lookup.byName(editorName)).size();
                if (num == 1) return tabbedPane.getBoundsAt(index);
            }
            return null;
        });
        assertNotEquals("Didn't find editor in the JTabbedPane ?? ", null, bounds);

        Point center = new Point((int)bounds.getCenterX(), (int)bounds.getCenterY());
        robot().click(tabbedPane, center, button, 1 );
    }


    protected void clickTab(String editorName) {
        clickTab(editorName, MouseButton.LEFT_BUTTON);
    }

    protected void closeTabWithMouse(String editorName) {
        RSyntaxTextArea textArea = Lookup.getChild(studioWindow, RSyntaxTextArea.class, Lookup.byName(editorName));
        JTabbedPane tabbedPane = Lookup.getParent(textArea, JTabbedPane.class);

        int editorsPanelCount = getEditorsPanes().size();
        int editorsCount = getEditors().size();

        if (editorsCount == 1) {
//            fail("Closing the last tab is not yet implemented in the StudioTest");
        }

        int tabCount = getTabCount(tabbedPane);
        clickTab(editorName, MouseButton.MIDDLE_BUTTON);

        if (tabCount > 1) {
            int newTabCount = getTabCount(tabbedPane);
            assertEquals("Number of tabs should decrease by 1", tabCount - 1, newTabCount);
        } else {
            if (editorsPanelCount > 1) {
                int newEditorsPanelCount = getEditorsPanes().size();
                assertEquals("Number of editors JTabbedPane should decrease by 1", editorsPanelCount - 1, newEditorsPanelCount);
            }
        }

        int newEditorsCount = getEditors().size();
        assertEquals("Number of editors should decrease by 1", editorsCount - 1, newEditorsCount);
    }

    protected void split(String editorName, boolean verticallySplit) {
        int count = getEditors().size();
        frameFixture.textBox(editorName).focus();
        String menuName = verticallySplit ? "Split down" : "Split right";
        frameFixture.menuItem(menuName).click();

        int newCount = getEditors().size();
        assertEquals("Number of editor should increase after split", count + 1, newCount);
    }

    protected void openFile(File file) {
        FileChooser.mock(file);
        frameFixture.menuItem("Open...").click();
    }

    protected void setServerConnectionText(String serverConnection) {
        frameFixture.textBox("serverEntryTextField").enterText(serverConnection).pressAndReleaseKey(keyCode(VK_ENTER));
    }

    protected void waitForQueryExecution(Runnable runQuery, long durationInSeconds) {
        JTabbedPaneFixture tabbedPaneFixture = frameFixture.tabbedPane("ResultTabbedPane");
        int count = getTabCount(tabbedPaneFixture.target());

        runQuery.run();

        pause(new Condition("mock query execution") {
            @Override
            public boolean test() {
                int newCount = getTabCount(tabbedPaneFixture.target());
                return newCount > count;
            }
        }, timeout(durationInSeconds, TimeUnit.SECONDS));


        int newCount = getTabCount(tabbedPaneFixture.target());
        assertEquals("Expect that one more result tab is added", count+1, newCount);
    }

    protected void waitForQueryExecution(Runnable runQuery) {
        waitForQueryExecution(runQuery,1);
    }

}
