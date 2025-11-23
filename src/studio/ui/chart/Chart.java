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
import org.jfree.data.xy.XYSeriesCollection;
import studio.kdb.Config;
import studio.kdb.KFormat;
import studio.kdb.KTableModel;
import studio.kdb.KType;
import studio.kdb.config.ColorSchema;
import studio.ui.*;
import studio.utils.WindowsAppUserMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Chart implements ComponentListener {

    private static final Logger log = LogManager.getLogger();
    private static final Config config = Config.getInstance();

    private static final int CONFIG_UPDATE_DELAY = 1000;

    private Timer configUpdateTimer;

    private ChartPanel chartPanel = null;
    private StudioFrame frame;
    private JPanel contentPane;
    private ChartConfigPanel pnlConfig;

    private KType domainType, rangeType;

    private final String defaultTitle;

    private static int chartIndex = 0;
    private static final List<Chart> charts = new ArrayList<>();

    private static final StandardChartTheme currentTheme = new StandardChartTheme("JFree");
    static {
        currentTheme.setXYBarPainter(new StandardXYBarPainter());
    }

    public Chart(KTableModel table) {
        chartIndex++;
        defaultTitle = "Studio for kdb+ [chart"+ chartIndex +"]";
        initComponents(table);
    }

    private void initComponents(KTableModel table) {
        PlotConfig plotConfig = new PlotConfig(table);
        plotConfig.setTitle(getChartTitle());

        if (plotConfig.size() < 2) {
            log.info("Nothing to chart. Number of columns which can be casted to decimal in {}", plotConfig.size());
            StudioOptionPane.showWarning(null, "It turns out that nothing is found to chart.", "Nothing to chart");
            return;
        }

        int plotMoveModifier = Util.MAC_OS_X ? KeyEvent.ALT_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
        int lineDragModifier = Util.MAC_OS_X ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;

        String defaultLabelText = "  Use mouse wheel or select a rectangle to zoom. " +
                "Hold " + InputEvent.getModifiersExText(plotMoveModifier) + " to move the chart. " +
                "ESC - to restore scale";

        String selectedLineText = "  Move the line wih mouse drag. " +
                "Hold " + InputEvent.getModifiersExText(lineDragModifier) + " to change the slope of the line. ";

        JLabel lbl = new JLabel(defaultLabelText);

        chartPanel = createChartPanel();
        pnlConfig = new ChartConfigPanel(this, plotConfig);

        JToolBar toolbar = new Toolbar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setFloatable(false);
        toolbar.add(UserAction.create("Merge", Util.PLUS2_ICON, "Merge another chart",
                0, null, this::merge)).setFocusable(false);
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
            frame = new StudioFrame();
            updateTitle();
            frame.setContentPane(contentPane);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    charts.remove(Chart.this);
                    pnlConfig.dispose();
                    StudioWindow.refreshAllMenus();
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

    public void merge(ActionEvent event) {
        if (charts.size() < 2) {
            StudioOptionPane.showWarning(getFrame(), "There is on other charts", "Nothing to select");
            return;
        }

        String[] captions = charts.stream()
                .filter(c -> c!=this )
                .map(c -> c.getChartTitle())
                .toArray(String[]::new);

        JComboBox<String> comboCaptions = new JComboBox<>(captions);
        JCheckBox chkColor = new JCheckBox("Update colors");
        chkColor.setSelected(true);
        JCheckBox chkClose = new JCheckBox("Close other chart");

        JPanel panel = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(panel);
        layout.setStacks(new GroupLayoutSimple.Stack()
                .addLine(comboCaptions)
                .addLine(chkColor)
                .addLine(chkClose)
        );

        int result = StudioOptionPane.showComplexDialog(getFrame(), panel, "Select Chart");
        if (result != JOptionPane.OK_OPTION) return;

        int selectedIndex = comboCaptions.getSelectedIndex();
        Chart chart = null;
        for(int index = 0 ;index < charts.size(); index++ ) {
            chart = charts.get(index);
            if (chart == this) continue;
            if (selectedIndex == 0) break;
            selectedIndex--;
        }

        if (chart == null) {
            log.warn("Something goes unexpectedly wrong");
            return;
        }

        PlotConfig[] plotConfigs = chart.pnlConfig.getPlotConfigs();
        if (chkColor.isSelected()) {
            int count = Arrays.stream(pnlConfig.getPlotConfigs())
                                .mapToInt(PlotConfig::size)
                                .sum();

            List<Color> colors = Config.getInstance().getChartColorSets().getColorSchema().getColors();
            int colorCount = colors.size();
            for (PlotConfig plotConfig: plotConfigs) {
                for (int i=0; i<plotConfig.size(); i++) {
                    LegendIcon icon = plotConfig.getIcon(i);
                    icon.setColor(colors.get(count % colorCount));
                    count++;
                }
            }
        }

        pnlConfig.addPlotConfigs(plotConfigs);

        if (chkClose.isSelected()) {
            chart.getFrame().dispose();
        }
        refreshPlot();
    }

    public JFrame getFrame() {
        return frame;
    }

    public static List<Chart> getCharts() {
        return Collections.unmodifiableList(charts);
    }

    private ChartPanel createChartPanel() {
        XYPlot plot = new XYPlot(null, null, null, null);
        JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        chart.addChangeListener(e -> updateTitle() );
        currentTheme.apply(chart);
        ColorSchema colorSchema = Config.getInstance().getChartColorSets().getColorSchema();
        plot.setBackgroundPaint(colorSchema.getBackground());
        plot.setDomainGridlinePaint(colorSchema.getGrid());
        plot.setRangeGridlinePaint(colorSchema.getGrid());
        return new ChartPanel(chart);
    }

    public String getChartTitle() {
        String title = null;

        if (chartPanel != null && chartPanel.getChart() != null) {
            JFreeChart chart = chartPanel.getChart();

            TextTitle chartTitle = chart.getTitle();
            if (chartTitle != null && chartTitle.isVisible()) {
                String text = chartTitle.getText();
                if (text != null && !text.trim().isEmpty()) {
                    title = text.trim();
                }
            }
        }

        if (title == null || title.trim().isEmpty()) {
            return defaultTitle;
        }

        return title;
    }

    private void updateTitle() {
        if (frame == null) {
            return;
        }
        String title = getChartTitle();
        if (! title.equals(frame.getRealTitle())) {
            frame.setTitle(title);
            StudioWindow.refreshAllMenus();
        }

        pnlConfig.setPlotTitle(title);
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
        return domainType;
    }

    public KType getRangeKType() {
        return rangeType;
    }

    public void refreshPlot() {
        JFreeChart chart = chartPanel.getChart();
        XYPlot plot = chart.getXYPlot();
        int count = plot.getDatasetCount();
        for (int i=0; i<count; i++) {
            plot.setDataset(i, null);
            plot.setRenderer(i, null);
        }
        plot.clearRangeAxes();

        domainType = null;
        rangeType = null;
        String rangeLabel = "";
        int extraAxisIndex = 0;

        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        int datasetIndex = 0;
        PlotConfig[] plotConfigs = pnlConfig.getPlotConfigs();
        for (PlotConfig plotConfig: plotConfigs) {
            if (domainType == null) {
                int domainIndex = plotConfig.getDomainIndex();
                domainType = plotConfig.getColumn(domainIndex).getElementType();

                NumberAxis xAxis = new NumberAxis("");
                xAxis.setNumberFormatOverride(new KFormat(domainType));
                xAxis.setAutoRangeIncludesZero(false);
                plot.setDomainAxis(xAxis);
            }

            for (int index = 0; index < plotConfig.size(); index++) {
                if (!plotConfig.getEnabled(index)) continue;
                if (index == plotConfig.getDomainIndex() ) continue;

                KType currentRangeType = plotConfig.getColumn(index).getElementType();
                if (plotConfig.getExtraAxis(index)) {
                    extraAxisIndex++;
                    NumberAxis yAxis = new NumberAxis(plotConfig.getColumn(index).getName());
                    yAxis.setNumberFormatOverride(new KFormat(currentRangeType));
                    yAxis.setAutoRangeIncludesZero(false);
                    plot.setRangeAxis(extraAxisIndex, yAxis);
                } else {
                    if (rangeType == null) {
                        rangeType = currentRangeType;
                        rangeLabel = plotConfig.getColumn(index).getName();
                        NumberAxis yAxis = new NumberAxis("");
                        yAxis.setNumberFormatOverride(new KFormat(rangeType));
                        yAxis.setAutoRangeIncludesZero(false);
                        plot.setRangeAxis(yAxis);
                    }
                    currentRangeType = rangeType;
                }

                XYSeriesCollection dataset = new XYSeriesCollection();
                dataset.setAutoWidth(true);
                dataset.addSeries(plotConfig.getSeries(index));

                XYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator(StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                        new KFormat(domainType), new KFormat(currentRangeType));
                XYItemRenderer renderer;

                LegendIcon icon = plotConfig.getIcon(index);
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
                ((AbstractRenderer) renderer).setAutoPopulateSeriesPaint(false);
                ((AbstractRenderer) renderer).setAutoPopulateSeriesShape(false);
                ((AbstractRenderer) renderer).setAutoPopulateSeriesStroke(false);

                plot.setRenderer(datasetIndex, renderer);
                plot.setDataset(datasetIndex, dataset);

                int axisIndex = plotConfig.getExtraAxis(index) ? extraAxisIndex : 0;
                plot.mapDatasetToRangeAxis(datasetIndex, axisIndex);

                datasetIndex++;
            }
        }

        if (rangeType!=null && extraAxisIndex>0) {
            plot.getRangeAxis().setLabel(rangeLabel);
        }
        chartPanel.setVisible(rangeType!=null);
        contentPane.revalidate();
        contentPane.repaint();
    }
}

