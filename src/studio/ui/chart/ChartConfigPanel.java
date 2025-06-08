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
    private final LegendListPanel listSeries;

    private final LegendListPanel listLines;
    private final List<Line> lines = new ArrayList<>();
    private final Map<Line,LineInfoFrame> infoFrameMap = new HashMap<>();

    public ChartConfigPanel(Chart chart, PlotConfig plotConfig) {
        this.chart = chart;

        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        comboCharType = new JComboBox<>(ChartType.values());
        comboCharType.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboCharType.getPreferredSize().height));
        comboCharType.addActionListener(this::charTypeSelected);


        JLabel typeLabel = new JLabel("Type: ");

        listSeries = new LegendListPanel(plotConfig);
        listSeries.addChangeListener(e -> refresh() );

        listLines = new LegendListPanel();
        listLines.addChangeListener(e -> refresh() );

        Component left = Box.createRigidArea(new Dimension(5,20));
        Component right = Box.createRigidArea(new Dimension(5,20));
        JComponent filler = new Box.Filler(new Dimension(0,1), new Dimension(0, 1), new Dimension(Integer.MAX_VALUE, 1));
        filler.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JPanel scrollPaneContent = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(scrollPaneContent, listSeries, filler, listLines);
        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);
        layout.setBaseline(false);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(listSeries)
                        .addLine(left, filler, right)
                        .addLine(listLines)
        );

        JScrollPane scrollPane = new JScrollPane(scrollPaneContent);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        layout = new GroupLayoutSimple(this, comboCharType, scrollPane);
        layout.setAutoCreateContainerGaps(false);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(typeLabel, comboCharType)
                        .addLine(scrollPane)
        );
    }

    public void dispose() {
        for (LineInfoFrame frame: infoFrameMap.values()) {
            frame.dispose();
        }
    }

    public PlotConfig getPlotConfig() {
        return listSeries.getPlotConfig();
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
        int count = listSeries.getListSize();
        for (int index = 0; index < count; index++) {
            listSeries.getIcon(index).setChartType(chartType);
        }
        refresh();
    }

}
