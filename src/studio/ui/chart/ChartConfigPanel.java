package studio.ui.chart;

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

public class ChartConfigPanel extends Box {

    private final Chart chart;

    private final JComboBox<ChartType> comboCharType;
    private final JComboBox<String> comboX;
    private final LegendListPanel listSeries;

    private final LegendListPanel listLines;
    private final List<Line> lines = new ArrayList<>();
    private final Map<Line,LineInfoFrame> infoFrameMap = new HashMap<>();
    private PlotConfig plotConfig;

    public ChartConfigPanel(Chart chart, PlotConfig plotConfig) {
        super(BoxLayout.Y_AXIS);
        this.chart = chart;
        this.plotConfig = plotConfig;

        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        comboCharType = new JComboBox<>(ChartType.values());
        comboCharType.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboCharType.getPreferredSize().height));
        comboCharType.addActionListener(this::charTypeSelected);

        Box boxStyle = Box.createHorizontalBox();
        boxStyle.add(new JLabel("Type: "));
        boxStyle.add(comboCharType);
        boxStyle.add(Box.createHorizontalGlue());
        add(boxStyle);

        String[] names = plotConfig.getNames();
        comboX = new JComboBox<>(names);
        comboX.setSelectedIndex(plotConfig.getDomainIndex());
        comboX.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboX.getPreferredSize().height));
        comboX.addActionListener(e -> validateState() );

        Box boxDomain = Box.createHorizontalBox();
        boxDomain.add(new JLabel("Domain axis: "));
        boxDomain.add(comboX);
        boxDomain.add(Box.createHorizontalGlue());
        add(boxDomain);

        listSeries = new LegendListPanel("Series:", true, true, true);
        listSeries.addChangeListener(e -> refresh() );

        for (int index = 0; index < names.length; index++) {
            listSeries.add(names[index], plotConfig.getIcon(index));
        }
        listSeries.setEnabled(0, false);

        listLines = new LegendListPanel("Lines:", true, false, false);
        listLines.addChangeListener(e -> refresh() );

        Box boxIndent = Box.createHorizontalBox();
        boxIndent.add(Box.createRigidArea(new Dimension(5,20)));
        JComponent filler = new Box.Filler(new Dimension(0,0), new Dimension(0, 0), new Dimension(Integer.MAX_VALUE, 1));
        filler.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        boxIndent.add(filler);
        boxIndent.add(Box.createRigidArea(new Dimension(5,20)));


        Box scrollPaneContent = Box.createVerticalBox();
        scrollPaneContent.add(listSeries);
        scrollPaneContent.add(boxIndent);
        scrollPaneContent.add(listLines);
        scrollPaneContent.add(Box.createVerticalBox());

        scrollPaneContent.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        add(new JScrollPane(scrollPaneContent));

        listSeries.setEnabled(0, false);
    }

    public void dispose() {
        for (LineInfoFrame frame: infoFrameMap.values()) {
            frame.dispose();
        }
    }

    private int getDomainIndex() {
        return comboX.getSelectedIndex();
    }

    public PlotConfig getPlotConfig() {
//        dataset = new Dataset(this.dataset);
        plotConfig.setDomainIndex(getDomainIndex());
        for (int index = 0; index < plotConfig.size(); index++) {
            plotConfig.setEnabled(index, listSeries.isSelected(index));
            plotConfig.setIcon(index, listSeries.getIcon(index));
        }
        return plotConfig;
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

    private void validateState() {
        int domainIndex = getDomainIndex();
        int count = listSeries.getListSize();
        for (int index = 0; index < count; index++) {
            listSeries.setEnabled(index, index !=domainIndex);
        }
        refresh();
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
