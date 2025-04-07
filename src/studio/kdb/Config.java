package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.*;
import studio.ui.Util;
import studio.ui.settings.StrokeStyleEditor;
import studio.utils.*;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static studio.kdb.config.ConfigVersion.*;

public class Config  {
    private static final Logger log = LogManager.getLogger();

    private static final ConfigTypeRegistry configTypeRegistry = new ConfigTypeRegistry();
    public static final String COMMENT = configDefault("comment", ConfigType.STRING, "");
    public static final String LOOK_AND_FEEL = configDefault("lookandfeel", ConfigType.STRING, UIManager.getLookAndFeel().getClass().getName());
    public static final String EXEC_ALL = configDefault("execAllOption", ConfigType.ENUM, ExecAllOption.Ask);
    public static final String MAX_CHARS_IN_RESULT = configDefault("maxCharsInResult", ConfigType.INT, 50000);
    public static final String MAX_CHARS_IN_TABLE_CELL = configDefault("maxCharsInTableCell", ConfigType.INT, 256);
    public static final String MRU_FILES = configDefault("mrufiles", ConfigType.STRING_ARRAY, List.of());
    public static final String NOTES_HASH = configDefault("notesHash", ConfigType.STRING, "");
    public static final String RESULT_TAB_COUNTS = configDefault("resultTabsCount", ConfigType.INT, 5);

    public static final String SHOW_SERVER_COMBOBOX = configDefault("showServerComboBox", ConfigType.BOOLEAN, true);
    public static final String AUTO_SAVE = configDefault("isAutoSave", ConfigType.BOOLEAN, false);
    public static final String ACTION_ON_EXIT = configDefault("actionOnExit", ConfigType.ENUM, ActionOnExit.SAVE);
    public static final String CHART_BOUNDS = configDefault("chartBounds", ConfigType.BOUNDS, Util.getDefaultBounds(0.5));
    public static final String CELL_RIGHT_PADDING = configDefault("cellRightPadding", ConfigType.DOUBLE, 0.5);
    public static final String CELL_MAX_WIDTH = configDefault("cellMaxWidth", ConfigType.INT, 200);

    public static final String RSTA_ANIMATE_BRACKET_MATCHING = configDefault("rstaAnimateBracketMatching", ConfigType.BOOLEAN, true);
    public static final String RSTA_HIGHLIGHT_CURRENT_LINE = configDefault("rstaHighlightCurrentLine", ConfigType.BOOLEAN, true);
    public static final String RSTA_WORD_WRAP = configDefault("rstaWordWrap", ConfigType.BOOLEAN, false);
    public static final String RSTA_INSERT_PAIRED_CHAR = configDefault("rstaInsertPairedChar", ConfigType.BOOLEAN, true);

    public static final String DEFAULT_LINE_ENDING = configDefault("defaultLineEnding", ConfigType.ENUM, LineEnding.Unix);

    public static final String COLOR_BACKGROUND = configDefault("backgroundColor", ConfigType.COLOR, Color.WHITE);

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

    public static final String DEFAULT_AUTH_CONFIG = configDefault("defaultAuthConfig", ConfigType.DEFAULT_AUTH_CONFIG, DefaultAuthConfig.DEFAULT);
    public static final String TABLE_CONN_EXTRACTOR = configDefault("tableExtractorConfig", ConfigType.TABLE_CONN_EXTRACTOR, TableConnExtractor.DEFAULT);
    public static final String COLOR_TOKEN_CONFIG = configDefault("tokenColors", ConfigType.COLOR_TOKEN_CONFIG, ColorTokenConfig.DEFAULT);
    public static final String SERVER_HISTORY = configDefault("serverHistory", ConfigType.SERVER_HISTORY, new ServerHistoryConfig(20, List.of()));
    public static final String CHART_COLORSETS = configDefault("chartColorSets", ConfigType.COLOR_SETS, ColorSets.DEFAULT);
    public static final String CHART_STROKE_STYLES = configDefault("chartStrokeStyles", ConfigType.STRING_ARRAY,
                                                                    List.of("", "10,10", "10,5", "5,5", "1.5,3", "10,3,3,3" ) );
    public static final String CHART_STROKE_WIDTHS = configDefault("chartStrokeWidths", ConfigType.DOUBLE_ARRAY,
                                                                    List.of(2.0, 1.0, 1.5, 3.0) );

    public static final String CONFIG_VERSION = configDefault("version", ConfigType.ENUM, ConfigVersion.V2_0);

    public static final String OLD_CONFIG_FILENAME = "studio.properties";
    public static final String CONFIG_FILENAME = "studio.json";
    public static final String WORKSPACE_FILENAME = "workspace.properties";
    public static final String SERVERCONFIG_FILENAME = "servers.json";

    protected StudioConfig studioConfig;
    protected PropertiesConfig workspaceConfig;
    protected ServerConfig serverConfig;
    private HistoricalList<Server> serverHistory;

