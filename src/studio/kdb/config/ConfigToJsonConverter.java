package studio.kdb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Config;
import studio.kdb.FileChooserConfig;
import studio.ui.Util;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigToJsonConverter {

    private final static Logger log = LogManager.getLogger();

    private Properties properties;
    private final ConfigTypeRegistry registry;
    // we convert from Properties to V2_0. If the latest (current) Config has something different,
    // we have to manually create the old structure which should converted to the latest version later
    private final JsonObject json = new JsonObject();

    public ConfigToJsonConverter(ConfigTypeRegistry registry) {
        this.registry = registry;
    }

    public void convert(Properties properties, Path path) {
        if (Files.exists(path)) throw new IllegalArgumentException(String.format("File %s already exist", path));
        this.properties = new Properties();
        this.properties.putAll(properties);

        int count = properties.size();

        convert(this::removeEncodingAndVersion, "encoding and version");
        convert(this::convertDefaultAuthConfig, "DefaultAuthConfig");
        convert(this::convertColorTokenConfig, "ColorTokenConfig");
        convert(this::convertBgColor, "backgroundColor");
        convert(this::convertMRUFiles, "mrufiles");
        convert(this::convertTableConnExtractor, "TableConnExtractor");
        convert(this::convertServerHistory, "serverHistory");
        convert(this::convertStandard, "standard properties");

        log.info("Converted {} out of {} properties", count - getRemainingProperties().size(), count);


        String comment = String.format("Converted from old Properties format at %s from a process with pid %d", Instant.now(), ProcessHandle.current().pid());
        json.addProperty(StudioConfig.COMMENT, comment);

        saveJson(path, json);
    }

    private void saveJson(Path path, JsonObject json) {
        try (Writer writer = Files.newBufferedWriter(path)) {

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(json, writer);
        } catch (IOException e) {
            log.error("Error on saving json {}", path, e);
        }
    }

    private void set(String key, Object value) {
        ConfigType type = registry.getConfigType(key);
        if (type == null) throw new IllegalArgumentException("Key " + key + " is not defined");
        JsonElement jsonValue = type.toJson(value);
        json.add(key, jsonValue);
    }

    private void convert(Runnable action, String step) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("Config conversion error during step {}", step, e);
        }
    }

    private void convertStandard() {
        for (String key: registry.keySet()) {
            try {
                ConfigType type = registry.getConfigType(key);

                Object value = null;
                switch (type) {
                    case STRING:
                        value = get(key);
                        break;
                    case INT:
                        String str = get(key);
                        if (str != null) value = Integer.parseInt(str);
                        break;
                    case DOUBLE:
                        str = get(key);
                        if (str != null) value = Double.parseDouble(str);
                        break;
                    case BOOLEAN:
                        str = get(key);
                        if (str != null) value = Boolean.parseBoolean(str);
                        break;
                    case ENUM:
                        str = get(key);
                        if (str != null) {
                            value = Enum.valueOf((Class<? extends Enum>) registry.getDefault(key).getClass(), str);
                        }
                        break;
                    case FONT:
                        String name = get(key+".name");
                        if (name == null) break;
                        int size = Integer.parseInt(get(key+".size"));
                        FontStyle style = FontStyle.valueOf(get(key+".style"));
                        value = style.getFont(name, size);
                        break;
                    case SIZE:
                        str = get(key+".width");
                        if (str == null) break;
                        int width = Integer.parseInt(str);
                        int height = Integer.parseInt(get(key+".height"));
                        value = new Dimension(width, height);
                        break;
                    case BOUNDS:
                        str = get(key+".width");
                        if (str == null) break;
                        width = Integer.parseInt(str);
                        height = Integer.parseInt(get(key+".height"));
                        int x = Integer.parseInt(get(key+".x"));
                        int y = Integer.parseInt(get(key+".y"));
                        value = new Rectangle(x, y, width, height);
                        break;
                    case FILE_CHOOSER:
                        name = get(key+".filename");
                        if (name == null) break;
                        width = Integer.parseInt(get(key+".prefSize.width"));
                        height = Integer.parseInt(get(key+".prefSize.height"));
                        value = new FileChooserConfig(name, new Dimension(width, height));
                        break;
                }

                if (value != null) set(key, value);
            } catch (Exception e) {
                log.error("Exception during conversion of key {}", key, e);
            }
        }
    }

    private void convertDefaultAuthConfig() {

        String defAuth = get("auth");
        if (defAuth == null) defAuth = DefaultAuthenticationMechanism.NAME;

        Pattern userPattern = Pattern.compile("auth\\.([^.]*)\\.user");
        Pattern passwordPattern = Pattern.compile("auth\\.([^.]*)\\.password");
        Set<String> addedMethods = new HashSet<>();

        Map<String, Credentials> map = new HashMap<>();
        List<String> keys = new ArrayList<>(properties.stringPropertyNames());
        for(String key: keys) {
            String authMethod = null;

            Matcher matcher = userPattern.matcher(key);
            if (matcher.matches()) authMethod = matcher.group(1);

            matcher = passwordPattern.matcher(key);
            if (matcher.matches()) authMethod = matcher.group(1);

            if (authMethod == null) continue;
            if (addedMethods.contains(authMethod)) continue;

            String user = get("auth." + authMethod + ".user");
            String password = get("auth." + authMethod + ".password");

            if (user == null) user = "";
            if (password == null) password = "";
            map.put(authMethod, new Credentials(user, password));
            addedMethods.add(authMethod);
        }

        DefaultAuthConfig authConfig = new DefaultAuthConfig(defAuth, map);
        set(Config.DEFAULT_AUTH_CONFIG, authConfig);
    }

    private Color toColor(String value) {
        return new Color(Integer.parseInt(value, 16));
    }

    private void convertColorTokenConfig() {
        JsonObject jsonTokens = new JsonObject();
        for(ColorToken token: ColorToken.values()) {
            String value = get("token." + token.name());
            if (value == null) continue;

            Color color = Util.stringToColor(value);
            TokenStyle tokenStyle = token.getDefaultStyle().derive(color);

            jsonTokens.addProperty(token.name().toLowerCase(), tokenStyle.toString());
        }
        json.add(Config.TOKEN_STYLE_CONFIG, jsonTokens);
    }

    private void convertBgColor() {
        String value = get("token.BACKGROUND");
        if (value == null) return;
        json.addProperty("backgroundColor", value);
    }

    private void convertMRUFiles() {
        String value = get(Config.MRU_FILES);
        if (value == null) return;

        set(Config.MRU_FILES, List.of(value.split(",")));
    }

    private void convertTableConnExtractor() {
        int maxConn = TableConnExtractor.DEFAULT.getMaxConn();
        String value = get("tableMaxConnectionPopup");
        if (value != null) {
            try {
                maxConn = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Error on parsing tableMaxConnectionPopup", e);
            }
        }
        value = get("connColWords");
        List<String> connWords = value == null ? TableConnExtractor.DEFAULT.getConnWords() : List.of(value.split(","));

        value = get("hostColWords");
        List<String> hostWords = value == null ? TableConnExtractor.DEFAULT.getHostWords() : List.of(value.split(","));

        value = get("portColWords");
        List<String> portWords = value == null ? TableConnExtractor.DEFAULT.getPortWords() : List.of(value.split(","));

        TableConnExtractor extractor = new TableConnExtractor(maxConn, connWords, hostWords, portWords);
        set(Config.TABLE_CONN_EXTRACTOR, extractor);
    }

    private void convertServerHistory() {
        int depth = 20;
        String value = get("serverHistoryDepth");
        if (value != null) {
            try {
                depth = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Error on parsing serverHistoryDepth", e);
            }
        }

        List<String> list = new ArrayList<>();
        for (int i=0; ; i++) {
            value = get("serverHistory." + i);
            if (value == null) break;
            list.add(value);
        }

        ServerHistoryConfig serverHistoryConfig = new ServerHistoryConfig(depth, list);
        set(Config.SERVER_HISTORY, serverHistoryConfig);
    }

    private void removeEncodingAndVersion() {
        String value = get("encoding");
        if (value != null && ! value.equals(StandardCharsets.UTF_8.name())) {
            log.warn("While encoding was {}, it will be {}", value, StandardCharsets.UTF_8.name());
        }

        value = get("version");
        if (! "1.4".equals(value)) {
            log.warn("Config version was {}, while it was expected to be 1.4", value);
        }
        set(Config.CONFIG_VERSION, ConfigVersion.V2_0);
    }

    private String get(String key) {
        String value = (String)properties.remove(key);
        if (value != null) log.info("Converting {}", key);
        return value;
    }

    public Properties getRemainingProperties() {
        return properties;
    }
}
