package studio.ui;

import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.kdb.TableHeaderRenderer;
import studio.kdb.TableRowHeader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

//@TODO: Should it be really a JPanel? It looks it should be just a JTabel. And anyway any additional components could be added to TabPanel
public class QGrid extends JPanel {
    private final TableModel model;
    private final JTable table;
    private CellRenderer cellRenderer;
    private KFormatContext formatContext = KFormatContext.DEFAULT;

    public JTable getTable() {
        return table;
    }


    public static String newline = System.getProperty("line.separator");

    public int getRowCount() {
        return model.getRowCount();
    }

    private final JPopupMenu popupMenu = new JPopupMenu();

    static class MYJTable extends JTable {
        public MYJTable(TableModel m) {
            super(m);
        }

        public Component prepareRenderer(TableCellRenderer renderer,
                                         int rowIndex,
                                         int vColIndex) {
            Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
            c.setFont(this.getFont());
            return c;
        }

        private Font originalFont;
        private int originalRowHeight;
        private float zoomFactor = 1.0f;

        public void setFont(Font font) {
            originalFont = font;
            // When setFont() is first called, zoomFactor is 0.
            if (zoomFactor != 0.0 && zoomFactor != 1.0) {
                float scaledSize = originalFont.getSize2D() * zoomFactor;
                font = originalFont.deriveFont(scaledSize);
            }

            super.setFont(font);
        }

        public void setRowHeight(int rowHeight) {
            originalRowHeight = rowHeight;
            // When setRowHeight() is first called, zoomFactor is 0.
            if (zoomFactor != 0.0 && zoomFactor != 1.0)
                rowHeight = (int) Math.ceil(originalRowHeight * zoomFactor);

            super.setRowHeight(rowHeight);
        }

        public Component prepareEditor(TableCellEditor editor, int row, int column) {
            Component comp = super.prepareEditor(editor, row, column);
            comp.setFont(this.getFont());
            return comp;
        }
    }


    public void setFormatContext(KFormatContext formatContext) {
        this.formatContext = formatContext;
        cellRenderer.setFormatContext(formatContext);
        table.repaint();
    }

    public QGrid(TableModel model) {
        this.model = model;
        table = new MYJTable(model);

        DefaultTableCellRenderer dhr = new TableHeaderRenderer();
        table.getTableHeader().setDefaultRenderer(dhr);
        table.setShowHorizontalLines(true);

        table.setDragEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);

        ToolTipManager.sharedInstance().unregisterComponent(table);
        ToolTipManager.sharedInstance().unregisterComponent(table.getTableHeader());

        cellRenderer = new CellRenderer(table);

        for (int i = 0; i < model.getColumnCount(); i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setCellRenderer(cellRenderer);
        }

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.getTableHeader().setReorderingAllowed(true);
        final JScrollPane scrollPane = new JScrollPane(table);

        if (table.getRowCount() > 0) {
            TableRowHeader trh = new TableRowHeader(table);
            scrollPane.setRowHeaderView(trh);

            scrollPane.getRowHeader().addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ev) {
                    Point header_pt = ((JViewport) ev.getSource()).getViewPosition();
                    Point main_pt = main.getViewPosition();
                    if (header_pt.y != main_pt.y) {
                        main_pt.y = header_pt.y;
                        main.setViewPosition(main_pt);
                    }
                }

