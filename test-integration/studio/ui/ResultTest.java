package studio.ui;

import org.assertj.swing.data.TableCell;
import org.assertj.swing.fixture.JPanelFixture;
import org.assertj.swing.fixture.JTabbedPaneFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.fixture.JTextComponentFixture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.kdb.K;
import studio.kdb.MockQSession;

import java.awt.*;

public class ResultTest extends StudioTest {

    @BeforeClass
    public static void mock() {
        MockQSession.mock();
        MockQSession.setResponse(new K.KLongVector(6, 5, 4, 3, 2, 1, 0));
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

    // The test heavily relies on the mocked response:
    // - asc sorting should change order
    // - location of a cell with "5" before and after sorting
    // - the index of the found cell before and after should have the same parity
    @Test
    public void testMarkersAfterSorting() {
        execute();
        frameFixture.menuItem("Find in Result").click();

        JTableFixture table = frameFixture.panel("resultPanel0").table();

        Color bgBefore = table.backgroundAt(TableCell.row(1).column(0)).target();

        JPanelFixture resultSearchPanel = frameFixture.panel("ResultSearchPanel");
        resultSearchPanel.textBox("FindField").enterText("5");
        resultSearchPanel.button("FindButton").click();

        Color bgAfter = table.backgroundAt(TableCell.row(1).column(0)).target();

        Assert.assertNotEquals(bgBefore, bgAfter);

        table.tableHeader().clickColumn(0).click();

        Assert.assertEquals(bgBefore, table.backgroundAt(TableCell.row(1).column(0)).target());
        Assert.assertEquals(bgAfter, table.backgroundAt(TableCell.row(5).column(0)).target());
    }

}
