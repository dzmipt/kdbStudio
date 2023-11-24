package kx;

import studio.kdb.K;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class KConnection {

    private final String host;
    private final int port;
    private final String userPassword;
    private final boolean useTLS;
    private volatile boolean closed = true;

    private DataInputStream inputStream;
    private OutputStream outputStream;
    private Socket s;

    private SocketReader socketReader;

    void io(Socket s) throws IOException {
        s.setTcpNoDelay(true);
        inputStream = new DataInputStream(s.getInputStream());
        outputStream = s.getOutputStream();
    }

    public void close() {
        if (socketReader != null) {
            socketReader.interrupt();
            socketReader = null;
        }

        if (closed) return;

        closed = true;
        if (inputStream != null)
            try {
                inputStream.close();
            } catch (IOException e) {}
        if (outputStream != null)
            try {
                outputStream.close();
            } catch (IOException e) {}
        if (s != null) {
            try {
                s.close();
            } catch (IOException e) {}
        }
    }


    public boolean isClosed() {
        return closed;
    }

    private void connect() throws IOException, K4AccessException {
        s = new Socket();
        s.setReceiveBufferSize(1024 * 1024);
        s.connect(new InetSocketAddress(host, port));

        if (useTLS) {
            try {
                s = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, port, true);
                ((SSLSocket) s).startHandshake();
            } catch (IOException e) {
                s.close();
                throw e;
            }
        }
        io(s);
        java.io.ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((userPassword + "\3").getBytes());
        baos.write(0);
        baos.flush();
        outputStream.write(baos.toByteArray());
        if (inputStream.read() == -1) {
            throw new K4AccessException();
        }
        closed = false;

        socketReader = new SocketReader(s);
        socketReader.setName("Reader " + host + ":" + port);
        socketReader.setDaemon(true);
        socketReader.start();
    }

    public KConnection(String h, int p, String userPassword, boolean useTLS) {
        host = h;
        port = p;
        this.userPassword = userPassword;
        this.useTLS = useTLS;
    }

    private final static byte[] HEADER = new byte[] {0,1,0,0};

    private void send(K.KBase query) throws IOException {
        ByteArrayOutputStream baosBody = new ByteArrayOutputStream();
        query.serialise(baosBody);

        ByteArrayOutputStream baosHeader = new ByteArrayOutputStream();
        baosHeader.write(HEADER);
        int msgSize = 8 + baosBody.size();
        K.write(baosHeader, msgSize);

        outputStream.write(baosHeader.toByteArray());
        outputStream.write(baosBody.toByteArray());
    }

    public synchronized K.KBase k(K.KBase x, ProgressCallback progress) throws K4Exception, IOException, InterruptedException {
        try {
            if (isClosed()) connect();
            socketReader.setProgressCallback(progress);
            send(x);
            return socketReader.getResponse();
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public K.KBase k(K.KBase x) throws K4Exception, IOException, InterruptedException {
        return k(x, null);
    }

    private class SocketReader extends Thread {

        private final DataInputStream inputStream;
        private KMessage message = null;
        private ProgressCallback progress = null;

        private final Object lockWrite = new Object();
        private final Object lockRead = new Object();

        SocketReader(Socket socket) throws IOException {
            inputStream = new DataInputStream(socket.getInputStream());
        }

        synchronized void setProgressCallback(ProgressCallback progress) {
            this.progress = progress;
        }

        private synchronized ProgressCallback getProgressCallback() {
            return progress;
        }

        K.KBase getResponse() throws InterruptedException, K4Exception, IOException {
            KMessage response;
            synchronized (lockRead) {
                while (message == null) {
                    lockRead.wait();
                }
                response = message;
            }

            synchronized (lockWrite) {
                message = null;
                lockWrite.notifyAll();
            }
            return response.getObject();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    byte[] buffer;
                    inputStream.readFully(buffer = new byte[8]);

                    boolean isLittleEndian = buffer[0] == 1;
                    boolean compressed = buffer[2] == 1;
                    byte msgType = buffer[1];
                    if (msgType == 1) {
                        throw new IOException("Cannot process sync msg from remote");
                    }
                    boolean response = msgType == 2;

                    IPC ipc = new IPC(buffer, 4, false, isLittleEndian);
                    final int msgLength = ipc.ri() - 8;

                    ProgressCallback progress = getProgressCallback();
                    if (progress!=null) {
                        progress.setCompressed(compressed);
                        progress.setMsgLength(msgLength);
                    }

                    buffer = new byte[msgLength];
                    int total = 0;

                    while (total < msgLength) {
                        int available = Math.max(1, Math.min(msgLength - total, inputStream.available()));
                        int count = inputStream.read(buffer, total, available);
                        if (count < 0) throw new EOFException("Connection is broken");
                        total += count;
                        if (progress != null) progress.setCurrentProgress(total);
                    }

                    if (response) {
                        synchronized (lockRead) {
                            message = IPC.deserialise(buffer, compressed, isLittleEndian);
                            lockRead.notifyAll();
                        }
                        synchronized (lockWrite) {
                            while (message != null) {
                                lockWrite.wait();
                            }
                        }
                        setProgressCallback(null);
                    }
                }
            } catch (IOException e) {
                synchronized (lockRead) {
                    message = new KMessage(e);
                    lockRead.notifyAll();
                }
            } catch (InterruptedException ignored) {
            }
            close();
        }
    }
}
