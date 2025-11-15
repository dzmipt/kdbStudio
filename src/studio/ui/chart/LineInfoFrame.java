package studio.ui.chart;

import studio.kdb.KType;
import studio.ui.GroupLayoutSimple;
import studio.ui.StudioFrame;
import studio.utils.WindowsAppUserMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.DoubleConsumer;

public class LineInfoFrame extends StudioFrame {

    private final Chart chart;
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

    public LineInfoFrame(Chart chart, Line line, KType xType, KType yType) {
        this.chart = chart;
        this.line = line;
        refreshTitle();
        txtDX = Editor.createDurationEditor(xType);
        txtDY = Editor.createDurationEditor(yType);

        txtX = Editor.createEditor(xType);
        txtY = Editor.createEditor(yType);

        line.addChangeListener(e -> refresh());

        WindowsAppUserMode.setChartId();
        try {
            initComponents();
        } finally {
            WindowsAppUserMode.setMainId();
        }
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

    private void updateTitle() {
        line.setTitle(txtTitle.getText());
    }

    private void initComponents() {
        setTitle(getTitle());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JLabel lblTitle = new JLabel("Title");
        txtTitle.addActionListener(e -> updateTitle());
        txtTitle.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateTitle();
            }
        });



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
        adjustLocation();
        setVisible(true);
    }

    private void adjustLocation() {
        Rectangle chartBounds = chart.getFrame().getBounds();
        double x = chartBounds.getMaxX();

        double maxX = Integer.MIN_VALUE;

        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice screen : screens) {
            maxX = Math.max(maxX, screen.getDefaultConfiguration().getBounds().getMaxX());
        }

        if (x + getWidth() > maxX) {
            x -= x + getWidth() - maxX;
        }

        setLocation((int) x, chartBounds.y);
    }

    private void refreshTitle() {
        setTitle(line.getTitle() + " info");
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
        refreshTitle();
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
