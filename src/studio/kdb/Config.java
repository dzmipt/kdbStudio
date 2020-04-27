package studio.kdb;

import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.ui.ServerList;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Config {
    private final static String PATH = System.getProperties().getProperty("user.home") + "/.studioforkdb/";
    private final static String FILENAME = PATH + "studio.properties";
    private final static String VERSION = "1.2";
    private final static String OLD_VERSION = "1.1";

    private Properties p = new Properties();
    private final Map<String, Server> servers = new HashMap<>();
    private Collection<String> serverNames;
    private ServerTreeNode serverTree;

    private final static Config instance = new Config();

    private Config() {
        init();
    }

    public Font getFont() {
        String name = p.getProperty("font.name", "Monospaced");
        int  size = Integer.parseInt(p.getProperty("font.size","14"));

        Font f = new Font(name, Font.PLAIN, size);
        setFont(f);

        return f;
    }

    public String getEncoding() {
        return p.getProperty("encoding", "UTF-8");
    }

    public void setFont(Font f) {
        p.setProperty("font.name", f.getFamily());
        p.setProperty("font.size", "" + f.getSize());
        save();
    }

    public Color getColorForToken(String tokenType, Color defaultColor) {
        String s = p.getProperty("token." + tokenType);
        if (s != null) {
            return new Color(Integer.parseInt(s, 16));
        }

        setColorForToken(tokenType, defaultColor);
        return defaultColor;
    }

    public void setColorForToken(String tokenType, Color c) {
        p.setProperty("token." + tokenType, Integer.toHexString(c.getRGB()).substring(2));
        save();
    }

    public Color getDefaultBackgroundColor() {
        return getColorForToken("BACKGROUND", Color.white);
    }

    public synchronized NumberFormat getNumberFormat() {
        String key = p.getProperty("DecimalFormat", "#.#######");

        return new DecimalFormat(key);
    }

    public static Config getInstance() {
        return instance;
    }

    private void init() {
        Path file = Paths.get(FILENAME);
        Path dir = file.getParent();
        if (Files.notExists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                System.err.println("Can't create configuration folder: " + PATH);
            }
            return;
        }

        try {
            InputStream in = Files.newInputStream(file);
            p.load(in);
            in.close();
        } catch (IOException e) {
            System.err.println("Cant't read configuration from file " + FILENAME);
            e.printStackTrace(System.err);
        }
        initServers();
    }


    public void save() {
        try {
            OutputStream out = new FileOutputStream(FILENAME);
            p.put("version", VERSION);
            p.store(out, "Auto-generated by Studio for kdb+");
            out.close();
        } catch (IOException e) {
            System.err.println("Can't save configuration to " + FILENAME);
            e.printStackTrace(System.err);  //To change body of catch statement use Options | File Templates.
        }
    }

    // "".split(",") return {""}; we need to get zero length array
    private String[] split(String str) {
        str = str.trim();
        if (str.length() == 0) return new String[0];
        return str.split(",");
    }

    public String[] getQKeywords() {
        String key = p.getProperty("qkeywords", "");
        return split(key);
    }

    public String getLRUServer() {
        return p.getProperty("lruServer", "");
    }

    public void setLRUServer(Server s) {
        if (s == null) return; // May be it should be an exception ?

        p.put("lruServer", s.getFullName());
        save();
    }


    public void saveQKeywords(String[] keywords) {
        p.put("qkeywords", String.join(",",keywords));
        save();
    }

    public void setAcceptedLicense(Date d) {
        p.put("licenseAccepted", d.toString());
        save();
    }

    public String[] getMRUFiles() {
        String mru = p.getProperty("mrufiles", "");
        return split(mru);
    }


    public void saveMRUFiles(String[] mruFiles) {
        String value = Stream.of(mruFiles).limit(9).collect(Collectors.joining(","));
        p.put("mrufiles", value);
        save();
    }

    public String getLookAndFeel() {
        return p.getProperty("lookandfeel");
    }

    public void setLookAndFeel(String lf) {
        p.put("lookandfeel", lf);
        save();
    }

    // Resolve or create a new server by connection string.
    // Accept possible various connectionString such as:
    // `:host:port:user:password
    // host:port
    // If user and password are not found, defaults form default AuthenticationMechanism are used
    public Server getServerByConnectionString(String connectionString) {
        connectionString = connectionString.trim();
        if (connectionString.startsWith("`")) connectionString = connectionString.substring(1);
        if (connectionString.startsWith(":")) connectionString = connectionString.substring(1);

        String[] nodes = connectionString.split(":");
        if (nodes.length < 2) {
            throw new IllegalArgumentException("Wrong format of connection string");
        }

        String host = nodes[0];
        int port = Integer.parseInt(nodes[1]); // could throw NumberFormatException

        String auth = getDefaultAuthMechanism();
        String user, password;
        if (nodes.length == 2) {
            Credentials credentials = getDefaultCredentials(auth);
            user = credentials.getUsername();
            password = credentials.getPassword();
        } else {
            user = nodes[2];
            password = nodes.length > 3 ? Stream.of(nodes).skip(3).collect(Collectors.joining(":")) : "";
        }

        Color bgColor = Config.getInstance().getDefaultBackgroundColor();

        for (Server s: getServers()) {
            if (s.getHost().equals(host) && s.getPort() == port && s.getUsername().equals(user) && s.getPassword().equals(password)) {
                return s;
            }
        }

        return new Server("", host, port, user, password, bgColor, auth, false);
    }

    public Credentials getDefaultCredentials(String authenticationMechanism) {
        String user = p.getProperty("auth." + authenticationMechanism + ".user", "");
        String password = p.getProperty("auth." + authenticationMechanism + ".password", "");
        return new Credentials(user, password);
    }

    public void setDefaultCredentials(String authenticationMechanism, Credentials credentials) {
        p.setProperty("auth." + authenticationMechanism + ".user", credentials.getUsername());
        p.setProperty("auth." + authenticationMechanism + ".password", credentials.getPassword());
        save();
    }

    public String getDefaultAuthMechanism() {
        return p.getProperty("auth", DefaultAuthenticationMechanism.NAME);
    }

    public void setDefaultAuthMechanism(String authMechanism) {
        p.setProperty("auth", authMechanism);
        save();
    }

    public void setServerListBounds(Rectangle rectangle) {
        p.setProperty("serverList.x", "" + (int)rectangle.getX());
        p.setProperty("serverList.y", "" + (int)rectangle.getY());
        p.setProperty("serverList.width", "" + (int)rectangle.getWidth());
        p.setProperty("serverList.height", "" + (int)rectangle.getHeight());
        save();
    }

    public Rectangle getServerListBounds() {
        String strX = p.getProperty("serverList.x");
        String strY = p.getProperty("serverList.y");
        String strWidth = p.getProperty("serverList.width");
        String strHeight = p.getProperty("serverList.height");

        if (strX != null && strY != null && strWidth != null && strHeight != null) {
            return new Rectangle(Integer.parseInt(strX), Integer.parseInt(strY),
                                Integer.parseInt(strWidth), Integer.parseInt(strHeight));
        }

        DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                            .getDefaultScreenDevice().getDisplayMode();

        int width = displayMode.getWidth();
        int height = displayMode.getHeight();

        int w = Math.min(width / 2, ServerList.DEFAULT_WIDTH);
        int h = Math.min(height / 2, ServerList.DEFAULT_HEIGHT);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        return new Rectangle(x,y,w,h);
    }

    public Collection<String> getServerNames() {
        return Collections.unmodifiableCollection(serverNames);
    }

    public Server[] getServers() {
        return servers.values().toArray(new Server[servers.size()]);
    }

    public Server getServer(String name) {
        return servers.get(name);
    }

    public ServerTreeNode getServerTree() {
        return serverTree;
    }


    private Server initServerFromKey(String key) {
        String host = p.getProperty("server." + key + ".host", "");
        int port = Integer.parseInt(p.getProperty("server." + key + ".port", "-1"));
        String username = p.getProperty("server." + key + ".user", "");
        String password = p.getProperty("server." + key + ".password", "");
        String backgroundColor = p.getProperty("server." + key + ".backgroundColor", "FFFFFF");
        String authenticationMechanism = p.getProperty("server." + key + ".authenticationMechanism", DefaultAuthenticationMechanism.NAME);
        boolean useTLS = Boolean.parseBoolean(p.getProperty("server." + key + ".useTLS", "false"));
        Color c = new Color(Integer.parseInt(backgroundColor, 16));
        return new Server("", host, port, username, password, c, authenticationMechanism, useTLS);
    }

    private Server initServerFromProperties(int number) {
        return initServerFromKey("" + number);
    }

    private void convertFromOldVerion() {
        try {
            System.out.println("Found old config. Converting...");
            String[] names = p.getProperty("Servers").split(",");
            List<Server> list = new ArrayList<>();
            for (String name : names) {
                Server server = initServerFromKey(name);
                server.setName(name);
                list.add(server);
            }
            p.remove("Servers");
            p.entrySet().removeIf(e -> e.getKey().toString().startsWith("server."));
            p.setProperty("version", VERSION);
            initServers();
            addServers(list.toArray(new Server[0]));
            System.out.println("Done");
        } catch (IllegalArgumentException e) {
            System.err.println("Ups... Can't convert: " + e);
            e.printStackTrace(System.err);
        }
    }

    private void initServers() {
        if (p.getProperty("version").equals(OLD_VERSION)) {
            convertFromOldVerion();
        }
        serverNames = new ArrayList<>();
        serverTree = new ServerTreeNode();
        initServerTree("serverTree.", serverTree, 0);
    }

    private int initServerTree(String keyPrefix, ServerTreeNode parent, int number) {
        for (int index = 0; ; index++) {
            String key = keyPrefix + index;
            String folderKey = key + "folder";
            if (p.containsKey(folderKey)) {
                ServerTreeNode node = parent.add(p.getProperty(folderKey));
                number = initServerTree(key + ".", node, number);
            } else if (p.containsKey(key)) {
                Server server = initServerFromProperties(number);
                server.setFolder(parent);
                String name = p.getProperty(key);
                server.setName(name);
                String fullName = server.getFullName();
                servers.put(fullName, server);
                serverNames.add(fullName);
                parent.add(server);
                number++;
            } else {
                break;
            }
        }
        return number;
    }

    private void saveAllServers() {
        p.entrySet().removeIf(e -> e.getKey().toString().startsWith("serverTree."));
        p.entrySet().removeIf(e -> e.getKey().toString().startsWith("server."));
        saveServerTree("serverTree.", serverTree, 0);

        save();
    }

    private void saveServerDetails(Server server, int number) {
        p.setProperty("server." + number + ".host", server.getHost());
        p.setProperty("server." + number + ".port", "" + server.getPort());
        p.setProperty("server." + number + ".user", "" + server.getUsername());
        p.setProperty("server." + number + ".password", "" + server.getPassword());
        p.setProperty("server." + number + ".backgroundColor", "" + Integer.toHexString(server.getBackgroundColor().getRGB()).substring(2));
        p.setProperty("server." + number + ".authenticationMechanism", server.getAuthenticationMechanism());
        p.setProperty("server." + number + ".useTLS", "" + server.getUseTLS());
    }

    private int saveServerTree(String keyPrefix, ServerTreeNode node, int number) {
        int count = node.getChildCount();
        for(int index = 0; index<count; index++) {
            String key = keyPrefix + index;
            ServerTreeNode child = node.getChild(index);
            if (child.isFolder()) {
                p.setProperty(key + "folder", child.getFolder());
                number = saveServerTree(key + ".", child, number);
            } else {
                Server server = child.getServer();
                p.setProperty(key, server.getName());
                saveServerDetails(server, number);
                number++;
            }
        }
        return number;
    }

    public void removeServer(Server server) {
        String name = server.getFullName();
        serverNames.remove(name);
        servers.remove(name);
        ServerTreeNode folder = server.getFolder();
        if (folder != null) {
            folder.remove(server);
        }

        saveAllServers();
    }

    private void purgeAll() {
        servers.clear();
        serverNames.clear();
        serverTree = new ServerTreeNode();
    }

    public void removeAllServers() {
        purgeAll();
        saveAllServers();
    }

    private void addServerInternal(Server server) {
        String name = server.getName();
        String fullName = server.getFullName();
        if (serverNames.contains(fullName)) {
            throw new IllegalArgumentException("Server with full name " + fullName + " already exist");
        }
        if (name.trim().length() == 0) {
            throw new IllegalArgumentException("Server name can't be empty");
        }
        if (name.contains(",")) {
            throw new IllegalArgumentException("Server name can't contains ,");
        }
        if (name.contains("/")) {
            throw new IllegalArgumentException("Server name can't contains /");
        }
        servers.put(fullName, server);
        serverNames.add(fullName);
    }


    public void addServer(Server server) {
        addServers(server);
    }

    public void addServers(Server... newServers) {
        Properties backup = new Properties();
        backup.putAll(p);
        try {
            for (Server server : newServers) {
                ServerTreeNode folder = server.getFolder();
                if (folder == null) {
                    server.setFolder(serverTree);
                    serverTree.add(server);
                } else {
                    folder.add(server);
                }
                addServerInternal(server);
            }

            saveAllServers();
        } catch (IllegalArgumentException e) {
            p = backup;
            initServers();
            throw e;
        }

    }

    public void setServerTree(ServerTreeNode serverTree) {
        Properties backup = new Properties();
        backup.putAll(p);
        try {
            purgeAll();
            this.serverTree = serverTree;

            for(Enumeration e = serverTree.depthFirstEnumeration(); e.hasMoreElements();) {
                ServerTreeNode node = (ServerTreeNode) e.nextElement();
                if (node.isRoot()) continue;

                if (node.isFolder()) {
                    String folder = node.getFolder();
                    if (folder.trim().length()==0) {
                        throw new IllegalArgumentException("Can't add folder with empty name");
                    }
                    if (folder.contains("/")) {
                        throw new IllegalArgumentException("Folder can't contain /");
                    }
                    if ( ((ServerTreeNode)node.getParent()).getChild(node.getFolder())!= node ) {
                        throw new IllegalArgumentException("Duplicate folder is found: " + node.fullPath());
                    }
                } else {
                    Server server = node.getServer();
                    server.setFolder((ServerTreeNode) node.getParent());
                    addServerInternal(server);
                }
            }

            saveAllServers();
        } catch (IllegalArgumentException e) {
            p = backup;
            initServers();
            throw e;
        }
    }

}
