package studio.kdb;

import studio.core.Credentials;

import java.awt.*;
import java.util.Objects;
import java.util.Properties;

public class Server {
    private String authenticationMechanism;
    private Color backgroundColor = Color.white;
    private String name = "";
    private String host = "";
    private int port = 0;
    private String username;
    private String password;
    private boolean useTLS = false;
    private ServerTreeNode folder = null;

    public static final Server NO_SERVER = new Server();

    public Properties getAsProperties() {
        Properties p = new Properties();
        p.put("NAME", name);
        p.put("HOST", host);
        p.put("PORT", port);
        p.put("USERNAME", username);
        p.put("PASSWORD", password);
        p.put("USETLS", useTLS);
        return p;
    }

    public String getAuthenticationMechanism() {
        return authenticationMechanism;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public static Server newServer() {
        Server server = new Server();
        server.authenticationMechanism = Config.getInstance().getDefaultAuthMechanism();
        Credentials credentials = Config.getInstance().getDefaultCredentials(server.authenticationMechanism);
        server.username = credentials.getUsername();
        server.password = credentials.getPassword();
        return server;
    }

    private Server() {}

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Server)) return false;
        Server s = (Server) obj;
        return s.name.equals(name)
                && Objects.equals(s.host, host)
                && s.port == port
                && Objects.equals(s.username, username)
                && Objects.equals(s.password, password)
                && Objects.equals(s.authenticationMechanism ,authenticationMechanism)
                && s.useTLS == useTLS;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Server(Server s) {
        this.name = s.name;
        this.host = s.host;
        this.port = s.port;
        this.username = s.username;
        this.password = s.password;
        this.backgroundColor = s.backgroundColor;
        this.authenticationMechanism = s.authenticationMechanism;
        this.useTLS = s.useTLS;
        this.setFolder(s.getFolder());
    }

    public Server(String name, String host, int port, String username, String password, Color backgroundColor, String authenticationMechanism, boolean useTLS) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.backgroundColor = backgroundColor;
        this.authenticationMechanism = authenticationMechanism;
        this.useTLS = useTLS;
    }

    public Server newName(String name) {
        Server server = new Server(this);
        server.name = name;
        return server;
    }

    public Server newAuthMethod(String authMethod){
        Server server = new Server(this);
        server.authenticationMechanism = authMethod;
        return server;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        if (folder == null) return name;
        String path = folder.fullPath();
        if (path.length() == 0) return name;
        return path + "/" + name;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String toString() {
        return getFullName();
    }

    public String getConnectionString() {
        return "`:" + host + ":" + port;
    }

    public String getConnectionStringWithPwd() {
        return "`:" + host + ":" + port + ":" + username + ":" + password;
    }

    public String getDescription(boolean fullName) {
        String serverName = fullName ? getFullName() : name;
        String connection = host + ":" + port;
        if (serverName.equals("")) return connection;

        return serverName + " (" + connection + ")";
    }

    public boolean getUseTLS(){
      return useTLS;
    }

    public ServerTreeNode getFolder() {
        return folder;
    }

    //@TODO: all other properties are immutable. May be we need to do this one non-modifiable as well?
    public void setFolder(ServerTreeNode folder) {
        this.folder = folder;
    }

}
