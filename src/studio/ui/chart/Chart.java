package studio.ui.chart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import studio.kdb.*;
import studio.ui.StudioOptionPane;
import studio.ui.Toolbar;
import studio.ui.Util;
import studio.utils.WindowsAppUserMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Chart implements ComponentListener {

    private static final Logger log = LogManager.getLogger();
    private static final Config config = Config.getInstance();

    private static final int CONFIG_UPDATE_DELAY = 1000;

    private Timer configUpdateTimer;

    private KTableModel table;
    private ChartPanel chartPanel = null;
    private JFrame frame;
    private JPanel contentPane;
    private ChartConfigPanel pnlConfig;

    private final List<Integer> indexes = new ArrayList<>();
    private int xIndex = -1;
    private int yIndex = -1;

    private final String defaultTitle;

    private static int chartIndex = 0;
    private static final List<Chart> charts = new ArrayList<>();


    private final static Set<KType> supportedClasses = new HashSet<>();

    static {
        supportedClasses.addAll(DurationEditor.VALUE_CLASSES);
        supportedClasses.addAll(DurationEditor.TEMPORAL_CLASSES);
    }

    private static StandardChartTheme currentTheme = new StandardChartTheme("JFree");
    static {
        currentTheme.setXYBarPainter(new StandardXYBarPainter());
    }

    public Chart(KTableModel table) {
        this.table = table;
        chartIndex++;
        defaultTitle = "Studio for kdb+ [chart"+ chartIndex +"]";
        initComponents();
    }

    private void initComponents() {
        List<String> namesList = new ArrayList<>();
        for (int index = 0; index < table.getColumnCount(); index++) {
            KType type = table.getColumnType(index).getElementType();
            if (supportedClasses.contains(type)) {
                indexes.add(index);
                namesList.add(table.getColumnName(index));
            }
        }

        if (indexes.size() < 2) {
            log.info("Nothing to chart. Number of columns which can be casted to decimal in {}", indexes.size());
            StudioOptionPane.showWarning(null, "It turns out that nothing is found to chart.", "Nothing to chart");
            return;
        }
        String[] names = namesList.toArray(new String[namesList.size()]);

        int plotMoveModifier = Util.MAC_OS_X ? KeyEvent.ALT_MASK : KeyEvent.CTRL_MASK;
        int lineDragModifier = Util.MAC_OS_X ? KeyEvent.META_MASK : KeyEvent.CTRL_MASK;

        String defaultLabelText = "  Use mouse wheel or select a rectangle to zoom. " +
                "Hold " + KeyEvent.getKeyModifiersText(plotMoveModifier) + " to move the chart. " +
                "ESC - to restore scale";

        String selectedLineText = "  Move the line wih mouse drag. " +
                "Hold " + KeyEvent.getKeyModifiersText(lineDragModifier) + " to change the slope of the line. ";

        JLabel lbl = new JLabel(defaultLabelText);

        chartPanel = createChartPanel();
        pnlConfig = new ChartConfigPanel(this, names);

        JToolBar toolbar = new Toolbar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setFloatable(false);
        toolbar.add(chartPanel.getLineAction());
        toolbar.add(chartPanel.getCopyAction()).setFocusable(false);
        toolbar.add(chartPanel.getSaveAction()).setFocusable(false);
        chartPanel.addNewLineListener(e -> {
            chartPanel.getRootPane().requestFocusInWindow();
            pnlConfig.addLine(e.getLine());
        });
        chartPanel.addLineSelectionListener(e -> lbl.setText(e.getLine() == null ? defaultLabelText : selectedLineText) );

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(pnlConfig, BorderLayout.CENTER);
        rightPanel.add(toolbar, BorderLayout.NORTH);

        contentPane = new JPanel(new BorderLayout());
        contentPane.add(rightPanel, BorderLayout.EAST);
        contentPane.add(lbl, BorderLayout.SOUTH);
        contentPane.add(chartPanel, BorderLayout.CENTER);
        refreshPlot();

        configUpdateTimer = new Timer(CONFIG_UPDATE_DELAY, e -> saveFrameBounds());

        WindowsAppUserMode.setChartId();
        try {
            charts.add(this);
            frame = new JFrame();
            updateTitle();
            frame.setContentPane(contentPane);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    charts.remove(Chart.this);
                }
            });
            frame.setIconImage(Util.CHART_BIG_ICON.getImage());

            frame.setBounds(config.getBounds(Config.CHART_BOUNDS));
            frame.addComponentListener(this);
            frame.setVisible(true);
            frame.requestFocus();
            frame.toFront();
        } finally {
            WindowsAppUserMode.setMainId();
        }
    }

    private ChartPanel createChartPanel() {
        XYPlot plot = new XYPlot(null, null, null, null);
        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.addChangeListener(e -> updateTitle() );
        currentTheme.apply(chart);
        return new ChartPanel(chart);
    }

    private String getChartTitle() {
        String title = null;

        if (chartPanel != null && chartPanel.getChart() != null) {
            JFreeChart chart = chartPanel.getChart();

            TextTitle chartTitle = chart.getTitle();
            if (chartTitle != null && chartTitle.isVisible()) {
                String text = chartTitle.getText();
                if (text != null && !text.trim().equals("")) {
                    title = text.trim();
                }
            }
        }

        if (title == null || title.trim().equals("")) {
            return defaultTitle;
        }

        return title;
    }

    private void updateTitle() {
        if (frame == null) {
            return;
        }
        String title = getChartTitle();
        if (! title.equals(frame.getTitle())) {
            frame.setTitle(title);
        }
    }

    private void saveFrameBounds() {
        SwingUtilities.invokeLater( () -> {
            configUpdateTimer.stop();
            if (frame == null) return;

            config.setBounds(Config.CHART_BOUNDS, frame.getBounds());
        });
    }

    private void updateFrameBounds() {
        configUpdateTimer.restart();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        updateFrameBounds();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        updateFrameBounds();
    }

    @Override
    public void componentShown(ComponentEvent e) {
        updateFrameBounds();
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        updateFrameBounds();
    }

    public KType getDomainKType() {
        return table.getColumnType(xIndex).getElementType();
    }

    public KType getRangeKType() {
        return table.getColumnType(yIndex).getElementType();
    }

    public void refreshPlot() {
        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = chart.getXYPlot();
        int count = plot.getDatasetCount();
        for (int i=0; i<count; i++) {
            plot.setDataset(i, null);
            plot.setRenderer(i, null);
        }

        int xIndex = indexes.get(pnlConfig.getDomainIndex());
        KType xType = table.getColumnType(xIndex).getElementType();

        if (this.xIndex != xIndex) {
            NumberAxis xAxis = new NumberAxis("");
            xAxis.setNumberFormatOverride(new KFormat(xType));
            xAxis.setAutoRangeIncludesZero(false);
            plot.setDomainAxis(xAxis);
            this.xIndex = xIndex;
        }

        KType yType = null;
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        int datasetIndex = 0;
        for (int index = 0; index<indexes.size(); index++) {
            if (! pnlConfig.isSeriesEnables(index)) continue;
            int yIndex = indexes.get(index);
            if (yIndex == xIndex) continue;;

            if (yType == null) {
                yType = table.getColumnType(yIndex).getElementType();
                if (this.yIndex != yIndex) {
                    NumberAxis yAxis = new NumberAxis("");
                    yAxis.setNumberFormatOverride(new KFormat(yType));
                    yAxis.setAutoRangeIncludesZero(false);
                    plot.setRangeAxis(yAxis);
                    this.yIndex = yIndex;
                }
            }

            IntervalXYDataset dataset = getDateset(xIndex, yIndex);

            XYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator(StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                    new KFormat(xType), new KFormat(yType));
            XYItemRenderer renderer;

            LegendIcon icon = pnlConfig.getLegendIcon(index);
            ChartType chartType = icon.getChartType();
            if (chartType == ChartType.BAR) {
                renderer = new BarRenderer();
            } else {
                renderer = new XYLineAndShapeRenderer(chartType.hasLine(), chartType.hasShape());
            }
            renderer.setDefaultToolTipGenerator(toolTipGenerator);
            renderer.setSeriesPaint(0, icon.getColor());
            renderer.setSeriesShape(0, icon.getShape());
            renderer.setSeriesStroke(0, icon.getStroke());
            ((AbstractRenderer)renderer).setAutoPopulateSeriesPaint(false);
            ((AbstractRenderer)renderer).setAutoPopulateSeriesShape(false);
            ((AbstractRenderer)renderer).setAutoPopulateSeriesStroke(false);

            plot.setRenderer(datasetIndex, renderer);
            plot.setDataset(datasetIndex, dataset);
            datasetIndex++;
        }

        chartPanel.setVisible(yType!=null);
        contentPane.revalidate();
        contentPane.repaint();
    }

    private IntervalXYDataset getDateset(int xCol, int yCol) {
        XYSeriesCollection collection = new XYSeriesCollection();
        collection.setAutoWidth(true);
        XYSeries series = new XYSeries(table.getColumnName(yCol), false, true);
        for (int row = 0; row < table.getRowCount(); row++) {
            K.KBase xValue = (K.KBase)table.getValueAt(row, xCol);
            K.KBase yValue = (K.KBase)table.getValueAt(row, yCol);
            if (xValue.isNull() || yValue.isNull()) continue;

            ToDouble x = (ToDouble)xValue;
            ToDouble y = (ToDouble)yValue;
            if (x.isInfinity() || y.isInfinity()) continue;

            series.add(x.toDouble(), y.toDouble());
        }
        collection.addSeries(series);
        return collection;
    }
}

