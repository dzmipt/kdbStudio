package studio.kdb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.FileChooserConfig;
import studio.utils.FileConfig;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.List;
import java.util.*;

public class StudioConfig {

    private final ConfigTypeRegistry registry;
    private final FileConfig fileConfig;
    private final Map<String, Object> config;
    private final Map<String, Object> defaults;
    private final Gson gson;

    private static final Logger log = LogManager.getLogger();

    public StudioConfig(ConfigTypeRegistry registry, FileConfig file) {
        this(registry, file, null);
    }

    public StudioConfig(ConfigTypeRegistry registry, FileConfig fileConfig, FileConfig defaultConfig) {
        this.registry = registry;
        this.fileConfig = fileConfig;
        config = load(fileConfig);
        defaults = load(defaultConfig);

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public FileConfig getFileConfig() {
        return fileConfig;
    }

    private Map<String, Object> load(FileConfig fileConfig) {
        Map<String, Object> map = new TreeMap<>();
        if (fileConfig == null) return map;
        if (! fileConfig.fileExists()) return map;

        try {
            String content = fileConfig.getContent();
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            for (String key: json.keySet()) {
                try {
                    ConfigType type = registry.getConfigType(key);
                    if (type == null) {
                        log.warn("Unknown config key {}. Skipping...", key);
                        continue;
                    }
                    Object value = type.fromJson(json.get(key), registry.getDefault(key));
                    if (key.equals(Config.COMMENT)) {
                        log.info("Loaded config with the comment: {}", value);
                    } else {
                        map.put(key, value);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing key {}. Skipping...", key, e);
                }

            }
        } catch (IOException | IllegalStateException e) {
            log.error("Error on parsing json {}", fileConfig.getPath(), e);
        }

        log.info("Loaded config {} with {} settings", fileConfig.getPath(), map.size());
        return map;
    }

    private void save() {
        try (Writer writer = fileConfig.getWriter()) {

            JsonObject json = new JsonObject();
            String comment = String.format("Auto-generated at %s from a process with pid %d", Instant.now(), ProcessHandle.current().pid());
            json.add(Config.COMMENT, ConfigType.STRING.toJson(comment));

            for (String key: config.keySet()) {
                ConfigType type = registry.getConfigType(key);

                json.add(key, type.toJson(config.get(key)));
            }

            gson.toJson(json, writer);
        } catch (IOException e) {
            log.error("Error on saving json {}", fileConfig.getPath(), e);
        }
    }

    public Object getDefault(String key) {
        Object value = defaults.get(key);
        if (value != null) return value;

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

        if (Objects.equals(getDefault(key), value)) {
            if (!config.containsKey(key)) {
                return false;
            }
            config.remove(key);
        } else {
            ConfigType type = registry.getConfigType(key);
            config.put(key, type.clone(value));
        }

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
