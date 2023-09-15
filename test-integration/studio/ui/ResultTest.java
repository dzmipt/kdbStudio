package studio.ui;

import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.assertj.swing.timing.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.kdb.K;
import studio.kdb.MockQSession;

import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.swing.timing.Pause.pause;
import static org.assertj.swing.timing.Timeout.timeout;

public class ResultTest extends StudioTest {

    @BeforeClass
    public static void mock() {
        MockQSession.mock();
        MockQSession.setResponse(new K.KLongVector(0, 1, 2, 3, 4, 5));
    }

    @Before
    public void setServer() {
        setServerConnectionText("s:1");
    }

    private void execute() {
        JTabbedPaneFixture tabbedPaneFixture = frameFixture.tabbedPane("ResultTabbedPane");
        int count = getTabCount(tabbedPaneFixture.target());

        JTextComponentFixture tb = frameFixture.textBox("editor1");
        tb.enterText("x");
        frameFixture.menuItem("Execute Current Line").click();

        pause(new Condition("mock query execution") {
            @Override
            public boolean test() {
                return getTabCount(tabbedPaneFixture.target()) > count;
            }
        }, timeout(1, TimeUnit.SECONDS));


        int newCount = getTabCount(tabbedPaneFixture.target());
        assertEquals("Expect that one more result tab is added", count+1, newCount);
    }

    @Test
    public void testExecute() {
        execute();
    }

    @Test
    public void testUploadButtonEnabled() {
        execute();
        frameFixture.button("UploadButton").requireEnabled();

        execute();
        frameFixture.button("UploadButton").requireEnabled();

        JTabbedPaneFixture tabbedPaneFixture = frameFixture.tabbedPane("ResultTabbedPane");
        tabbedPaneFixture.selectTab(0);

        frameFixture.button("UploadButton").requireEnabled();

    }

}
