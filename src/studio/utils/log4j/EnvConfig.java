package studio.utils.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

@Plugin(name="studiobase", category = StrLookup.CATEGORY)
public class EnvConfig implements StrLookup {

    private final static String environment = System.getProperty("env");
    private final static String homeFolder = getValue("KDBSTUDIO_CONFIG_HOME", System.getProperty("user.home") + "/.studioforkdb");

    public static String getEnvironment() {
        return environment;
    }

    public static String getBaseFolder(String env) {
        return env == null ? homeFolder : homeFolder + "/" + env;
    }

    public static String getFilepath(String env, String filename) {
        return getBaseFolder(env) + "/" + filename;
    }

    public static String getFilepath(String filename) {
        return getFilepath(environment, filename);
    }

    private static String getValue(String key, String defaultValue) {
        String value = System.getProperty(key, System.getenv(key));
        return value == null ? defaultValue : value;
    }

    @Override
    public String lookup(String key) {
        return getFilepath(key);
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }
}
