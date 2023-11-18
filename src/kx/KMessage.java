package kx;

import studio.kdb.K;

import java.io.IOException;

public class KMessage {

    private K.KBase object = null;
    private K4Exception error = null;
    private IOException exception = null;

    public KMessage(K.KBase result) {
        this.object = result;
    }

    public KMessage(K4Exception error) {
        this.error = error;
    }

    public KMessage(IOException exception) {
        this.exception = exception;
    }

    public K.KBase getObject() throws K4Exception, IOException {
        if (error != null) throw error;
        if (exception != null) throw exception;

        return object;
    }
}
