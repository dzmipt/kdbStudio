package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.AbstractConfig;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.KdbMessageLimitAction;
import studio.ui.ServerList;
import studio.utils.*;
import studio.utils.log4j.EnvConfig;

import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Config extends AbstractConfig {
    private static final Logger log = LogManager.getLogger();

    //@TODO migrate all other keys under such approach

    public static final String SHOW_SERVER_COMBOBOX = configDefault("showServerComboBox", ConfigType.BOOLEAN, true);
    public static final String AUTO_SAVE = configDefault("isAutoSave", ConfigType.BOOLEAN, false);
    public static final String ACTION_ON_EXIT = configDefault("actionOnExit", ConfigType.ENUM, ActionOnExit.SAVE);
    public static final String SERVER_LIST_BOUNDS = configDefault("serverList", ConfigType.BOUNDS, new Dimension(ServerList.DEFAULT_WIDTH, ServerList.DEFAULT_HEIGHT));
    public static final String CHART_BOUNDS = configDefault("chartBounds", ConfigType.BOUNDS, 0.5);
    public static final String CELL_RIGHT_PADDING = configDefault("cellRightPadding", ConfigType.DOUBLE, 0.5);
    public static final String CELL_MAX_WIDTH = configDefault("cellMaxWidth", ConfigType.INT, 200);

    public static final String RSTA_ANIMATE_BRACKET_MATCHING = configDefault("rstaAnimateBracketMatching", ConfigType.BOOLEAN, true);
    public static final String RSTA_HIGHLIGHT_CURRENT_LINE = configDefault("rstaHighlightCurrentLine", ConfigType.BOOLEAN, true);
    public static final String RSTA_WORD_WRAP = configDefault("rstaWordWrap", ConfigType.BOOLEAN, false);
    public static final String RSTA_INSERT_PAIRED_CHAR = configDefault("rstaInsertPairedChar", ConfigType.BOOLEAN, true);

    public static final String DEFAULT_LINE_ENDING = configDefault("defaultLineEnding", ConfigType.ENUM, LineEnding.Unix);

    public static final String COLOR_CHARVECTOR = configDefault("token.CHARVECTOR", ConfigType.COLOR, new Color(0,200,20));
    public static final String COLOR_EOLCOMMENT = configDefault("token.EOLCOMMENT", ConfigType.COLOR,  Color.GRAY);
    public static final String COLOR_IDENTIFIER = configDefault("token.IDENTIFIER", ConfigType.COLOR, new Color(180,160,0));
    public static final String COLOR_OPERATOR = configDefault("token.OPERATOR", ConfigType.COLOR, Color.BLACK);
    public static final String COLOR_BOOLEAN = configDefault("token.BOOLEAN", ConfigType.COLOR, new Color(51,204,255));
    public static final String COLOR_BYTE = configDefault("token.BYTE", ConfigType.COLOR, new Color(51,104,255));
    public static final String COLOR_SHORT = configDefault("token.SHORT", ConfigType.COLOR, new Color(51,104,255));
    public static final String COLOR_LONG = configDefault("token.LONG", ConfigType.COLOR, new Color(51,104,255));
    public static final String COLOR_REAL = configDefault("token.REAL", ConfigType.COLOR, new Color(51,104,255));
    public static final String COLOR_INTEGER = configDefault("token.INTEGER", ConfigType.COLOR, new Color(51,104,255));
    public static final String COLOR_FLOAT = configDefault("token.FLOAT", ConfigType.COLOR, new Color(51,104,255));
    public static final String COLOR_TIMESTAMP = configDefault("token.TIMESTAMP", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_TIMESPAN = configDefault("token.TIMESPAN", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_DATETIME = configDefault("token.DATETIME", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_DATE = configDefault("token.DATE", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_MONTH = configDefault("token.MONTH", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_MINUTE = configDefault("token.MINUTE", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_SECOND = configDefault("token.SECOND", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_TIME = configDefault("token.TIME", ConfigType.COLOR, new Color(184,138,0));
    public static final String COLOR_SYMBOL = configDefault("token.SYMBOL", ConfigType.COLOR, new Color(179,0,134));
    public static final String COLOR_KEYWORD = configDefault("token.KEYWORD", ConfigType.COLOR, new Color(0,0,255));
    public static final String COLOR_COMMAND = configDefault("token.COMMAND", ConfigType.COLOR, new Color(240,180,0));
    public static final String COLOR_SYSTEM = configDefault("token.SYSTEM", ConfigType.COLOR, new Color(240,180,0));
    public static final String COLOR_WHITESPACE = configDefault("token.WHITESPACE", ConfigType.COLOR, Color.BLACK);
    public static final String COLOR_DEFAULT = configDefault("token.DEFAULT", ConfigType.COLOR, Color.BLACK);
    public static final String COLOR_BRACKET = configDefault("token.BRACKET", ConfigType.COLOR, Color.BLACK);

    public static final String COLOR_ERROR = configDefault("token.ERROR", ConfigType.COLOR, Color.RED);

    public static final String COLOR_BACKGROUND = configDefault("token.BACKGROUND", ConfigType.COLOR, Color.WHITE);

    public static final String FONT_EDITOR = configDefault("font", ConfigType.FONT, new Font("Monospaced", Font.PLAIN, 14));
    public static final String FONT_TABLE = configDefault("fontTable", ConfigType.FONT, new Font("SansSerif", Font.PLAIN, 12));

    public static final String EDITOR_TAB_SIZE = configDefault("editorTabSize", ConfigType.INT, 5); // 5 is a default value for RSyntaxTextArea
    public static final String EDITOR_TAB_EMULATED = configDefault("editorTabEmulated", ConfigType.BOOLEAN, false);
    public static final String AUTO_REPLACE_TAB_ON_OPEN = configDefault("autoReplaceTabOnOpen", ConfigType.BOOLEAN, false);

    public static final String MAX_FRACTION_DIGITS = configDefault("maxFractionDigits", ConfigType.INT, 7);
    public static final String EMULATED_DOUBLE_CLICK_TIMEOUT = configDefault("emulatedDoubleClickTimeout", ConfigType.INT, 500);

    public static final String OPEN_FILE_CHOOSER = configDefault("openFileChooser", ConfigType.FILE_CHOOSER, new FileChooserConfig());
    public static final String SAVE_FILE_CHOOSER = configDefault("saveFileChooser", ConfigType.FILE_CHOOSER, new FileChooserConfig());
    public static final String EXPORT_FILE_CHOOSER = configDefault("exportFileChooser", ConfigType.FILE_CHOOSER, new FileChooserConfig());

    public static final String SESSION_INVALIDATION_ENABLED = configDefault("sessionInvalidationEnabled", ConfigType.BOOLEAN, false);
    public static final String SESSION_INVALIDATION_TIMEOUT_IN_HOURS = configDefault("sessionInvalidationTimeoutInHours", ConfigType.INT, 12);
    public static final String SESSION_REUSE = configDefault("sessionsReuse", ConfigType.BOOLEAN, true);
    public static final String KDB_MESSAGE_SIZE_LIMIT_MB = configDefault("kdbMessageSizeLimitMB", ConfigType.INT, 10);
    public static final String KDB_MESSAGE_SIZE_LIMIT_ACTION = configDefault("kdbMessageSizeLimitAction", ConfigType.ENUM, KdbMessageLimitAction.ASK);


    private static final String CONFIG_FILENAME = "studio.properties";
    private static final String WORKSPACE_FILENAME = "workspace.properties";

    private static final String VERSION13 = "1.3";
    private static final String VERSION12 = "1.2";
    private static final String OLD_VERSION = "1.1";

    private static final String VERSION = VERSION13;


    private PropertiesConfig workspaceConfig;

    private Map<String, Server> servers;
    private Collection<String> serverNames;
    private ServerTreeNode serverTree;
    private HistoricalList<Server> serverHistory;

    private static final String CONN_COL_WORDS = "server, host, connection, handle";
    private static final String HOST_COL_WORDS = "server, host";
    private static final String PORT_COL_WORDS = "port";

    private TableConnExtractor tableConnExtractor;

    // Can be overridden in test cases
    protected static Config instance = new Config();

    public enum ExecAllOption {Execute, Ask, Ignore}

    protected Config(String filename) {
        super(filename);
        init();
    }

    private static void copyConfig(String configFileName) throws IOException {
        Path src = Paths.get(EnvConfig.getFilepath(null, configFileName));
        Path target = Paths.get(EnvConfig.getFilepath(configFileName));
        if (Files.exists(src)) {
            log.info("Copying from {} to {}", src, target);
            Files.copy(src, target);
        }
    }

    private static String getDefaulConfigFilename() {
        String filename = EnvConfig.getFilepath(CONFIG_FILENAME);

        String env = EnvConfig.getEnvironment();
        if (env != null && ! Files.exists(Paths.get(filename))) {
            log.info("Config for environment {} is not found. Copying from default location: {}", env, EnvConfig.getBaseFolder(null));
            try {
                copyConfig(CONFIG_FILENAME);
                copyConfig(WORKSPACE_FILENAME);
            } catch (IOException e) {
                log.error("Error during copying configs", e);
            }
        }
        return filename;
    }

    private Config() {
        super(getDefaulConfigFilename());
        init();
    }

    void saveToDisk() {
        config.saveToDisk();
        workspaceConfig.saveToDisk();
    }

    public void exit() {
        saveToDisk();
        log.info("Shutting down");
        System.exit(0);
    }

    private String getWorkspaceFilename() {
        return EnvConfig.getFilepath(WORKSPACE_FILENAME);
    }

	public Workspace loadWorkspace() {
		Workspace workspace = new Workspace();
        String filename = getWorkspaceFilename();
        log.info("Loading workspace from {}", filename);
		File workspaceFile = new File(filename);
		if (workspaceFile.exists()) {
			try (InputStream inp = new FileInputStream(workspaceFile)) {
				Properties p = new Properties();
				p.load(inp);
                log.info("Loaded {} properties in workspace file", p.size());

                workspace.load(p);

                StringBuilder str = new StringBuilder();
                Workspace.Window[] windows = workspace.getWindows();
                for (Workspace.Window window: windows) {
                    if (str.length()>0) str.append(", ");
                    if (window == null) str.append("null");
                    else {
                        Workspace.Tab[] tabs = window.getAllTabs();
                        str.append(tabs == null ? "null tabs" : "" + tabs.length);
                    }
                }

                log.info("Number of tabs in loaded windows: " + str);
			} catch (IOException e) {
				log.error("Can't load workspace", e);
			}
		}
		return workspace;
	}

    public void saveWorkspace(Workspace workspace) {
        workspaceConfig.clear();
        workspace.save(workspaceConfig);
        workspaceConfig.save();
    }

    private String[] getWords(String value) {
        return Stream.of(value.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toArray(String[]::new);
    }

    private void initTableConnExtractor() {
        tableConnExtractor = new TableConnExtractor();
        tableConnExtractor.setMaxConn(getTableMaxConnectionPopup());
        tableConnExtractor.setConnWords(getWords(getConnColWords()));
        tableConnExtractor.setHostWords(getWords(getHostColWords()));
        tableConnExtractor.setPortWords(getWords(getPortColWords()));
    }

    public TableConnExtractor getTableConnExtractor() {
        return tableConnExtractor;
    }

    public int getTableMaxConnectionPopup() {
        return Integer.parseInt(config.getProperty("tableMaxConnectionPopup","5"));
    }

    public String getConnColWords() {
        return config.getProperty("connColWords", CONN_COL_WORDS);
    }

    public String getHostColWords() {
        return config.getProperty("hostColWords", HOST_COL_WORDS);

    }

    public String getPortColWords() {
        return config.getProperty("portColWords", PORT_COL_WORDS);
    }

    public void setTableMaxConnectionPopup(int maxConn) {
        config.setProperty("tableMaxConnectionPopup", "" + maxConn);
        save();
        initTableConnExtractor();
    }

    public void setConnColWords(String words) {
        config.setProperty("connColWords", words);
        save();
        initTableConnExtractor();
    }

    public void setHostColWords(String words) {
        config.setProperty("hostColWords", words);
        save();
        initTableConnExtractor();
    }

    public void setPortColWords(String words) {
        config.setProperty("portColWords", words);
        save();
        initTableConnExtractor();
    }

    public ExecAllOption getExecAllOption() {
        String value = config.getProperty("execAllOption", "Ask");
        try {
            return ExecAllOption.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.info(value + " - can't parse execAllOption from Config. Reset to default: Ask");
            return ExecAllOption.Ask;
        }
    }

    public void setExecAllOption(ExecAllOption option) {
        config.setProperty("execAllOption", option.toString());
        save();
    }

    public String getNotesHash() {
        return config.getProperty("notesHash","");
    }

    public void setNotesHash(String notesHash) {
        config.setProperty("notesHash", notesHash);
        save();
    }

    public String getEncoding() {
        return config.getProperty("encoding", "UTF-8");
    }

    public static Config getInstance() {
        return instance;
    }

    private void init() {
        workspaceConfig = new PropertiesConfig(getWorkspaceFilename());

        checkForUpgrade();
        initTableConnExtractor();
    }

    private void upgradeTo12() {
        try {
            log.info("Found old config. Converting...");
            String[] names = config.getProperty("Servers", "").split(",");
            List<Server> list = new ArrayList<>();
            for (String name : names) {
                name = name.trim();
                if (name.equals("")) continue;
                try {
                    Server server = initServerFromKey(name).newName(name);
                    list.add(server);
                } catch (IllegalArgumentException e) {
                    log.warn("Error during parsing server " + name, e);
                }
            }
            config.remove("Servers");
            config.entrySet().removeIf(e -> e.getKey().toString().startsWith("server."));
            config.setProperty("version", VERSION12);
            initServers();
            String[] results = addServers(true, list.toArray(new Server[0]));
            boolean error = false;
            for(String result: results) {
                if (result == null) continue;
                if (!error) {
                    error = true;
                    log.warn("Found errors during conversion");
                }
                log.warn(result);
            }
            log.info("Done");
        } catch (IllegalArgumentException e) {
            log.error("Ups... Can't convert", e);
        }
    }

    private void upgradeTo13() {
        String fullName = config.getProperty("lruServer", "");
        config.remove("lruServer");
        if (! fullName.equals("")) {
            Server server = getServer(fullName);
            if (server != Server.NO_SERVER) addServerToHistory(server);
        }
        save();
    }

    private void migrateSaveOnExit() {
        String oldSaveOnExitKey = "isSaveOnExit";
        if (config.containsKey(oldSaveOnExitKey)) {
            boolean saveOnExit = get(oldSaveOnExitKey, true);
            log.info("Migrate isSaveOnExit config property with old value {}", saveOnExit);
            config.remove(oldSaveOnExitKey);
            setEnum(ACTION_ON_EXIT, saveOnExit ? ActionOnExit.SAVE : ActionOnExit.NOTHING);
        }
    }

    private void checkForUpgrade() {
        if (config.size() == 0) {
            log.info("Found no or empty config");
            config.setProperty("version", VERSION);

            initServers();
            initServerHistory();
            return;
        }

        if (config.getProperty("version", OLD_VERSION).equals(OLD_VERSION)) {
            upgradeTo12();
            config.setProperty("version", VERSION12);
        }

        initServers();
        if (config.getProperty("version").equals(VERSION12)) {
            initServerHistory();
            upgradeTo13();
            config.setProperty("version", VERSION13);
        }
        initServerHistory();
        migrateSaveOnExit();

        config.setProperty("version", VERSION);
    }

    // "".split(",") return {""}; we need to get zero length array
    private String[] split(String str) {
        str = str.trim();
        if (str.length() == 0) return new String[0];
        return str.split(",");
    }

    public int getServerHistoryDepth() {
        return Integer.parseInt(config.getProperty("serverHistoryDepth", "20"));
    }

    public void setServerHistoryDepth(int depth) {
        serverHistory.setDepth(depth);
        config.setProperty("serverHistoryDepth", "" + depth);
        save();
    }

    private void initServerHistory() {
        int depth = getServerHistoryDepth();
        serverHistory = new HistoricalList<>(depth);
        for (int i=depth-1; i>=0; i--) {
            String key = "serverHistory." + i;
            if (! config.containsKey(key)) continue;
            Server server = getServer(config.getProperty(key));
            if (server == Server.NO_SERVER) continue;
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
            config.setProperty(key, serverHistory.get(i).getFullName());
        }
        save();
    }

    public String[] getMRUFiles() {
        String mru = config.getProperty("mrufiles", "");
        return split(mru);
    }


    public void saveMRUFiles(String[] mruFiles) {
        String value = Stream.of(mruFiles).limit(9).collect(Collectors.joining(","));
        config.put("mrufiles", value);
        save();
    }

    public String getLookAndFeel() {
        return config.getProperty("lookandfeel");
    }

    public void setLookAndFeel(String lf) {
        config.put("lookandfeel", lf);
        save();
    }

    // Resolve or create a new server by connection string.
    // Accept possible various connectionString such as:
    // `:host:port:user:password
    // host:port
    // If user and password are not found, defaults form default AuthenticationMechanism are used
    public Server getServerByConnectionString(String connectionString) {
        String defaultAuth = getDefaultAuthMechanism();
        return QConnection.getByConnection(connectionString, defaultAuth, getDefaultCredentials(defaultAuth), servers.values());
    }

    public Credentials getDefaultCredentials(String authenticationMechanism) {
        String user = config.getProperty("auth." + authenticationMechanism + ".user", "");
        String password = config.getProperty("auth." + authenticationMechanism + ".password", "");
        return new Credentials(user, password);
    }

    public void setDefaultCredentials(String authenticationMechanism, Credentials credentials) {
        config.setProperty("auth." + authenticationMechanism + ".user", credentials.getUsername());
        config.setProperty("auth." + authenticationMechanism + ".password", credentials.getPassword());
        save();
    }

    public String getDefaultAuthMechanism() {
        return config.getProperty("auth", DefaultAuthenticationMechanism.NAME);
    }

    public void setDefaultAuthMechanism(String authMechanism) {
        config.setProperty("auth", authMechanism);
        save();
    }

    public int getResultTabsCount() {
        return Integer.parseInt(config.getProperty("resultTabsCount","5"));
    }

    public void setResultTabsCount(int value) {
        config.setProperty("resultTabsCount", "" + value);
        save();
    }

    public int getMaxCharsInResult() {
        return Integer.parseInt(config.getProperty("maxCharsInResult", "50000"));
    }

    public void setMaxCharsInResult(int value) {
        config.setProperty("maxCharsInResult", "" + value);
        save();
    }

    public int getMaxCharsInTableCell() {
        return Integer.parseInt(config.getProperty("maxCharsInTableCell", "256"));
    }

    public void setMaxCharsInTableCell(int value) {
        config.setProperty("maxCharsInTableCell", "" + value);
        save();
    }

    public Collection<String> getServerNames() {
        return Collections.unmodifiableCollection(serverNames);
    }

    public Server[] getServers() {
        return servers.values().toArray(new Server[servers.size()]);
    }

    public Server getServer(String name) {
        if (servers.containsKey(name)) return servers.get(name);
        return Server.NO_SERVER;
    }

    public ServerTreeNode getServerTree() {
        return serverTree;
    }

    private Server initServerFromKey(String key) {
        String host = config.getProperty("server." + key + ".host", "");
        int port = Integer.parseInt(config.getProperty("server." + key + ".port", "-1"));
        String username = config.getProperty("server." + key + ".user", "");
        String password = config.getProperty("server." + key + ".password", "");
        Color backgroundColor = get("server." + key + ".backgroundColor", Color.WHITE);
        String authenticationMechanism = config.getProperty("server." + key + ".authenticationMechanism", DefaultAuthenticationMechanism.NAME);
        boolean useTLS = Boolean.parseBoolean(config.getProperty("server." + key + ".useTLS", "false"));
        return new Server("", host, port, username, password, backgroundColor, authenticationMechanism, useTLS);
    }

    private Server initServerFromProperties(int number) {
        return initServerFromKey("" + number);
    }

    private void initServers() {
        serverNames = new ArrayList<>();
        serverTree = new ServerTreeNode();
        servers = new HashMap<>();
        initServerTree("serverTree.", serverTree, 0);
        log.info("Loaded {} server from the config", servers.size());
    }

    private int initServerTree(String keyPrefix, ServerTreeNode parent, int number) {
        for (int index = 0; ; index++) {
            String key = keyPrefix + index;
            String folderKey = key + "folder";
            if (config.containsKey(folderKey)) {
                ServerTreeNode node = parent.add(config.getProperty(folderKey));
                number = initServerTree(key + ".", node, number);
            } else if (config.containsKey(key)) {
                String name = config.getProperty(key);
                Server server = initServerFromProperties(number).newName(name);
                server.setFolder(parent);
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
        config.entrySet().removeIf(e -> e.getKey().toString().startsWith("serverTree."));
        config.entrySet().removeIf(e -> e.getKey().toString().startsWith("server."));
        saveServerTree("serverTree.", serverTree, 0);

        save();
    }

    private void saveServerDetails(Server server, int number) {
        config.setProperty("server." + number + ".host", server.getHost());
        config.setProperty("server." + number + ".port", "" + server.getPort());
        config.setProperty("server." + number + ".user", "" + server.getUsername());
        config.setProperty("server." + number + ".password", "" + server.getPassword());
        config.setProperty("server." + number + ".backgroundColor", "" + Integer.toHexString(server.getBackgroundColor().getRGB()).substring(2));
        config.setProperty("server." + number + ".authenticationMechanism", server.getAuthenticationMechanism());
        config.setProperty("server." + number + ".useTLS", "" + server.getUseTLS());
    }

    private int saveServerTree(String keyPrefix, ServerTreeNode node, int number) {
        int count = node.getChildCount();
        for(int index = 0; index<count; index++) {
            String key = keyPrefix + index;
            ServerTreeNode child = node.getChild(index);
            if (child.isFolder()) {
                config.setProperty(key + "folder", child.getFolder());
                number = saveServerTree(key + ".", child, number);
            } else {
                Server server = child.getServer();
                config.setProperty(key, server.getName());
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
            throw new IllegalArgumentException("Server with full name " + fullName + " already exists");
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
        if (AuthenticationManager.getInstance().lookup(server.getAuthenticationMechanism()) == null) {
            throw new IllegalArgumentException("Unknown Authentication Mechanism: " + server.getAuthenticationMechanism());
        }

        TreeNode[] path = server.getFolder().getPath();
        for (int index = 1; index<path.length; index++) {
            ServerTreeNode node = (ServerTreeNode) path[index];
            if (node.getFolder().trim().length() == 0) {
                throw new IllegalArgumentException("Folder name can't be empty");
            }
        }
        servers.put(fullName, server);
        serverNames.add(fullName);
    }


    public void addServer(Server server) {
        addServers(false, server);
    }

    public String[] addServers(boolean tryAll, Server... newServers) {
        String[] result = tryAll ? new String[newServers.length] : null;
        PropertiesConfig backup = config.cloneConfig();
        try {
            int index = -1;
            for (Server server : newServers) {
                index++;
                try {
                    ServerTreeNode folder = server.getFolder();
                    if (folder == null) {
                        server.setFolder(serverTree);
                        folder = serverTree;
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
            config = backup;
            initServers();
            throw e;
        }
        return result;
    }

    public void setServerTree(ServerTreeNode serverTree) {
        PropertiesConfig backup = config.cloneConfig();
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
            config = backup;
            initServers();
            throw e;
        }
    }

}
