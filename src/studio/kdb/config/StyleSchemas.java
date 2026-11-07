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
 * All style schemas: predefined (built-in and from the plugin folder; read-only) and user-defined. Immutable.
 * <p>
 * Schemas are kept as they are defined in the config. A schema is resolved (values missing in its definition are
 * taken from its basedOn schema, or from Light/Dark depending on the Look and Feel) when it is requested;
 * the result is cached.
 */
public class StyleSchemas {

    private final static Logger log = LogManager.getLogger();

    public final static String LIGHT = "Light";
    public final static String DARK = "Dark";

    public final static String BUILT_IN_RESOURCE = "styleSchemas.json";

    private static Map<String, StyleSchema> builtIn = null;
    private static StyleSchemas builtInSchemas = null;
    private final static Map<String, Boolean> darkLookAndFeels = new ConcurrentHashMap<>();

    // predefined schemas go first
    private final Map<String, StyleSchemaDefinition> definitions;
    private final Set<String> predefined;
    private final Map<String, StyleSchema> resolved = new ConcurrentHashMap<>();

    private StyleSchemas(Map<String, StyleSchemaDefinition> definitions, Set<String> predefined) {
        this.definitions = Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        this.predefined = Collections.unmodifiableSet(new LinkedHashSet<>(predefined));
    }

    /**
     * Only built-in schemas
     */
    public static synchronized StyleSchemas builtIn() {
        if (builtInSchemas == null) {
            Map<String, StyleSchemaDefinition> definitions = new LinkedHashMap<>();
            getBuiltIn().forEach( (name, schema) -> definitions.put(name, StyleSchemaDefinition.of(schema)) );
            builtInSchemas = new StyleSchemas(definitions, definitions.keySet());
        }
        return builtInSchemas;
    }

    /**
     * @param definitions schemas as they are defined in the config. Built-in schemas are added if missing.
     * @param predefined names of predefined schemas (from the plugin folder). Built-in schemas are always predefined.
     */
    public static StyleSchemas of(Map<String, StyleSchemaDefinition> definitions, Collection<String> predefined) {
        StyleSchemas builtInSchemas = builtIn();
        Map<String, StyleSchemaDefinition> map = new LinkedHashMap<>(builtInSchemas.definitions);
        Set<String> predefinedNames = new LinkedHashSet<>(builtInSchemas.predefined);

        Map<String, StyleSchemaDefinition> userDefinitions = new LinkedHashMap<>();
        for (String name: definitions.keySet()) {
            if (builtInSchemas.contains(name)) {
                log.warn("Style schema {} is ignored, as the name is used by a built-in schema", name);
            } else if (predefined.contains(name)) {
                map.put(name, definitions.get(name));
                predefinedNames.add(name);
            } else {
                userDefinitions.put(name, definitions.get(name));
            }
        }
        // predefined schemas go first
        map.putAll(userDefinitions);
        return new StyleSchemas(map, predefinedNames);
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
        return new ArrayList<>(definitions.keySet());
    }

    public boolean contains(String name) {
        return definitions.containsKey(name);
    }

    public boolean isPredefined(String name) {
        return predefined.contains(name);
    }

    public Map<String, StyleSchemaDefinition> getDefinitions() {
        return definitions;
    }

    /**
     * Definitions of user schemas, which are saved in the user's config
     */
    public Map<String, StyleSchemaDefinition> getUserDefinitions() {
        Map<String, StyleSchemaDefinition> map = new LinkedHashMap<>();
        definitions.forEach( (name, definition) -> {
            if (! predefined.contains(name)) map.put(name, definition);
        });
        return map;
    }

    /**
     * The schema with values missing in its definition taken from its basedOn schema.
     * If basedOn is not defined, they are taken from Dark for a dark Look and Feel and from Light otherwise.
     * @return null if there is no schema with the name
     */
    public StyleSchema get(String name, String lookAndFeelClassName) {
        StyleSchemaDefinition definition = definitions.get(name);
        if (definition == null) return null;

        String fallbackName = definition.getBasedOn();
        if (fallbackName == null || ! getBuiltIn().containsKey(fallbackName)) {
            fallbackName = getDefaultSchemaName(lookAndFeelClassName);
        }
        String fallback = fallbackName;
        return resolved.computeIfAbsent(name + "\n" + fallback,
                key -> definition.resolve(getBuiltIn().get(fallback)));
    }

    /**
     * The schema name used for the given Look and Feel: the remembered one if it still exists,
     * otherwise Dark for dark Look and Feels and Light for the others.
     */
    public String getSchemaName(Map<String, String> lookAndFeelSchemas, String lookAndFeelClassName) {
        String name = lookAndFeelSchemas.get(lookAndFeelClassName);
        if (name != null && contains(name)) return name;
        return getDefaultSchemaName(lookAndFeelClassName);
    }

    public StyleSchemas set(String name, StyleSchema schema) {
        if (isPredefined(name)) throw new IllegalArgumentException("Predefined style schema can't be changed: " + name);
        Map<String, StyleSchemaDefinition> map = new LinkedHashMap<>(definitions);
        map.put(name, StyleSchemaDefinition.of(schema));
        return new StyleSchemas(map, predefined);
    }

    /**
     * Creates a new user schema as a copy of the given one. The copy is based on the same built-in schema as the
     * original; a copy of a built-in schema is based on it. If that is not known (a schema from the plugin folder
     * without basedOn), the copy is based on the default schema for the given Look and Feel.
     */
    public StyleSchemas duplicate(String name, String newName, String lookAndFeelClassName) {
        StyleSchema schema = get(name, lookAndFeelClassName);
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
        if (!contains(name)) throw new IllegalArgumentException("Unknown style schema: " + name);
        if (contains(newName)) throw new IllegalArgumentException("Style schema already exists: " + newName);

        Map<String, StyleSchemaDefinition> map = new LinkedHashMap<>();
        definitions.forEach( (key, definition) -> map.put(key.equals(name) ? newName : key, definition) );
        return new StyleSchemas(map, predefined);
    }

    public StyleSchemas delete(String name) {
        if (isPredefined(name)) throw new IllegalArgumentException("Predefined style schema can't be deleted: " + name);
        Map<String, StyleSchemaDefinition> map = new LinkedHashMap<>(definitions);
        map.remove(name);
        return new StyleSchemas(map, predefined);
    }

    /**
     * The mapping with references to the renamed schema updated
     */
    public static Map<String, String> renameInLookAndFeelSchemas(Map<String, String> lookAndFeelSchemas, String name, String newName) {
        Map<String, String> map = new TreeMap<>(lookAndFeelSchemas);
        map.replaceAll( (lf, schemaName) -> schemaName.equals(name) ? newName : schemaName);
        return Collections.unmodifiableMap(map);
    }

    /**
     * The mapping without references to the deleted schema
     */
    public static Map<String, String> deleteFromLookAndFeelSchemas(Map<String, String> lookAndFeelSchemas, String name) {
        Map<String, String> map = new TreeMap<>(lookAndFeelSchemas);
        map.values().removeIf(schemaName -> schemaName.equals(name));
        return Collections.unmodifiableMap(map);
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
        return definitions.equals(that.definitions) && predefined.equals(that.predefined);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitions, predefined);
    }
}