                final JViewport main = scrollPane.getViewport();
            });

        }
        WidthAdjuster wa = new WidthAdjuster(table);
        wa.resizeAllColumns();

        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getViewport().setBackground(UIManager.getColor("Table.background"));
        JLabel rowCountLabel = new JLabel("");
        rowCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowCountLabel.setVerticalAlignment(SwingConstants.CENTER);
        rowCountLabel.setOpaque(true);
        rowCountLabel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        rowCountLabel.setFont(UIManager.getFont("Table.font"));
        rowCountLabel.setBackground(UIManager.getColor("TableHeader.background"));
        rowCountLabel.setForeground(UIManager.getColor("TableHeader.foreground"));
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowCountLabel);

        rowCountLabel = new JLabel("");
        rowCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowCountLabel.setVerticalAlignment(SwingConstants.CENTER);
        rowCountLabel.setOpaque(true);
        rowCountLabel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        rowCountLabel.setFont(UIManager.getFont("Table.font"));
        rowCountLabel.setBackground(UIManager.getColor("TableHeader.background"));
        rowCountLabel.setForeground(UIManager.getColor("TableHeader.foreground"));
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, rowCountLabel);


        setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);

        UserAction copyExcelFormatAction = new UserAction("Copy (Excel format)",
                Util.COPY_ICON,
                "Copy the selected cells to the clipboard using Excel format",
                KeyEvent.VK_E,
                null) {
            public void actionPerformed(ActionEvent e) {
                StringBuilder sb = new StringBuilder();
                int numcols = table.getSelectedColumnCount();
                int numrows = table.getSelectedRowCount();
                int[] rowsselected = table.getSelectedRows();
                int[] colsselected = table.getSelectedColumns();
                if (!isTableSelectionValid()) {
                    JOptionPane.showMessageDialog(null,
                            "Invalid Copy Selection",
                            "Invalid Copy Selection",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (table.getSelectedRowCount() == table.getRowCount()) {
                    for (int col = 0; col < numcols; col++) {
                        sb.append(table.getColumnName(colsselected[col]));
                        if (col < numcols - 1)
                            sb.append("\t");
                    }
                    sb.append(newline);
                }

                for (int row = 0; row < numrows; row++) {
                    if (row > 0)
                        sb.append(newline);
                    for (int col = 0; col < numcols; col++) {
                        boolean symColumn = table.getColumnClass(col) == K.KSymbolVector.class;
                        if (symColumn)
                            sb.append("\"");

                        K.KBase b = (K.KBase) table.getValueAt(rowsselected[row], colsselected[col]);
                        if (!b.isNull())
                            sb.append(KFormatContext.NO_TYPE);
                        if (symColumn)
                            sb.append("\"");
                        if (col < numcols - 1)
                            sb.append("\t");
                    }
                }
                Util.copyTextToClipboard(sb.toString());
            }
        };

        UserAction copyHtmlFormatAction = new UserAction("Copy (HTML)",
                Util.COPY_ICON,
                "Copy the selected cells to the clipboard using HTML",
                KeyEvent.VK_H,
                null) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                StringBuilder sb = new StringBuilder();
                int numcols = table.getSelectedColumnCount();
                int numrows = table.getSelectedRowCount();

                int[] rowsselected = table.getSelectedRows();
                int[] colsselected = table.getSelectedColumns();

                if (!isTableSelectionValid()) {
                    JOptionPane.showMessageDialog(null,
                            "Invalid Copy Selection",
                            "Invalid Copy Selection",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                sb.append("<meta http-equiv=\"content-type\" content=\"text/html\"><table>");

                sb.append("<tr>");
                for (int col = 0; col < numcols; col++) {
                    sb.append("<th>").append(table.getColumnName(colsselected[col])).append("</th>");
                }
                sb.append("</tr>").append(newline);

                for (int row = 0; row < numrows; row++) {
                    if (row > 0) {
                        sb.append(newline);
                    }
                    sb.append("<tr>");
                    for (int col = 0; col < numcols; col++) {
                        K.KBase b = (K.KBase) table.getValueAt(rowsselected[row], colsselected[col]);
                        sb.append("<td>");
                        if (!b.isNull())
                            sb.append(b.toString(KFormatContext.NO_TYPE));
                        sb.append("</td>");
                    }
                    sb.append("</tr>");
                }

                sb.append("</table>");
                Util.copyHtmlToClipboard(sb.toString());
            }
        };

        popupMenu.add(new JMenuItem(copyExcelFormatAction));
        popupMenu.add(new JMenuItem(copyHtmlFormatAction));

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger())
                    popupMenu.show(e.getComponent(),
                            e.getX(), e.getY());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                K.KBase b = (K.KBase) table.getValueAt(table.getSelectedRow(), table.getSelectedColumn());
                //@TODO: we shouldn't duplicate the logic here.
                KFormatContext formatContextForCell = new KFormatContext(formatContext);
                formatContextForCell.setShowType(b instanceof K.KBaseVector);
                Util.copyTextToClipboard(b.toString(formatContextForCell));

            }
        });
    }

    private boolean isTableSelectionValid() {
        int numcols = table.getSelectedColumnCount();
        int numrows = table.getSelectedRowCount();
        int[] rowsselected = table.getSelectedRows();
        int[] colsselected = table.getSelectedColumns();
        return ((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0] &&
                numrows == rowsselected.length) &&
                (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0] &&
                        numcols == colsselected.length));
    }
}
