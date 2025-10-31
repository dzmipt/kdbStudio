package studio.ui.settings;

import studio.kdb.config.GridColorConfig;
import studio.kdb.config.GridColorToken;
import studio.ui.ColorLabel;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GridColorsEditor extends JPanel {

    private final ColorLabel nullLabel;
    private final Map<GridColorToken, ColorLabel> fgLabels = new HashMap<>();
    private final Map<GridColorToken, ColorLabel> bgLabels = new HashMap<>();

    private GridColorConfig config;

    public GridColorsEditor(GridColorConfig config) {
        this.config = config;

        nullLabel = getLabel(GridColorToken.NULL, true);

        GridColorToken[] tokens = GridColorToken.BG;

        JComponent[] fgComponents = new JComponent[tokens.length * 2];
        JComponent[] bgComponents = new JComponent[tokens.length * 2];
        for (int i=0; i < tokens.length; i++) {
            GridColorToken token = tokens[i];
            ColorLabel label = getLabel(token, true);
            fgLabels.put(token, label);
            fgComponents[2*i] = label;
            fgComponents[2*i+1] = new JLabel(token.getDescription());

            label = getLabel(token, false);
            bgLabels.put(token, label);
            bgComponents[2*i] = label;
            bgComponents[2*i+1] = new JLabel(token.getDescription());
        }

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(new JLabel("Foreground colors: "), nullLabel, new JLabel("null"))
                        .addLine(fgComponents)
                        .addLine(new JLabel("Background colors:"))
                        .addLine(bgComponents)

        );
    }

    public void set(GridColorConfig config) {
        this.config = config;
        nullLabel.setColor(config.getColor(GridColorToken.NULL, true));
        for (GridColorToken token: GridColorToken.BG) {
            ColorLabel label = fgLabels.get(token);
            label.setColor(config.getColor(token, true));

            label = bgLabels.get(token);
            label.setColor(config.getColor(token, false));
        }
    }

    private ColorLabel getLabel(GridColorToken token, boolean isForeground) {
        Color color = config.getColor(token, isForeground);
        ColorLabel label = new ColorLabel(color);
        label.setSingleClick(true);
        label.addChangeListener(e -> {
            if (config.isUnmodifiable()) {
                config = new GridColorConfig(config);
            }
            Color newColor = ((ColorLabel)e.getSource()).getColor();
            config.setColor(newColor, token, isForeground);
        });

        return label;
    }

    public GridColorConfig getConfig() {
        return config;
    }
}
