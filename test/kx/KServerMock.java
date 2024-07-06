package kx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.K;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class KServerMock implements Runnable {

    private final ServerSocket serverSocket;
    private volatile boolean running = true;
    private List<Session> sessions = new ArrayList<>();
    private int port;

    private static final Logger log = LogManager.getLogger();

    public KServerMock() throws IOException {
        this(0);
    }

    public KServerMock(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        this.port = serverSocket.getLocalPort();
        new Thread(this, "Server listen port " + port).start();
        log.info("Server started on port {}", this.port);
    }

    public int getPort() {
        return port;
    }
    @Override
    public void run() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                sessions.add(new Session(socket));
            } catch (IOException e) {
                if (running) {
                    log.error("Error in the server socket", e);
                }
            }
        }
        running = false;
    }

    public void shutdown() {
        if (! running) {
            log.info("Already stopped");
            return;
        }

        running = false;
        try {
            for (Session session: sessions) {
                if (session.isRunning()) {
                    session.shutdown();
                }
            }
            serverSocket.close();
        } catch (IOException e) {
            log.info("Error during server socket closure", e);
        }
    }


    static class Session implements Runnable {

        private static int index = 0;
        private int thisIndex = index++;

        private final Socket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private boolean running = true;

        Session(Socket socket) throws IOException {
            this.socket = socket;
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            new Thread(this, "Session " + thisIndex).start();
        }

        public void run() {
            try {
                ByteArrayOutputStream baosConnection = new ByteArrayOutputStream();
                for (;;) {
                    int next = inputStream.read();
                    if (next == -1) {
                        throw new IOException("Socket closed");
                    }

                    if (next == 0) break;
                    baosConnection.write((byte)next);
                }

                byte[] bytes = baosConnection.toByteArray();
                log.info("Session {}: got connection '{}' with version {}", thisIndex, new String(bytes, 0, bytes.length-1), bytes[bytes.length-1]);
                outputStream.write(3);
                outputStream.flush();

                while (running) {
                    byte[] header = new byte[8];
                    for (int index = 0; index<header.length; index++) {
                        int next = inputStream.read();
                        if (next == -1) throw new IOException("Socket closed");
                        header[index] = (byte) next;
                    }

                    log.info("Session {}: got header {} {} {} {} ", thisIndex, header[0], header[1], header[2], header[3]);
                    boolean isLittleEndian = header[0] == 1;
                    int msgType = header[1];
                    boolean compressed = header[3] == 1;

                    IPC ipc = new IPC(header, 4, false, isLittleEndian);
                    final int msgLength = ipc.ri() - 8;

                    byte[] message = new byte[msgLength];

                    int readCount = 0;
                    while (readCount < msgLength) {
                        int size = inputStream.read(message, readCount, msgLength - readCount);
                        readCount += size;
                    }

                    KMessage kmessage = IPC.deserialise(message, compressed, isLittleEndian);

                    ByteArrayOutputStream baosMessage = new ByteArrayOutputStream();
                    kmessage.getObject().serialise(baosMessage);

                    outputStream.write(0);
                    outputStream.write(2);
                    outputStream.write(0);
                    outputStream.write(0);
                    K.write(outputStream, baosMessage.size() + 8);
                    outputStream.write(baosMessage.toByteArray());
                }

            } catch (IOException e) {
                if (running) {
                    log.info("Exception in session {}", thisIndex, e);
                    shutdown();
                }
            }
        }

        public boolean isRunning() {
            return running;
        }

        public void shutdown() {
            if (! running) {
                log.info("Session {} is already stopped", thisIndex);
                return;
            }

            running = false;
            try {
                socket.close();
            } catch (IOException e) {
                log.info("Exception during session {} socket closure", thisIndex, e);
            }
        }
    }

    public static void main(String... args) throws IOException {
        new KServerMock(2345);
    }
}
