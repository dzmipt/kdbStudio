package studio.ui.settings;

import studio.kdb.config.GridColorConfig;
import studio.kdb.config.GridColorToken;
import studio.ui.ColorLabel;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import java.awt.*;

public class GridColorsEditor extends JPanel {

    private GridColorConfig config;

    public GridColorsEditor(GridColorConfig config) {
        this.config = config;

        GridColorToken[] tokens = GridColorToken.BG;

        JComponent[] fgComponents = new JComponent[tokens.length * 2];
        JComponent[] bgComponents = new JComponent[tokens.length * 2];
        for (int i=0; i < tokens.length; i++) {
            GridColorToken token = tokens[i];
            fgComponents[2*i] = getLabel(token, true);
            fgComponents[2*i+1] = new JLabel(token.getDescription());
            bgComponents[2*i] = getLabel(token, false);
            bgComponents[2*i+1] = new JLabel(token.getDescription());
        }

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(new JLabel("Foreground colors: "), getLabel(GridColorToken.NULL, true), new JLabel("null"))
                        .addLine(fgComponents)
                        .addLine(new JLabel("Background colors:"))
                        .addLine(bgComponents)

        );
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
