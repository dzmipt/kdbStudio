package studio.kdb.config;

import com.formdev.flatlaf.FlatLaf;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * All style schemas available to the user: predefined (built-in and from the plugin folder; read-only) and user-defined,
 * together with the schema chosen for every Look and Feel. Immutable.
 */
public class StyleSchemas {

    private final static Logger log = LogManager.getLogger();

    public final static String LIGHT = "Light";
    public final static String DARK = "Dark";

    public final static String BUILT_IN_RESOURCE = "styleSchemas.json";

    private static Map<String, StyleSchema> builtIn = null;
    private final static Map<String, Boolean> darkLookAndFeels = new ConcurrentHashMap<>();

    private final Map<String, StyleSchema> predefined;
    private final Map<String, StyleSchema> user;
    private final Map<String, String> lookAndFeelSchemas;

    public StyleSchemas(Map<String, StyleSchema> pluginSchemas, Map<String, StyleSchema> userSchemas,
                        Map<String, String> lookAndFeelSchemas) {
        predefined = new LinkedHashMap<>(getBuiltIn());
        for (String name: pluginSchemas.keySet()) {
            if (predefined.containsKey(name)) {
                log.warn("Style schema {} from the plugin folder is ignored, as the name is reserved", name);
                continue;
            }
            predefined.put(name, pluginSchemas.get(name));
        }

        user = new LinkedHashMap<>();
        for (String name: userSchemas.keySet()) {
            if (predefined.containsKey(name)) continue;
            user.put(name, userSchemas.get(name));
        }

        this.lookAndFeelSchemas = new TreeMap<>(lookAndFeelSchemas);
    }

    private StyleSchemas(StyleSchemas schemas) {
        this.predefined = schemas.predefined;
        this.user = new LinkedHashMap<>(schemas.user);
        this.lookAndFeelSchemas = new TreeMap<>(schemas.lookAndFeelSchemas);
    }

    public static synchronized Map<String, StyleSchema> getBuiltIn() {
        if (builtIn == null) {
            builtIn = Collections.unmodifiableMap(loadBuiltIn());
        }
        return builtIn;
    }

    private static Map<String, StyleSchema> loadBuiltIn() {
        Map<String, StyleSchema> map = new LinkedHashMap<>();
        try (InputStream inputStream = StyleSchemas.class.getClassLoader().getResourceAsStream(BUILT_IN_RESOURCE)) {
            if (inputStream == null) throw new IllegalStateException("Resource is not found: " + BUILT_IN_RESOURCE);
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (String name : json.keySet()) {
                    // built-in schemas have to define all values
                    map.put(name, StyleSchemaDefinition.fromJson(json.getAsJsonObject(name)).resolve(null));
                }
            }
        } catch (Exception e) {
            log.error("Error loading built-in style schemas", e);
        }

