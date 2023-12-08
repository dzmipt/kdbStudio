package kx;

import studio.kdb.K;

import java.io.IOException;

public class KMessage {

    private K.KBase object = null;
    private K4Exception error = null;
    private IOException exception = null;
    private long bytesSent = 0;
    private long bytesReceived = 0;
    private K.KTimestamp started = K.KTimestamp.NULL;
    private K.KTimestamp finished = K.KTimestamp.NULL;


    public KMessage(K.KBase result) {
        this.object = result;
    }

    public KMessage(K4Exception error) {
        this.error = error;
    }

    public KMessage(IOException exception) {
        this.exception = exception;
    }

    public K.KBase getObject() {
        return object;
    }

    public Throwable getError() {
        if (error != null) return  error;
        if (exception != null) return  exception;
        return null;
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public K.KTimestamp getStarted() {
        return started;
    }

    public void setStarted(K.KTimestamp started) {
        this.started = started;
    }

    public K.KTimestamp getFinished() {
        return finished;
    }

    public void setFinished(K.KTimestamp finished) {
        this.finished = finished;
    }
}
