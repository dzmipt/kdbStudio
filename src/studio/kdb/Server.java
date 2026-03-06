package studio.kdb;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.EditorColorToken;
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

    public static final Server NO_SERVER = new Server("", "", 0, "", "", Color.WHITE, DefaultAuthenticationMechanism.NAME, false);

    public String getAuthenticationMechanism() {
        return authenticationMechanism;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public String getPassword() {
        return conn.getPassword();
    }

    public String getUsername() {
        return conn.getUser();
    }

    public boolean isFlipTLS() {
        return  flipTLS;
    }

    public static Server newServer() {
        String authMethod = Config.getInstance().getDefaultAuthMechanism();
        Credentials credentials = Config.getInstance().getDefaultCredentials(authMethod);
        QConnection conn = new QConnection("", 0, credentials.getUsername(), credentials.getPassword(), false);
        return new Server("", conn, authMethod, Config.getInstance().getEditorColors().get(EditorColorToken.BACKGROUND), null);
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

    public Server(String name, QConnection conn, String authMethod, Color bgColor, ServerTreeNode parent, boolean flipTLS) {
        if (parent != null && ! parent.isFolder()) throw new IllegalArgumentException("Parent ServerTreeNode can be folder only");

        this.name = name;
        this.conn = conn;
        this.backgroundColor = bgColor;
        this.authenticationMechanism = authMethod;
        this.parent = parent;
        this.flipTLS = flipTLS;
    }

    public Server newName(String name) {
        if (this.name.equals(name)) return this;
        return new Server(name, conn, authenticationMechanism, backgroundColor, parent);
    }

    public Server newAuthMethod(String authMethod) {
        if (this.authenticationMechanism.equals(authMethod)) return this;

        return new Server(name, conn, authMethod, backgroundColor, parent);
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
        return parent.getFolderPath().stream().skip(1).collect(Collectors.joining("/"));
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
        return new Server(name, conn, authenticationMechanism, backgroundColor, parent);
    }

}
