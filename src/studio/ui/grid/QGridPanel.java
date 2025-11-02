package studio.ui.grid;

import studio.kdb.KTableModel;
import studio.ui.IndexHeader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public class QGridPanel extends JScrollPane {

    private final WidthAdjuster widthAdjuster;
    private final TableRowHeader tableRowHeader;
    private final JLabel indexHeader;
    private final QGrid table;

    private GraphicsConfiguration graphicsConfiguration = null;

    public QGridPanel(KTableModel model) {
        table = new QGrid(model);
        setViewportView(table);

        tableRowHeader = new TableRowHeader(table);
        setRowHeaderView(tableRowHeader);

        getRowHeader().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ev) {
                Point header_pt = ((JViewport) ev.getSource()).getViewPosition();
                Point main_pt = main.getViewPosition();
                if (header_pt.y != main_pt.y) {
                    main_pt.y = header_pt.y;
                    main.setViewPosition(main_pt);
                }
            }

            final JViewport main = getViewport();
        });

        widthAdjuster = new WidthAdjuster(table, this);
        table.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                GraphicsConfiguration gc = table.getGraphicsConfiguration();
                if (graphicsConfiguration != gc) {
                    graphicsConfiguration = gc;
                    widthAdjuster.revalidate();
                }
            }
        });


        indexHeader = new IndexHeader(model, this);
        indexHeader.setHorizontalAlignment(SwingConstants.CENTER);
        indexHeader.setVerticalAlignment(SwingConstants.BOTTOM);
        indexHeader.setOpaque(false);
        setCorner(JScrollPane.UPPER_LEFT_CORNER, indexHeader);

//        rowCountLabel = new JLabel("");
//        rowCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
//        rowCountLabel.setVerticalAlignment(SwingConstants.CENTER);
//        rowCountLabel.setOpaque(true);
//        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, rowCountLabel);

        updateUI();
    }

    public QGrid getTable() {
        return table;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (indexHeader == null) return;

        getViewport().setBackground(UIManager.getColor("Table.background"));

        indexHeader.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        indexHeader.setFont(UIManager.getFont("TableHeader.font"));
        indexHeader.setBackground(UIManager.getColor("TableHeader.background"));
        indexHeader.setForeground(UIManager.getColor("TableHeader.foreground"));
    }

    public void resizeColumns() {
        widthAdjuster.revalidate();
        revalidate();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        if (table == null) return;

        table.setFont(font);

        int rowHeight = getFontMetrics(font).getHeight();

        tableRowHeader.setFont(font);
        ((JComponent)tableRowHeader.getCellRenderer()).setFont(font);
        tableRowHeader.setFixedCellHeight(rowHeight);
        tableRowHeader.recalcWidth();
        widthAdjuster.revalidate();

        revalidate();
        repaint();
    }


}
