package studio.ui;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StudioIcon extends ImageIcon {

    private final ImageIcon lightIcon, darkIcon;
    private ImageIcon icon;

    private final static Map<String, StudioIcon> icons = new ConcurrentHashMap<>();

    public static void updateUI() {
        for(StudioIcon icon: icons.values()) {
            icon.updateIcon();
        }
    }

    public static StudioIcon getIcon(String name) {
        return icons.computeIfAbsent(name, StudioIcon::new);
    }

    public static Set<String> getAllNames() {
        return icons.keySet();
    }

    private StudioIcon(String name) {
        String filename = name + ".png";

        lightIcon = getImage(filename);
        darkIcon = getImage("dark/" + filename);
        if (lightIcon == null) throw new IllegalArgumentException("Icon " + name + " not found");

        updateIcon();
    }

    private ImageIcon getImage(String strFilename) {
        URL url = Util.class.getClassLoader().getResource(strFilename);
        if (url == null) return null;

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = toolkit.getImage(url);
        return new ImageIcon(image);
    }

    private void updateIcon() {
        icon = darkIcon == null ? lightIcon :
                                    FlatLaf.isLafDark() ? darkIcon : lightIcon;
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
