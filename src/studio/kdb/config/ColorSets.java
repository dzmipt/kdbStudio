package studio.kdb.config;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import java.awt.*;
import java.util.List;
import java.util.*;

public class ColorSets {
    private final String selected;
    private final Map<String, List<Color>> map;

    private final static String DEFAULT_NAME = "Default";
    public final static ColorSets DEFAULT = new ColorSets(DEFAULT_NAME, getDefaultMap());

    private static Map<String, List<Color>> getDefaultMap() {
        Paint[] paints = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE;
        List<Color> colors = new ArrayList<>(paints.length);
        for (Paint p: paints) {
            if (p instanceof Color) colors.add( (Color)p);
        }
        String name = "Default";
        Map<String, List<Color>> map = new HashMap<>();
        map.put(name, colors);
        return map;
    }

    public ColorSets(String selected, Map<String, List<Color>> map) {
        this.map = new LinkedHashMap<>();
        for (String name: map.keySet()) {
            List<Color> colors = new ArrayList<>(map.get(name));
            if (!colors.isEmpty())
                this.map.put(name, colors);
        }
        if (this.map.isEmpty()) {
            this.map.putAll(getDefaultMap());
        }

        if (this.map.containsKey(selected)) {
            this.selected = selected;
        } else {
            this.selected = this.map.keySet().iterator().next();
        }

    }

    public ColorSets newSelected(String newSelected) {
        return new ColorSets(newSelected, map);
    }

    public ColorSets setColorSet(String name, List<Color> colors) {
        ColorSets colorSets = new ColorSets(selected, map);
        colorSets.map.put(name, colors);
        return colorSets;
    }

    public ColorSets deleteColorSet(String name) {
        Iterator<String> iterator = map.keySet().iterator();
        String newSelected = iterator.next();
        if (newSelected.equals(name)) {
            newSelected = iterator.next();
        }
        ColorSets colorSets = new ColorSets(newSelected, map);
        colorSets.map.remove(name);
        return colorSets;
    }


    public String getDefaultName() {
        return selected;
    }

    public List<Color> getColors(String name) {
        List<Color> colors = map.get(name);
        if (colors == null) return null;
        return Collections.unmodifiableList(colors);
    }

    public Set<String> getNames() {
        return map.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColorSets)) return false;
        ColorSets colorSets = (ColorSets) o;
        return Objects.equals(selected, colorSets.selected) && Objects.equals(map, colorSets.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selected, map);
    }
}
