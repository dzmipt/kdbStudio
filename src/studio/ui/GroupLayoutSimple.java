package studio.ui;

import studio.core.Studio;

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
    private Dimension preferredSize = null;

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

    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        if (preferredSize != null) return preferredSize;
        return super.preferredLayoutSize(parent);
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

        for (int i=0; i<lineCount; i++) {
            boolean parallel = false;
            for (Stack stack: stacks) {
                parallel |= stack.lines.get(i) == null;
            }
            for (Stack stack: stacks) {
                Line line = stack.lines.get(i);
                if (line != null) line.parallel = parallel;
            }
        }


        SequentialGroup normalHorizontalGroup = createSequentialGroup();
        for (Stack stack: stacks) {
            ParallelGroup stackGroup = createParallelGroup(Alignment.LEADING);
            for (Line line: stack.lines) {
                if (line == null || line.parallel) continue;
                SequentialGroup lineGroup = line.addToLineGroup(this, createSequentialGroup(), stack.maxWidth);
                stackGroup.addGroup(lineGroup);
            }
            normalHorizontalGroup.addGroup(stackGroup);
        }

        ParallelGroup horizontalGroup = createParallelGroup().addGroup(normalHorizontalGroup);
        for (int i=0; i<lineCount; i++) {
            SequentialGroup lineGroup = createSequentialGroup();
            for (Stack stack: stacks) {
                Line line = stack.lines.get(i);
                if (line == null) continue;
                if (! line.parallel) continue;

                line.addToLineGroup(this, lineGroup, stack.maxWidth);
            }
            horizontalGroup.addGroup(lineGroup);
        }
        setHorizontalGroup(horizontalGroup);


        SequentialGroup verticalGroup = createSequentialGroup();
        for (int lineIndex = 0; lineIndex<lineCount; lineIndex++) {
            ParallelGroup lineGroup = createParallelGroup(baseline ? Alignment.BASELINE : Alignment.CENTER);
            for (Stack stack: stacks) {
                Line line = stack.lines.get(lineIndex);
                if (line == null) continue;
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

    public static Stack maxWidthStack() {
        Stack stack = new Stack();
        stack.maxWidth = true;
        return stack;
    }

    public static class Stack {
        List<Line> lines = new ArrayList<>();
        boolean maxWidth = false;

        public Stack addLine(Component... line) {
            lines.add(new Line(line, false));
            return this;
        }
        public Stack addLineAndGlue(Component... line) {
            lines.add(new Line(line, true));
            return this;
        }
        public Stack skipLine() {
            lines.add(null);
            return this;
        }
        public int size() {
            return lines.size();
        }
    }

    private static class Line {
        boolean parallel = false;
        Component[] components;
        Component glue = null;
        Line(Component[] components, boolean hasGlue) {
            this.components = components;
            if (hasGlue) glue = Box.createGlue();
        }

        SequentialGroup addToLineGroup(GroupLayoutSimple groupLayout, SequentialGroup lineGroup, boolean isMaxWidth) {
            for (int i=0; i<components.length; i++) {
                Component component = components[i];
                if (isMaxWidth || groupLayout.maxWidthComponents.contains(component)) {
                    // Why we don't add here with max=Integer.MAX_VALUE ??
                    lineGroup.addComponent(component);
                } else {
                    lineGroup.addComponent(component, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE);
                }
                if (groupLayout.padding >-1 && i<components.length-1) lineGroup.addGap(groupLayout.padding);
            }
            if (glue != null) {
                lineGroup.addComponent(glue);
            }
            return lineGroup;
        }
    }

    private static JPanel testPanel(String name, Color color) {
        JPanel p = new JPanel();
        p.setBackground(color);
        p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        p.add(new JLabel(name));
        return p;
    }
    public static void main(String... args) {
        Studio.initLF();

        JPanel A = testPanel("A", Color.RED);
        JPanel B = testPanel("B", Color.BLUE);
        JPanel C = testPanel("C", Color.GREEN);
        JPanel D = testPanel("D", Color.YELLOW);
        JPanel E = testPanel("E", Color.CYAN);
        JPanel F = testPanel("F", Color.ORANGE);
        JPanel G = testPanel("G", Color.PINK);

        JPanel root = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(root, A, B, C, D, E, F, G);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(A)
                        .addLine(B)
                        .addLine(C) ,
                new GroupLayoutSimple.Stack()
                        .addLine(D)
                        .skipLine()
                        .addLine(E) ,
//                        .skipLine() ,
        new GroupLayoutSimple.Stack()
                        .addLine(F)
                        .skipLine()
                        .addLine(G)
        );

        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(root);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
