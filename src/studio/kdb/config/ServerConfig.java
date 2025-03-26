package studio.kdb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.utils.FileConfig;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.*;


public class ServerConfig {
    private static final Logger log = LogManager.getLogger();

    private final FileConfig fileConfig;

    private Map<String, Server> servers;
    private List<String> serverNames;
    private ServerTreeNode serverTree;
    private Gson gson;

    public ServerConfig(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
        gson = new GsonBuilder()
                .registerTypeAdapter(ServerTreeNode.class, new ServerTreeNodeSerializer())
                .setPrettyPrinting()
                .create();
        init();
    }

    private void init() {
        fileConfig.saveOnDisk();
        setRoot(load());
    }

    public Collection<Server> allServers() {
        return servers.values();
    }

    public ServerTreeNode getServerTree() {
        return serverTree;
    }

    public ServerTreeNode load() {
        if (! fileConfig.fileExists()) {
            log.info("Server config {} not found", fileConfig);
            return new ServerTreeNode();
        }
        try {
            String content = fileConfig.getContent();
            ServerTreeNode node = gson.fromJson(content, ServerTreeNode.class);
            if (node == null) node = new ServerTreeNode();
            if (node.isFolder()) return node;

            ServerTreeNode root = new ServerTreeNode();
            root.add(node.getServer());
            return root;
        } catch (IOException e) {
            log.error("Error in reading server config from {}", fileConfig, e);
        } catch (Exception e) {
            log.error("Error in the file format {}", fileConfig, e);
        }
        return new ServerTreeNode();
    }

    private void save() {
        try (Writer writer = fileConfig.getWriter()) {
            gson.toJson(serverTree, writer);
        } catch (IOException e) {
            log.error("Error in writing server config to {}", fileConfig, e);
        }
    }

    public void setRoot(ServerTreeNode root) {
        this.serverTree = root;
        servers = new HashMap<>();
        serverNames = new ArrayList<>();
        for(Enumeration e = serverTree.depthFirstEnumeration(); e.hasMoreElements();) {
            ServerTreeNode node = (ServerTreeNode) e.nextElement();
            if (node.isFolder()) continue;
            Server server = node.getServer();
            ServerTreeNode parent = (ServerTreeNode) node.getParent();
            if (!server.getFolderPath().equals(parent.getFolderPath()) ) {
                server = server.newFolder(parent);
            }
            String name = server.getFullName();
            servers.put(name, server);
            serverNames.add(name);
        }

        serverNames = Collections.unmodifiableList(serverNames);
        save();
    }


    private void validate(Server server) {
        String name = server.getName();
        String fullName = server.getFullName();

        server.getFolderPath()
                .stream()
                .skip(1)
                .forEach(folderName -> {
                    if (folderName.trim().isEmpty()) {
                        throw new IllegalArgumentException("Folder name can't be empty");
                    }
                });

        ServerTreeNode parent = serverTree.findPath(server.getFolderPath(), false);
        if (parent != null && parent.getChild(server) != null) {
            throw new IllegalArgumentException("Server with full name " + fullName + " already exists");
        }

        if (serverNames.contains(fullName)) {
            throw new IllegalArgumentException("Server with full name " + fullName + " already exists");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Server name can't be empty");
        }
        if (name.contains(",")) {
            throw new IllegalArgumentException("Server name can't contains ,");
        }
        if (name.contains("/")) {
            throw new IllegalArgumentException("Server name can't contains /");
        }
    }


    public void addServer(Server server) {
        addServers(false, server);
    }

    public String[] addServers(boolean tryAll, Server... newServers) {
        String[] result = tryAll ? new String[newServers.length] : null;
        try {
            int index = -1;
            for (Server server : newServers) {
                index++;
                try {
                    validate(server);
                    serverTree.findPath(server.getFolderPath(), true).add(server);
                } catch (IllegalArgumentException e) {
                    if (tryAll) {
                        result[index] = e.getMessage();
                    } else {
                        throw e;
                    }
                }
            }

            setRoot(serverTree);
        } catch (IllegalArgumentException e) {
            init();
            throw e;
        }
        return result;
    }

    public void removeServer(Server server) {
        ServerTreeNode folder = serverTree.findPath(server.getFolderPath(), false);
        if (folder != null) {
            folder.remove(server);
            setRoot(serverTree);
        }
    }

