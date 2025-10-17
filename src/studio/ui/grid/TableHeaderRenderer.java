package studio.ui.grid;

import studio.kdb.Config;
import studio.kdb.KTableModel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TableHeaderRenderer extends DefaultTableCellRenderer {

    private final JTable table;

    public TableHeaderRenderer(JTable table) {
        super();
        this.table = table;
        setHorizontalAlignment(SwingConstants.LEFT);
        setVerticalAlignment(SwingConstants.CENTER);
        setOpaque(true);

        setFont(Config.getInstance().getFont(Config.FONT_TABLE));
    }

    @Override
    public void updateUI() {
        super.updateUI();
        Border border = UIManager.getBorder("TableHeader.cellBorder");
        if (border == null) {
            border = BorderFactory.createMatteBorder(0, 0, 2, 1, Color.BLACK);
        }
        // add gap for sorter icon
        setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(5,2,1,0)));

        setBackground(UIManager.getColor("TableHeader.background"));
        setForeground(UIManager.getColor("TableHeader.foreground"));
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return table.getFontMetrics(font);
    }

    public void setFont(Font f) {
        super.setFont(f);
        revalidate();
    }

    private boolean asc = false;
    private boolean desc = false;

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        // setFont(table.getFont());

        if (table.getModel() instanceof KTableModel) {
            column = table.convertColumnIndexToModel(column);
            KTableModel ktm = (KTableModel) table.getModel();
            if (ktm.isSortedAsc(column)) {
                asc = true;
                desc = false;
            } else if (ktm.isSortedDesc(column)) {
                asc = false;
                desc = true;
            } else {
                asc = false;
                desc = false;
            }
        }

        setText(value == null ? " " : value.toString());

        return this;
    }

    @Override
    public void paint(Graphics g) {
        int width = SwingUtilities.computeStringWidth(getFontMetrics(getFont()), getText());
        int availableWidth = Math.min(getInsets().left + width, getSize().width);
        SorterDrawer.paint(asc, desc, this, availableWidth, g);
        super.paint(g);
    }
}
