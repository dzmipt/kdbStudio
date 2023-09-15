package studio.ui;

import org.assertj.swing.data.Index;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;

import static java.awt.event.KeyEvent.VK_ENTER;
import static org.assertj.swing.core.KeyPressInfo.keyCode;

public class SplitTest extends StudioTest {

    @Test
    public void splitRightTest() {
        JTextComponentFixture editor1 = frameFixture.textBox("editor1");
        Rectangle bound = getScreenBound(editor1.target());

        split("editor1", false);

        JTextComponentFixture editor2 = frameFixture.textBox("editor2");
        Rectangle bound1 = getScreenBound(editor1.target());
        Rectangle bound2 = getScreenBound(editor2.target());

        Assert.assertTrue(bound1.x + bound1.width < bound2.x);
        Assert.assertTrue(bound1.y == bound2.y );
        Assert.assertTrue(bound1.height == bound2.height );
        Assert.assertTrue(Math.abs(bound.x - bound1.x) < 10 );
        Assert.assertTrue(Math.abs(bound1.y - bound.y) < 10 );
        Assert.assertTrue(bound1.width + bound2.width < bound.width);
    }

    @Test
    public void splitDownTest() {
        JTextComponentFixture editor1 = frameFixture.textBox("editor1");
        Rectangle bound = getScreenBound(editor1.target());

        split("editor1", true);

        JTextComponentFixture editor2 = frameFixture.textBox("editor2");
        Rectangle bound1 = getScreenBound(editor1.target());
        Rectangle bound2 = getScreenBound(editor2.target());

        Assert.assertTrue(bound1.y + bound1.height < bound2.y);
        Assert.assertTrue(bound1.x == bound2.x );
        Assert.assertTrue(bound1.width == bound2.width );
        Assert.assertTrue(Math.abs(bound.x - bound1.x) < 10 );
        Assert.assertTrue(Math.abs(bound1.y - bound.y) < 10 );
        Assert.assertTrue(bound1.height + bound2.height < bound.height);
    }

    @Test
    public void closeLasTabOnSplitTest() {
        split("editor1", true);
        closeTabWithMouse("editor2");
        frameFixture.textBox("editor1").requireFocused();
    }

    @Test
    public void closeLasTabOnSplitTest1() {
        split("editor1", false);
        closeTabWithMouse("editor1");
        frameFixture.textBox("editor2").requireFocused();
    }

    @Test
    public void closeLasTabOnSplitTest2() {
        split("editor1", false);
        split("editor1", true);
        closeTabWithMouse("editor1");
    }

    @Test
    public void focusAfterSeverSelection() {
        split("editor1", false);
        JTextComponentFixture editor1 = frameFixture.textBox("editor1");
        editor1.focus();
        setServerConnectionText("`:server:1111");

        editor1.requireFocused();
        frameFixture.tabbedPane("editorTabbedPane1").requireTitle("server:1111", Index.atIndex(0));
    }

    @Test
    public void focusAfterSeverSelection1() {
        split("editor1", false);
        JTextComponentFixture editor2= frameFixture.textBox("editor2");
        editor2.focus();
        frameFixture.textBox("serverEntryTextField").enterText("`:server:1111").pressAndReleaseKey(keyCode(VK_ENTER));

        editor2.requireFocused();
        frameFixture.tabbedPane("editorTabbedPane2").requireTitle("server:1111", Index.atIndex(0));
    }


}
