package studio.kdb.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigTypeRegistry {

    private final Map<String,Object> configDefaultValues = new HashMap<>();
    private final Map<String,ConfigType> configTypes = new HashMap<>();

    public String add(String key, ConfigType type, Object defaultValue) {
        if (configDefaultValues.containsKey(key)) throw new IllegalArgumentException(String.format("Key %s is already available", key));

        configDefaultValues.put(key, defaultValue);
        configTypes.put(key, type);
        return key;
    }

    public ConfigType getConfigType(String key) {
        return configTypes.get(key);
    }

    public Object getDefault(String key) {
        return configDefaultValues.get(key);
    }

    public Set<String> keySet() {
        return configTypes.keySet();
    }
}
