package studio.kdb;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private String name = "";
    private String host = "";
    private int port = 0;
    private List<String> folderPath = (List<String>) Collections.EMPTY_LIST;

    private Optional<String> cfgUsername;
    private Optional<String> cfgPassword;
    private Optional<Boolean> cfgUseTLS;
    private Optional<String> cfgAuthMethod;
    private Optional<Color> cfgBgColor;

    private String username;
    private String password;
    private boolean useTLS = false;
    private Color backgroundColor = Color.white;
    private String authenticationMechanism = DefaultAuthenticationMechanism.NAME;

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
        this.folderPath = s.folderPath;
    }

    public Server(String name, String host, int port, String username, String password, Color backgroundColor,
                  String authenticationMechanism, boolean useTLS) {
        this(name, host, port, Optional.of(username), Optional.of(password), Optional.of(backgroundColor),
                Optional.of(authenticationMechanism), Optional.of(useTLS));
    }

    public Server(String name, String host, int port, Optional<String> username, Optional<String> password, Optional<Color> backgroundColor,
                  Optional<String> authenticationMechanism, Optional<Boolean> useTLS) {
        this(name, host, port, username, password, backgroundColor, authenticationMechanism, useTLS, (List<String>) null);
    }

    public Server(String name, String host, int port, String username, String password, Color backgroundColor,
                  String authenticationMechanism, boolean useTLS, ServerTreeNode parent) {
        this(name, host, port, Optional.of(username), Optional.of(password), Optional.of(backgroundColor),
                Optional.of(authenticationMechanism), Optional.of(useTLS), parent);
    }

    public Server(String name, String host, int port, Optional<String> username, Optional<String> password, Optional<Color> backgroundColor,
                  Optional<String> authenticationMechanism, Optional<Boolean> useTLS, ServerTreeNode parent) {
        this(name, host, port, username, password, backgroundColor, authenticationMechanism, useTLS,
                parent == null ? null : parent.getFolderPath() );
    }

    public Server(String name, String host, int port, Optional<String> username, Optional<String> password, Optional<Color> backgroundColor,
                  Optional<String> authenticationMechanism, Optional<Boolean> useTLS, List<String> folderPath) {
        this.name = name;
        this.host = host;
        this.port = port;

        cfgUsername = username;
        cfgUsername.ifPresent(s -> this.username = s);

        cfgPassword = password;
        cfgPassword.ifPresent(s -> this.password = s);

        cfgBgColor = backgroundColor;
        cfgBgColor.ifPresent(color -> this.backgroundColor = color);

        cfgAuthMethod = authenticationMechanism;
        cfgAuthMethod.ifPresent(s -> this.authenticationMechanism = s);

        cfgUseTLS = useTLS;
        cfgUseTLS.ifPresent(aBoolean -> this.useTLS = aBoolean);

        if (folderPath != null) this.folderPath = folderPath;

    }

    public Optional<String> getCfgUsername() {
        return cfgUsername;
    }

    public Optional<String> getCfgPassword() {
        return cfgPassword;
    }

    public Optional<Boolean> getCfgUseTLS() {
        return cfgUseTLS;
    }

    public Optional<String> getCfgAuthMethod() {
        return cfgAuthMethod;
    }

    public Optional<Color> getCfgBgColor() {
        return cfgBgColor;
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
        if (folderPath.size() < 2) return  name;
        return getFolderName() + "/" + name;
    }

    public String getFolderName() {
        return folderPath.stream().skip(1).collect(Collectors.joining("/"));
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

    public List<String> getFolderPath() {
        return folderPath;
    }

    public Server newFolder(ServerTreeNode folder) {
        Server newServer = new Server(this);
        newServer.folderPath = folder.getFolderPath();
        return newServer;
    }

}
