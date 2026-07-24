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
    private final Border componentBorder;
    private Border borderNotInFocus = null;
    private Border borderInFocus = null;

    public static void add(JComponent component) {
        new FocusDecorator(component);
    }

    public FocusDecorator(JComponent component) {
        this.component = component;
        componentBorder = component.getBorder();
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
        if (componentBorder != null) {
            borderInFocus = BorderFactory.createCompoundBorder( borderInFocus, componentBorder );
            borderNotInFocus = BorderFactory.createCompoundBorder( borderNotInFocus, componentBorder );
        }


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

