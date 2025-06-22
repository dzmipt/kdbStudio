package studio.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

public class BetterCardLayout implements LayoutManager, ContainerListener {

    private final Container parent;
    private int selected = -1;

    public BetterCardLayout(Container parent) {
        this.parent = parent;
        parent.setLayout(this);
        parent.addContainerListener(this);
    }

    private int getIndex(Component component) {
        for (int index = 0; index<parent.getComponentCount(); index++) {
            if (component == parent.getComponent(index)) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        Component child = e.getChild();
        int index = getIndex(child);
        select(index);

        child.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                show(e.getComponent());
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                hide(e.getComponent());
            }
        });
    }

    public void show(Component component) {
        select(getIndex(component));
    }

    public void hide(Component component) {
        int index = getIndex(component);
        if (index == selected) {
            next();
        }
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        if (selected >= parent.getComponentCount()) {
            select(0);
        }
    }

    public void select(int index) {
        if (index == -1) return;
        if (selected == index) return;
        selected = index;
        for (int i = 0; i<parent.getComponentCount(); i++) {
            parent.getComponent(i).setVisible(i == selected);
        }
    }

    public void next() {
        select(selected+1 >= parent.getComponentCount() ? 0 : selected+1);
    }

    public void previous() {
        select(selected <= 0 ? parent.getComponentCount() - 1 : selected-1);
    }

    public int getSelected() {
        return selected;
    }

    public Component getSelectedComponent() {
        if (selected == -1) return null;
        return parent.getComponent(selected);
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {}

    @Override
    public void removeLayoutComponent(Component comp) {}

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int width = 0;
        int height = 0;
        for (Component component: parent.getComponents()) {
            Dimension pref = component.getPreferredSize();
            width = Math.max(width, pref.width);
            height = Math.max(height, pref.height);
        }
        return new Dimension(width, height);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        Dimension size = parent.getSize();
        for (Component component: parent.getComponents()) {
            component.setBounds(0, 0, size.width, size.height);
        }
    }
}
