package studio.kdb.config;

import studio.kdb.Config;
import studio.utils.FileConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InstrumentedMockConfig extends Config {

    private ConfigTypeRegistry configTypeRegistry;
    private final HashSet<String> accessedKeys = new HashSet<>();

    public InstrumentedMockConfig(Path path) {
        super(path);
    }

    @Override
    protected StudioConfig createStudioConfig(ConfigTypeRegistry configTypeRegistry, FileConfig fileConfig, FileConfig defaultFileConfig, ConfigConverter configConverter) {
        this.configTypeRegistry = configTypeRegistry;
        return new MockStudioConfig(configTypeRegistry, fileConfig, defaultFileConfig, configConverter);
    }

    public ConfigTypeRegistry getConfigTypeRegistry() {
        return configTypeRegistry;
    }

    public void resetAccessedKeys() {
        accessedKeys.clear();
    }

    public Set<String> getAccessedKeys() {
        return Collections.unmodifiableSet(accessedKeys);
    }

    private class MockStudioConfig extends StudioConfig {
        public MockStudioConfig(ConfigTypeRegistry registry, FileConfig fileConfig, FileConfig defaultFileConfig, JsonConverter converter) {
            super(registry, fileConfig, defaultFileConfig, converter);
        }

        @Override
        public Object get(String key) {
            if (accessedKeys != null) { //indirectly the method called from super constructor when the field is not yet initialized
                accessedKeys.add(key);
            }
            return super.get(key);
        }
    }
}
