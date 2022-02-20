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
import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.ToDouble;
import studio.ui.StudioOptionPane;
import studio.ui.Toolbar;
import studio.ui.Util;
import studio.utils.WindowsAppUserMode;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;


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

    private List<Integer> yIndex;

    private final static Set<Class> supportedClasses = new HashSet<>();

    static {
        supportedClasses.addAll(Arrays.asList(
            K.KIntVector.class,
            K.KDoubleVector.class,
            K.KFloatVector.class,
            K.KShortVector.class,
            K.KLongVector.class,

            K.KDateVector.class,
            K.KTimeVector.class,
            K.KTimestampVector.class,
            K.KTimespanVector.class,
            K.KDatetimeVector.class,
            K.KMonthVector.class,
            K.KSecondVector.class,
            K.KMinuteVector.class) );
    }

    private static StandardChartTheme currentTheme = new StandardChartTheme("JFree");
    static {
        currentTheme.setXYBarPainter(new StandardXYBarPainter());
    }

    public Chart(KTableModel table) {
        this.table = table;
        initComponents();
    }

    private void initComponents() {
        List<String> names = new ArrayList<>();
        List<Integer> xIndex = new ArrayList<>();
        yIndex = new ArrayList<>();
        for (int index = 0; index<table.getColumnCount(); index++) {
            names.add(table.getColumnName(index));
            Class clazz = table.getColumnClass(index);
            if (supportedClasses.contains(clazz)) {
                xIndex.add(index);
                yIndex.add(index);
            }
        }

        if (xIndex.size() == 0 || yIndex.size() ==0) {
            log.info("Nothing to chart. Number of columns for x axes is {}. Number of columns for y axes is {}", xIndex.size(), yIndex.size());
            StudioOptionPane.showWarning(null, "It turns out that nothing is found to chart.", "Nothing to chart");
            return;
        }

        int modifier = Util.MAC_OS_X ? KeyEvent.ALT_MASK : KeyEvent.CTRL_MASK;
        JLabel lbl = new JLabel("  Use mouse wheel or select a rectangle to zoom. " +
                "Hold " + KeyEvent.getKeyModifiersText(modifier) + " to move the chart. " +
                "ESC - to restore scale");

        chartPanel = createChartPanel();
        pnlConfig = new ChartConfigPanel(this, names, xIndex, yIndex);

        JToolBar toolbar = new Toolbar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setFloatable(false);
        toolbar.add(chartPanel.getCopyAction());
        toolbar.add(chartPanel.getSaveAction());

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
            frame = new JFrame();
            updateTitle();
            frame.setContentPane(contentPane);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
        if (chartPanel == null) {
            return null;
        }
        JFreeChart chart = chartPanel.getChart();
        if (chart == null) {
            return null;
        }

        String title = null;
        TextTitle chartTitle = chart.getTitle();
        if (chartTitle != null && chartTitle.isVisible()) {
            String text = chartTitle.getText();
            if (text != null && ! text.trim().equals("")) {
                title = text.trim();
            }
        }
        return title;
    }

    private void updateTitle() {
        if (frame == null) {
            return;
        }
        String title = getChartTitle();
        if (title == null) {
            title = "Studio for kdb+ [chart]";
        }
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

    public void refreshPlot() {
        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = chart.getXYPlot();
        int count = plot.getDatasetCount();
        for (int i=0; i<count; i++) {
            plot.setDataset(i, null);
            plot.setRenderer(i, null);
        }

        int xIndex = pnlConfig.getDomainIndex();

        Class xClazz = table.getColumnClass(xIndex);
        NumberAxis xAxis = new NumberAxis("");
        xAxis.setNumberFormatOverride(new KFormat(xClazz));
        xAxis.setAutoRangeIncludesZero(false);
        plot.setDomainAxis(xAxis);

        Class yClazz = null;
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        int datasetIndex = 0;
        for (int index = 0; index<yIndex.size(); index++) {
            if (! pnlConfig.isSeriesEnables(index)) continue;

            if (yClazz == null) {
                yClazz = table.getColumnClass(yIndex.get(index));
                NumberAxis yAxis = new NumberAxis("");
                yAxis.setNumberFormatOverride(new KFormat(yClazz));
                yAxis.setAutoRangeIncludesZero(false);

                plot.setRangeAxis(yAxis);
            }

            IntervalXYDataset dataset = getDateset(yIndex.get(index));

            XYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator(StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                    new KFormat(xClazz), new KFormat(yClazz));
            XYItemRenderer renderer;

            ChartType chartType = pnlConfig.getChartType(index);
            if (chartType == ChartType.BAR) {
                renderer = new BarRenderer();
            } else {
                renderer = new XYLineAndShapeRenderer(chartType.hasLine(), chartType.hasShape());
            }
            renderer.setDefaultToolTipGenerator(toolTipGenerator);
            renderer.setSeriesPaint(0, pnlConfig.getColor(index));
            renderer.setSeriesShape(0, pnlConfig.getShape(index));
            renderer.setSeriesStroke(0, pnlConfig.getStroke(index));
            ((AbstractRenderer)renderer).setAutoPopulateSeriesPaint(false);
            ((AbstractRenderer)renderer).setAutoPopulateSeriesShape(false);
            ((AbstractRenderer)renderer).setAutoPopulateSeriesStroke(false);

            plot.setRenderer(datasetIndex, renderer);
            plot.setDataset(datasetIndex, dataset);
            datasetIndex++;
        }

        contentPane.revalidate();
        contentPane.repaint();
    }

    private IntervalXYDataset getDateset(int col) {
        int xIndex = pnlConfig.getDomainIndex();

        XYSeriesCollection collection = new XYSeriesCollection();
        collection.setAutoWidth(true);
        XYSeries series = new XYSeries(table.getColumnName(col));
        for (int row = 0; row < table.getRowCount(); row++) {
            K.KBase xValue = (K.KBase)table.getValueAt(row, xIndex);
            K.KBase yValue = (K.KBase)table.getValueAt(row, col);
            if (xValue.isNull() || yValue.isNull()) continue;

            double x = ((ToDouble)xValue).toDouble();
            double y = ((ToDouble)yValue).toDouble();
            series.add(x, y);
        }
        collection.addSeries(series);
        return collection;
    }
}

