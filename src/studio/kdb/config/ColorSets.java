package studio.kdb.config;

import java.util.*;

public class ColorSets {
    private final String selected;
    private final Map<String, ColorSchema> map;

    private final static String DEFAULT_NAME = "Default";
    public final static ColorSets DEFAULT = new ColorSets(DEFAULT_NAME, defaultMap());

    private static Map<String, ColorSchema> defaultMap() {
        String name = "Default";
        Map<String, ColorSchema> map = new HashMap<>();
        map.put(name, ColorSchema.DEFAULT);
        return map;
    }

    public ColorSets(String selected, Map<String, ColorSchema> map) {
        this.map = new LinkedHashMap<>(map);
        if (this.map.containsKey(selected)) {
            this.selected = selected;
        } else {
            this.selected = this.map.keySet().iterator().next();
        }

    }

    public ColorSets newSelected(String newSelected) {
        return new ColorSets(newSelected, map);
    }

    public ColorSets setColorSchema(String name, ColorSchema colorSchema) {
        ColorSets colorSets = new ColorSets(selected, map);
        colorSets.map.put(name, colorSchema);
        return colorSets;
    }

    public ColorSets deleteColorSchema(String name) {
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

    public ColorSchema getColorSchema(String name) {
        return map.get(name);
    }
    public ColorSchema getColorSchema() {
        return getColorSchema(getDefaultName());
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
