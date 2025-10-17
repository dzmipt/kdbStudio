package studio.ui.settings;

import studio.kdb.config.ColorToken;
import studio.kdb.config.ColorTokenConfig;
import studio.ui.ColorLabel;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorTokenEditor extends JPanel {

    private final JLabel[] lblColorTokens;
    private final ColorLabel[] colorLabels;
    private final Map<ColorToken, Color> colorMap = new HashMap<>();

    private final List<ChangeListener> listeners = new ArrayList<>();

    public ColorTokenEditor(ColorTokenConfig colorTokenConfig) {
        int count = ColorToken.values().length;
        int colsCount = 6;
        int lines = count / colsCount;
        if (lines * colsCount < count) lines++;

        GroupLayoutSimple.Stack[] stacks = new GroupLayoutSimple.Stack[colsCount];
        for (int cols = 0; cols<colsCount; cols++) {
            stacks[cols] = new GroupLayoutSimple.Stack();
        }

        lblColorTokens = new JLabel[count];
        colorLabels = new ColorLabel[count];
        for (int i = 0; i<count; i++) {
            final ColorToken token = ColorToken.values()[i];
            Color color = colorTokenConfig.getColor(token);
            colorMap.put(token, color);

            lblColorTokens[i] = new JLabel(token.getDescription());
            colorLabels[i] = new ColorLabel(color);
            colorLabels[i].setSingleClick(true);

            colorLabels[i].addChangeListener(e -> {
                Color newColor = ((ColorLabel)e.getSource()).getColor();
                colorMap.put(token, newColor);
                fireEvent();
            });

            stacks[i / lines].addLineAndGlue(colorLabels[i], lblColorTokens[i]);
        }

        int emptyLines = lines*colsCount - count;
        for (int i=0; i<emptyLines; i++) {
            stacks[colsCount-1].addLineAndGlue();
        }

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);
        layout.setStacks(stacks);
    }

    public ColorTokenConfig getColorTokenConfig() {
        return new ColorTokenConfig(colorMap);
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private void fireEvent() {
        ChangeEvent event = new ChangeEvent(this);
        listeners.forEach(l -> l.stateChanged(event));
    }

}
