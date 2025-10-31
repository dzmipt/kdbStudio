package studio.ui.grid;

import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.kdb.KTableModel;
import studio.kdb.config.GridColorConfig;
import studio.kdb.config.GridColorToken;
import studio.ui.Util;
import studio.ui.search.TableMarkers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CellRenderer extends DefaultTableCellRenderer {

    private GridColorConfig config;


    private KFormatContext formatContextWithType, formatContextNoType;
    private final JTable table;
    private final TableMarkers markers;

    public CellRenderer(JTable table, TableMarkers markers, GridColorConfig config) {
        this.table = table;
        this.markers = markers;
        this.config = config;
        setFormatContext(KFormatContext.DEFAULT);
        setHorizontalAlignment(SwingConstants.LEFT);
        setOpaque(true);
    }

    public void setGridColorConfig(GridColorConfig config) {
        this.config = config;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(UIManager.getColor("Table.background"));
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return table.getFontMetrics(font);
    }

    public void setFormatContext(KFormatContext formatContext) {
        formatContextWithType = new KFormatContext(formatContext).setShowType(true);
        formatContextNoType = new KFormatContext(formatContext).setShowType(false);
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        if (value != null) {
            K.KBase kb = (K.KBase) value;
            String text = kb.toString(
                kb instanceof K.KBaseVector ? formatContextWithType : formatContextNoType);
            text = Util.limitString(text, Config.getInstance().getInt(Config.MAX_CHARS_IN_TABLE_CELL));
            setText(text);

            boolean isMarked = markers.isMarked(table.convertRowIndexToModel(row), table.convertColumnIndexToModel(column));
            GridColorToken fgToken, bgToken;

            if (!isSelected && !isMarked) {
                KTableModel ktm = (KTableModel) table.getModel();
                column = table.convertColumnIndexToModel(column);
                if (ktm.isKey(column)) {
                    fgToken = bgToken = GridColorToken.KEY;
                } else {
                    bgToken = row % 2 == 0 ? GridColorToken.EVEN : GridColorToken.ODD;
                    fgToken = kb.isNull() ? GridColorToken.NULL : bgToken;
                }
            } else {
                if (isSelected && isMarked) {
                    fgToken = bgToken = GridColorToken.MARK_SELECTED;
                } else {
                    fgToken = bgToken = isMarked ? GridColorToken.MARK : GridColorToken.SELECTED;
                }
            }

            setForeground(config.getColor(fgToken, true));
            setBackground(config.getColor(bgToken, false));
        }
        return this;
    }
}
