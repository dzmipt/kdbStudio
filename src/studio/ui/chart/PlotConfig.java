package studio.ui.chart;

import studio.kdb.Config;
import studio.kdb.KColumn;
import studio.kdb.KTableModel;
import studio.kdb.KType;
import studio.kdb.config.ColorSets;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class PlotConfig {

    private String title;
    private final KColumn[] columns;
    private final boolean[] enabled;
    private final boolean[] extraAxis;
    private final LegendIcon[] icons;
    private int domainIndex;
    private final KXYSeries[] series;

    private final static Set<KType> supportedClasses = new HashSet<>();

    static {
        supportedClasses.addAll(Editor.VALUE_CLASSES);
        supportedClasses.addAll(Editor.TEMPORAL_CLASSES);
    }

    public PlotConfig(PlotConfig plotConfig) {
        this.title = plotConfig.title;
        this.columns = plotConfig.columns;
        this.enabled = Arrays.copyOf(plotConfig.enabled, plotConfig.enabled.length);
        this.extraAxis = Arrays.copyOf(plotConfig.extraAxis, plotConfig.extraAxis.length);
        this.icons = new LegendIcon[plotConfig.icons.length];
        for (int i=0; i<icons.length; i++) {
            this.icons[i] = new LegendIcon(plotConfig.icons[i]);
        }
        this.domainIndex = plotConfig.domainIndex;
        this.series = Arrays.copyOf(plotConfig.series, plotConfig.series.length);
    }

    public PlotConfig(KTableModel table) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < table.getColumnCount(); index++) {
            KType type = table.getColumn(index).getElementType();
            if (supportedClasses.contains(type)) {
                indexes.add(index);
            }
        }

        int count = indexes.size();
        columns = new KColumn[count];
        enabled = new boolean[count];
        extraAxis = new boolean[count];
        icons = new LegendIcon[count];
        series = new KXYSeries[count];

        ColorSets colorSets = Config.getInstance().getChartColorSets();
        List<Color> baseColors = colorSets.getColorSchema().getColors();
        for (int i=0; i<count; i++) {
            int index = indexes.get(i);
            columns[i] = table.getColumn(index);
            enabled[i] = true;
            extraAxis[i] = false;
            icons[i] = new LegendIcon(baseColors.get(i % baseColors.size()),
                    LegendButton.SHAPES[i % LegendButton.SHAPES.length],
                    LegendButton.getDefaultStroke());
            icons[i].setChartType(ChartType.LINE);
        }
        domainIndex = 0;

        resetSeriesCache();
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int size() {
        return columns.length;
    }

    public KColumn getColumn(int index) {
        return columns[index];
    }

    public String[] getNames() {
        return Stream.of(columns).map(KColumn::getName).toArray(String[]::new);
    }

    public boolean getEnabled(int index) {
        return enabled[index];
    }

    public void setEnabled(int index, boolean value) {
        enabled[index] = value;
    }

    public boolean getExtraAxis(int index) {
        return extraAxis[index];
    }

    public void setExtraAxis(int index, boolean value) {
        extraAxis[index] = value;
    }

    public LegendIcon getIcon(int index) {
        return icons[index];
    }

    public void setIcon(int index, LegendIcon icon) {
        icons[index] = icon;
    }

    public int getDomainIndex() {
        return domainIndex;
    }

    public void setDomainIndex(int domainIndex) {
        if (this.domainIndex == domainIndex) return;

        this.domainIndex = domainIndex;
        resetSeriesCache();
    }

    public int getAxes(boolean extra) {
        int res = 0;
        int count = extraAxis.length;
        for (int index=0; index<count; index++) {
            if (!enabled[index]) continue;
            if (index ==  domainIndex) continue;

            if (extraAxis[index] == extra) res++;
        }
        return res;
    }

    private void resetSeriesCache() {
        Arrays.fill(series, null);
    }

    public KXYSeries getSeries(int index) {
        if (series[index] != null) return series[index];

        series[index] = new KXYSeries(columns[domainIndex], columns[index]);
        return series[index];
    }
}
