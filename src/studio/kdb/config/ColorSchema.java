package studio.kdb.config;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ColorSchema {

    private final Color background;
    private final Color grid;
    private final List<Color> colors;
    public final static ColorSchema DEFAULT = new ColorSchema(Color.LIGHT_GRAY, Color.WHITE, defaultColors());

    private static List<Color> defaultColors() {
        Paint[] paints = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE;
        List<Color> colors = new ArrayList<>(paints.length);
        for (Paint p: paints) {
            if (p instanceof Color) colors.add( (Color)p);
        }
        return colors;
    }

    public ColorSchema() {
        this(DEFAULT.getBackground(), DEFAULT.getGrid(), List.of(Color.BLACK));
    }

    public ColorSchema(Color background, Color grid, List<Color> colors) {
        this.background = background;
        this.grid = grid;
        this.colors = Collections.unmodifiableList(new ArrayList<>(colors));
    }

    public Color getBackground() {
        return background;
    }

    public Color getGrid() {
        return grid;
    }

    public List<Color> getColors() {
        return colors;
    }

    public ColorSchema newBackgroundColor(Color background) {
        return new ColorSchema(background, this.grid, this.colors);
    }

    public ColorSchema newGridColor(Color grid) {
        return new ColorSchema(this.background, grid, this.colors);
    }

    public ColorSchema newColors(List<Color> colors) {
        return new ColorSchema(this.background, this.grid, colors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColorSchema)) return false;
        ColorSchema that = (ColorSchema) o;
        return Objects.equals(background, that.background) && Objects.equals(grid, that.grid) && Objects.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(background, grid, colors);
    }
}
