package studio.kdb.config;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.FileChooserConfig;
import studio.ui.Util;
import studio.utils.FileConfig;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.*;
import java.util.List;

public class StudioConfig {

    public final static String COMMENT = "comment";
    private final ConfigTypeRegistry registry;
    private final FileConfig fileConfig;
    private final FileConfig defaultFileConfig;
    private final JsonConverter jsonConverter;
    private final Map<String, Object> config;
    private final Gson gson;

    private static final Logger log = LogManager.getLogger();

    public StudioConfig(ConfigTypeRegistry registry, FileConfig file) {
        this(registry, file, null);
    }

    public StudioConfig(ConfigTypeRegistry registry, FileConfig fileConfig, FileConfig defaultFileConfig) {
        this(registry, fileConfig, defaultFileConfig, null);
    }

    public StudioConfig(ConfigTypeRegistry registry, FileConfig fileConfig, FileConfig defaultFileConfig, JsonConverter converter) {
        this.registry = registry;
        this.fileConfig = fileConfig;
        this.defaultFileConfig = defaultFileConfig;
        this.jsonConverter = converter;

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        config = initConfig();
    }

    public FileConfig getFileConfig() {
        return fileConfig;
    }

    private JsonObject getJson(FileConfig fileConfig) {
        if (fileConfig == null || ! fileConfig.fileExists()) return new JsonObject();

        try {
            String content = fileConfig.getContent();
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (IOException | JsonParseException e) {
            log.error("Error in loading and parsing {}", fileConfig);
        }
        return new JsonObject();
    }

    private JsonObject getJsonFromRegistry() {
        JsonObject jRegistry = new JsonObject();
        for (String key: registry.keySet()) {
            JsonElement jsonElement = registry.getConfigType(key).toJson(registry.getDefault(key));
            jRegistry.add(key, jsonElement);
        }
        return jRegistry;
    }

    public JsonObject getJsonFromConfig() {
        JsonObject json = new JsonObject();
        for (String key: config.keySet()) {
            ConfigType type = registry.getConfigType(key);
            if (type == null) {
                log.error("type for key {} is null", key);
            } else {
                json.add(key, type.toJson(config.get(key)));
            }
        }
        return json;
    }

    private Map<String, Object> parseConfig(JsonObject json) {
        Map<String, Object> map = new TreeMap<>();
        for (String key: json.keySet()) {
            try {
                ConfigType type = registry.getConfigType(key);
                if (type == null) {
                    log.warn("Unknown config key {}. Skipping...", key);
                    continue;
                }
                Object value = type.fromJson(json.get(key), registry.getDefault(key));
                map.put(key, value);
            } catch (Exception e) {
                log.warn("Error parsing key {}. Skipping...", key, e);
            }
        }
        return map;
    }

    private Map<String, Object> initConfig() {
        Map<String, Object> map = new TreeMap<>();
        try {
            JsonObject json = getJson(fileConfig);
            if (json.has(COMMENT)) {
                log.info("Loaded config with the comment: {}", json.get(COMMENT).getAsString());
                json.remove(COMMENT);
            }

            if (jsonConverter != null) {
                boolean changed = jsonConverter.convert(json);
                if (changed) {
                    log.info("The config has been converted");
                    save(json);
                }
            }

            JsonObject jConfig = Util.deepMerge(getJsonFromRegistry(), getJson(defaultFileConfig));

            json = Util.deepMerge(jConfig, json);
            map = parseConfig(json);
        } catch (Exception e) {
            log.error("Error on parsing json {}", fileConfig == null ? "<null>" : fileConfig.getPath(), e);
        }

        log.info("Loaded config {} with {} settings", fileConfig == null ? "<null>" : fileConfig.getPath(), map.size());
        return map;
    }

    private void save() {
        if (fileConfig == null) return;
        JsonObject jConfig = toJson();
        save(jConfig);
    }

    private JsonObject toJson() {
        JsonObject jConfig = getJsonFromConfig();
        JsonObject jDefault = Util.deepMerge(getJsonFromRegistry(), getJson(defaultFileConfig));
        return Util.deepExclude(jConfig, jDefault);
    }

    private void save(JsonObject jsonConfig) {
        if (fileConfig == null) return;
        try (Writer writer = fileConfig.getWriter()) {

            JsonObject json = new JsonObject();
            String comment = String.format("Auto-generated at %s from a process with pid %d", Instant.now(), ProcessHandle.current().pid());
            json.addProperty(COMMENT, comment);

            json = Util.deepMerge(json, jsonConfig);
            gson.toJson(json, writer);
        } catch (IOException e) {
            log.error("Error on saving json {}", fileConfig.getPath(), e);
        }
    }

    public Object getDefault(String key) {
        ConfigType type = getType(key);
        return type.clone(registry.getDefault(key));
    }

    public int size() {
        return config.size();
    }

    private ConfigType getType(String key) {
        ConfigType type = registry.getConfigType(key);
        if (type == null) throw new IllegalArgumentException("Unknown key: " + key);
        return type;
    }

    public Object get(String key) {
        ConfigType type = getType(key);
        Object value = config.get(key);
        if (value != null) return type.clone(value);

        return getDefault(key);
    }

    public boolean set(String key, Object value) {
        Object currentValue = get(key);

        if (Objects.equals(currentValue, value)) return false;

        ConfigType type = registry.getConfigType(key);
        value = type.clone(value);
        if (value instanceof Freezable) ((Freezable) value).freeze();
        config.put(key, value);

        save();
        return true;
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public boolean setString(String key, String value) {
        return set(key, value);
    }

    public int getInt(String key) {
        return (Integer) get(key);

    }

    public boolean setInt(String key, int value) {
        return set(key, value);
    }

    public double getDouble(String key) {
        return (Double) get(key);

    }

    public boolean setDouble(String key, double value) {
        return set(key, value);
    }

    public boolean getBoolean(String key) {
        return (Boolean) get(key);

    }

    public boolean setBoolean(String key, boolean value) {
        return set(key, value);
    }

    public Font getFont(String key) {
        return (Font) get(key);

    }

    public boolean setFont(String key, Font value) {
        return set(key, value);
    }

    public Rectangle getBounds(String key) {
        return (Rectangle) get(key);

    }

    public boolean setBounds(String key, Rectangle value) {
        return set(key, value);
    }

    public Color getColor(String key) {
        return (Color) get(key);

    }

    public boolean setColor(String key, Color value) {
        return set(key, value);
    }

    public <T extends Enum<T>> T getEnum(String key) {
        return (T) get(key);

    }

    public <T extends Enum<T>> boolean setEnum(String key, T value) {
        return set(key, value);
    }

    public Dimension getSize(String key) {
        return (Dimension) get(key);

    }

    public boolean setSize(String key, Dimension value) {
        return set(key, value);
    }

    public FileChooserConfig getFileChooserConfig(String key) {
        return (FileChooserConfig) get(key);

    }

    public boolean setFileChooserConfig(String key, FileChooserConfig value) {
        return set(key, value);
    }

    public <T> List<T> getArray(String key) {
        return Collections.unmodifiableList( (List<T>) get(key));
    }

    public <T> boolean setArray(String key, List<T> value) {
        return set(key, new ArrayList<>(value));
    }

}
