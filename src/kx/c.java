// licensed according to http://code.kx.com/wiki/TermsAndConditions
package kx;

/*
types
20+ userenums
98 table
99 dict
100 lambda
101 unary prim
102 binary prim
103 ternary(operator)
104 projection
105 composition
106 f'
107 f/
108 f\
109 f':
110 f/:
111 f\:
112 dynamic load
 */

import studio.kdb.K;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class c {
    DataInputStream inputStream;
    OutputStream outputStream;
    Socket s;
    byte[] b;
    int rxBufferSize;

    void io(Socket s) throws IOException {
        s.setTcpNoDelay(true);
        inputStream = new DataInputStream(s.getInputStream());
        outputStream = s.getOutputStream();
        rxBufferSize = s.getReceiveBufferSize();
    }

    public void close() {
        // this will force k() to break out i hope
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

    public static class K4AccessException extends K4Exception {
        K4AccessException() {
            super("Authentication failed");
        }
    }

    boolean closed = true;

    public boolean isClosed() {
        return closed;
    }

    private void connect(boolean retry) throws IOException, K4AccessException {
        s = new Socket();
        s.setReceiveBufferSize(1024 * 1024);
        s.connect(new InetSocketAddress(host, port));

        if (useTLS) {
            try {
                s = ((SSLSocketFactory) SSLSocketFactory.getDefault()).createSocket(s, host, port, true);
                ((SSLSocket) s).startHandshake();
            } catch (Exception e) {
                s.close();
                throw e;
            }
        }
        io(s);
        java.io.ByteArrayOutputStream baos = new ByteArrayOutputStream();
        java.io.DataOutputStream dos = new DataOutputStream(baos);
        dos.write((up + (retry ? "\3" : "")).getBytes());
        dos.writeByte(0);
        dos.flush();
        outputStream.write(baos.toByteArray());
        byte[] bytes = new byte[2 + up.getBytes().length];
        if (1 != inputStream.read(bytes, 0, 1))
            if (retry)
                connect(false);
            else
                throw new K4AccessException();
        closed = false;
    }

    private String host;
    private int port;
    private String up;
    private boolean useTLS;

    public c(String h, int p, String u, boolean useTLS) {
        host = h;
        port = p;
        up = u;
        this.useTLS = useTLS;
    }

    void w(int i, K.KBase x) throws IOException {
        java.io.ByteArrayOutputStream baosBody = new ByteArrayOutputStream();
        java.io.DataOutputStream dosBody = new DataOutputStream(baosBody);
        x.serialise(dosBody);

        java.io.ByteArrayOutputStream baosHeader = new ByteArrayOutputStream();
        java.io.DataOutputStream dosHeader = new DataOutputStream(baosHeader);
        dosHeader.writeByte(0);
        dosHeader.writeByte(i);
        dosHeader.writeByte(0);
        dosHeader.writeByte(0);
        int msgSize = 8 + dosBody.size();
        K.write(dosHeader, msgSize);
        byte[] b = baosHeader.toByteArray();
        outputStream.write(b);
        b = baosBody.toByteArray();
        outputStream.write(b);
    }

    public static class K4Exception extends Exception {
        K4Exception(String s) {
            super(s);
        }
    }


    private K.KBase k(ProgressCallback progress) throws K4Exception, IOException {
        boolean firstMessage = true;
        boolean responseMsg = false;
        boolean c = false;
        boolean isLittleEndian = true;
        while (!responseMsg) { // throw away incoming aync, and error out on incoming sync
            if (firstMessage) {
                firstMessage = false;
            } else {
                inputStream.readFully(b = new byte[8]);
            }
            isLittleEndian = b[0] == 1;
            c = b[2] == 1;
            byte msgType = b[1];
            if (msgType == 1) {
                close();
                throw new IOException("Cannot process sync msg from remote");
            }
            responseMsg = msgType == 2;

            IPC ipc = new IPC(b, 4, false, isLittleEndian);
            final int msgLength = ipc.ri() - 8;

            if (progress!=null) {
                progress.setCompressed(c);
                progress.setMsgLength(msgLength);
            }

            b = new byte[msgLength];
            int total = 0;
            int packetSize = 1 + msgLength / 100;
            if (packetSize < rxBufferSize)
                packetSize = rxBufferSize;

            while (total < msgLength) {
                int remainder = msgLength - total;
                if (remainder < packetSize)
                    packetSize = remainder;

                int count = inputStream.read(b, total, packetSize);
                if (count < 0) throw new EOFException("Connection is broken");
                total += count;
                if (progress != null) progress.setCurrentProgress(total);
            }
        }

        return IPC.deserialise(b, c, isLittleEndian);
    }


    public synchronized K.KBase k(K.KBase x, ProgressCallback progress) throws K4Exception, IOException {
        try {

            if (isClosed()) connect(true);
            try {
                w(1, x);
                inputStream.readFully(b = new byte[8]);
            } catch (IOException e) {
                close();
                // may be the socket was closed on the server side?
                connect(true);
                w(1, x);
            inputStream.readFully(b = new byte[8]);
            }

            return k(progress);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public K.KBase k(K.KBase x) throws K4Exception, IOException {
        return k(x, null);
    }
}
