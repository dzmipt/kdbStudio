package studio.kdb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.FileChooserConfig;
import studio.utils.FilesBackup;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class StudioConfig {

    private final ConfigTypeRegistry registry;
    private final Path path;
    private final Map<String, Object> config;
    private final Map<String, Object> defaults;
    private final Gson gson;

    private static final Logger log = LogManager.getLogger();

    public StudioConfig(ConfigTypeRegistry registry, Path path) {
        this(registry, path, null);
    }

    public StudioConfig(ConfigTypeRegistry registry, Path path, Path pathToDefaults) {
        this.registry = registry;
        this.path = path;
        config = load(path);
        defaults = load(pathToDefaults);

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    private Map<String, Object> load(Path path) {
        Map<String, Object> map = new TreeMap<>();
        if (path == null) return map;
        if (! Files.exists(path)) return map;

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

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
            log.error("Error on parsing json {}", path, e);
        }

        log.info("Loaded config {} with {} settings", path, map.size());
        return map;
    }

    // @TODO: FIX me
    public void saveToDisk() {

    }

    private void save() {
        try (Writer writer = new OutputStreamWriter(FilesBackup.getInstance().newFileOutputStream(path))) {

            JsonObject json = new JsonObject();
            String comment = String.format("Auto-generated at %s from a process with pid %d", Instant.now(), ProcessHandle.current().pid());
            json.add(Config.COMMENT, ConfigType.STRING.toJson(comment));

            for (String key: config.keySet()) {
                ConfigType type = registry.getConfigType(key);

                json.add(key, type.toJson(config.get(key)));
            }

            gson.toJson(json, writer);
        } catch (IOException e) {
            log.error("Error on saving json {}", path, e);
        }
    }

    private Object getDefault(String key) {
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

    public List<String> getStringArray(String key) {
        return (List<String>) get(key);

    }

    public boolean setStringArray(String key, List<String> value) {
        return set(key, value);
    }

    public List<Integer> getIntArray(String key) {
        return (List<Integer>) get(key);

    }

    public boolean setIntArray(String key, List<Integer> value) {
        return set(key, value);
    }

    public <T extends Enum<T>> List<T> getEnumArray(String key) {
        return (List<T>) get(key);

    }

    public <T extends Enum<T>> boolean setEnumArray(String key, List<T> value) {
        return set(key, value);
    }

}
