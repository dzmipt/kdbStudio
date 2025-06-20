package studio.ui;

import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.kdb.KTableModel;
import studio.ui.search.TableMarkers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

class CellRenderer extends DefaultTableCellRenderer {
    private static final Color keyColor = new Color(220,255,220);
    private static final Color altColor = new Color(220,220,255);
    private static final Color nullColor = new Color(255,150,150);

    private static final Color bgColor = UIManager.getColor("Table.background");

    private static final Color selBgColor = UIManager.getColor("Table.selectionBackground");
    private static final Color defaultBgColor = new Color(145, 79, 206);
    private static final Color selColor = selBgColor == null ? defaultBgColor : selBgColor;

    private static final Color selFgColor = UIManager.getColor("Table.selectionForeground");

    private static final Color fgColor = UIManager.getColor("Table.foreground");

    private static final Color markColor = new Color(255,200,0);

    private static final Color markKeyColor = Util.blendColors(markColor, keyColor);
    private static final Color markBgColor = Util.blendColors(markColor, bgColor);
    private static final Color markAltColor = Util.blendColors(markColor, altColor);
    private static final Color markSelColor = Util.blendColors(markColor, selColor);

    private KFormatContext formatContextWithType, formatContextNoType;
    private final JTable table;
    private final TableMarkers markers;

    public CellRenderer(JTable table, TableMarkers markers) {
        this.table = table;
        this.markers = markers;
        setFormatContext(KFormatContext.DEFAULT);
        setHorizontalAlignment(SwingConstants.LEFT);
        setOpaque(true);
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
            setForeground(kb.isNull() ? nullColor : fgColor);

            boolean isMarked = markers.isMarked(table.convertRowIndexToModel(row), table.convertColumnIndexToModel(column));

            if (!isSelected) {
                KTableModel ktm = (KTableModel) table.getModel();
                column = table.convertColumnIndexToModel(column);
                if (ktm.isKey(column))
                    setBackground(isMarked ? markKeyColor : keyColor);
                else if (row % 2 == 0)
                    setBackground(isMarked ? markAltColor : altColor);
                else
                    setBackground(isMarked ? markBgColor : bgColor);
            } else {
                setForeground(selFgColor);
                setBackground(isMarked ? markSelColor : selColor);
            }
        }
        return this;
    }
}
