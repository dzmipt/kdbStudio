package studio.utils;

import studio.core.Credentials;
import studio.kdb.Server;

import java.awt.*;
import java.util.Objects;

public class QConnection {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final boolean useTLS;

    private final static String TCPS_PREFIX = "tcps://";
    private final static String TCP_PREFIX = "tcp://";

    public static QConnection get(String connectionString) throws IllegalArgumentException {
        return new Parser(connectionString).getConnection();
    }

    public QConnection(String host, int port, String user, String password, boolean useTLS) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.useTLS = useTLS;
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
        return new Server(name, this, authMethod, bgColor);
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


    public QConnection changeTLS(boolean newUseTLS) {
        if (newUseTLS == this.useTLS) return this;

        return new QConnection(host, port, user, password, newUseTLS);
    }

    public QConnection changeUserPassword(Credentials credentials) {
        return changeUserPassword(credentials.getUsername(), credentials.getPassword());
    }

    public QConnection changeUserPassword(String newUser, String newPassword) {
        if (Objects.equals(newUser, this.user) && Objects.equals(newPassword, this.password)) return this;

        return new QConnection(host, port, newUser, newPassword, useTLS);
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
    
    
    public static class Parser {
        private IllegalArgumentException error = null;
        private QConnection connection = null;
        private boolean specifiedProtocol = false;
        private boolean specifiedUser = false;
        private boolean specifiedPassword = false;

        public Parser(QConnection connection) {
            this.connection = connection;
        }

        public Parser(String connectionString) {
            connectionString = connectionString.trim();
            if (connectionString.startsWith("`")) connectionString = connectionString.substring(1);
            if (connectionString.startsWith(":")) connectionString = connectionString.substring(1);

            boolean useTLS = connectionString.startsWith(TCPS_PREFIX);
            if (useTLS) {
                connectionString = connectionString.substring(TCPS_PREFIX.length());
                specifiedProtocol = true;
            } else if (connectionString.startsWith(TCP_PREFIX)) {
                connectionString = connectionString.substring(TCP_PREFIX.length());
                specifiedProtocol = true;
            }

            int i0 = connectionString.indexOf(':');
            if (i0 == -1) {
                error = new IllegalArgumentException("Wrong format of connectionString string");
                return;
            }
            String host = connectionString.substring(0, i0);

            i0++;
            int i1 = connectionString.indexOf(':', i0);
            if (i1 == -1) i1 = connectionString.length();
            int port;
            try {
                port = Integer.parseInt(connectionString.substring(i0, i1)); // could throw NumberFormatException
            } catch (NumberFormatException e) {
                error = new IllegalArgumentException("Port must be an integer", e);
                return;
            }

            String user = "";
            String password = "";
            if (i1 < connectionString.length()) {
                specifiedUser = true;
                String credentialsPart = connectionString.substring(i1 + 1);
                int credSeparatorIndex = credentialsPart.indexOf(':');
                if (credSeparatorIndex != -1) {
                    specifiedPassword = true;
                    user = credentialsPart.substring(0, credSeparatorIndex);
                    password = credentialsPart.substring(credSeparatorIndex + 1);
                } else {
                    user = credentialsPart;
                }
            }

            this.connection = new QConnection(host, port, user, password, useTLS);
        }

        public QConnection getConnection() throws IllegalArgumentException {
            if (error != null) throw error;
            return connection;
        }

        public boolean hasError() {
            return error != null;
        }

        public boolean isSpecifiedProtocol() {
            return specifiedProtocol;
        }

        public void setSpecifiedProtocol(boolean specifiedProtocol) {
            this.specifiedProtocol = specifiedProtocol;
        }

        public boolean isSpecifiedUser() {
            return specifiedUser;
        }

        public boolean isSpecifiedPassword() {
            return specifiedPassword;
        }
    }
}
