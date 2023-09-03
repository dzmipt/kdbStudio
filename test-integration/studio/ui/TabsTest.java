package studio.ui;

import org.junit.Test;

public class TabsTest extends StudioTest {
    @Test
    public void testClosureOfTheLastEditor() {
        closeTabWithMouse("editor1");
    }

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
    public void closeLasTabOnSplitTest() {
        split("editor1", true);
        closeTabWithMouse("editor2");
        frameFixture.textBox("editor1").requireFocused();
    }

    @Test
    public void openFileTest() {

    }
}
