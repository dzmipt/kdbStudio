package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JMenuItemFixture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.assertj.swing.timing.Pause.pause;
import static org.junit.Assert.assertEquals;

public class EditorTest extends StudioTest {

    private static File unixFile, winFile, mac9File;
    private static final Logger log = LogManager.getLogger();

    private static File createFile(String content) throws IOException {
        File file = File.createTempFile("kdbStudioFileLineEnding", ".q");
        Files.write(file.toPath(), content.getBytes());
        return file;
    }

    @BeforeClass
    public static void prepareFiles() throws IOException {
        unixFile = createFile("a:1\n\nb:1\n");
        winFile = createFile("a:1\r\n\r\nb:1\r\n");
        mac9File = createFile("a:1\r\rb:1\r");
    }

    @AfterClass
    public static void removeFiles() throws IOException {
        Files.delete(unixFile.toPath());
        Files.delete(winFile.toPath());
        Files.delete(mac9File.toPath());
    }

    private void assertMenuLE(boolean unix, boolean win, boolean mac) {

        JMenuItemFixture unixMenu = frameFixture.menuItem("Unix");
        JMenuItemFixture winMenu = frameFixture.menuItem("Windows");
        JMenuItemFixture macMenu = frameFixture.menuItem("MacOS 9");

        boolean[] sels = execute(() -> new boolean[] {
                                            unixMenu.target().isSelected(),
                                            winMenu.target().isSelected(),
                                            macMenu.target().isSelected() }
        );

        assertEquals(unix, sels[0]);
        assertEquals(win, sels[1]);
        assertEquals(mac, sels[2]);
    }

    @Test
    public void testUnixLE() {
        openFile(unixFile);
        assertMenuLE(true, false, false);
    }

    @Test
    public void testWinE() {
        openFile(winFile);
        assertMenuLE(false, true, false);
    }

    @Test
    public void testMac9LE() {
        openFile(mac9File);
        assertMenuLE(false, false, true);
    }

    @Test
    public void testMenuLEWhenSwitching() {
        openFile(unixFile);
        openFile(winFile);

        assertMenuLE(false, true, false);
        clickTab("editor2");
        assertMenuLE(true, false, false);

        clickTab("editor3");
        assertMenuLE(false, true, false);
    }


    @Test
    public void testUndoActionEnable() {
        frameFixture.menuItem("Undo").requireDisabled();
        frameFixture.button("toolbarUndo").requireDisabled();

        frameFixture.textBox("editor1").enterText("x");

        frameFixture.menuItem("Undo").requireEnabled();
        frameFixture.button("toolbarUndo").requireEnabled();
    }

    @AfterClass
    public static void resetMockedOptionPane() {
        StudioOptionPane.setMocked(false);
    }

    @Test
    public void testCancelOnFrameClosure() {
        StudioOptionPane.setMocked(true);
        clickMenu( "New Window");
        FrameFixture newFrameFixture = WindowFinder.findFrame(
                new GenericTypeMatcher<StudioWindow>(StudioWindow.class, true) {
                        @Override
                        protected boolean isMatching(StudioWindow f) {
                            return f != studioWindow;
                        }
        }).using(robot());
        newFrameFixture.textBox("editor1").enterText("x");

        StudioOptionPane.setMockedResult(JOptionPane.CANCEL_OPTION);
        clickMenu(newFrameFixture, "Close Window");
        pause(50, TimeUnit.MILLISECONDS); // wait as closure happens asynchronously
        newFrameFixture.requireVisible();

        StudioOptionPane.setMockedResult(JOptionPane.CLOSED_OPTION);
        clickMenu(newFrameFixture, "Close Window");
        pause(50, TimeUnit.MILLISECONDS); // wait as closure happens asynchronously
        newFrameFixture.requireVisible();


        //tear down
        StudioOptionPane.setMockedResult(JOptionPane.NO_OPTION);
        newFrameFixture.close();
        pause(50, TimeUnit.MILLISECONDS); // wait as closure happens asynchronously
        newFrameFixture.requireNotVisible();
    }
}
