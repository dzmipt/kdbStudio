package studio.kdb.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
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
        return new StyleSchemas(Map.of(), Map.of(), Map.of());
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
                StyleSchema schema = StyleSchema.fromJson(json.getAsJsonObject(name), null);
                assertNull(schema.getBasedOn(), name);
                assertFalse(schema.getGridColors().isDefaultConfig(), name + " should have explicit grid colors");
                assertEquals(schema, StyleSchemas.getBuiltIn().get(name));
            }
        }
    }

    @Test
    public void testDefaultSchemaForLookAndFeel() {
        StyleSchemas schemas = newSchemas();
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(LIGHT_LAF));
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(NIMBUS_LAF));
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName("not.existing.Laf"));
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(DARK_LAF));
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(DARCULA_LAF));
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
        StyleSchemas schemas = newSchemas().duplicate(StyleSchemas.DARK, "My dark");
        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK, "My dark"), schemas.getNames());
        assertFalse(schemas.isPredefined("My dark"));
        StyleSchema schema = schemas.get("My dark");
        assertEquals(StyleSchemas.DARK, schema.getBasedOn());
        assertEquals(schemas.get(StyleSchemas.DARK).getEditorColors(), schema.getEditorColors());

        // basedOn is kept when a user schema is duplicated
        schemas = schemas.duplicate("My dark", "Another");
        assertEquals(StyleSchemas.DARK, schemas.get("Another").getBasedOn());

        StyleSchemas finalSchemas = schemas;
        assertThrows(IllegalArgumentException.class, () -> finalSchemas.duplicate(StyleSchemas.LIGHT, "Another"));
    }

    @Test
    public void testRenameUpdatesLookAndFeels() {
        StyleSchemas schemas = newSchemas()
                .duplicate(StyleSchemas.LIGHT, "A")
                .select(LIGHT_LAF, "A")
                .select(NIMBUS_LAF, "A")
                .select(DARK_LAF, StyleSchemas.DARK);

        schemas = schemas.rename("A", "B");
        assertFalse(schemas.contains("A"));
        assertTrue(schemas.contains("B"));
        assertEquals("B", schemas.getSchemaName(LIGHT_LAF));
        assertEquals("B", schemas.getSchemaName(NIMBUS_LAF));
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(DARK_LAF));
    }

    @Test
    public void testDeleteFallsBackToDefault() {
        StyleSchemas schemas = newSchemas()
                .duplicate(StyleSchemas.LIGHT, "A")
                .select(DARK_LAF, "A")
                .select(LIGHT_LAF, "A");

        schemas = schemas.delete("A");
        assertFalse(schemas.contains("A"));
        assertEquals(StyleSchemas.DARK, schemas.getSchemaName(DARK_LAF));
        assertEquals(StyleSchemas.LIGHT, schemas.getSchemaName(LIGHT_LAF));
        assertTrue(schemas.getLookAndFeelSchemas().isEmpty());
    }

    @Test
    public void testValidateNewName() {
        StyleSchemas schemas = newSchemas().duplicate(StyleSchemas.LIGHT, "A");
        assertNotNull(schemas.validateNewName(""));
        assertNotNull(schemas.validateNewName("  "));
        assertNotNull(schemas.validateNewName(null));
        assertNotNull(schemas.validateNewName(StyleSchemas.LIGHT));
        assertNotNull(schemas.validateNewName("A"));
        assertNull(schemas.validateNewName("B"));
    }

    @Test
    public void testPluginSchemasArePredefined() {
        StyleSchema corp = light().newBasedOn(StyleSchemas.LIGHT);
        StyleSchemas schemas = new StyleSchemas(
                Map.of("Corp", corp, StyleSchemas.LIGHT, corp),
                Map.of("Corp", corp, "Mine", corp),
                Map.of());

        assertEquals(List.of(StyleSchemas.LIGHT, StyleSchemas.DARK, "Corp", "Mine"), schemas.getNames());
        assertTrue(schemas.isPredefined("Corp"));
        assertFalse(schemas.isPredefined("Mine"));
        // the plugin can't override built-in schemas
        assertEquals(light(), schemas.get(StyleSchemas.LIGHT));
        assertEquals(Map.of("Mine", corp), schemas.getUserSchemas());
    }

    @Test
    public void testMissingValuesAreTakenFromBasedOn() {
        JsonObject json = JsonParser.parseString("{" +
                "'Partial': {'basedOn': 'Dark', 'editorColors': {'background': '010203'}, 'tokens': {'keyword': '040506:Italic'}}," +
                "'NoBase': {'tokens': {'keyword': '040506'}}" +
                "}").getAsJsonObject();

        Map<String, StyleSchema> map = StyleSchemas.fromJson(json);
        StyleSchema dark = StyleSchemas.getBuiltIn().get(StyleSchemas.DARK);

        StyleSchema partial = map.get("Partial");
        assertEquals(new Color(0x010203), partial.getBackgroundColor());
        assertEquals(dark.getEditorColors().get(EditorColorToken.SELECTED), partial.getEditorColors().get(EditorColorToken.SELECTED));
        assertEquals(new TokenStyle(new Color(0x040506), FontStyle.Italic), partial.getTokens().get(ColorToken.KEYWORD));
        assertEquals(dark.getTokens().get(ColorToken.SYMBOL), partial.getTokens().get(ColorToken.SYMBOL));
        assertEquals(dark.getGridColors(), partial.getGridColors());

        StyleSchema noBase = map.get("NoBase");
        assertEquals(light().getEditorColors(), noBase.getEditorColors());
        assertEquals(light().getGridColors(), noBase.getGridColors());
    }

    @Test
    public void testJsonRoundTrip() {
        StyleSchema schema = light().newBasedOn(StyleSchemas.DARK);
        assertEquals(schema, StyleSchema.fromJson(schema.toJson(), null));

        // empty gridColors means colors derived from Look and Feel
        StyleSchema auto = schema.newGridColors(GridColorConfig.DEFAULT);
        JsonObject json = auto.toJson();
        assertTrue(json.getAsJsonObject(StyleSchema.GRID_COLORS).isEmpty());
        assertTrue(StyleSchema.fromJson(json, null).getGridColors().isDefaultConfig());
    }
}
