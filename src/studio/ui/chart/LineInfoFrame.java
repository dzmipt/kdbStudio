package studio.ui.chart;

import studio.kdb.K;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
    private final DurationEditor txtDX;
    private final DurationEditor txtDY;
    private final JTextField txtX = new JTextField();
    private final JTextField txtY = new JTextField();

    private double dx = 1;
    private double dy = Double.NaN;
    private double x = 0;
    private double y = Double.NaN;
    private boolean lockDX = true;
    private boolean lockX = true;

    public LineInfoFrame(Line line, Class<? extends K.KBase> xClazz, Class<? extends K.KBase> yClazz) {
        this.line = line;
        txtDX = DurationEditor.create(xClazz);
        txtDY = DurationEditor.create(yClazz);

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

    private void performAction(JTextField textField, DoubleConsumer action) {
        try {
            double value = Double.parseDouble(textField.getText());
            action.accept(value);
        } catch (NumberFormatException e) {}
        refresh();
    }

    private JTextField withAction(JTextField textField, DoubleConsumer action) {
        textField.addActionListener(e -> performAction(textField, action));
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                performAction(textField, action);
            }
        });
        return textField;
    }

    private DurationEditor withAction(DurationEditor editor, DoubleConsumer action) {
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
        txtX.setText("" + x);
        txtY.setText("" + y);

        txtTitle.setText(line.getTitle());

        lblDX.setText(fmtLabel("Δx", lockDX));
        lblDY.setText(fmtLabel("Δy", !lockDX));
        lblX.setText(fmtLabel("x", lockX));
        lblY.setText(fmtLabel("y", !lockX));

    }

}
