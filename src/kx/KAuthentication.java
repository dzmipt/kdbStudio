package kx;

import java.io.IOException;

public interface KAuthentication {

    String getUserPassword(ConnectionContext context) throws IOException;
}
