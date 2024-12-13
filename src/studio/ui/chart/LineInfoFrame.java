package studio.ui.chart;

import studio.kdb.KType;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.DoubleConsumer;

public class LineInfoFrame extends JFrame {

    private final Line line;
    private final JLabel lblDX = new JLabel("");
    private final JLabel lblDY = new JLabel("");
    private final JLabel lblX = new JLabel("");
    private final JLabel lblY = new JLabel("");
    private final JTextField txtTitle = new JTextField();
    private final Editor txtDX;
    private final Editor txtDY;
    private final Editor txtX;
    private final Editor txtY;

    private double dx = 1;
    private double dy = Double.NaN;
    private double x = 0;
    private double y = Double.NaN;
    private boolean lockDX = true;
    private boolean lockX = true;

    public LineInfoFrame(Line line, KType xType, KType yType) {
        this.line = line;
        txtDX = Editor.createDurationEditor(xType);
        txtDY = Editor.createDurationEditor(yType);

        txtX = Editor.createEditor(xType);
        txtY = Editor.createEditor(yType);

        line.addChangeListener(e -> refresh());
        initComponents();
    }


    private JComponent createPanelLabel(JLabel label, Runnable action) {
        JPanel panel = new JPanel(new BorderLayout(0,0));
        panel.add(label, BorderLayout.EAST);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                action.run();
                refresh();
            }
        });
        return panel;
    }

    private Editor withAction(Editor editor, DoubleConsumer action) {
        editor.addValueChangedListener(e -> {
            action.accept(e.getValue());
            refresh();
        });
        return editor;
    }

    private void initComponents() {
        setTitle(getTitle());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel lblTitle = new JLabel("Title");



        JPanel content = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(content);

        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(Box.createGlue(),lblTitle)
                        .addLine(createPanelLabel(lblDX, () -> lockDX=true ) )
                        .addLine(createPanelLabel(lblDY, () -> lockDX=false ))
                        .addLine(createPanelLabel(lblX, () -> lockX=true ))
                        .addLine(createPanelLabel(lblY, () -> lockX=false )),
                new GroupLayoutSimple.Stack()
                        .addLine(txtTitle)
                        .addLine(withAction(txtDX, this::updateDX))
                        .addLine(withAction(txtDY, this::updateDY))
                        .addLine(withAction(txtX, this::updateX))
                        .addLine(withAction(txtY, this::updateY))
        );

        refresh();
        setContentPane(content);
        pack();
        setSize(getPreferredSize());
        setVisible(true);
    }

    @Override
    public String getTitle() {
        return line.getTitle() + " info";
    }

    private String fmtLabel(String text, boolean locked) {
        if (locked) {
            return "<html><b>" + text + "</b></html>";
        } else {
            return text;
        }
    }

    private void updateDX(double newDX) {
        dx = newDX;
    }

    private void updateDY(double newDY) {
        dy = newDY;
    }

    private void updateX(double newX) {
        x = newX;
    }

    private void updateY(double newY) {
        y = newY;
    }

    private void refresh() {
        if (lockDX) dy = line.getDY(dx);
        else dx = line.getDX(dy);

        if (lockX) y = line.getY(x);
        else x = line.getX(y);

        txtDX.setValue(dx);
        txtDY.setValue(dy);
        txtX.setValue(x);
        txtY.setValue(y);

        txtTitle.setText(line.getTitle());

        lblDX.setText(fmtLabel("Δx", lockDX));
        lblDY.setText(fmtLabel("Δy", !lockDX));
        lblX.setText(fmtLabel("x", lockX));
        lblY.setText(fmtLabel("y", !lockX));

    }

}
