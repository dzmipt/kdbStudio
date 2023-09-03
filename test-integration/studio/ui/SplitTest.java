package studio.ui;

import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.Assert;
import org.junit.Test;

import java.awt.*;

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

}
