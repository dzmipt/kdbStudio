package studio.ui;

import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.kdb.K;
import studio.kdb.MockQSession;

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
        waitForQueryExecution(() -> {
            JTextComponentFixture tb = frameFixture.textBox("editor1");
            tb.enterText("x");
            frameFixture.menuItem("Execute Current Line").click();
        });
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

//    @Test
//    public void testMarkersAfterSorting() {
//        execute();
//        frameFixture.panel("resultPanel0")
//    }

}
