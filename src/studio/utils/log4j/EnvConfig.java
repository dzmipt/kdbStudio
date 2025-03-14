package studio.utils.log4j;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

import java.nio.file.Path;
import java.nio.file.Paths;

@Plugin(name="studiobase", category = StrLookup.CATEGORY)
public class EnvConfig implements StrLookup {

    private final static String environment = System.getProperty("env");
    private final static Path homeFolder = Paths.get(getValue("KDBSTUDIO_CONFIG_HOME", System.getProperty("user.home") + "/.studioforkdb"));

    private static Path baseFolder = null;

    public static String getEnvironment() {
        return environment;
    }

    public static Path getPluginFolder() {
        return Paths.get(System.getProperty("user.dir")).resolve("plugins");
    }

    //Useful method which could be used in test environment to point to a pre-configured or empty location
    public static void setBaseFolder(Path baseFolder) {
        EnvConfig.baseFolder = baseFolder;
    }

    public static Path getBaseFolder(String env) {
        if (baseFolder != null) return baseFolder;

        return env == null ? homeFolder : homeFolder.resolve(env);
    }

    public static Path getFilepath(String env, String filename) {
        return getBaseFolder(env).resolve(filename);
    }

    public static Path getFilepath(String filename) {
        return getFilepath(environment, filename);
    }

    private static String getValue(String key, String defaultValue) {
        String value = System.getProperty(key, System.getenv(key));
        return value == null ? defaultValue : value;
    }

    @Override
    public String lookup(String key) {
        return getFilepath(key).toString();
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }
}