    public void replaceServer(Server oldServer, Server newServer) {
        ServerTreeNode oldFolder = serverTree.findPath(oldServer.getFolderPath(), false);
        if (oldFolder == null) {
            // ?? Is it possible
            return;
        }
        ServerTreeNode newFolder = serverTree.findPath(newServer.getFolderPath(), true);

//        String name = oldServer.getFullName();
//        serverNames.remove(name);
//        servers.remove(name);
        int index = oldFolder.remove(oldServer);
        if (index == -1 || oldFolder!=newFolder) {
            index = newFolder.getChildCount();
        }
        newFolder.add(index, newServer);
        setRoot(serverTree);
    }

    public List<String> getServerNames() {
        return serverNames;
    }

    public Server[] getServers() {
        return servers.values().toArray(new Server[servers.size()]);
    }

    public Server getServer(String name) {
        if (servers.containsKey(name)) return servers.get(name);
        return Server.NO_SERVER;
    }




    public boolean tryToLoadOldConifg(Properties config) {
        boolean modified = false;

        if (config.contains("Servers")) {
            log.info("Found old config. Converting...");
            String[] names = config.getProperty("Servers", "").split(",");
            List<Server> list = new ArrayList<>();
            for (String name : names) {
                name = name.trim();
                if (name.equals("")) continue;
                try {
                    Server server = initServerFromKey(config, name, null).newName(name);
                    list.add(server);
                } catch (IllegalArgumentException e) {
                    log.warn("Error during parsing server " + name, e);
                }
            }

            String[] results = addServers(true, list.toArray(new Server[0]));
            boolean error = false;
            for (String result : results) {
                if (result == null) continue;
                if (!error) {
                    error = true;
                    log.warn("Found errors during conversion");
                }
                log.warn(result);
            }
            log.info("Done");

            config.remove("Servers");
            modified = true;
        } else {
            ServerTreeNode root = new ServerTreeNode();
            int count = initServerTree(config, "serverTree.", root, 0);
            if (count > 0) {
                setRoot(root);
            }
        }

        List<String> keysToRemove = new ArrayList<>();
        for (String key: config.stringPropertyNames()) {
            if (key.startsWith("serverTree.") || key.startsWith("server.")) {
                keysToRemove.add(key);
            }
        }
        for (String key: keysToRemove) {
            config.remove(key);
        }

        return modified || keysToRemove.size()>0;
    }

    private int initServerTree(Properties config, String keyPrefix, ServerTreeNode parent, int number) {
        for (int index = 0; ; index++) {
            String key = keyPrefix + index;
            String folderKey = key + "folder";
            if (config.containsKey(folderKey)) {
                ServerTreeNode node = parent.add(config.getProperty(folderKey));
                number = initServerTree(config, key + ".", node, number);
            } else if (config.containsKey(key)) {
                String name = config.getProperty(key);
                Server server = initServerFromProperties(config, number, parent).newName(name);
                parent.add(server);
                number++;
            } else {
                break;
            }
        }
        return number;
    }

    private Server initServerFromKey(Properties config, String key, ServerTreeNode parent) {
        String host = config.getProperty("server." + key + ".host", "");
        int port = Integer.parseInt(config.getProperty("server." + key + ".port", "-1"));
        String username = config.getProperty("server." + key + ".user", "");
        String password = config.getProperty("server." + key + ".password", "");
        Color backgroundColor = getColor(config,"server." + key + ".backgroundColor", Color.WHITE);
        String authenticationMechanism = config.getProperty("server." + key + ".authenticationMechanism", DefaultAuthenticationMechanism.NAME);
        boolean useTLS = Boolean.parseBoolean(config.getProperty("server." + key + ".useTLS", "false"));
        return new Server("", host, port, username, password, backgroundColor, authenticationMechanism, useTLS, parent);
    }

    private Server initServerFromProperties(Properties config, int number, ServerTreeNode parent) {
        return initServerFromKey(config,"" + number, parent);
    }

    private Color getColor(Properties config, String key, Color defaultValue) {
        String value = config.getProperty(key);
        if (value == null) return defaultValue;

        try {
            return new Color(Integer.parseInt(value, 16));
        } catch (NumberFormatException e) {
            log.error("Failed to parse {} for config key {}", value, key, e);
        }
        return defaultValue;
    }

}