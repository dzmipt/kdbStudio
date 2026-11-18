package studio.ui;

import javax.swing.*;
import java.awt.*;

// A highlighted notice: an icon and a text on an accent-tinted background with an accent border
public class NoticeBanner extends JLabel {

    public NoticeBanner(Icon icon) {
        super(icon);
        setOpaque(true);
        setIconTextGap(8);
        updateColors();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateColors();
    }

    private void updateColors() {
        Color accent = UIManager.getColor("Component.warning.focusedBorderColor");
        if (accent == null) accent = new Color(0xe2a53a);
        Color background = UIManager.getColor("Panel.background");
        if (background == null) background = getBackground();
        if (background == null) background = Color.WHITE;

        setBackground(Util.blendColors(accent, background, background));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }
}
