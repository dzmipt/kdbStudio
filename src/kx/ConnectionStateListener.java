package kx;

import java.io.IOException;

public interface ConnectionStateListener {

    void connectionStateChange(boolean connected);
    void checkIncomingLimit(long msgLength) throws IOException;

}
