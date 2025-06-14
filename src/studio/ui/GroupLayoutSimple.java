package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupLayoutSimple extends GroupLayout {

    private final Set<Component> maxWidthComponents = new HashSet<>();
    private int padding = -1;
    private boolean baseline = true;

    public GroupLayoutSimple(Container container, Component ... maxWidthComponents) {
        super(container);
        addMaxWidthComponents(maxWidthComponents);
        container.setLayout(this);
        setAutoCreateGaps(true);
        setAutoCreateContainerGaps(true);
    }

    public GroupLayoutSimple(Container container) {
        this(container, new Component[0]);
    }

    public void addMaxWidthComponents(Component ... maxWidthComponents) {
        this.maxWidthComponents.addAll(List.of(maxWidthComponents));
    }

    public void setPadding(int padding) {
        this.padding = padding;
        setAutoCreateGaps(false);
        setAutoCreateContainerGaps(false);
    }

    public void setBaseline(boolean baseline) {
        this.baseline = baseline;
    }

    public void setStacks(Stack... stacks) {

        int lineCount = stacks[0].lines.size();
        for (int i = 1; i<stacks.length; i++) {
            if (lineCount != stacks[i].lines.size()) {
                throw new IllegalArgumentException("Number of lines in every stack should be the same");
            }
        }

        SequentialGroup horizontalGroup = createSequentialGroup();
        for (Stack stack: stacks) {
            ParallelGroup stackGroup = createParallelGroup(Alignment.LEADING);
            for (Line line: stack.lines) {
                SequentialGroup lineGroup = createSequentialGroup();
                for (int i=0; i<line.components.length; i++) {
                    Component component = line.components[i];
                    if (maxWidthComponents.contains(component)) {
                        lineGroup.addComponent(component);
                    } else {
                        lineGroup.addComponent(component, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
                    }
                    if (padding >-1 && i<line.components.length-1) lineGroup.addGap(padding);
                }
                if (line.glue != null) {
                    lineGroup.addComponent(line.glue);
                }
                stackGroup.addGroup(lineGroup);
            }
            horizontalGroup.addGroup(stackGroup);
        }
        setHorizontalGroup(horizontalGroup);

        SequentialGroup verticalGroup = createSequentialGroup();
        for (int lineIndex = 0; lineIndex<lineCount; lineIndex++) {
            ParallelGroup lineGroup = createParallelGroup(baseline ? Alignment.BASELINE : Alignment.CENTER);
            for (Stack stack: stacks) {
                Line line = stack.lines.get(lineIndex);
                for (int i=0; i<line.components.length; i++) {
                    Component component = line.components[i];
                    lineGroup.addComponent(component, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
                    if (padding >-1 && i<line.components.length-1) lineGroup.addGap(padding);
                }
                if (line.glue != null) {
                    lineGroup.addComponent(line.glue, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
                }
            }
            verticalGroup.addGroup(lineGroup);
        }
        setVerticalGroup(verticalGroup);
    }

    public static class Stack {
        List<Line> lines = new ArrayList<>();
        public Stack addLine(Component... line) {
            lines.add(new Line(line, false));
            return this;
        }
        public Stack addLineAndGlue(Component... line) {
            lines.add(new Line(line, true));
            return this;
        }
    }

    private static class Line {
        Component[] components;
        Component glue = null;
        Line(Component[] components, boolean hasGlue) {
            this.components = components;
            if (hasGlue) glue = Box.createGlue();
        }
    }
}
