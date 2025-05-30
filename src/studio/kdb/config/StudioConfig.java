package studio.kdb.config;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.FileChooserConfig;
import studio.utils.FileConfig;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.List;
import java.util.*;

public class StudioConfig {

    private final static String COMMENT = "comment";
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


    private JsonObject deepMerge(JsonObject jBase, JsonObject jAdd) {
        JsonObject json = new JsonObject();
        for (String key: jBase.keySet()) {
            if (jAdd.has(key)) {
                JsonElement elBase = jBase.get(key);
                JsonElement elAdd = jAdd.get(key);
                if (elBase.isJsonObject() && elAdd.isJsonObject()) {
                    json.add(key, deepMerge(elBase.getAsJsonObject(), elAdd.getAsJsonObject()));
                } else {
                    json.add(key, elAdd);
                }
            } else {
                json.add(key, jBase.get(key));
            }
        }

        for (String key: jAdd.keySet()) {
            if (! jBase.has(key)) {
                json.add(key, jAdd.get(key));
            }
        }
        return json;
    }

    private JsonObject deepExclude(JsonObject jBase, JsonObject jMinus) {
        JsonObject json = new JsonObject();
        for (String key: jBase.keySet()) {
            if (jMinus.has(key)) {
                JsonElement elBase = jBase.get(key);
                JsonElement elMinus = jMinus.get(key);
                if (elBase.isJsonObject() && elMinus.isJsonObject()) {
                    json.add(key, deepExclude(elBase.getAsJsonObject(), elMinus.getAsJsonObject()));
                } else {
                    if (! elBase.equals(elMinus)) {
                        json.add(key, elBase);
                    } // we exclude, if elBase equals to elMinus
                }
            } else {
                json.add(key, jBase.get(key));
            }
        }
        return json;
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

    private JsonObject getJsonFromConfig() {
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

            JsonObject jConfig = deepMerge(getJsonFromRegistry(), getJson(defaultFileConfig));

            json = deepMerge(jConfig, json);
            map = parseConfig(json);
        } catch (Exception e) {
            log.error("Error on parsing json {}", fileConfig.getPath(), e);
        }

        log.info("Loaded config {} with {} settings", fileConfig.getPath(), map.size());
        return map;
    }

    private void save() {
        JsonObject jConfig = getJsonFromConfig();
        JsonObject jDefault = deepMerge(getJsonFromRegistry(), getJson(defaultFileConfig));
        jConfig = deepExclude(jConfig, jDefault);
        save(jConfig);
    }

    private void save(JsonObject jsonConfig) {
        try (Writer writer = fileConfig.getWriter()) {

            JsonObject json = new JsonObject();
            String comment = String.format("Auto-generated at %s from a process with pid %d", Instant.now(), ProcessHandle.current().pid());
            json.add(COMMENT, new JsonPrimitive(comment));

            json = deepMerge(json, jsonConfig);
            gson.toJson(json, writer);
        } catch (IOException e) {
            log.error("Error on saving json {}", fileConfig.getPath(), e);
        }
    }

    public Object getDefault(String key) {
        return registry.getDefault(key);
    }

    public int size() {
        return config.size();
    }

    public Object get(String key) {
        ConfigType type = registry.getConfigType(key);
        if (type == null) throw new IllegalArgumentException("Unknown key: " + key);

        Object value = config.get(key);
        if (value != null) return type.clone(value);

        return type.clone(getDefault(key));
    }

    public boolean set(String key, Object value) {
        Object currentValue = get(key);

        if (Objects.equals(currentValue, value)) return false;

        ConfigType type = registry.getConfigType(key);
        config.put(key, type.clone(value));

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

    public boolean setDouble(String key, boolean value) {
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
