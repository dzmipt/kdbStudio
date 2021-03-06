package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.ui.ServerList;
import studio.utils.HistoricalList;

import javax.swing.tree.TreeNode;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Config {

    private static final Logger log = LogManager.getLogger();

    // The folder is also referenced in lon4j2.xml config
    private static final String PATH = System.getProperties().getProperty("user.home") + "/.studioforkdb/";
    private static final String FILENAME = PATH + "studio.properties";
    private static final String VERSION13 = "1.3";
    private static final String VERSION12 = "1.2";
    private static final String OLD_VERSION = "1.1";

    private static final String VERSION = VERSION13;


    private String filename;
    private Properties p = new Properties();
    private Map<String, Server> servers;
    private Collection<String> serverNames;
    private ServerTreeNode serverTree;
    private HistoricalList<Server> serverHistory;

    private final static Map<String, Config> instances = new ConcurrentHashMap<>();

    private Config(String filename) {
        init(filename);
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

    public static Config getInstance() {
        return getInstance(FILENAME);
    }

    public static Config getInstance(String filename) {
        return instances.computeIfAbsent(filename, Config::new);
    }

    private void init(String filename) {
        this.filename = filename;
        Path file = Paths.get(filename);
        Path dir = file.getParent();
        if (Files.notExists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.error("Can't create configuration folder {}", dir, e);
            }
        }

        if (Files.exists(file)) {
            try {
                InputStream in = Files.newInputStream(file);
                p.load(in);
                in.close();
            } catch (IOException e) {
                log.error("Can't read configuration from file {}", filename, e);
            }
        }
        checkForUpgrade();
        initServers();
        initServerHistory();
    }

    private void upgradeTo12() {
        try {
            log.info("Found old config. Converting...");
            String[] names = p.getProperty("Servers", "").split(",");
            List<Server> list = new ArrayList<>();
            for (String name : names) {
                name = name.trim();
                if (name.equals("")) continue;
                Server server = initServerFromKey(name);
                server.setName(name);
                list.add(server);
            }
            p.remove("Servers");
            p.entrySet().removeIf(e -> e.getKey().toString().startsWith("server."));
            p.setProperty("version", VERSION12);
            initServers();
            addServers(false, list.toArray(new Server[0]));
            log.info("Done");
        } catch (IllegalArgumentException e) {
            log.error("Ups... Can't convert", e);
        }
    }

    private void upgradeTo13() {
        String fullName = p.getProperty("lruServer", "");
        p.remove("lruServer");
        if (! fullName.equals("")) {
            Server server = getServer(fullName);
            if (server != null) addServerToHistory(server);
        }
        save();
    }

    private void checkForUpgrade() {
        if (p.getProperty("version", OLD_VERSION).equals(OLD_VERSION)) {
            upgradeTo12();
            p.setProperty("version", VERSION12);
        }

        initServers();
        if (p.getProperty("version").equals(VERSION12)) {
            initServerHistory();
            upgradeTo13();
            p.setProperty("version", VERSION13);
        }
        initServerHistory();
    }

    public void save() {
        try {
            OutputStream out = new FileOutputStream(filename);
            p.put("version", VERSION);
            p.store(out, "Auto-generated by Studio for kdb+");
            out.close();
        } catch (IOException e) {
            log.error("Can't save configuration to {}", filename, e);
        }
    }

    // "".split(",") return {""}; we need to get zero length array
    private String[] split(String str) {
        str = str.trim();
        if (str.length() == 0) return new String[0];
        return str.split(",");
    }

    public int getServerHistoryDepth() {
        return Integer.parseInt(p.getProperty("serverHistoryDepth", "20"));
    }

    public void setServerHistoryDepth(int depth) {
        serverHistory.setDepth(depth);
        p.setProperty("serverHistoryDepth", "" + depth);
        save();
    }

    private void initServerHistory() {
        int depth = getServerHistoryDepth();
        serverHistory = new HistoricalList<>(depth);
        for (int i=depth-1; i>=0; i--) {
            String key = "serverHistory." + i;
            if (! p.containsKey(key)) continue;
            Server server = getServer(p.getProperty(key));
            if (server == null) continue;
            serverHistory.add(server);
        }
    }

    public List<Server> getServerHistory() {
        return Collections.unmodifiableList(serverHistory);
    }

    public void addServerToHistory(Server server) {
        serverHistory.add(server);
        for (int i=serverHistory.size()-1; i>=0; i--) {
            String key = "serverHistory." + i;
            p.setProperty(key, serverHistory.get(i).getFullName());
        }
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

        String auth = nodes.length == 2 ? getDefaultAuthMechanism() : DefaultAuthenticationMechanism.NAME;
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

    public boolean isShowServerComboBox() {
        return Boolean.parseBoolean(p.getProperty("showServerComboBox","true"));
    }

    public void setShowServerComboBox(boolean value) {
        p.setProperty("showServerComboBox", "" + value);
        save();
    }

    public int getResultTabsCount() {
        return Integer.parseInt(p.getProperty("resultTabsCount","5"));
    }

    public void setResultTabsCount(int value) {
        p.setProperty("resultTabsCount", "" + value);
        save();
    }

    public int getMaxCharsInResult() {
        return Integer.parseInt(p.getProperty("maxCharsInResult", "50000"));
    }

    public void setMaxCharsInResult(int value) {
        p.setProperty("maxCharsInResult", "" + value);
        save();
    }

    public int getMaxCharsInTableCell() {
        return Integer.parseInt(p.getProperty("maxCharsInTableCell", "256"));
    }

    public void setMaxCharsInTableCell(int value) {
        p.setProperty("maxCharsInTableCell", "" + value);
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

    private void initServers() {
        serverNames = new ArrayList<>();
        serverTree = new ServerTreeNode();
        servers = new HashMap<>();
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
        addServers(false, server);
    }

    public String[] addServers(boolean tryAll, Server... newServers) {
        String[] result = tryAll ? new String[newServers.length] : null;
        Properties backup = new Properties();
        backup.putAll(p);
        try {
            int index = -1;
            for (Server server : newServers) {
                index++;
                try {
                    ServerTreeNode folder = server.getFolder();
                    if (folder == null) {
                        server.setFolder(serverTree);
                    }
                    addServerInternal(server);
                    serverTree.findPath(folder.getPath(), true).add(server);
                } catch (IllegalArgumentException e) {
                    if (tryAll) {
                        result[index] = e.getMessage();
                    } else {
                        throw e;
                    }
                }
            }

            saveAllServers();
        } catch (IllegalArgumentException e) {
            p = backup;
            initServers();
            throw e;
        }
        return result;
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
