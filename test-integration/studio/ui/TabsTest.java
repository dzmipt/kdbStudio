package studio.ui;

import org.assertj.swing.data.Index;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

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
        frameFixture.textBox("editor2").requireFocused();
        closeTabWithMouse("editor1");
        frameFixture.textBox("editor2").requireFocused();
    }

    @Test
    public void openFileTest() throws IOException {
        String text = "a:1;";

        File file = File.createTempFile("kdbStudioFileOpen", ".q");
        Files.write(file.toPath(), Collections.singletonList(text));
        FileChooser.mock(file);

        JTabbedPaneFixture tabbedPaneFixture  = frameFixture.tabbedPane("editorTabbedPane0");
        frameFixture.menuItem("Open...").click();

        assertEquals("Number of tabs should be now 2", 2, tabbedPaneFixture.tabTitles().length);
        tabbedPaneFixture.requireTitle(file.getName(), Index.atIndex(1));

        String endOfLine = System.getProperty("line.separator");
        frameFixture.textBox("editor2").requireText(text + endOfLine);
    }
}
