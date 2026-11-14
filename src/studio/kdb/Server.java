package studio.kdb;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.utils.QConnection;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Server {
    // null means the default auth.method from the Settings (with the default credentials for it)
    private final String authenticationMechanism;
    // null means the default background color from the style schema
    private final Color backgroundColor;
    private final String name;
    // null user and password mean the default credentials from the Settings for the auth.method
    private final QConnection conn;
    private final ServerTreeNode parent;
    private final boolean flipTLS;

    public static final Server NO_SERVER = new Server("", "", 0, "", "", null, DefaultAuthenticationMechanism.NAME, false);

    // null means the default auth.method from the Settings
    public String getAuthenticationMechanism() {
        return authenticationMechanism;
    }

    public boolean isDefaultAuthMethod() {
        return authenticationMechanism == null;
    }

    public boolean isDefaultCredentials() {
        return conn.getUser() == null;
    }

    // null means the default background color from the style schema
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Server newBackgroundColor(Color backgroundColor) {
        if (Objects.equals(this.backgroundColor, backgroundColor)) return this;
        return new Server(name, conn, authenticationMechanism, backgroundColor, parent, flipTLS);
    }

    // null means the default credentials
    public String getPassword() {
        return conn.getPassword();
    }

    // null means the default credentials
    public String getUsername() {
        return conn.getUser();
    }

    public boolean isFlipTLS() {
        return  flipTLS;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Server)) return false;
        Server s = (Server) obj;
        boolean res =  s.name.equals(name)
                    && Objects.equals(s.conn, conn)
                    && s.flipTLS == flipTLS
                    && Objects.equals(s.authenticationMechanism ,authenticationMechanism);

        if (! res) return false;
        if (parent == null|| s.parent == null) return true;

        return parent.getFolderPath().equals(s.parent.getFolderPath());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Server(String name, String host, int port, String username, String password, Color backgroundColor, String authenticationMechanism, boolean useTLS) {
        this(name, host, port, username, password, backgroundColor, authenticationMechanism, useTLS, null);
    }

    public Server(String name, String host, int port, String username, String password, Color backgroundColor,
                  String authenticationMechanism, boolean useTLS, ServerTreeNode parent) {
        this(name, new QConnection(host, port, username, password, useTLS), authenticationMechanism, backgroundColor, parent);
    }

    public Server(String name, QConnection conn, String authMethod, Color bgColor) {
        this(name, conn, authMethod, bgColor, null);
    }

    public Server(String name, QConnection conn, String authMethod, Color bgColor, ServerTreeNode parent) {
        this (name, conn, authMethod, bgColor, parent, false);
    }

    /**
     * @param conn connection; null user (and password) mean default credentials for the auth.method
     * @param authMethod null means default auth.method (and default credentials)
     * @param bgColor null means default background color
     */
    public Server(String name, QConnection conn, String authMethod, Color bgColor, ServerTreeNode parent, boolean flipTLS) {
        if (parent != null && ! parent.isFolder()) throw new IllegalArgumentException("Parent ServerTreeNode can be folder only");

        this.name = name;
        this.conn = normalizeCredentials(conn, authMethod);
        this.backgroundColor = bgColor;
        this.authenticationMechanism = authMethod;
        this.parent = parent;
        this.flipTLS = flipTLS;
    }

    // The default auth.method implies the default credentials; user and password are either both null or both not null
    private static QConnection normalizeCredentials(QConnection conn, String authMethod) {
        String user = conn.getUser();
        String password = conn.getPassword();
        if (authMethod == null || user == null) return conn.changeUserPassword(null, null);
        if (password == null) return conn.changeUserPassword(user, "");
        return conn;
    }

    public Server newCredentials(Credentials credentials) {
        QConnection newConn = credentials == null ?
                conn.changeUserPassword(null, null) : conn.changeUserPassword(credentials);
        if (newConn == conn) return this;
        return new Server(name, newConn, authenticationMechanism, backgroundColor, parent, flipTLS);
    }


    public Server newName(String name) {
        if (this.name.equals(name)) return this;
        return new Server(name, conn, authenticationMechanism, backgroundColor, parent, false);
    }

    public Server newAuthMethod(String authMethod) {
        if (Objects.equals(this.authenticationMechanism, authMethod)) return this;

        return new Server(name, conn, authMethod, backgroundColor, parent, flipTLS);
    }

    public Server newFlipTLS(boolean flipTLS) {
        if (this.flipTLS == flipTLS) return this;

        return new Server(name, conn, authenticationMechanism, backgroundColor, parent, flipTLS);
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        if (parent == null || parent.isRoot()) return name;
        return getFolderName() + "/" + name;
    }

    public String getFolderName() {
        if (parent == null) return "";
        return getFolderName(parent.getFolderPath());
    }

    public static String getFolderName(List<String> folderPath) {
        if (folderPath == null) return "";
        return folderPath.stream().skip(1).collect(Collectors.joining("/"));
    }

    public String getHost() {
        return conn.getHost();
    }

    public int getPort() {
        return conn.getPort();
    }

    public String toString() {
        return getFullName();
    }

    public String getConnectionString() {
        return conn.toString(false);
    }

    // null user and password mean the default credentials
    public QConnection getConnection() {
        return conn;
    }

    public String getConnectionStringWithPwd() {
        return conn.toString();
    }

    public String getDescription(boolean fullName) {
        String serverName = fullName ? getFullName() : name;
        String connection = getHost() + ":" + getPort();
        if (serverName.equals("")) return connection;

        return serverName + " (" + connection + ")";
    }

    public boolean getUseTLS(){
      return conn.isUseTLS();
    }

    public List<String> getFolderPath() {
        if (parent == null) return Collections.EMPTY_LIST;
        return parent.getFolderPath();
    }

    public boolean inServerTree() {
        return parent != null;
    }

    public ServerTreeNode getParent() {
        return parent;
    }

    public Server newParent(ServerTreeNode parent) {
        return new Server(name, conn, authenticationMechanism, backgroundColor, parent, false);
    }

}
