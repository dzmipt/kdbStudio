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
    private final String authenticationMechanism;
    private final Color backgroundColor;
    private final String name;
    private final QConnection conn;
    private final ServerTreeNode parent;
    private final boolean flipTLS;
    private final boolean defaultAuthMethod;
    private final boolean defaultCredentials;

    public static final Server NO_SERVER = new Server("", "", 0, "", "", Color.WHITE, DefaultAuthenticationMechanism.NAME, false);

    /**
     * Effective auth. method: from the Settings if the server uses default auth.method
     */
    public String getAuthenticationMechanism() {
        if (defaultAuthMethod) return Config.getInstance().getDefaultAuthMechanism();
        return authenticationMechanism;
    }

    /**
     * Auth. method stored with the server (ignoring the default auth.method flag)
     */
    public String getServerAuthenticationMechanism() {
        return authenticationMechanism;
    }

    public boolean isDefaultAuthMethod() {
        return defaultAuthMethod;
    }

    public boolean isDefaultCredentials() {
        return defaultCredentials;
    }

    private boolean useDefaultCredentials() {
        return defaultAuthMethod || defaultCredentials;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public String getPassword() {
        return getConnection().getPassword();
    }

    public String getUsername() {
        return getConnection().getUser();
    }

    public String getServerPassword() {
        return conn.getPassword();
    }

    public String getServerUsername() {
        return conn.getUser();
    }

    public boolean isFlipTLS() {
        return  flipTLS;
    }

    public static Server newServer() {
        String authMethod = Config.getInstance().getDefaultAuthMechanism();
        Credentials credentials = Config.getInstance().getDefaultCredentials(authMethod);
        QConnection conn = new QConnection("", 0, credentials.getUsername(), credentials.getPassword(), false);
        return new Server("", conn, authMethod, Config.getInstance().getBackgroundColor(), null, false, true, true);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Server)) return false;
        Server s = (Server) obj;
        boolean res =  s.name.equals(name)
                    && Objects.equals(s.conn, conn)
                    && s.flipTLS == flipTLS
                    && s.defaultAuthMethod == defaultAuthMethod
                    && s.defaultCredentials == defaultCredentials
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

    public Server(String name, QConnection conn, String authMethod, Color bgColor, ServerTreeNode parent, boolean flipTLS) {
        this(name, conn, authMethod, bgColor, parent, flipTLS, false, false);
    }

    public Server(String name, QConnection conn, String authMethod, Color bgColor, ServerTreeNode parent, boolean flipTLS,
                  boolean defaultAuthMethod, boolean defaultCredentials) {
        if (parent != null && ! parent.isFolder()) throw new IllegalArgumentException("Parent ServerTreeNode can be folder only");

        this.name = name;
        this.conn = conn;
        this.backgroundColor = bgColor;
        this.authenticationMechanism = authMethod;
        this.parent = parent;
        this.flipTLS = flipTLS;
        this.defaultAuthMethod = defaultAuthMethod;
        this.defaultCredentials = defaultCredentials;
    }


    public Server newName(String name) {
        if (this.name.equals(name)) return this;
        return new Server(name, conn, authenticationMechanism, backgroundColor, parent, false, defaultAuthMethod, defaultCredentials);
    }

    public Server newAuthMethod(String authMethod) {
        if (!defaultAuthMethod && this.authenticationMechanism.equals(authMethod)) return this;

        return new Server(name, conn, authMethod, backgroundColor, parent, false, false, defaultCredentials);
    }

    public Server newFlipTLS(boolean flipTLS) {
        if (this.flipTLS == flipTLS) return this;

        return new Server(name, conn, authenticationMechanism, backgroundColor, parent, flipTLS, defaultAuthMethod, defaultCredentials);
    }

    public Server newBgColor(Color bgColor) {
        if (this.backgroundColor.equals(bgColor)) return this;

        return new Server(name, conn, authenticationMechanism, bgColor, parent, flipTLS, defaultAuthMethod, defaultCredentials);
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
        return getConnection().toString(false);
    }

    /**
     * Effective connection: user and password are taken from the Settings if the server uses default credentials
     */
    public QConnection getConnection() {
        if (! useDefaultCredentials()) return conn;
        Credentials credentials = Config.getInstance().getDefaultCredentials(getAuthenticationMechanism());
        return conn.changeUserPassword(credentials);
    }

    /**
     * Connection with user and password stored with the server (ignoring default credentials flags)
     */
    public QConnection getServerConnection() {
        return conn;
    }

    public String getConnectionStringWithPwd() {
        return getConnection().toString();
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
        return new Server(name, conn, authenticationMechanism, backgroundColor, parent, false, defaultAuthMethod, defaultCredentials);
    }

}
