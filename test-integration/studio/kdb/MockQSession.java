package studio.kdb;

import kx.ProgressCallback;
import kx.c;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MockQSession extends kx.c {

    private static final Logger log = LogManager.getLogger();

    private static K.KBase kResponse = null;
    private static K4Exception kError = null;
    private static IOException ioException = null;
    private static Object lock = null;

    private AtomicInteger queryCount = new AtomicInteger(0);
    private int index;

    private static int sessionIndex = 0;

    public final static MockQSessionCreator mockQSessionCreator = new MockQSessionCreator();

    public static void mock() {
        Session.mock(mockQSessionCreator);
    }

    public static void setEchoMode() {
        MockQSession.kResponse = null;
        MockQSession.kError = null;
        MockQSession.ioException = null;
    }

    public static void setResponse(K.KBase kResponse) {
        MockQSession.kResponse = kResponse;
        MockQSession.kError = null;
        MockQSession.ioException = null;
    }

    public static void setResponse(K4Exception kError) {
        MockQSession.kResponse = null;
        MockQSession.kError = kError;
        MockQSession.ioException = null;
    }

    public static void setResponse(IOException ioException) {
        MockQSession.kResponse = null;
        MockQSession.kError = null;
        MockQSession.ioException = ioException;
    }

    public static void lockResponse(boolean lock) {
        if (lock) {
            MockQSession.lock = new Object();
        } else {
            MockQSession.lock = null;
        }
    }

    public static void unlockResponse() {
        synchronized (MockQSession.lock) {
            MockQSession.lock.notifyAll();
        }
    }

    private boolean closed = true;

    MockQSession() {
        super("no host", 0, "mock user", false);
        index = sessionIndex++;
    }

    public int getIndex() {
        return getIndex();
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized K.KBase k(K.KBase x, ProgressCallback progress) throws K4Exception, IOException {
        queryCount.getAndIncrement();
        closed = false;

        if (lock != null) {
            try {
                synchronized (lock) {
                    lock.wait();
                }
            } catch (InterruptedException e) {
                log.info("MockQSession {} was interrupted", index);
            }
        }

        if (kResponse != null) return kResponse;
        if (kError != null) throw kError;

        if (ioException != null) {
            closed = true;
            throw ioException;
        }


        return x;
    }

    public int getQueryCount() {
        return queryCount.get();
    }

    public void resetQueryCount() {
        queryCount.set(0);
    }

    public static void resetAllQueryCount() {
        for (MockQSession session : mockQSessionCreator.sessions) {
            session.resetQueryCount();
        }
    }

    public static MockQSession[] getLastActiveSessions() {
        return mockQSessionCreator.sessions.stream()
                .filter(s -> s.getQueryCount() > 0)
                .toArray(MockQSession[]::new);
    }

    public static List<MockQSession> getAllSessions() {
        return mockQSessionCreator.sessions;
    }

    public static class MockQSessionCreator extends Session.SessionCreator {

        private List<MockQSession> sessions = new ArrayList<>();

        @Override
        public c createConnection(Server server) {
            MockQSession session = new MockQSession();
            sessions.add(session);
            return session;
        }

    }
}
