package studio.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupLayoutSimple extends GroupLayout {

    private final Set<Component> maxWidthComponents;

    public GroupLayoutSimple(Container container, Component ... maxWidthComponents) {
        super(container);
        this.maxWidthComponents = new HashSet<>(List.of(maxWidthComponents));
        container.setLayout(this);
        setAutoCreateGaps(true);
        setAutoCreateContainerGaps(true);
    }

    public GroupLayoutSimple(Container container) {
        this(container, new Component[0]);
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
            ParallelGroup stackGroup = createParallelGroup(GroupLayout.Alignment.LEADING);
            for (Line line: stack.lines) {
                SequentialGroup lineGroup = createSequentialGroup();
                for (Component component: line.components) {
                    if (maxWidthComponents.contains(component)) {
                        lineGroup.addComponent(component);
                    } else {
                        lineGroup.addComponent(component, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
                    }
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
            ParallelGroup lineGroup = createParallelGroup(GroupLayout.Alignment.BASELINE);
            for (Stack stack: stacks) {
                Line line = stack.lines.get(lineIndex);
                for (Component component: line.components) {
                    lineGroup.addComponent(component, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
                }
                if (line.glue != null) {
                    lineGroup.addComponent(line.glue, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
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
