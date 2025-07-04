package studio.utils;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Config;
import studio.kdb.Server;

import java.awt.*;
import java.util.Collection;
import java.util.Objects;

public class QConnection {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final boolean useTLS;

    private final static String TCPS_PREFIX = "tcps://";

    public QConnection(String host, int port, String user, String password, boolean useTLS) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.useTLS = useTLS;
    }

    public QConnection(String connection) throws IllegalArgumentException {
        connection = connection.trim();
        if (connection.startsWith("`")) connection = connection.substring(1);
        if (connection.startsWith(":")) connection = connection.substring(1);

        useTLS = connection.startsWith(TCPS_PREFIX);
        if (useTLS) {
            connection = connection.substring(TCPS_PREFIX.length());
        }

        int i0 = connection.indexOf(':');
        if (i0 == -1) {
            throw new IllegalArgumentException("Wrong format of connection string");
        }
        host = connection.substring(0, i0);

        i0++;
        int i1 = connection.indexOf(':', i0);
        if (i1 == -1) i1 = connection.length();
        try {
            port = Integer.parseInt(connection.substring(i0, i1)); // could throw NumberFormatException
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }

        if (i1 == connection.length()) {
            user = "";
            password = "";
        } else {
            i1++;
            int i2 = connection.indexOf(':', i1);
            if (i2 == -1) i2 = connection.length();

            user = connection.substring(i1, i2);
            password = i2 == connection.length() ? "" : connection.substring(i2+1);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUseTLS() {
        return useTLS;
    }

    public Server toServer(String name, String authMethod, Color bgColor) {
        return new Server(name, host, port, user, password, bgColor, authMethod, useTLS);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includeUserPassword) {
        StringBuilder str = new StringBuilder("`:");
        if (useTLS) str.append(TCPS_PREFIX);
        str.append(host).append(':').append(port);

        if (includeUserPassword &&
                (!user.isEmpty() || !password.isEmpty()) ) {
            str.append(':').append(user);
            if (! password.isEmpty()) {
                str.append(':').append(password);
            }
        }
        return str.toString();
    }


    public static Server getByConnection(String connection , String defaultAuth,
                                         Credentials defaultCredentials, Collection<Server> servers) {

        QConnection conn = new QConnection(connection);
        String auth, user, password;
        if (conn.user.isEmpty() && conn.password.isEmpty()) {
            auth = defaultAuth;
            user = defaultCredentials.getUsername();
            password = defaultCredentials.getPassword();
        } else {
            auth = DefaultAuthenticationMechanism.NAME;
            user = conn.user;
            password = conn.password;
        }

        Color bgColor = Config.getInstance().getColor(Config.COLOR_BACKGROUND);

        for (Server s: servers) {
            if (s.getHost().equals(conn.host) && s.getPort() == conn.port && s.getUsername().equals(user) && s.getPassword().equals(password)
                        && s.getAuthenticationMechanism().equals(auth) && s.getUseTLS() == conn.useTLS) {
                return s;
            }
        }

        return new Server("", conn.host, conn.port, user, password, bgColor, auth, conn.useTLS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QConnection)) return false;
        QConnection that = (QConnection) o;
        return port == that.port && useTLS == that.useTLS && Objects.equals(host, that.host) && Objects.equals(user, that.user) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, user, password, useTLS);
    }
}
