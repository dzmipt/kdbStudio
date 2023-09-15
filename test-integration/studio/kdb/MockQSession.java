package studio.kdb;

import kx.ProgressCallback;
import kx.c;

import java.io.IOException;

public class MockQSession extends kx.c {

    private static K.KBase kResponse = null;
    private static K4Exception kError = null;
    private static IOException ioException = null;

    public static void mock() {
        Session.mock(new MockQSessionCreator());
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


    private boolean closed = true;

    private MockQSession() {
        super("no host", 0, "mock user", false);
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
        closed = false;
        if (kResponse != null) return kResponse;
        if (kError != null) throw kError;

        if (ioException != null) {
            closed = true;
            throw ioException;
        }

        return x;
    }

    public static class MockQSessionCreator extends Session.SessionCreator {
        @Override
        public c createConnection(Server s) {
            return new MockQSession();
        }
    }
}
