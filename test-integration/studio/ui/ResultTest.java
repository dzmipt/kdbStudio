package studio.ui;

import kx.K4AccessException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.swing.data.TableCell;
import org.assertj.swing.fixture.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.kdb.K;
import studio.kdb.MockQSession;

import java.awt.*;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ResultTest extends StudioTest {

    private static final Logger log = LogManager.getLogger();

    @BeforeClass
    public static void mock() {
        MockQSession.mock();
    }

    @Before
    public void setServer() {
        setServerConnectionText("s:1");
        MockQSession.setResponse(new K.KLongVector(6, 5, 4, 3, 2, 1, 0));
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

        Color bgAfter = table.backgroundAt(TableCell.row(1).column(0)).target();

        Assert.assertNotEquals(bgBefore, bgAfter);

        table.tableHeader().clickColumn(0).click();

        Assert.assertEquals(bgBefore, table.backgroundAt(TableCell.row(1).column(0)).target());
        Assert.assertEquals(bgAfter, table.backgroundAt(TableCell.row(5).column(0)).target());
    }

    @Test
    public void testTableConnExtractor() {
        MockQSession.setResponse(
                new K.Flip(new K.KSymbolVector("host","port"),
                            new K.KList(
                                    new K.KSymbolVector("a","z","b"),
                                    new K.KLongVector(1,2,3)
                            ))  );

        execute();
        JTableFixture table = frameFixture.panel("resultPanel0").table();
        table.tableHeader().clickColumn(0).click();
        JPopupMenuFixture popupMenu = table.showPopupMenuAt(TableCell.row(1).column(0));
        String[] labels = popupMenu.menuLabels();
        log.info("Got the following menu items {}", Arrays.toString(labels));
        Assert.assertEquals("Open b:3", labels[0]);
    }

    @Test
    public void testAuthenticationException() {
        MockQSession.setResponse(new K4AccessException());
        execute();

        JTextComponentFixture textArea = frameFixture.panel("resultPanel0").textBox();
        textArea.requireText(Pattern.compile(".*error.*Authentication failed.*", Pattern.DOTALL));
    }

    @Test
    public void testEOFException() {
        setExpectedNumberOfLogErrors(1);
        MockQSession.setResponse(new EOFException());
        execute();

        JTextComponentFixture textArea = frameFixture.panel("resultPanel0").textBox();
        textArea.requireText(Pattern.compile(".*Error: java.io.EOFException", Pattern.DOTALL));
    }

    @Test
    public void testIOException() {
        setExpectedNumberOfLogErrors(1);
        MockQSession.setResponse(new IOException("ioMessageHere"));
        execute();

        JTextComponentFixture textArea = frameFixture.panel("resultPanel0").textBox();
        textArea.requireText(Pattern.compile(".*Error: java.io.IOException.*ioMessageHere.*", Pattern.DOTALL));
    }


}
