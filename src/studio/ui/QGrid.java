package studio.ui;

import studio.kdb.*;
import studio.ui.action.CopyTableSelectionAction;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

//@TODO: Should it be really a JPanel? It looks it should be just a JTabel. And anyway any additional components could be added to TabPanel
public class QGrid extends JPanel {
    private StudioPanel panel;
    private final TableModel model;
    private final JTable table;
    private CellRenderer cellRenderer;
    private KFormatContext formatContext = KFormatContext.DEFAULT;

    public JTable getTable() {
        return table;
    }

    public int getRowCount() {
        return model.getRowCount();
    }

    private final JPopupMenu popupMenu = new JPopupMenu();
    private final UserAction copyExcelFormatAction;
    private final UserAction copyHtmlFormatAction;

    static class MYJTable extends JTable {
        public MYJTable(TableModel m) {
            super(m);
            Font tableFont = Config.getInstance().getFont(Config.FontKind.TABLE);
            setFont(tableFont);
            setRowHeight(getFontMetrics(tableFont).getHeight());
            updateUI();
        }

        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            c.setFont(this.getFont());
            return c;
        }

        @Override
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

    public QGrid(StudioPanel panel, KTableModel model) {
        this.panel = panel;
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
        table.setShowVerticalLines(true);
        table.getTableHeader().setReorderingAllowed(true);
        final JScrollPane scrollPane = new JScrollPane(table);

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

        WidthAdjuster wa = new WidthAdjuster(table, scrollPane);
        wa.resizeAllColumns();

        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getViewport().setBackground(UIManager.getColor("Table.background"));

        JLabel rowCountLabel = new IndexHeader(model, scrollPane);
        rowCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rowCountLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        rowCountLabel.setOpaque(false);
        rowCountLabel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        rowCountLabel.setFont(UIManager.getFont("TableHeader.font"));
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

        copyExcelFormatAction = UserAction.create("Copy (Excel format)",
                Util.COPY_ICON,"Copy the selected cells to the clipboard using Excel format",
                KeyEvent.VK_E,null,
                new CopyTableSelectionAction(CopyTableSelectionAction.Format.Excel, table));

        copyHtmlFormatAction = UserAction.create("Copy (HTML)",
                Util.COPY_ICON, "Copy the selected cells to the clipboard using HTML",
                KeyEvent.VK_H, null,
                new CopyTableSelectionAction(CopyTableSelectionAction.Format.Html, table));

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
                if (!e.isPopupTrigger()) return;

                JPopupMenu popupMenu = getPopupMenu(e.getPoint());
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
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

    public void setPanel(StudioPanel panel) {
        this.panel = panel;
    }

    private JPopupMenu getPopupMenu(Point point) {
        int row = table.rowAtPoint(point);
        int col = table.columnAtPoint(point);
        if (row == -1 || col == -1) return popupMenu;

        String[] connections = Config.getInstance().getTableConnExtractor().getConnections(table.getModel(), row, col);
        if (connections.length == 0) return popupMenu;

        JPopupMenu popupMenu = new JPopupMenu();
        for (String connection: connections) {
            Server server = Config.getInstance().getServerByConnectionString(connection);
            String name = server.getName().length() == 0 ? connection : server.getName();
            Action action = UserAction.create("Open " + connection,
                    "Open " + name + " in a new tab", 0,
                    e -> panel.addTab(server, null) );
            popupMenu.add(action);
        }
        popupMenu.add(new JSeparator());
        popupMenu.add(copyExcelFormatAction);
        popupMenu.add(copyHtmlFormatAction);
        return popupMenu;
    }

}
