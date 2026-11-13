package studio.kdb.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StyleSchemasTest {

    private final static String LIGHT_LAF = "com.formdev.flatlaf.FlatLightLaf";
    private final static String DARK_LAF = "com.formdev.flatlaf.FlatDarkLaf";
    private final static String DARCULA_LAF = "com.formdev.flatlaf.FlatDarculaLaf";
    private final static String NIMBUS_LAF = "javax.swing.plaf.nimbus.NimbusLookAndFeel";

    private StyleSchema light() {
        return StyleSchemas.getBuiltIn().get(StyleSchemas.LIGHT);
    }

    private StyleSchemas newSchemas() {
        return StyleSchemas.builtIn();
    }

    private static StyleSchemaDefinition def(StyleSchema schema) {
        return StyleSchemaDefinition.of(schema);
    }

    // a map keeping the order of the keys
    private static <V> Map<String, V> map(Object... keyValues) {
        Map<String, V> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], (V) keyValues[i + 1]);
        }
        return map;
    }

    @Test
    public void testBuiltInResourceIsComplete() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(StyleSchemas.BUILT_IN_RESOURCE);
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue(json.has(StyleSchemas.LIGHT));
            assertTrue(json.has(StyleSchemas.DARK));

            for (String name: json.keySet()) {
                // without a fallback, parsing fails if any value is missing
                StyleSchema schema = StyleSchemaDefinition.fromJson(json.getAsJsonObject(name)).resolve(null);
                assertNull(schema.getBasedOn(), name);
                assertFalse(schema.getGridColors().isDefaultConfig(), name + " should have explicit grid colors");
                assertEquals(schema, StyleSchemas.getBuiltIn().get(name));
            }
        }
    }

    @Test
    public void testDefaultSchemaForLookAndFeel() {
        StyleSchemas schemas = newSchemas();
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(Map.of(), LIGHT_LAF));
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(Map.of(), NIMBUS_LAF));
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(Map.of(), "not.existing.Laf"));
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(Map.of(), DARK_LAF));
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(Map.of(), DARCULA_LAF));
    }

    @Test
    public void testRememberedSchemaForLookAndFeel() {
        StyleSchemas schemas = newSchemas().duplicate(StyleSchemas.LIGHT, "A", LIGHT_LAF);
        assertEquals("A", schemas.getSchemaName(Map.of(DARK_LAF, "A"), DARK_LAF));
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(Map.of(DARK_LAF, StyleSchemas.LIGHT), DARK_LAF));
        // the remembered schema doesn't exist
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(Map.of(DARK_LAF, "B"), DARK_LAF));
    }

    @Test
    public void testPredefinedAreReadOnly() {
        StyleSchemas schemas = newSchemas();
        assertTrue(schemas.isPredefined(StyleSchemas.LIGHT));
        assertTrue(schemas.isPredefined(StyleSchemas.DARK));
        assertThrows(IllegalArgumentException.class, () -> schemas.set(StyleSchemas.LIGHT, light()));
        assertThrows(IllegalArgumentException.class, () -> schemas.rename(StyleSchemas.DARK, "aaa"));
        assertThrows(IllegalArgumentException.class, () -> schemas.delete(StyleSchemas.DARK));
    }

    @Test
    public void testDuplicate() {
        StyleSchemas schemas = newSchemas().duplicate(StyleSchemas.DARK, "My dark", LIGHT_LAF);
        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK, "My dark"), schemas.getNames());
        assertFalse(schemas.isPredefined("My dark"));
        StyleSchema schema = schemas.get("My dark", LIGHT_LAF);
        assertEquals(StyleSchemas.DARK, schema.getBasedOn());
        assertEquals(schemas.get(StyleSchemas.DARK, LIGHT_LAF).getEditorColors(), schema.getEditorColors());

        // basedOn is kept when a user schema is duplicated
        schemas = schemas.duplicate("My dark", "Another", LIGHT_LAF);
        assertEquals(StyleSchemas.DARK, schemas.get("Another", LIGHT_LAF).getBasedOn());

        StyleSchemas finalSchemas = schemas;
        assertThrows(IllegalArgumentException.class, () -> finalSchemas.duplicate(StyleSchemas.LIGHT, "Another", LIGHT_LAF));
    }

    @Test
    public void testRename() {
        StyleSchemas schemas = newSchemas()
                .duplicate(StyleSchemas.LIGHT, "A", LIGHT_LAF)
                .duplicate(StyleSchemas.LIGHT, "C", LIGHT_LAF);

        schemas = schemas.rename("A", "B");
        // the order is kept
        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK, "B", "C"), schemas.getNames());

        Map<String, String> lookAndFeelSchemas = Map.of(LIGHT_LAF, "A", NIMBUS_LAF, "A", DARK_LAF, StyleSchemas.DARK);
        assertEquals(Map.of(LIGHT_LAF, "B", NIMBUS_LAF, "B", DARK_LAF, StyleSchemas.DARK),
                StyleSchemas.renameInLookAndFeelSchemas(lookAndFeelSchemas, "A", "B"));
    }

    @Test
    public void testDeleteFallsBackToDefault() {
        StyleSchemas schemas = newSchemas().duplicate(StyleSchemas.LIGHT, "A", LIGHT_LAF);
        Map<String, String> lookAndFeelSchemas = Map.of(DARK_LAF, "A", LIGHT_LAF, "A");

        schemas = schemas.delete("A");
        assertFalse(schemas.contains("A"));
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(lookAndFeelSchemas, DARK_LAF));
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(lookAndFeelSchemas, LIGHT_LAF));
        assertTrue(StyleSchemas.deleteFromLookAndFeelSchemas(lookAndFeelSchemas, "A").isEmpty());
    }

    @Test
    public void testValidateNewName() {
        StyleSchemas schemas = newSchemas().duplicate(StyleSchemas.LIGHT, "A", LIGHT_LAF);
        assertNotNull(schemas.validateNewName(""));
        assertNotNull(schemas.validateNewName("  "));
        assertNotNull(schemas.validateNewName(null));
        assertNotNull(schemas.validateNewName(StyleSchemas.LIGHT));
        assertNotNull(schemas.validateNewName("A"));
        assertNull(schemas.validateNewName("B"));
    }

    @Test
    public void testPredefinedSchemas() {
        StyleSchemaDefinition corp = def(light().newBasedOn(StyleSchemas.LIGHT));
        StyleSchemas schemas = StyleSchemas.of(
                map("Mine", corp, "Corp", corp, StyleSchemas.LIGHT, corp),
                List.of("Corp"));

        // predefined schemas go first
        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK, "Corp", "Mine"), schemas.getNames());
        assertTrue(schemas.isPredefined("Corp"));
        assertFalse(schemas.isPredefined("Mine"));
        // built-in schemas can't be overridden
        assertEquals(light(), schemas.get(StyleSchemas.LIGHT, LIGHT_LAF));
        assertEquals(Map.of("Mine", corp), schemas.getUserDefinitions());
    }

    @Test
    public void testConfigTypeKeepsPredefinedFromDefaults() {
        JsonObject jsonDefaults = JsonParser.parseString("{'Corp': {'basedOn': 'Dark'}}").getAsJsonObject();
        StyleSchemas defaults = (StyleSchemas) ConfigType.STYLE_SCHEMAS.fromJson(jsonDefaults, StyleSchemas.builtIn());

        JsonObject json = JsonParser.parseString("{'Corp': {'basedOn': 'Dark'}, 'Mine': {'basedOn': 'Light'}}").getAsJsonObject();
        StyleSchemas schemas = (StyleSchemas) ConfigType.STYLE_SCHEMAS.fromJson(json, defaults);

        assertTrue(schemas.isPredefined("Corp"));
        assertFalse(schemas.isPredefined("Mine"));
        // only user schemas are saved
        assertEquals(JsonParser.parseString("{'Mine': {'basedOn': 'Light'}}"), ConfigType.STYLE_SCHEMAS.toJson(schemas));
    }

    @Test
    public void testMissingValuesAreTakenFromBasedOn() {
        JsonObject json = JsonParser.parseString("{" +
                "'Partial': {'basedOn': 'Dark', 'editorColors': {'background': '010203'}, 'tokens': {'keyword': '040506:Italic'}}," +
                "'NoBase': {'tokens': {'keyword': '040506'}}" +
                "}").getAsJsonObject();

        StyleSchemas schemas = (StyleSchemas) ConfigType.STYLE_SCHEMAS.fromJson(json, null);
        StyleSchema dark = StyleSchemas.getBuiltIn().get(StyleSchemas.DARK);

        StyleSchema partial = schemas.get("Partial", LIGHT_LAF);
        assertEquals(new Color(0x010203), partial.getBackgroundColor());
        assertEquals(dark.getEditorColors().get(EditorColorToken.SELECTED), partial.getEditorColors().get(EditorColorToken.SELECTED));
        assertEquals(new TokenStyle(new Color(0x040506), FontStyle.Italic), partial.getTokens().get(ColorToken.KEYWORD));
        assertEquals(dark.getTokens().get(ColorToken.SYMBOL), partial.getTokens().get(ColorToken.SYMBOL));
        assertEquals(dark.getGridColors(), partial.getGridColors());

        // without basedOn, missing values depend on the Look and Feel
        StyleSchema noBase = schemas.get("NoBase", LIGHT_LAF);
        assertEquals(light().getEditorColors(), noBase.getEditorColors());
        assertEquals(light().getGridColors(), noBase.getGridColors());

        noBase = schemas.get("NoBase", DARK_LAF);
        assertEquals(dark.getEditorColors(), noBase.getEditorColors());
        assertEquals(dark.getGridColors(), noBase.getGridColors());
        assertEquals(new TokenStyle(new Color(0x040506), FontStyle.Plain), noBase.getTokens().get(ColorToken.KEYWORD));

        // resolved schemas are cached
        assertSame(noBase, schemas.get("NoBase", DARK_LAF));
    }

    @Test
    public void testDuplicateWithoutBasedOn() {
        StyleSchemaDefinition corp = def(light()); // basedOn is null
        StyleSchemas schemas = StyleSchemas.of(Map.of("Corp", corp), List.of("Corp"));

        assertEquals(StyleSchemas.LIGHT, schemas.duplicate("Corp", "A", LIGHT_LAF).get("A", LIGHT_LAF).getBasedOn());
        assertEquals(StyleSchemas.LIGHT, schemas.duplicate("Corp", "A", NIMBUS_LAF).get("A", LIGHT_LAF).getBasedOn());
        assertEquals(StyleSchemas.DARK, schemas.duplicate("Corp", "A", DARK_LAF).get("A", LIGHT_LAF).getBasedOn());
        // a copy of a built-in schema is based on it, whatever the Look and Feel
        assertEquals(StyleSchemas.LIGHT, schemas.duplicate(StyleSchemas.LIGHT, "A", DARK_LAF).get("A", LIGHT_LAF).getBasedOn());
    }

    @Test
    public void testJsonRoundTrip() {
        StyleSchema schema = light().newBasedOn(StyleSchemas.DARK);
        StyleSchemaDefinition definition = StyleSchemaDefinition.of(schema);
        assertEquals(definition, StyleSchemaDefinition.fromJson(definition.toJson()));
        assertEquals(schema, StyleSchemaDefinition.fromJson(definition.toJson()).resolve(null));

        // empty gridColors means colors derived from Look and Feel
        StyleSchema auto = schema.newGridColors(GridColorConfig.DEFAULT);
        JsonObject json = StyleSchemaDefinition.of(auto).toJson();
        assertTrue(json.getAsJsonObject("gridColors").isEmpty());
        assertTrue(StyleSchemaDefinition.fromJson(json).resolve(null).getGridColors().isDefaultConfig());
    }

    @Test
    public void testPartialDefinitionKeepsOnlyDefinedValues() {
        JsonObject json = JsonParser.parseString("{'basedOn': 'Dark', 'tokens': {'keyword': '040506:Italic'}, " +
                "'gridColors': {'background': {'odd': '010101'}}}").getAsJsonObject();
        StyleSchemaDefinition definition = StyleSchemaDefinition.fromJson(json);
        assertEquals(json, definition.toJson());

        StyleSchema dark = StyleSchemas.getBuiltIn().get(StyleSchemas.DARK);
        StyleSchema schema = definition.resolve(dark);
        assertEquals(new Color(0x010101), schema.getGridColors().getColor(GridColorToken.ODD, false));
        assertEquals(dark.getGridColors().getColor(GridColorToken.EVEN, false), schema.getGridColors().getColor(GridColorToken.EVEN, false));
        assertThrows(IllegalArgumentException.class, () -> definition.resolve(null));
    }
}
