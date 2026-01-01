package studio.ui;

import org.assertj.swing.data.Index;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.util.Platform;
import org.junit.Test;
import studio.utils.LogErrors;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.junit.Assert.*;

public class TabsTest extends StudioTest {

//    @Test
//    public void testClosureOfTheLastEditor() {
//        closeTabWithMouse("editor1");
//    }

    @Test
    public void addTabTest() {
        addTab("editor1");
        frameFixture.textBox("editor2").requireFocused();
    }

    @Test
    public void closeTabTest() {
        addTab("editor1");
        closeTabWithMouse("editor1");
        frameFixture.textBox("editor2").requireFocused();
    }

    @Test
    public void openFileTest() throws IOException {
        String text = "a:1;";

        File file = File.createTempFile("kdbStudioFileOpen", ".q");
        String filename = file.getName();

        Files.write(file.toPath(), Collections.singletonList(text));

        JTabbedPaneFixture tabbedPaneFixture  = frameFixture.tabbedPane("editorTabbedPane0");
        openFile(file);

        assertEquals("Number of tabs should be now 2", 2, tabbedPaneFixture.tabTitles().length);
        tabbedPaneFixture.requireTitle(filename, Index.atIndex(1));

        String endOfLine = System.getProperty("line.separator");
        frameFixture.textBox("editor2").requireText(text + endOfLine);

        frameFixture.textBox("editor2").enterText("x");
        tabbedPaneFixture.requireTitle(filename + " *", Index.atIndex(1));
    }


    @Test
    public void openFileWithErrorTest() {
        FileChooser.mock(new File("/unknownFile.q"));


        JTabbedPaneFixture tabbedPaneFixture  = frameFixture.tabbedPane("editorTabbedPane0");
        String[] titles = tabbedPaneFixture.tabTitles();

        StudioOptionPane.setMockedResult(JOptionPane.CLOSED_OPTION);
        LogErrors.pause();
        clickMenu("Open...");
        LogErrors.enable();

        assertEquals("New tab shouldn't be opened", 1, tabbedPaneFixture.tabTitles().length);
        assertArrayEquals("JTabbenPane titles shouldn't changed", titles, tabbedPaneFixture.tabTitles());

    }

    @Test
    public void emptyTabNameTest() {
        JTabbedPaneFixture tabbedPaneFixture  = frameFixture.tabbedPane("editorTabbedPane0");
        String title = tabbedPaneFixture.tabTitles()[0];
        assertTrue(title.startsWith("Script"));
    }

    @Test
    public void tabNameWithServerTest() {
        String connection = "s:10";
        setServerConnectionText(connection);
        JTabbedPaneFixture tabbedPaneFixture  = frameFixture.tabbedPane("editorTabbedPane0");
        String title = tabbedPaneFixture.tabTitles()[0];
        assertTrue(title.equals(connection));
    }

    private String getWindowTitle() {
        return execute(() -> {
            Frame f = frameFixture.target();
            if (Platform.isMacintosh() && f instanceof StudioFrame) {
                return ((StudioFrame)f).getRealTitle();
            }
            return f.getTitle();
        });
    }

    private JMenuItem[] getLastWindowMenuItems(FrameFixture frameFixture, int n) {
        return execute(() -> {
            JMenu menu = (JMenu) frameFixture.menuItem("Window").target();
            int count = menu.getMenuComponentCount();

            JMenuItem[] items = new JMenuItem[n];
            for(int i = count - n; i < count; i++) {
                items[i - (count-n)] = menu.getItem(i);
            }
            return items;
        } );
    }

    private void assertMenuItemContainsText(JMenuItem menuItem, String text) {
        assertTrue("Expect that menuItem {" + menuItem.getText() + "} contains text {" + text + "}",
                menuItem.getText().contains(text));
    }

    @Test
    public void frameTitleTest() {
        JTabbedPaneFixture tabbedPaneFixture  = frameFixture.tabbedPane("editorTabbedPane0");
        String title1 = tabbedPaneFixture.tabTitles()[0];
        assertTrue("Tab title: " + title1, title1.startsWith("Script"));
        assertTrue("Window titles: " + getWindowTitle(), getWindowTitle().contains(title1));

        addTab("editor1");

        String title2 = tabbedPaneFixture.tabTitles()[1];

        assertTrue("Window titles: " + getWindowTitle(), getWindowTitle().contains(title2));
        JMenuItem[] menuItems = getLastWindowMenuItems(frameFixture, 1);
        assertMenuItemContainsText(menuItems[0], title2);

        clickTab("editor1");

        assertTrue("Window titles: " + getWindowTitle(), getWindowTitle().contains(title1));
        menuItems = getLastWindowMenuItems(frameFixture, 1);
        assertMenuItemContainsText(menuItems[0], title1);

    }


}
