package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.*;
import studio.utils.HistoricalList;
import studio.utils.LineEnding;
import studio.utils.PropertiesConfig;
import studio.utils.QConnection;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Config extends AbstractConfig {
    private static final Logger log = LogManager.getLogger();

    //@TODO migrate all other keys under such approach
    public static final String LOOK_AND_FEEL = configDefault("lookandfeel", ConfigType.STRING, UIManager.getLookAndFeel().getClass().getName());
    public static final String EXEC_ALL = configDefault("execAllOption", ConfigType.ENUM, ExecAllOption.Ask);
    public static final String MAX_CHARS_IN_RESULT = configDefault("maxCharsInResult", ConfigType.INT, 50000);
    public static final String MAX_CHARS_IN_TABLE_CELL = configDefault("maxCharsInTableCell", ConfigType.INT, 256);
    public static final String MRU_FILES = configDefault("mrufiles", ConfigType.STRING_ARRAY, new String[0]);
    public static final String NOTES_HASH = configDefault("notesHash", ConfigType.STRING, "");
    public static final String RESULT_TAB_COUNTS = configDefault("resultTabsCount", ConfigType.INT, 5);
    public static final String POPUP_MAX_CONNECTIONS = configDefault("tableMaxConnectionPopup", ConfigType.INT, 5);

    public static final String POPUP_CONN_COLUMNS_WORDS = configDefault("connColWords", ConfigType.STRING_ARRAY, new String[]{"server", "host", "connection", "handle"});
    public static final String POPUP_HOST_COLUMNS_WORDS = configDefault("hostColWords", ConfigType.STRING_ARRAY, new String[]{"server", "host"});
    public static final String POPUP_PORT_COLUMNS_WORDS = configDefault("portColWords", ConfigType.STRING_ARRAY, new String[]{"port"});

    public static final String SERVER_HISTORY_DEPTH = configDefault("serverHistoryDepth", ConfigType.INT, 20);
    public static final String SERVER_HISTORY_LIST = configDefault("serverHistory", ConfigType.STRING_ARRAY, new String[0]);

    public static final String SHOW_SERVER_COMBOBOX = configDefault("showServerComboBox", ConfigType.BOOLEAN, true);
    public static final String AUTO_SAVE = configDefault("isAutoSave", ConfigType.BOOLEAN, false);
    public static final String ACTION_ON_EXIT = configDefault("actionOnExit", ConfigType.ENUM, ActionOnExit.SAVE);
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
    private static final String SERVERCONFIG_FILENAME = "servers.json";

    private static final String OLD_DEF_AUTHMETHOD = "Username and password";
    private static final String VERSION14 = "1.4";
    private static final String VERSION13 = "1.3";
    private static final String VERSION12 = "1.2";
    private static final String OLD_VERSION = "1.1";

    private static final String VERSION = VERSION14;


    protected PropertiesConfig workspaceConfig;
    protected ServerConfig serverConfig;
    private HistoricalList<Server> serverHistory;

    // Can be overridden in test cases
    protected static Config instance = new Config();

    protected Config(Path path) {
        super(path);
        init();
    }

    private static void copyConfig(String configFileName) throws IOException {
        Path src = EnvConfig.getFilepath(null, configFileName);
        Path target = EnvConfig.getFilepath(configFileName);
        if (Files.exists(src)) {
            log.info("Copying from {} to {}", src, target);
            Files.copy(src, target);
        }
    }

    private static Path getDefaultConfigPath() {
        Path path = EnvConfig.getFilepath(CONFIG_FILENAME);

        String env = EnvConfig.getEnvironment();
        if (env != null && ! Files.exists(path)) {
            log.info("Config for environment {} is not found. Copying from default location: {}", env, EnvConfig.getBaseFolder(null));
            try {
                copyConfig(CONFIG_FILENAME);
                copyConfig(WORKSPACE_FILENAME);
            } catch (IOException e) {
                log.error("Error during copying configs", e);
            }
        }
        return path;
    }

    private static Properties getDefaults() {
        Path pluginProperties = EnvConfig.getPluginFolder().resolve(CONFIG_FILENAME);
        if (! Files.exists((pluginProperties))) return null;

        try (InputStream inputStream = Files.newInputStream(pluginProperties)) {
            Properties defaults = new Properties();
            defaults.load(inputStream);
            log.info("Loaded {} default properties from {}", defaults.size(), pluginProperties);
            return defaults;
        } catch (IOException e) {
            log.error("Error loading default config from {}", pluginProperties, e);
        }
        return null;
    }

    private Config() {
        super(getDefaultConfigPath(), getDefaults());
        init();
    }

    public void saveToDisk() {
        super.saveToDisk();
        workspaceConfig.saveToDisk();
    }

    public void exit() {
        saveToDisk();
        log.info("Shutting down");
        System.exit(0);
    }

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    protected Path getWorkspacePath() {
        return EnvConfig.getFilepath(WORKSPACE_FILENAME);
    }

    protected Path getServerConfigPath() {
        return EnvConfig.getFilepath(SERVERCONFIG_FILENAME);
    }

	public Workspace loadWorkspace() {
		Workspace workspace = new Workspace();
        Path path = getWorkspacePath();
        log.info("Loading workspace from {}", path);
		if (Files.exists(path)) {
			try (InputStream inp = Files.newInputStream(path)) {
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
        Properties previousConfig = new Properties();
        previousConfig.putAll(workspaceConfig);
        workspaceConfig.clear();
        workspace.save(workspaceConfig);

        if (previousConfig.equals(workspaceConfig)) return;

        workspaceConfig.save();
    }

    private String[] getWords(String[] values) {
        return Stream.of(values)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .toArray(String[]::new);
    }

    public TableConnExtractor getTableConnExtractor() {
        TableConnExtractor tableConnExtractor = new TableConnExtractor();
        tableConnExtractor.setMaxConn(getInt(Config.POPUP_MAX_CONNECTIONS));
        tableConnExtractor.setConnWords(getWords(getStringArray(Config.POPUP_CONN_COLUMNS_WORDS)));
        tableConnExtractor.setHostWords(getWords(getStringArray(Config.POPUP_HOST_COLUMNS_WORDS)));
        tableConnExtractor.setPortWords(getWords(getStringArray(Config.POPUP_PORT_COLUMNS_WORDS)));
        return tableConnExtractor;
    }

    public static Config getInstance() {
        return instance;
    }

    protected void init() {
        workspaceConfig = new PropertiesConfig(getWorkspacePath());
        serverConfig = new ServerConfig(getServerConfigPath());

        checkForUpgrade();
    }

    private void upgradeTo14() {
        log.info("Upgrading config to version 1.4");
        String defAuth = DefaultAuthenticationMechanism.NAME;
        if (getDefaultAuthMechanism().equals(OLD_DEF_AUTHMETHOD) ) {
            setDefaultAuthMechanism(defAuth);
        }

        setDefaultCredentials(defAuth, getDefaultCredentials(OLD_DEF_AUTHMETHOD));
        config.remove("auth." + OLD_DEF_AUTHMETHOD + ".user");
        config.remove("auth." + OLD_DEF_AUTHMETHOD + ".password");

        //server.1.authenticationMechanism
        Pattern pattern = Pattern.compile("server\\.[0-9]+\\.authenticationMechanism");
        for (String key: config.keySet().toArray(new String[0]) ) {
            if (pattern.matcher(key).matches() ) {
                String value = config.getProperty(key);
                if (value.equals(OLD_DEF_AUTHMETHOD)) {
                    config.setProperty(key, defAuth);
                }
            }
        }

        config.setProperty("version", VERSION);
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

    private void removeServerListConfig() {
        config.remove("serverList.x");
        config.remove("serverList.y");
        config.remove("serverList.width");
        config.remove("serverList.height");
    }

    private void checkForUpgrade() {
        if (config.isEmpty()) {
            log.info("Found no or empty config");
            config.setProperty("version", VERSION);
            initServerHistory();
            return;
        }


        String version = config.getProperty("version", OLD_VERSION);
        if (version.equals(OLD_VERSION) || version.equals(VERSION12)) {
            log.warn("Very old version: {}. Will try to upgrade...", version);
            config.setProperty("version", VERSION13);
        }


        if (config.getProperty("version", VERSION).equals(VERSION13)) {
            upgradeTo14();
        }

        initServerHistory();
        migrateSaveOnExit();
        removeServerListConfig();

        if (serverConfig.tryToLoadOldConifg(config)) {
            save();
        }

        config.setProperty("version", VERSION);
    }

    private void initServerHistory() {
        int depth = getInt(Config.SERVER_HISTORY_DEPTH);
        serverHistory = new HistoricalList<>(depth);
        String[] serverArray = getStringArray(Config.SERVER_HISTORY_LIST);
        for (int i = serverArray.length-1; i>=0; i--) {
            Server server = serverConfig.getServer(serverArray[i]);
            if (server == Server.NO_SERVER) continue;
            serverHistory.add(server);
        }
    }

    public List<Server> getServerHistory() {
        return Collections.unmodifiableList(serverHistory);
    }

    public void addServerToHistory(Server server) {
        serverHistory.add(server);
        String[] newArray = serverHistory.stream()
                                        .map(Server::getFullName)
                                        .toArray(String[]::new);
        setStringArray(Config.SERVER_HISTORY_LIST, newArray);
    }

    // Resolve or create a new server by connection string.
    // Accept possible various connectionString such as:
    // `:host:port:user:password
    // host:port
    // If user and password are not found, defaults form default AuthenticationMechanism are used
    public Server getServerByConnectionString(String connectionString) {
        return getServerByConnectionString(connectionString, getDefaultAuthMechanism());
    }

    public Server getServerByConnectionString(String connectionString, String authMethod) {
        return QConnection.getByConnection(connectionString, authMethod, getDefaultCredentials(authMethod), serverConfig.allServers());
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

    public ServerTreeNode getServerTree() {
        return serverConfig.getServerTree();
    }

}
