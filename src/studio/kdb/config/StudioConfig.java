package studio.kdb.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class StudioConfig {

    private static final Map<String,Object> configDefaultValues = new HashMap<>();
    private static final Map<String,ConfigType> configTypes = new HashMap<>();

    private final Path path;
    private final Map<String, Object> config;
    private final Map<String, Object> defaults;
    private final Gson gson;

    private static final Logger log = LogManager.getLogger();

    private static final String COMMENT = configDefault("comment", ConfigType.STRING, "");

    public StudioConfig(Path path) {
        this(path, null);
    }

    public StudioConfig(Path path, Path pathToDefaults) {
        this.path = path;
        config = load(path);
        defaults = load(pathToDefaults);

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }


    public static String configDefault(String key, ConfigType type, Object defaultValue) {
        if (configDefaultValues.containsKey(key)) throw new IllegalArgumentException(String.format("Key %s is already available", key));

        configDefaultValues.put(key, defaultValue);
        configTypes.put(key, type);
        return key;
    }

    private Map<String, Object> load(Path path) {
        Map<String, Object> map = new TreeMap<>();
        if (path == null) return map;

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            for (String key: json.keySet()) {
                try {
                    ConfigType type = configTypes.get(key);
                    if (type == null) {
                        log.warn("Unknown config key {}. Skipping...", key);
                        continue;
                    }
                    Object value = type.fromJson(json.get(key), getDefault(key));
                    if (key.equals(COMMENT)) {
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

    private void save() {
        try (Writer writer = new OutputStreamWriter(FilesBackup.getInstance().newFileOutputStream(path))) {

            JsonObject json = new JsonObject();
            String comment = String.format("Auto-generated at %s from a process with pid %d", Instant.now(), ProcessHandle.current().pid());
            json.add(COMMENT, ConfigType.STRING.toJson(comment));

            for (String key: config.keySet()) {
                ConfigType type = configTypes.get(key);

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

        return configDefaultValues.get(key);
    }

    public Object get(String key, ConfigType type) {
        if (! configTypes.containsKey(key)) throw new IllegalArgumentException("Unknown key: " + key);
        if (type != configTypes.get(key)) throw new IllegalArgumentException(String.format("Unexpected type. %s != %s", type, configTypes.get(key)));

        Object value = config.get(key);
        if (value != null) return value;

        return getDefault(key);
    }

    public boolean set(String key, ConfigType type, Object value) {
        Object currentValue = get(key, type);

        if (Objects.equals(currentValue, value)) return false;

        if (Objects.equals(getDefault(key), value)) {
            if (!config.containsKey(key)) {
                return false;
            }
            config.remove(key);
        }

        config.put(key, value);
        save();
        return true;
    }

    public String getString(String key) {
        return (String) get(key, ConfigType.STRING);
    }

    public boolean setString(String key, String value) {
        return set(key, ConfigType.STRING, value);
    }

    public int getInt(String key) {
        return (Integer) get(key, ConfigType.INT);

    }

    public boolean setInt(String key, int value) {
        return set(key, ConfigType.INT, value);
    }

    public double getDouble(String key) {
        return (Double) get(key, ConfigType.DOUBLE);

    }

    public boolean setDouble(String key, double value) {
        return set(key, ConfigType.DOUBLE, value);
    }

    public boolean getBoolean(String key) {
        return (Boolean) get(key, ConfigType.BOOLEAN);

    }

    public boolean setDouble(String key, boolean value) {
        return set(key, ConfigType.BOOLEAN, value);
    }

    public Font getFont(String key) {
        return (Font) get(key, ConfigType.FONT);

    }

    public boolean setFont(String key, Font value) {
        return set(key, ConfigType.FONT, value);
    }

    public Rectangle getBounds(String key) {
        return (Rectangle) get(key, ConfigType.BOUNDS);

    }

    public boolean setBounds(String key, Rectangle value) {
        return set(key, ConfigType.BOUNDS, value);
    }

    public Color getColor(String key) {
        return (Color) get(key, ConfigType.COLOR);

    }

    public boolean setColor(String key, Color value) {
        return set(key, ConfigType.COLOR, value);
    }

    public <T extends Enum<T>> T getEnum(String key) {
        return (T) get(key, ConfigType.ENUM);

    }

    public <T extends Enum<T>> boolean setEnum(String key, T value) {
        return set(key, ConfigType.ENUM, value);
    }

    public Dimension getSize(String key) {
        return (Dimension) get(key, ConfigType.SIZE);

    }

    public boolean setSize(String key, Dimension value) {
        return set(key, ConfigType.SIZE, value);
    }

    public FileChooserConfig getFileChooserConfig(String key) {
        return (FileChooserConfig) get(key, ConfigType.FILE_CHOOSER);

    }

    public boolean setFileChooserConfig(String key, FileChooserConfig value) {
        return set(key, ConfigType.FILE_CHOOSER, value);
    }

    public String[] getStringArray(String key) {
        return (String[]) get(key, ConfigType.STRING_ARRAY);

    }

    public boolean setStringArray(String key, String[] value) {
        return set(key, ConfigType.STRING_ARRAY, value);
    }

    public int[] getIntArray(String key) {
        return (int[]) get(key, ConfigType.INT_ARRAY);

    }

    public boolean setIntArray(String key, int[] value) {
        return set(key, ConfigType.INT_ARRAY, value);
    }

    public <T extends Enum<T>> T[] getEnumArray(String key) {
        return (T[]) get(key, ConfigType.ENUM_ARRAY);

    }

    public <T extends Enum<T>> boolean setEnumArray(String key, T[] value) {
        return set(key, ConfigType.ENUM_ARRAY, value);
    }

}