    private final Path basePath;
    // Can be overridden in test cases
    protected static Config instance = new Config();

    protected Config(Path path) {
        this.basePath = path;
        init();
    }

    private Config() {
        this(EnvConfig.getBaseFolder());
    }

    public void saveToDisk() {
        FileConfig.saveAllOnDisk();
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
        return basePath.resolve(WORKSPACE_FILENAME);
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

    public TableConnExtractor getTableConnExtractor() {
        return (TableConnExtractor) studioConfig.get(Config.TABLE_CONN_EXTRACTOR);
    }

    public static Config getInstance() {
        return instance;
    }

    protected void init() {
        serverConfig = new ServerConfig(new FileConfig(basePath.resolve(SERVERCONFIG_FILENAME)));

        checkOldPropertiesToUpgrade();

        FileConfig defaultFileConfig = new FileConfig(EnvConfig.getPluginFolder().resolve(CONFIG_FILENAME));
        FileConfig fileConfig = new FileConfig(basePath.resolve(CONFIG_FILENAME));
        studioConfig = new StudioConfig(configTypeRegistry, fileConfig, defaultFileConfig);

        workspaceConfig = new PropertiesConfig(getWorkspacePath());
        initServerHistory();
    }

    private void upgradeTo14(Properties config) {
        final String OLD_DEF_AUTHMETHOD = "Username and password";

        log.info("Upgrading config to version 1.4");
        String defAuth = DefaultAuthenticationMechanism.NAME;
        if (config.getProperty("auth").equals(OLD_DEF_AUTHMETHOD) ) {
            config.setProperty("auth", defAuth);
        }

        String oldUserKey = "auth." + OLD_DEF_AUTHMETHOD + ".user";
        String oldPasswordKey = "auth." + OLD_DEF_AUTHMETHOD + ".password";
        String oldUser = config.getProperty(oldUserKey, "");
        String oldPassword = config.getProperty(oldPasswordKey, "");
        config.setProperty("auth." + defAuth + ".user", oldUser);
        config.setProperty("auth." + defAuth + ".password", oldPassword);
        config.remove(oldUserKey);
        config.remove(oldPasswordKey);

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

        config.setProperty("version", V1_4.getVersion());
    }

    private void removeServerListConfig(Properties config) {
        config.remove("serverList.x");
        config.remove("serverList.y");
        config.remove("serverList.width");
        config.remove("serverList.height");
    }

    private void checkOldPropertiesToUpgrade() {
        Path oldPath = basePath.resolve(OLD_CONFIG_FILENAME);

        if (!Files.exists(oldPath)) return;

        Path newPath = basePath.resolve(CONFIG_FILENAME);

        log.info("Found old config {}. Converting to {}...", oldPath, newPath);
        try (Reader reader = Files.newBufferedReader(oldPath)) {
            Properties properties = new Properties();
            properties.load(reader);
            properties = checkOldToUpgrade(properties);

            log.info("Converting {} properties", properties.size());
            ConfigToJsonConverter converter = getConfigToJsonConverter();
            converter.convert(properties, newPath);
            Properties remaining = converter.getRemainingProperties();
            if (remaining.isEmpty()) {
                log.info("All properties were successfully converted");
            } else {
                log.warn("The following properties are either not used or not converted. They will be discarded");
                log.warn(remaining);
            }

        } catch (Exception e) {
            log.error("Error during old properties {} conversion", oldPath, e);
        } finally {
            try {
                log.info("Deleting of old properties {}", oldPath);
                Files.delete(oldPath);
            } catch (IOException e) {
                log.error("Error on old properties file {} removal", oldPath, e);
            }
        }
    }

    private Properties checkOldToUpgrade(Properties config) {
        String version = config.getProperty("version", V1_1.getVersion());
        if (version.equals(V1_1.getVersion()) || version.equals(V1_2.getVersion())) {
            log.warn("Very old version: {}. Will try to upgrade...", version);
            config.setProperty("version", V1_3.getVersion());
        }

        if (config.getProperty("version", V1_4.getVersion()).equals(V1_3.getVersion())) {
            upgradeTo14(config);
        }

        removeServerListConfig(config);
        serverConfig.tryToLoadOldConifg(config);
        return config;
    }

    private void initServerHistory() {
        serverHistory = getServerHistoryConfig().toServerHistory(serverConfig);
    }

    public HistoricalList<Server> getServerHistory() {
        return serverHistory;
    }

    public void refreshServerToHistory() {
        setServerHistoryConfig(ServerHistoryConfig.fromServerHistory(serverHistory));
    }

    public ServerHistoryConfig getServerHistoryConfig() {
        return (ServerHistoryConfig) studioConfig.get(Config.SERVER_HISTORY);
    }

    public void setServerHistoryConfig(ServerHistoryConfig serverHistoryConfig) {
        studioConfig.set(Config.SERVER_HISTORY, serverHistoryConfig);
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

    public DefaultAuthConfig getDefaultAuthConfig() {
        return (DefaultAuthConfig) studioConfig.get(Config.DEFAULT_AUTH_CONFIG);
    }

    public boolean setDefaultAuthConfig(DefaultAuthConfig config) {
        return studioConfig.set(Config.DEFAULT_AUTH_CONFIG, config);
    }

    public Credentials getDefaultCredentials(String authenticationMechanism) {
        return getDefaultAuthConfig().getCredential(authenticationMechanism);
    }

    public void setDefaultCredentials(String authenticationMechanism, Credentials credentials) {
        DefaultAuthConfig config = getDefaultAuthConfig();
        DefaultAuthConfig newConfig = new DefaultAuthConfig(config, authenticationMechanism, credentials);
        setDefaultAuthConfig(newConfig);
    }

    public String getDefaultAuthMechanism() {
        return getDefaultAuthConfig().getDefaultAuth();
    }

    public void setDefaultAuthMechanism(String authMechanism) {
        DefaultAuthConfig config = getDefaultAuthConfig();
        DefaultAuthConfig newConfig = new DefaultAuthConfig(config, authMechanism);
        setDefaultAuthConfig(newConfig);
    }

    public ColorTokenConfig getColorTokenConfig() {
        return (ColorTokenConfig) studioConfig.get(Config.COLOR_TOKEN_CONFIG);
    }

    public ServerTreeNode getServerTree() {
        return serverConfig.getServerTree();
    }


    public static ConfigToJsonConverter getConfigToJsonConverter() {
        return new ConfigToJsonConverter(configTypeRegistry);
    }

    public static String configDefault(String key, ConfigType type, Object defaultValue) {
        return configTypeRegistry.add(key, type, defaultValue);
    }

    public String getString(String key) {
        return studioConfig.getString(key);
    }
    public boolean setString(String key, String value) {
        return studioConfig.setString(key, value);
    }

    public int getInt(String key) {
        return studioConfig.getInt(key);
    }
    public boolean setInt(String key, int value) {
        return studioConfig.setInt(key, value);
    }

    public double getDouble(String key) {
        return studioConfig.getDouble(key);
    }
    public boolean setDouble(String key, double value) {
        return studioConfig.setDouble(key, value);
    }

    public boolean getBoolean(String key) {
        return studioConfig.getBoolean(key);
    }
    public boolean setBoolean(String key, boolean value) {
        return studioConfig.setDouble(key, value);
    }

    public <T extends Enum<T>> T getEnum(String key) {
        return studioConfig.getEnum(key);
    }
    public <T extends Enum<T>> boolean setEnum(String key, T value) {
        return studioConfig.setEnum(key, value);
    }

    //@TODO: do we need?
    public Color getColor(String key) {
        return studioConfig.getColor(key);
    }

    public Font getFont(String key) {
        return studioConfig.getFont(key);
    }
    public boolean setFont(String key, Font value) {
        return studioConfig.setFont(key, value);
    }

    public Dimension getSize(String key) {
        return studioConfig.getSize(key);
    }
    public boolean setSize(String key, Dimension value) {
        return studioConfig.setSize(key, value);
    }

    public Rectangle getBounds(String key) {
        Rectangle bounds = studioConfig.getBounds(key);

        // Hack? If bounds do not fit to screen, return default
        if (! Util.fitToScreen(bounds) ) {
            return (Rectangle) studioConfig.getDefault(key);
        }

        return bounds;
    }
    public boolean setBounds(String key, Rectangle value) {
        return studioConfig.setBounds(key, value);
    }

    public FileChooserConfig getFileChooserConfig(String key) {
        return studioConfig.getFileChooserConfig(key);
    }
    public boolean setFileChooserConfig(String key, FileChooserConfig value) {
        return studioConfig.setFileChooserConfig(key, value);
    }

    public List<String> getStringArray(String key) {
        return studioConfig.getArray(key);
    }
    public boolean setStringArray(String key, List<String> value) {
        return studioConfig.setArray(key, value);
    }

    public List<Double> getDoubleArray(String key) {
        return studioConfig.getArray(key);
    }
    public boolean setDoubleArray(String key, List<Double> value) {
        return studioConfig.setArray(key, value);
    }

    public ColorSets getChartColorSets() {
        return (ColorSets) studioConfig.get(Config.CHART_COLORSETS);
    }

    public boolean setChartColorSets(ColorSets colorSets) {
        return studioConfig.set(Config.CHART_COLORSETS, colorSets);
    }

    public List<BasicStroke> getStyleStrokes() {
        List<String> texts = studioConfig.getArray(Config.CHART_STROKE_STYLES);
        List<BasicStroke> strokes = new ArrayList<>(texts.size());
        for (String text: texts) {
            strokes.add(StrokeStyleEditor.parseDashArray(text));
        }
        return strokes;
    }

}
