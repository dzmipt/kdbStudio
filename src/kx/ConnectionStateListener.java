package kx;

import java.io.IOException;

public interface ConnectionStateListener {

    void connectionStateChange(ConnectionContext context);
    void checkIncomingLimit(long msgLength) throws IOException;

}
