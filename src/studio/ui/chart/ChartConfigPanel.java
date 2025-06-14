package studio.ui.chart;

import studio.ui.GroupLayoutSimple;
import studio.ui.UserAction;
import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartConfigPanel extends JPanel {

    private final Chart chart;

    private final JComboBox<ChartType> comboCharType;

    private final List<LegendListPanel> series = new ArrayList<>();

    private final LegendListPanel listLines;
    private final List<Line> lines = new ArrayList<>();
    private final Map<Line,LineInfoFrame> infoFrameMap = new HashMap<>();


    public ChartConfigPanel(Chart chart, PlotConfig plotConfig) {
        this.chart = chart;

        comboCharType = new JComboBox<>(ChartType.values());
        comboCharType.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboCharType.getPreferredSize().height));
        comboCharType.addActionListener(this::charTypeSelected);

        listLines = new LegendListPanel();
        listLines.addChangeListener(e -> refresh() );

        addPlotConfigs(plotConfig);
    }

    public void addPlotConfigs(PlotConfig... plotConfigs) {
        for (PlotConfig plotConfig: plotConfigs) {
            LegendListPanel panel = new LegendListPanel(plotConfig, series.size()>0);
            panel.addChangeListener(e -> refresh());
            series.add(panel);
        }
        initComponents();
        revalidate();
    }

    public void setPlotTitle(String title) {
        if (series.size() == 0) return;
        series.get(0).setPlotTitle(title);
    }

    private void initComponents() {
        removeAll();
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        JLabel typeLabel = new JLabel("Type: ");

        List<Component> lefts = new ArrayList<>();
        List<Component> rights = new ArrayList<>();
        List<Component> fillers = new ArrayList<>();
        for (int i = 0; i< series.size(); i++) {
            Component left = Box.createRigidArea(new Dimension(5,20));
            Component right = Box.createRigidArea(new Dimension(5,20));
            JComponent filler = new Box.Filler(new Dimension(0,1), new Dimension(0, 1), new Dimension(Integer.MAX_VALUE, 1));
            filler.setBorder(BorderFactory.createLineBorder(Color.GRAY));

            lefts.add(left);
            rights.add(right);
            fillers.add(filler);
        }

        JPanel scrollPaneContent = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(scrollPaneContent, listLines);
        layout.addMaxWidthComponents(series.toArray(new LegendListPanel[0]));
        layout.addMaxWidthComponents(fillers.toArray(new Component[0]));

        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);
        layout.setBaseline(false);

        GroupLayoutSimple.Stack stack = new GroupLayoutSimple.Stack();
        for (int i = 0; i< series.size(); i++) {
            stack.addLine(series.get(i))
                    .addLine(lefts.get(i), fillers.get(i), rights.get(i));
        }
        stack.addLine(listLines);
        layout.setStacks(stack);

        scrollPaneContent.setBorder(BorderFactory.createEmptyBorder(0,0,0,2));
        JScrollPane scrollPane = new JScrollPane(scrollPaneContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        setLayout(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        top.add(typeLabel, BorderLayout.WEST);
        top.add(comboCharType, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

    }

    public void dispose() {
        for (LineInfoFrame frame: infoFrameMap.values()) {
            frame.dispose();
        }
    }

    public PlotConfig[] getPlotConfigs() {
        return series.stream()
                .map(LegendListPanel::getPlotConfig)
                .toArray(PlotConfig[]::new);
    }

    public void addLine(Line line) {
        Action detailsAction = UserAction.create("Line Info", Util.BLANK_ICON, "Show line's parameters", KeyEvent.VK_I, null,
                e->showDetails(line) );

        String title = "Line " + (1+ listLines.getListSize());
        line.setTitle(title);
        lines.add(line);
        int index = listLines.add(title, line.getIcon(), detailsAction);
        line.addChangeListener(e -> {
            listLines.updateTitle(index, line.getTitle());
        });
    }

    private void showDetails(Line line) {
        LineInfoFrame frame = infoFrameMap.get(line);
        if (frame != null) {
            if ((frame.getState() & Frame.NORMAL) != 0) {
                frame.setState(Frame.NORMAL);
            }
            frame.toFront();
        } else {
            frame = new LineInfoFrame(chart, line, chart.getDomainKType(), chart.getRangeKType());
            infoFrameMap.put(line, frame);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    infoFrameMap.remove(line);
                }
            } );
        }
    }

    private void refresh() {
        for (int index = 0; index < lines.size(); index++) {
            lines.get(index).setVisible(listLines.isSelected(index));
        }
        invalidate();
        repaint();
        chart.refreshPlot();
    }

    private void charTypeSelected(ActionEvent e) {
        ChartType chartType = (ChartType) comboCharType.getSelectedItem();
        for(LegendListPanel panel: series) {
            panel.setChartType(chartType);
        }
        refresh();
    }

}
