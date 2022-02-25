package studio.ui.chart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ChartConfigPanel extends Box {

    private Chart chart;
    private List<String> names;
    private List<Integer> xIndex;
    private List<Integer> yIndex;

    private JComboBox<ChartType> comboCharType;

    public ChartConfigPanel(Chart chart, List<String> names, List<Integer> xIndex, List<Integer> yIndex) {
        super(BoxLayout.Y_AXIS);
        this.chart = chart;
        this.names = names;
        this.xIndex = xIndex;
        this.yIndex = yIndex;
        int count = yIndex.size();
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        Box boxStyle = Box.createHorizontalBox();
        boxStyle.add(new JLabel("Type: "));

        comboCharType = new JComboBox<>(ChartType.values());
        comboCharType.setMaximumSize(new Dimension(Integer.MAX_VALUE, comboCharType.getPreferredSize().height));
        comboCharType.addActionListener(this::charTypeSelected);
        ChartType chartType = (ChartType) comboCharType.getSelectedItem();

        boxStyle.add(comboCharType);
        boxStyle.add(Box.createHorizontalGlue());
        add(boxStyle);
    }

    public int getDomainIndex() {
        return xIndex.get(comboX.getSelectedIndex());
    }

    public boolean isSeriesEnables(int index) {
        return chkY[index].isSelected() && chkY[index].isEnabled();
    }

    public Paint getColor(int index) {
        return icons[index].getColor();
    }

    public Shape getShape(int index) {
        return icons[index].getShape();
    }

    public Stroke getStroke(int index) {
        return icons[index].getStroke();
    }

    public ChartType getChartType(int index) {
        return icons[index].getChartType();
    }

    private void charTypeSelected(ActionEvent e) {
        ChartType chartType = (ChartType) comboCharType.getSelectedItem();
        for (LegendIcon icon: icons) {
            icon.setChartType(chartType);
        }
        actionPerformed(e);
    }

}
