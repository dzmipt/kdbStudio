package studio.ui;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import studio.kdb.MockQSession;

import static org.junit.Assert.*;

public class StopQueryTest extends StudioTest {

    @BeforeClass
    public static void init() {
        MockQSession.mock();
        MockQSession.lockResponse(true);
    }

    @AfterClass
    public static void restore() {
        MockQSession.lockResponse(false);
    }

    @Test
    public void stopActionTest() {
        setServerConnectionText("s:1");
        MockQSession.resetAllQueryCount();

        frameFixture.button("toolbarStop").requireDisabled();

        frameFixture.textBox("editor1").enterText("x").selectAll();
        frameFixture.button("toolbarExecute").click();

        frameFixture.button("toolbarExecute").requireDisabled();
        frameFixture.button("toolbarStop").requireEnabled();

        frameFixture.button("toolbarStop").click();
        MockQSession.unlockResponse();

        frameFixture.button("toolbarExecute").requireEnabled();
        frameFixture.button("toolbarStop").requireDisabled();

        int count = getTabCount(frameFixture.tabbedPane("ResultTabbedPane").target());
        assertEquals(0, count);


        MockQSession[] sessions = MockQSession.getLastActiveSessions();
        assertEquals(1, sessions.length);
        assertTrue(sessions[0].isClosed());
        int sessionCount = MockQSession.getAllSessions().size();


        waitForQueryExecution(() -> {
            MockQSession.setEchoMode();
            MockQSession.lockResponse(false);
            frameFixture.button("toolbarExecute").click();
        });

        //We do not create a new session. But the session should be opened now
        assertEquals(sessionCount, MockQSession.getAllSessions().size());
        assertFalse(sessions[0].isClosed());

    }
}
