package studio.ui;

import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.junit.Test;

public class RSTextAreaTest extends AssertJSwingJUnitTestCase {

    private FrameFixture frameFixture;

    @Override
    protected void onSetUp() throws Exception {
        RSTextAreaFrameForTest frame = GuiActionRunner.execute(() -> new RSTextAreaFrameForTest());

        frameFixture = new FrameFixture(robot(), frame);
    }

    @Test
    public void test() {
        JTextComponentFixture textField = frameFixture.textBox("textField");
        textField.requireText("");
        textField.enterText("test");
        textField.requireText("test");

        JTextComponentFixture textArea = frameFixture.textBox("textArea");
        textArea.requireText("");
        textArea.enterText("something");
        textArea.requireText("something");
    }
}