        if (! map.containsKey(LIGHT)) {
            map.put(LIGHT, new StyleSchema(null, ColorMap.DEFAULT_EDITOR_COLORS, TokenStyleMap.DEFAULT, GridColorConfig.DEFAULT));
        }
        if (! map.containsKey(DARK)) {
            map.put(DARK, map.get(LIGHT));
        }
        return map;
    }

    /**
     * The schema used to fill values missing in the definition of a schema based on the given one.
     * If basedOn is missing or unknown, it is Dark for a dark Look and Feel and Light otherwise.
     */
    public static StyleSchema getFallback(String basedOn, String lookAndFeelClassName) {
        StyleSchema schema = basedOn == null ? null : getBuiltIn().get(basedOn);
        if (schema == null) schema = getBuiltIn().get(getDefaultSchemaName(lookAndFeelClassName));
        return schema;
    }

    /**
     * Builds schemas from their definitions. Values missing in a definition are taken from the schema in its basedOn,
     * or (if basedOn is missing) from the default schema for the given Look and Feel.
     */
    public static Map<String, StyleSchema> resolve(Map<String, StyleSchemaDefinition> definitions, String lookAndFeelClassName) {
        Map<String, StyleSchema> map = new LinkedHashMap<>();
        for (String name: definitions.keySet()) {
            StyleSchemaDefinition definition = definitions.get(name);
            map.put(name, definition.resolve(getFallback(definition.getBasedOn(), lookAndFeelClassName)));
        }
        return Collections.unmodifiableMap(map);
    }

    public static Map<String, StyleSchemaDefinition> toDefinitions(Map<String, StyleSchema> schemas) {
        Map<String, StyleSchemaDefinition> map = new LinkedHashMap<>();
        for (String name: schemas.keySet()) {
            map.put(name, StyleSchemaDefinition.of(schemas.get(name)));
        }
        return Collections.unmodifiableMap(map);
    }

    public static boolean isDarkLookAndFeel(String className) {
        if (className == null) return false;
        return darkLookAndFeels.computeIfAbsent(className, name -> {
            try {
                Object lf = Class.forName(name).getDeclaredConstructor().newInstance();
                return (lf instanceof FlatLaf) && ((FlatLaf) lf).isDark();
            } catch (Throwable e) {
                log.debug("Can't check if Look and Feel {} is dark", name, e);
                return false;
            }
        });
    }

    public static String getDefaultSchemaName(String lookAndFeelClassName) {
        return isDarkLookAndFeel(lookAndFeelClassName) ? DARK : LIGHT;
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>(predefined.keySet());
        names.addAll(user.keySet());
        return names;
    }

    public boolean contains(String name) {
        return predefined.containsKey(name) || user.containsKey(name);
    }

    public boolean isPredefined(String name) {
        return predefined.containsKey(name);
    }

    public StyleSchema get(String name) {
        StyleSchema schema = predefined.get(name);
        if (schema != null) return schema;
        return user.get(name);
    }

    public Map<String, StyleSchema> getUserSchemas() {
        return Collections.unmodifiableMap(user);
    }

    public Map<String, String> getLookAndFeelSchemas() {
        return Collections.unmodifiableMap(lookAndFeelSchemas);
    }

    /**
     * The schema name used for the given Look and Feel: the remembered one if it still exists,
     * otherwise Dark for dark Look and Feels and Light for the others.
     */
    public String getSchemaName(String lookAndFeelClassName) {
        String name = lookAndFeelSchemas.get(lookAndFeelClassName);
        if (name != null && contains(name)) return name;
        return getDefaultSchemaName(lookAndFeelClassName);
    }

    public StyleSchema getSchema(String lookAndFeelClassName) {
        return get(getSchemaName(lookAndFeelClassName));
    }

    public StyleSchemas select(String lookAndFeelClassName, String name) {
        if (!contains(name)) throw new IllegalArgumentException("Unknown style schema: " + name);
        StyleSchemas schemas = new StyleSchemas(this);
        schemas.lookAndFeelSchemas.put(lookAndFeelClassName, name);
        return schemas;
    }

    public StyleSchemas set(String name, StyleSchema schema) {
        if (isPredefined(name)) throw new IllegalArgumentException("Predefined style schema can't be changed: " + name);
        StyleSchemas schemas = new StyleSchemas(this);
        schemas.user.put(name, schema);
        return schemas;
    }

    /**
     * Creates a new user schema as a copy of the given one. The copy is based on the same built-in schema as the
     * original; a copy of a built-in schema is based on it. If that is not known (a schema from the plugin folder
     * without basedOn), the copy is based on the default schema for the given Look and Feel.
     */
    public StyleSchemas duplicate(String name, String newName, String lookAndFeelClassName) {
        StyleSchema schema = get(name);
        if (schema == null) throw new IllegalArgumentException("Unknown style schema: " + name);
        if (contains(newName)) throw new IllegalArgumentException("Style schema already exists: " + newName);

        String basedOn = schema.getBasedOn();
        if (basedOn == null || !getBuiltIn().containsKey(basedOn)) {
            basedOn = getBuiltIn().containsKey(name) ? name : getDefaultSchemaName(lookAndFeelClassName);
        }
        return set(newName, schema.newBasedOn(basedOn));
    }

    public StyleSchemas rename(String name, String newName) {
        if (isPredefined(name)) throw new IllegalArgumentException("Predefined style schema can't be renamed: " + name);
        if (!user.containsKey(name)) throw new IllegalArgumentException("Unknown style schema: " + name);
        if (contains(newName)) throw new IllegalArgumentException("Style schema already exists: " + newName);

        StyleSchemas schemas = new StyleSchemas(this);
        schemas.user.clear();
        for (String key: user.keySet()) {
            schemas.user.put(key.equals(name) ? newName : key, user.get(key));
        }
        schemas.lookAndFeelSchemas.replaceAll((lf, schemaName) -> schemaName.equals(name) ? newName : schemaName);
        return schemas;
    }

    public StyleSchemas delete(String name) {
        if (isPredefined(name)) throw new IllegalArgumentException("Predefined style schema can't be deleted: " + name);
        StyleSchemas schemas = new StyleSchemas(this);
        schemas.user.remove(name);
        schemas.lookAndFeelSchemas.values().removeIf(schemaName -> schemaName.equals(name));
        return schemas;
    }

    /**
     * Returns an error message if the name can't be used for a new schema, otherwise null.
     */
    public String validateNewName(String name) {
        if (name == null || name.isBlank()) return "The name can't be empty";
        if (isPredefined(name)) return "The name is used by a predefined schema";
        if (contains(name)) return "A schema with this name already exists";
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StyleSchemas)) return false;
        StyleSchemas that = (StyleSchemas) o;
        return predefined.equals(that.predefined) && user.equals(that.user) &&
                lookAndFeelSchemas.equals(that.lookAndFeelSchemas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predefined, user, lookAndFeelSchemas);
    }
}
