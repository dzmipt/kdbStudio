package studio.ui.settings;

import studio.kdb.config.ColorMap;
import studio.kdb.config.ColorToken;
import studio.kdb.config.EditorColorToken;
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

public class EditorColorEditor extends JPanel {

    private final Map<ColorToken, ColorLabel> tokenColorLabels = new HashMap<>();
    private final Map<EditorColorToken, ColorLabel> editorColorLabels = new HashMap<>();
    private ColorMap editorColors, tokenColors;
    private boolean muteEvents = true;

    private final List<ChangeListener> listeners = new ArrayList<>();

    private final static int COLS_COUNT = 6;

    public EditorColorEditor(ColorMap editorColorConfig, ColorMap colorTokenConfig) {
        editorColors = new ColorMap(editorColorConfig);
        tokenColors = new ColorMap(colorTokenConfig);

        GroupLayoutSimple.Stack[] stacks = new GroupLayoutSimple.Stack[COLS_COUNT];
        for (int cols = 0; cols<COLS_COUNT; cols++) {
            stacks[cols] = new GroupLayoutSimple.Stack();
        }

        for (int index=0; index< EditorColorToken.values().length; index++) {
            EditorColorToken token = EditorColorToken.values()[index];
            JLabel label = new JLabel(token.getDescription());
            ColorLabel colorLabel = new ColorLabel();
            colorLabel.setSingleClick(true);
            editorColorLabels.put(token, colorLabel);
            colorLabel.addChangeListener(e -> {
                Color newColor = ((ColorLabel)e.getSource()).getColor();
                editorColors.put(token, newColor);
                fireEvent();
            });

            stacks[index].addLineAndGlue(colorLabel, label);
        }


        int count = ColorToken.values().length ;
        int index = 0;

        for (int col = 0; col< COLS_COUNT; col++) {
            int remainCols = COLS_COUNT - col;
            int lines = count / remainCols;
            if (lines * remainCols < count) lines++;

            if (stacks[col].size() == 0) {
                stacks[col].addLineAndGlue();
            }
            for (int row = 0; row<lines; row++) {
                final ColorToken token = ColorToken.values()[index++];
                JLabel lblToken = new JLabel(token.getDescription());
                ColorLabel colorLabel = new ColorLabel();
                colorLabel.setSingleClick(true);
                tokenColorLabels.put(token, colorLabel);

                colorLabel.addChangeListener(e -> {
                    Color newColor = ((ColorLabel)e.getSource()).getColor();
                    tokenColors.put(token, newColor);
                    fireEvent();
                });

                stacks[col].addLineAndGlue(colorLabel, lblToken);

            }
            count -= lines;
        }

        int size = stacks[0].size();
        for (int col=1; col<COLS_COUNT; col++) {
            for (int row = stacks[col].size(); row<size; row++) {
                stacks[col].addLineAndGlue();
            }
        }

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setBaseline(false);
        layout.setStacks(stacks);

        set(editorColorConfig, colorTokenConfig);
    }

    public void set(ColorMap editorColorConfig, ColorMap colorTokenConfig) {
        muteEvents = true;

        editorColors = new ColorMap(editorColorConfig);
        for (EditorColorToken token: EditorColorToken.values() ) {
            editorColorLabels.get(token).setColor(editorColorConfig.get(token));
        }

        tokenColors = new ColorMap(colorTokenConfig);
        for (ColorToken token: ColorToken.values()) {
            tokenColorLabels.get(token).setColor(colorTokenConfig.get(token));
        }

        muteEvents = false;
        fireEvent();
    }

    public ColorMap getColorTokenConfig() {
        return tokenColors;
    }

    public ColorMap getEditorColorConfig() {
        return editorColors;
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
