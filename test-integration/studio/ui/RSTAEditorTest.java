package studio.ui;

import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.Test;

public class RSTAEditorTest extends StudioTest {

    @Test
    public void testPairedChars() {
        JTextComponentFixture editor = frameFixture.textBox("editor1");
        editor.enterText("`'()[]{}\"");
        editor.requireText("`'()[]{}\"");
    }

    @Test
    public void testNotWrappedSpecChars() {
        JTextComponentFixture editor = frameFixture.textBox("editor1");
        editor.enterText("select");
        editor.selectAll();
        editor.enterText("`");
        editor.requireText("`");

        editor.deleteText();
        editor.enterText("select");
        editor.selectAll();
        editor.enterText("'");
        editor.requireText("'");
    }


    @Test
    public void testWrappedChars() {
        JTextComponentFixture editor = frameFixture.textBox("editor1");
        editor.enterText("select");
        editor.selectAll();
        editor.enterText("(");
        editor.requireText("(select)");

        editor.deleteText();
        editor.enterText("select");
        editor.selectAll();
        editor.enterText("[");
        editor.requireText("[select]");

        editor.deleteText();
        editor.enterText("select");
        editor.selectAll();
        editor.enterText("{");
        editor.requireText("{select}");

        editor.deleteText();
        editor.enterText("select");
        editor.selectAll();
        editor.enterText("\"");
        editor.requireText("\"select\"");
    }

}
