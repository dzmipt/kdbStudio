package studio.ui;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.List;

public class AlteringIcon extends ImageIcon {

    private final ImageIcon lightIcon, darkIcon;
    private ImageIcon icon;

    private final static List<AlteringIcon> icons = new ArrayList<>();

    public static void updateUI() {
        for(AlteringIcon icon: icons) {
            icon.updateIcon();
        }
    }

    public AlteringIcon(String filename) {
        icons.add(this);
        lightIcon = Util.getImage("/" + filename);
        darkIcon = Util.getImage("/dark/" + filename);
        updateIcon();
    }

    private void updateIcon() {
        icon = FlatLaf.isLafDark() ? darkIcon : lightIcon;
    }

    @Override
    public Image getImage() {
        return icon.getImage();
    }

    @Override
    public int getImageLoadStatus() {
        return icon.getImageLoadStatus();
    }

    @Override
    public String getDescription() {
        return icon.getDescription();
    }

    @Override
    public ImageObserver getImageObserver() {
        return icon.getImageObserver();
    }

    @Override
    public void setImageObserver(ImageObserver observer) {
        lightIcon.setImageObserver(observer);
        darkIcon.setImageObserver(observer);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        icon.paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
        return icon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return icon.getIconHeight();
    }
}
