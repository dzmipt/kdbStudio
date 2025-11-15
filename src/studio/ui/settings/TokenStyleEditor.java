package studio.ui.settings;

import studio.kdb.config.*;
import studio.ui.ColorLabel;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenStyleEditor extends JPanel {

    private final Map<ColorToken, ColorLabel> tokenColorLabels = new HashMap<>();
    private final Map<ColorToken, JLabel> mapTokenTitle = new HashMap<>();
    private final Map<EditorColorToken, ColorLabel> editorColorLabels = new HashMap<>();
    private ColorMap editorColors;
    private TokenStyleMap tokenStyleMap;
    private boolean muteEvents = true;

    private final List<ChangeListener> listeners = new ArrayList<>();

    private final static int COLS_COUNT = 6;
    private final Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    public TokenStyleEditor(ColorMap editorColorConfig, TokenStyleMap tokenStyleMap) {
        editorColors = new ColorMap(editorColorConfig);
        this.tokenStyleMap = new TokenStyleMap(tokenStyleMap);

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
                lblToken.setCursor(handCursor);
                lblToken.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        JPopupMenu popupMenu = new JPopupMenu();
                        popupMenu.add(getStylePopupMenuItem(token, FontStyle.Bold));
                        popupMenu.add(getStylePopupMenuItem(token, FontStyle.Italic));
                        popupMenu.add(getStylePopupMenuItem(token, FontStyle.Underline));

                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                });
                mapTokenTitle.put(token, lblToken);
                ColorLabel colorLabel = new ColorLabel();
                colorLabel.setSingleClick(true);
                tokenColorLabels.put(token, colorLabel);

                colorLabel.addChangeListener(e -> {
                    Color newColor = ((ColorLabel)e.getSource()).getColor();
                    this.tokenStyleMap.set(token, newColor);
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

        set(editorColorConfig, tokenStyleMap);
    }

    private JCheckBoxMenuItem getStylePopupMenuItem(ColorToken token, final FontStyle style) {
        FontStyle tokenStyle = tokenStyleMap.get(token).getStyle();
        JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(style.name());

        boolean selected = false;
        if (style == FontStyle.Bold && tokenStyle.isBold()) selected = true;
        if (style == FontStyle.Italic && tokenStyle.isItalic()) selected = true;
        if (style == FontStyle.Underline && tokenStyle.isUnderline()) selected = true;
        menuItem.setState(selected);

        menuItem.addChangeListener(e -> {
            boolean newState = menuItem.isSelected();

            FontStyle newTokenStyle = tokenStyleMap.get(token).getStyle();
            if (style == FontStyle.Bold) newTokenStyle = newTokenStyle.setBold(newState);
            else if (style == FontStyle.Italic) newTokenStyle = newTokenStyle.setItalic(newState);
            else if (style == FontStyle.Underline) newTokenStyle = newTokenStyle.setUnderline(newState);

            tokenStyleMap.set(token, newTokenStyle);
            refreshTokenStyle(token);

            fireEvent();
        });

        return menuItem;
    }

    public void set(ColorMap editorColorConfig, TokenStyleMap tokenStyleMap) {
        muteEvents = true;

        editorColors = new ColorMap(editorColorConfig);
        for (EditorColorToken token: EditorColorToken.values() ) {
            editorColorLabels.get(token).setColor(editorColorConfig.get(token));
        }

        this.tokenStyleMap = new TokenStyleMap(tokenStyleMap);
        for (ColorToken token: ColorToken.values()) {
            refreshTokenStyle(token);
        }

        muteEvents = false;
        fireEvent();
    }

    private void refreshTokenStyle(ColorToken token) {
        TokenStyle tokenStyle = tokenStyleMap.get(token);
        Font defaultFont = new JLabel().getFont();
        tokenColorLabels.get(token).setColor(tokenStyle.getColor());
        mapTokenTitle.get(token).setFont(tokenStyle.getStyle().applyStyle(defaultFont));
    }

    public TokenStyleMap getTokenStyleMap() {
        return tokenStyleMap;
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
