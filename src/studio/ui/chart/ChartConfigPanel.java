package studio.ui.chart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class ChartConfigPanel extends Box {

    private final Chart chart;

    private final JComboBox<ChartType> comboCharType;
    private final JComboBox<String> comboX;
    private final LegendListPanel listSeries;

    private final LegendListPanel listLines;
    private final List<Line> lines = new ArrayList<>();


    public ChartConfigPanel(Chart chart, String[] names) {
        super(BoxLayout.Y_AXIS);
        this.chart = chart;
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        comboCharType = new JComboBox<>(ChartType.values());
        comboCharType.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboCharType.getPreferredSize().height));
        comboCharType.addActionListener(this::charTypeSelected);

        Box boxStyle = Box.createHorizontalBox();
        boxStyle.add(new JLabel("Type: "));
        boxStyle.add(comboCharType);
        boxStyle.add(Box.createHorizontalGlue());
        add(boxStyle);

        comboX = new JComboBox<>(names);
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
            LegendIcon icon = new LegendIcon(LegendButton.BASE_COLORS[index % LegendButton.BASE_COLORS.length], LegendButton.SHAPES[index % LegendButton.SHAPES.length],
                    LegendButton.getDefaultStroke());
            icon.setChartType(ChartType.values()[0]);
            listSeries.add(names[index], icon);
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

    public int getDomainIndex() {
        return comboX.getSelectedIndex();
    }

    public boolean isSeriesEnables(int index) {
        return listSeries.isSelected(index);
    }

    public LegendIcon getLegendIcon(int index) {
        return listSeries.getIcon(index);
    }

    public void addLine(Line line) {
        lines.add(line);
        listLines.add("Line " + (1+ listLines.getListSize()), line.getIcon());
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
