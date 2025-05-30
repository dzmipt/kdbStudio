package studio.kdb.config;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ColorSetsTest {

    @Test
    public void testDefault() {
        ColorSets colorSets = ColorSets.DEFAULT;
        assertEquals(colorSets.getDefaultName(), "Default");
        assertEquals(1, colorSets.getNames().size());
        assertTrue(colorSets.getColorSchema("Default").getColors().size() > 0);
        assertNull(colorSets.getColorSchema("something"));
    }

    @Test
    public void testAdd() {
        ColorSets colorSets = ColorSets.DEFAULT.setColorSchema("newOne", ColorSchema.DEFAULT.newColors(List.of(Color.BLACK, Color.RED)));
        assertEquals(colorSets.getDefaultName(), "Default");
        assertEquals(2, colorSets.getNames().size());
        assertEquals(List.of(Color.BLACK, Color.RED), colorSets.getColorSchema("newOne").getColors());

        colorSets = colorSets.setColorSchema("newOne", ColorSchema.DEFAULT.newColors(List.of(Color.BLUE)));
        assertEquals(colorSets.getDefaultName(), "Default");
        assertEquals(2, colorSets.getNames().size());
        assertEquals(List.of(Color.BLUE), colorSets.getColorSchema("newOne").getColors());
    }

    @Test
    public void testSelectNew() {
        ColorSets colorSets = ColorSets.DEFAULT.setColorSchema("newOne", ColorSchema.DEFAULT.newColors(List.of(Color.BLACK, Color.RED)));
        colorSets = colorSets.newSelected("newOne");
        assertEquals(colorSets.getDefaultName(), "newOne");

        colorSets = colorSets.setColorSchema("newOne", ColorSchema.DEFAULT.newColors(List.of(Color.BLUE)));
        assertEquals(colorSets.getDefaultName(), "newOne");
        assertEquals(2, colorSets.getNames().size());
        assertEquals(List.of(Color.BLUE), colorSets.getColorSchema("newOne").getColors());
    }

    @Test
    public void testDelete() {
        ColorSets colorSets = ColorSets.DEFAULT.setColorSchema("newOne", ColorSchema.DEFAULT.newColors(List.of(Color.BLACK, Color.RED)));
        colorSets = colorSets.deleteColorSchema("newOne");
        assertEquals(colorSets.getDefaultName(), "Default");
        assertEquals(1, colorSets.getNames().size());

        colorSets = ColorSets.DEFAULT.setColorSchema("newOne", ColorSchema.DEFAULT.newColors(List.of(Color.BLACK, Color.RED)));
        colorSets = colorSets.deleteColorSchema("Default");
        assertEquals(colorSets.getDefaultName(), "newOne");
        assertEquals(1, colorSets.getNames().size());

        colorSets = ColorSets.DEFAULT.setColorSchema("newOne", ColorSchema.DEFAULT.newColors(List.of(Color.BLACK, Color.RED)));
        colorSets = colorSets.deleteColorSchema("something");
        assertEquals(colorSets.getDefaultName(), "Default");
        assertEquals(2, colorSets.getNames().size());
    }
}
