package studio.ui.settings;

import studio.kdb.config.ColorMap;
import studio.kdb.config.ColorToken;
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

    private final ColorLabel bgColorLabel;
    private final Map<ColorToken, ColorLabel> colorLabels = new HashMap<>();
    private ColorMap colorMap;
    private boolean muteEvents = true;

    private final List<ChangeListener> listeners = new ArrayList<>();

    public ColorTokenEditor(Color bgColor, ColorMap colorTokenConfig) {
        bgColorLabel = new ColorLabel();
        bgColorLabel.setSingleClick(true);
        bgColorLabel.addChangeListener(e -> {
            fireEvent();
        });

        int count = ColorToken.values().length + 1; // +1 as we need to include bg color
        int colsCount = 6;
        int lines = count / colsCount;
        if (lines * colsCount < count) lines++;

        GroupLayoutSimple.Stack[] stacks = new GroupLayoutSimple.Stack[colsCount];
        for (int cols = 0; cols<colsCount; cols++) {
            stacks[cols] = new GroupLayoutSimple.Stack();
        }

        stacks[0].addLineAndGlue(bgColorLabel, new JLabel("Background"));

        for (int i = 0; i<count -1; i++) {
            final ColorToken token = ColorToken.values()[i];
            JLabel lblToken = new JLabel(token.getDescription());
            ColorLabel colorLabel = new ColorLabel();
            colorLabel.setSingleClick(true);
            colorLabels.put(token, colorLabel);

            colorLabel.addChangeListener(e -> {
                Color newColor = ((ColorLabel)e.getSource()).getColor();
                colorMap.put(token, newColor);
                fireEvent();
            });

            stacks[(i+1) / lines].addLineAndGlue(colorLabel, lblToken);
        }

        int emptyLines = lines*colsCount - count;
        for (int i=0; i<emptyLines; i++) {
            stacks[colsCount-1].addLineAndGlue();
        }

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);
        layout.setStacks(stacks);

        colorMap = new ColorMap(colorTokenConfig);
        set(bgColor, colorTokenConfig);
    }

    public void set(Color bgColor, ColorMap colorTokenConfig) {
        muteEvents = true;

        bgColorLabel.setColor(bgColor);
        colorMap = new ColorMap(colorTokenConfig);
        for (ColorToken token: ColorToken.values()) {
            colorLabels.get(token).setColor(colorTokenConfig.get(token));
        }

        muteEvents = false;
        fireEvent();
    }

    public ColorMap getColorTokenConfig() {
        return colorMap;
    }

    public Color getBgColor() {
        return bgColorLabel.getColor();
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    private void fireEvent() {
        if (muteEvents) return;
        ChangeEvent event = new ChangeEvent(this);
        listeners.forEach(l -> l.stateChanged(event));
    }

}
