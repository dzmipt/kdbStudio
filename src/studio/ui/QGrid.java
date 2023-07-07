package studio.ui;

import org.fife.ui.rtextarea.SearchContext;
import studio.kdb.*;
import studio.ui.action.CopyTableSelectionAction;
import studio.ui.search.SearchAction;
import studio.ui.search.SearchPanel;
import studio.ui.search.SearchPanelListener;
import studio.ui.search.TableSearch;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;

//@TODO: Should it be really a JPanel? It looks it should be just a JTabel. And anyway any additional components could be added to TabPanel
public class QGrid extends JPanel implements MouseWheelListener, SearchPanelListener {
    private StudioPanel panel;
    private final TableModel model;
    private final JTable table;
    private WidthAdjuster widthAdjuster;
    private TableRowHeader tableRowHeader;
    private TableHeaderRenderer tableHeaderRenderer;
    private JScrollPane scrollPane;
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

    private long doubleClickTimeout;

    public void setFormatContext(KFormatContext formatContext) {
        this.formatContext = formatContext;
        cellRenderer.setFormatContext(formatContext);
        table.repaint();
    }

    public QGrid(StudioPanel panel, KTableModel model) {
        this.panel = panel;
        this.model = model;

        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchPanel searchPanel = panel.getResultSearchPanel();
                searchPanel.setVisible(true);
            }
        };

        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, StudioPanel.menuShortcutKeyMask);
        KeyStroke keyStroke1 = KeyStroke.getKeyStroke(KeyEvent.VK_F, StudioPanel.menuShortcutKeyMask | InputEvent.SHIFT_MASK);

        inputMap.put(keyStroke, "searchPanel");
        inputMap.put(keyStroke1, "searchPanel");
        actionMap.put("searchPanel", action);

        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeSearchPanel();
                table.clearSelection();
            }
        };
        inputMap.put(keyStroke, "closeSearchPanel");
        actionMap.put("closeSearchPanel", action);

        setDoubleClickTimeout(Config.getInstance().getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT));

        table = new JTable(model);

        tableHeaderRenderer = new TableHeaderRenderer();
        table.getTableHeader().setDefaultRenderer(tableHeaderRenderer);
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
        scrollPane = new JScrollPane(table);
        scrollPane.addMouseWheelListener(this);

        tableRowHeader = new TableRowHeader(table);
        scrollPane.setRowHeaderView(tableRowHeader);

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

        widthAdjuster = new WidthAdjuster(table, scrollPane);

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
            private int lastRow = -1;
            private int lastCol = -1;
            private long lastTimestamp = -1;

            public void mousePressed(MouseEvent e) {
                if (maybeShowPopup(e)) return;

                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if ((e.getModifiers() & InputEvent.ALT_MASK) == InputEvent.ALT_MASK ) copy(row, col);
                else if (row == lastRow && col == lastCol &&
                        System.currentTimeMillis() - lastTimestamp < doubleClickTimeout) {
                    copy(row, col);
                } else {
                    lastRow = row;
                    lastCol = col;
                    lastTimestamp = System.currentTimeMillis();
                }
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private boolean maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return false;

                JPopupMenu popupMenu = getPopupMenu(e.getPoint());
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
                return true;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                copy(lastRow, lastCol);
            }


            private void copy(int row, int col) {
                lastCol = lastRow = -1;
                lastTimestamp = -1;
                if (row == -1 || col == -1) return;

                K.KBase b = (K.KBase) table.getValueAt(row, col);
                //@TODO: we shouldn't duplicate the logic here.
                KFormatContext formatContextForCell = new KFormatContext(formatContext);
                formatContextForCell.setShowType(b instanceof K.KBaseVector);
                Util.copyTextToClipboard(b.toString(formatContextForCell));
            }
        });
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ((e.getModifiers() & StudioPanel.menuShortcutKeyMask) == 0) return;

        Font font = Config.getInstance().getFont(Config.FONT_TABLE);
        int newFontSize = font.getSize() + e.getWheelRotation();
        if (newFontSize < 6) return;
        font = font.deriveFont((float) newFontSize);

        Config.getInstance().setFont(Config.FONT_TABLE, font);
        StudioPanel.refreshResultSettings();
    }

    public void setFont(Font font) {
        super.setFont(font);
        if (table == null) return;

        table.setFont(font);
        tableHeaderRenderer.setFont(font);
        cellRenderer.setFont(font);
        int rowHeight = getFontMetrics(font).getHeight();
        table.setRowHeight(rowHeight);

        tableRowHeader.setFont(font);
        ((JComponent)tableRowHeader.getCellRenderer()).setFont(font);
        tableRowHeader.setFixedCellHeight(rowHeight);
        tableRowHeader.recalcWidth();
        widthAdjuster.revalidate();

        revalidate();
        repaint();
    }

    public void setDoubleClickTimeout(long doubleClickTimeout) {
        this.doubleClickTimeout = doubleClickTimeout;
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

    @Override
    public void search(SearchContext context, SearchAction action) {
        String text = context.getSearchFor();

        TableSearch search = new TableSearch(table, context.getSearchForward());

        while (search.hasNext()) {
            String val = "";
            K.KBase o = search.next();
            if (!o.isNull()) {
                val = o.toString(KFormatContext.NO_TYPE);
            }

            if (val.contains(text)) {
                search.scrollTo();
                break;
            }
        }
    }

    @Override
    public void closeSearchPanel() {
        panel.getResultSearchPanel().setVisible(false);
        table.requestFocus();
    }
}
