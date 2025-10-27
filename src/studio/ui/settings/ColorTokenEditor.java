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
import java.util.List;

public class ColorTokenEditor extends JPanel {

    private final ColorLabel bgColorLabel;
    private final ColorMap colorMap = new ColorMap();

    private final List<ChangeListener> listeners = new ArrayList<>();

    public ColorTokenEditor(Color bgColor, ColorMap colorTokenConfig) {
        bgColorLabel = new ColorLabel(bgColor);
        bgColorLabel.setSingleClick(true);
        bgColorLabel.addChangeListener(e -> {
            fireEvent();
        });

        int count = ColorToken.values().length;
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
            Color color = colorTokenConfig.get(token);
            colorMap.put(token, color);

            JLabel lblToken = new JLabel(token.getDescription());
            ColorLabel colorLabel = new ColorLabel(color);
            colorLabel.setSingleClick(true);

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
        ChangeEvent event = new ChangeEvent(this);
        listeners.forEach(l -> l.stateChanged(event));
    }

}
