package studio.ui;

import org.assertj.swing.fixture.JMenuItemFixture;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.junit.Assert.assertEquals;

public class EditorTest extends StudioTest {

    private static File unixFile, winFile, mac9File;

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

}
