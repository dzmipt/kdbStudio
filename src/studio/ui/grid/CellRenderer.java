package studio.ui.grid;

import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.kdb.KTableModel;
import studio.ui.Util;
import studio.ui.search.TableMarkers;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CellRenderer extends DefaultTableCellRenderer {

    private static Color keyColor, altColor, nullColor;
    private static Color bgColor;
    private static Color selColor, selFgColor, fgColor;
    private static Color markKeyColor;
    private static Color markBgColor;
    private static Color markAltColor;
    private static Color markSelColor;
    static {
        installUI();
    }

    public static void installUI() {
        bgColor = UIManager.getColor("Table.background");
        bgColor = bgColor == null ? Color.WHITE : bgColor;

        keyColor = Util.blendColors(new Color(215,255,215), bgColor);
        altColor = Util.blendColors(new Color(215,215,255), bgColor);
        nullColor = Util.blendColors(new Color(255,45,45), bgColor);


        Color selBgColor = UIManager.getColor("Table.selectionBackground");
        Color defaultBgColor = new Color(145, 79, 206);
        selColor = selBgColor == null ? defaultBgColor : selBgColor;

        selFgColor = UIManager.getColor("Table.selectionForeground");

        fgColor = UIManager.getColor("Table.foreground");

        Color markColor = Util.blendColors(new Color(255, 145, 0), bgColor);

        markKeyColor = Util.blendColors(markColor, keyColor);
        markBgColor = Util.blendColors(markColor, bgColor);
        markAltColor = Util.blendColors(markColor, altColor);
        markSelColor = Util.blendColors(markColor, selColor);
    }

    private KFormatContext formatContextWithType, formatContextNoType;
    private final JTable table;
    private final TableMarkers markers;

    public CellRenderer(JTable table, TableMarkers markers) {
        this.table = table;
        this.markers = markers;
        setFormatContext(KFormatContext.DEFAULT);
        setHorizontalAlignment(SwingConstants.LEFT);
        setOpaque(true);
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
