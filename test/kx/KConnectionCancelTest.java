package kx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class KConnectionCancelTest {

    private ServerSocket serverSocket;
    private final List<Socket> accepted = new CopyOnWriteArrayList<>();

    // Busy server: accepts connections but never replies to the handshake
    @BeforeEach
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(0);
        Thread thread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    accepted.add(serverSocket.accept());
                } catch (IOException ignored) {}
            }
        }, "Busy server");
        thread.setDaemon(true);
        thread.start();
    }

    @AfterEach
    public void stopServer() throws IOException {
        serverSocket.close();
        for (Socket socket: accepted) socket.close();
    }

    private void assertCloseAbortsConnect(boolean flipTLS) throws Exception {
        KConnection conn = new KConnection("localhost", serverSocket.getLocalPort(), false, flipTLS, null);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(() -> {
                conn.connect(null);
                return null;
            });

            // wait until the handshake is sent and the connect is blocked waiting for the reply
            long deadline = System.currentTimeMillis() + 5000;
            while (accepted.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Thread.sleep(100);
            assertFalse(future.isDone());

            conn.close();

            ExecutionException e = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertInstanceOf(KConnection.ConnectionAbortedException.class, e.getCause());
            assertFalse(conn.getConnectionContext().isConnected());
            assertEquals(1, accepted.size(), "No attempt with flipped TLS after cancel");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testCloseAbortsConnect() throws Exception {
        assertCloseAbortsConnect(false);
    }

    @Test
    public void testCloseAbortsConnectWithFlipTLS() throws Exception {
        assertCloseAbortsConnect(true);
    }

    @Test
    public void testConnectAfterAbort() throws Exception {
        KConnection conn = new KConnection("localhost", serverSocket.getLocalPort(), false, false, null);
        // close() without a connect in progress must not affect the next connect
        conn.close();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> future = executor.submit(() -> {
                conn.connect(null);
                return null;
            });
            assertThrows(TimeoutException.class, () -> future.get(300, TimeUnit.MILLISECONDS));
            conn.close();
            assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }
}
