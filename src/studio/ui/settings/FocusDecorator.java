package studio.ui.settings;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public class FocusDecorator implements HierarchyListener, FocusListener {
    private final JComponent component;
    private Border borderNotInFocus = null;
    private Border borderInFocus = null;

    public static void add(JComponent component) {
        new FocusDecorator(component);
    }

    public FocusDecorator(JComponent component) {
        this.component = component;
        component.addHierarchyListener(this);
        component.addFocusListener(this);
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        borderNotInFocus = BorderFactory.createLineBorder(component.getParent().getBackground(), 4);
        borderInFocus = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 181, 241)),
                BorderFactory.createLineBorder(component.getParent().getBackground(), 3)
        );
        component.setBorder(borderNotInFocus);
    }

    @Override
    public void focusGained(FocusEvent e) {
        if (borderInFocus != null) component.setBorder(borderInFocus);
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (borderNotInFocus != null) component.setBorder(borderNotInFocus);
    }
}

