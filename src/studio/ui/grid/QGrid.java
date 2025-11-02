package studio.ui.grid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.KFormatContext;
import studio.kdb.KTableModel;
import studio.kdb.config.GridColorConfig;
import studio.ui.search.TableMarkers;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;

public class QGrid extends JTable {
    private final KTableModel model;

    private final TableHeaderRenderer tableHeaderRenderer;
    private final CellRenderer cellRenderer;
    private final TableMarkers markers;

    private KFormatContext formatContext = KFormatContext.DEFAULT;

    private static final Logger log = LogManager.getLogger();

    public QGrid(KTableModel model) {
        super(model);
        this.model = model;
        tableHeaderRenderer = new TableHeaderRenderer(this);
        getTableHeader().setDefaultRenderer(tableHeaderRenderer);
        setShowHorizontalLines(true);

        setDragEnabled(true);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        setCellSelectionEnabled(true);

        ToolTipManager.sharedInstance().unregisterComponent(this);
        ToolTipManager.sharedInstance().unregisterComponent(getTableHeader());

        markers = new TableMarkers(model.getColumnCount());
        cellRenderer = new CellRenderer(this, markers, Config.getInstance().getGridColorConfig());

        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn col = getColumnModel().getColumn(i);
            col.setCellRenderer(cellRenderer);
        }

        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setShowVerticalLines(true);
        getTableHeader().setReorderingAllowed(true);

    }

    public int getRowCount() {
        return model.getRowCount();
    }

    public TableMarkers getMarkers() {
        return markers;
    }

    @Override
    public int convertRowIndexToView(int modelRowIndex) {
        throw new IllegalStateException("Not yet implemented");
    }

    @Override
    public int convertRowIndexToModel(int viewRowIndex) {
        return model.getIndex()[viewRowIndex];
    }

    public void setFormatContext(KFormatContext formatContext) {
        this.formatContext = formatContext;
        cellRenderer.setFormatContext(formatContext);
        repaint();
    }

    public KFormatContext getFormatContext() {
        return formatContext;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (tableHeaderRenderer == null) return;

        tableHeaderRenderer.setFont(font);
        cellRenderer.setFont(font);
        int rowHeight = getFontMetrics(font).getHeight();
        setRowHeight(rowHeight);

        revalidate();
        repaint();
    }

    public void setGridColorConfig(GridColorConfig config) {
        cellRenderer.setGridColorConfig(config);
    }


}
