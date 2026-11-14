package kx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class KConnectionFlipTLSTest {

    private ServerSocket serverSocket;
    private final AtomicInteger connections = new AtomicInteger();

    // Plain TCP server which rejects authentication: reads the credentials and closes the socket
    @BeforeEach
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(0);
        Thread thread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    connections.incrementAndGet();
                    InputStream in = socket.getInputStream();
                    int next;
                    while ( (next = in.read()) > 0 ) ;
                } catch (IOException ignored) {}
            }
        }, "Rejecting server");
        thread.setDaemon(true);
        thread.start();
    }

    @AfterEach
    public void stopServer() throws IOException {
        serverSocket.close();
    }

    @Test
    public void testAuthFailureWithoutFlip() {
        KConnection conn = new KConnection("localhost", serverSocket.getLocalPort(), false, false, null);
        assertThrows(K4AccessException.class, () -> conn.connect(null));
        assertEquals(1, connections.get());
        assertFalse(conn.getConnectionContext().isConnected());
    }

    @Test
    public void testAuthFailureWithFlip() {
        KConnection conn = new KConnection("localhost", serverSocket.getLocalPort(), false, true, null);
        assertThrows(K4AccessException.class, () -> conn.connect(null));
        assertEquals(2, connections.get());
        assertFalse(conn.getConnectionContext().isConnected());
    }
}
