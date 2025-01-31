package studio.kdb;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.utils.QConnection;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

public class Server {
    private final String authenticationMechanism;
    private final Color backgroundColor;
    private final String name;
    private final QConnection conn;
    private final List<String> folderPath;
    private final static List<String> ROOT_FOLDER = (List<String>) Collections.EMPTY_LIST;

    public static final Server NO_SERVER = new Server("", "", 0, "", "", Color.WHITE, DefaultAuthenticationMechanism.NAME, false);

    public Properties getAsProperties() {
        Properties p = new Properties();
        p.put("NAME", name);
        p.put("HOST", conn.getHost());
        p.put("PORT", conn.getPort());
        p.put("USERNAME", conn.getUser());
        p.put("PASSWORD", conn.getPassword());
        p.put("USETLS", conn.isUseTLS());
        return p;
    }

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

    public static Server newServer() {
        String authMethod = Config.getInstance().getDefaultAuthMechanism();
        Credentials credentials = Config.getInstance().getDefaultCredentials(authMethod);
        QConnection conn = new QConnection("", 0, credentials.getUsername(), credentials.getPassword(), false);
        return new Server("", conn, authMethod, Color.WHITE, ROOT_FOLDER);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Server)) return false;
        Server s = (Server) obj;
        return s.name.equals(name)
                && Objects.equals(s.conn, conn)
                && Objects.equals(s.authenticationMechanism ,authenticationMechanism);
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

    public Server(String name, QConnection conn, String authMethod, Color bgColor, ServerTreeNode parent) {
        this(name, conn, authMethod, bgColor,
                parent == null ? ROOT_FOLDER : parent.getFolderPath() );
    }

    public Server(String name, QConnection conn, String authMethod, Color bgColor, List<String> folderPath) {
        this.name = name;
        this.conn = conn;
        this.backgroundColor = bgColor;
        this.authenticationMechanism = authMethod;
        this.folderPath = folderPath;
    }

    public Server newName(String name) {
        return new Server(name, conn, authenticationMechanism, backgroundColor, folderPath);
    }

    public Server newAuthMethod(String authMethod){
        return new Server(name, conn, authMethod, backgroundColor, folderPath);
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        if (folderPath.size() < 2) return  name;
        return getFolderName() + "/" + name;
    }

    public String getFolderName() {
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
        return "`:" + getHost() + ":" + getPort();
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
        return folderPath;
    }

    public Server newFolder(ServerTreeNode folder) {
        return new Server(name, conn, authenticationMechanism, backgroundColor, folder.getFolderPath());
    }

}
