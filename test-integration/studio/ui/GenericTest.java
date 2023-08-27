package studio.ui;

import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.Test;

public class GenericTest extends StudioTest {

    @Test
    public void test() {
        JTextComponentFixture tb = frameFixture.textBox("editor1");
        tb.requireEmpty();
        tb.enterText("a");
        tb.requireText("a");
        tb.enterText("bc\ndef");
        tb.requireText("abc\ndef");
    }

}
