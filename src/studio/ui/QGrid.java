package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rtextarea.SearchContext;
import studio.kdb.*;
import studio.ui.action.CopyTableSelectionAction;
import studio.ui.action.TableUserAction;
import studio.ui.grid.CellRenderer;
import studio.ui.grid.TableHeaderRenderer;
import studio.ui.grid.TableRowHeader;
import studio.ui.grid.WidthAdjuster;
import studio.ui.search.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

//@TODO: Should it be really a JPanel? It looks it should be just a JTabel. And anyway any additional components could be added to TabPanel
public class QGrid extends JPanel implements MouseWheelListener, SearchPanelListener {
    private final JScrollPane scrollPane;
    private StudioWindow studioWindow;
    private final KTableModel model;
    private final JTable table;
    private final WidthAdjuster widthAdjuster;
    private final TableRowHeader tableRowHeader;
    private final TableHeaderRenderer tableHeaderRenderer;
    private final CellRenderer cellRenderer;

    private final TableMarkers markers;
    private SearchContext lastSearchContext;
    private Position lastSearchPos;

    private KFormatContext formatContext = KFormatContext.DEFAULT;

    private static final Logger log = LogManager.getLogger();
    private JLabel rowCountLabel;

    public JTable getTable() {
        return table;
    }

    public int getRowCount() {
        return model.getRowCount();
    }

    private final JPopupMenu popupMenu = new JPopupMenu();
    private final TableUserAction inspectCellAction;

    private long doubleClickTimeout;

    private GraphicsConfiguration graphicsConfiguration = null;

    public void setFormatContext(KFormatContext formatContext) {
        this.formatContext = formatContext;
        cellRenderer.setFormatContext(formatContext);
        table.repaint();
    }

    public QGrid(StudioWindow studioWindow, ResultTab resultTab, KTableModel model) {
        this.studioWindow = studioWindow;
        this.model = model;

        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchPanel searchPanel = studioWindow.getResultSearchPanel();
                searchPanel.setVisible(true);
            }
        };

        KeyStroke keyStroke = Util.getMenuShortcut(KeyEvent.VK_F);
        KeyStroke keyStroke1 = Util.getMenuShortcut(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK);

        inputMap.put(keyStroke, "searchPanel");
        inputMap.put(keyStroke1, "searchPanel");
        actionMap.put("searchPanel", action);

        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeSearchPanel();
            }
        };
        inputMap.put(keyStroke, "closeSearchPanel");
        actionMap.put("closeSearchPanel", action);

        setDoubleClickTimeout(Config.getInstance().getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT));

        table = new JTable(model) {
            @Override
            public int convertRowIndexToView(int modelRowIndex) {
                throw new IllegalStateException("Not yet implemented");
            }

            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                return model.getIndex()[viewRowIndex];
            }
        };

        tableHeaderRenderer = new TableHeaderRenderer(table);
        table.getTableHeader().setDefaultRenderer(tableHeaderRenderer);
        table.setShowHorizontalLines(true);

        table.setDragEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setCellSelectionEnabled(true);

        ToolTipManager.sharedInstance().unregisterComponent(table);
        ToolTipManager.sharedInstance().unregisterComponent(table.getTableHeader());

        markers = new TableMarkers(model.getColumnCount());
        cellRenderer = new CellRenderer(table, markers);

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


        scrollPane.setWheelScrollingEnabled(true);

        rowCountLabel = new IndexHeader(model, scrollPane);
        rowCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rowCountLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        rowCountLabel.setOpaque(false);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowCountLabel);

        rowCountLabel = new JLabel("");
        rowCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowCountLabel.setVerticalAlignment(SwingConstants.CENTER);
        rowCountLabel.setOpaque(true);
        scrollPane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, rowCountLabel);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        UserAction copyExcelFormatAction = UserAction.create("Copy selection (Excel format)",
                Util.COPY_ICON, "Copy the selected cells to the clipboard using Excel format",
                KeyEvent.VK_E, null,
                new CopyTableSelectionAction(CopyTableSelectionAction.Format.Excel, table));

        UserAction copyHtmlFormatAction = UserAction.create("Copy selection (HTML)",
                Util.COPY_ICON, "Copy the selected cells to the clipboard using HTML",
                KeyEvent.VK_H, null,
                new CopyTableSelectionAction(CopyTableSelectionAction.Format.Html, table));

        inspectCellAction = new TableUserAction.InspectCellAction(resultTab, table);
        TableUserAction inspectLineAction = new TableUserAction.InspectLineAction(resultTab, table);

        popupMenu.add(inspectCellAction);
        popupMenu.add(inspectLineAction);
        popupMenu.add(new JMenuItem(copyExcelFormatAction));
        popupMenu.add(new JMenuItem(copyHtmlFormatAction));

        table.addMouseListener(new MouseAdapter() {
            private int lastRow = -1;
            private int lastCol = -1;
            private long lastTimestamp = -1;

            public void mousePressed(MouseEvent e) {
                if (maybeShowPopup(e)) return;

                if (! SwingUtilities.isLeftMouseButton(e)) return;

                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if ((e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) == InputEvent.ALT_DOWN_MASK ) doubleClick(row, col);
                else if (row == lastRow && col == lastCol &&
                        System.currentTimeMillis() - lastTimestamp < doubleClickTimeout) {
                    doubleClick(row, col);
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
                if (! SwingUtilities.isLeftMouseButton(e)) return;

                doubleClick(lastRow, lastCol);
            }


            private void doubleClick(int row, int col) {
                lastCol = lastRow = -1;
                lastTimestamp = -1;
                if (row == -1 || col == -1) return;

                K.KBase b = (K.KBase) table.getValueAt(row, col);

                int type = b.getType().getType();
                if ( (type >= -19 && type <= -1) ||
                        (type >= 101 && type <= 103 ) ||
                        type == 10 || type == 4) {

                    //@TODO: we shouldn't duplicate the logic here.
                    KFormatContext formatContextForCell = new KFormatContext(formatContext);
                    formatContextForCell.setShowType(b instanceof K.KBaseVector);
                    Util.copyTextToClipboard(b.toString(formatContextForCell));
                } else {
                    inspectCellAction.setLocation(row, col);
                    inspectCellAction.actionPerformed(null);
                }
            }
        });

        studioWindow.getMainStatusBar().bindTable(table);
        initSearch();
        updateUI();
    }

    @Override
    public void updateUI() {
        CellRenderer.installUI();
        super.updateUI();

        if (scrollPane == null) return ; //called from super constructor. not yet initialized
        scrollPane.getViewport().setBackground(UIManager.getColor("Table.background"));

        rowCountLabel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        rowCountLabel.setFont(UIManager.getFont("TableHeader.font"));
        rowCountLabel.setBackground(UIManager.getColor("TableHeader.background"));
        rowCountLabel.setForeground(UIManager.getColor("TableHeader.foreground"));

        rowCountLabel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        rowCountLabel.setFont(UIManager.getFont("Table.font"));
        rowCountLabel.setBackground(UIManager.getColor("TableHeader.background"));
        rowCountLabel.setForeground(UIManager.getColor("TableHeader.foreground"));
    }

    private void resetSearch() {
        lastSearchContext = null;
        lastSearchPos = null;
    }

    private void initSearch() {
        resetSearch();
        table.getModel().addTableModelListener( e-> resetSearch() ); // happens during sorting
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            @Override
            public void columnAdded(TableColumnModelEvent e) {}
            @Override
            public void columnRemoved(TableColumnModelEvent e) {}
            @Override
            public void columnMarginChanged(ChangeEvent e) {}

            @Override
            public void columnMoved(TableColumnModelEvent e) {
                resetSearch();
            }

            @Override
            public void columnSelectionChanged(ListSelectionEvent e) {
                resetSearch();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> resetSearch() );
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ((e.getModifiersEx() & Util.menuShortcutKeyMask) == 0) return;

        Font font = Config.getInstance().getFont(Config.FONT_TABLE);
        int newFontSize = font.getSize() + e.getWheelRotation();
        if (newFontSize < 6) return;
        font = font.deriveFont((float) newFontSize);

        Config.getInstance().setFont(Config.FONT_TABLE, font);
        StudioWindow.refreshResultSettings();
    }

    public void resizeColumns() {
        widthAdjuster.revalidate();
        revalidate();
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

    public void setStudioWindow(StudioWindow studioWindow) {
        this.studioWindow = studioWindow;
    }

    private JPopupMenu getPopupMenu(Point point) {
        int row = table.rowAtPoint(point);
        int col = table.columnAtPoint(point);
        if (row == -1 || col == -1) return popupMenu;

        int rowModel = table.convertRowIndexToModel(row);
        int colModel = table.convertColumnIndexToModel(col);

        String[] connections = Config.getInstance().getTableConnExtractor().getConnections(table.getModel(), rowModel, colModel);

        JPopupMenu newPopupMenu;

        if (connections.length == 0) newPopupMenu = popupMenu;
        else {
            boolean currentTab = Config.getInstance().getBoolean(Config.SERVER_FROM_RESULT_IN_CURRENT);
            newPopupMenu = new JPopupMenu();
            for (String connection : connections) {
                Server server = Config.getInstance().getServerByConnectionString(connection);
                String name = server.getName().isEmpty() ? connection : server.getName();
                Action action;
                if (currentTab) {
                    action = UserAction.create("Open " + connection,
                            "Open " + name + " in the current tab", 0,
                            e -> studioWindow.setServer(server) );
                } else {
                    action = UserAction.create("Open " + connection,
                            "Open " + name + " in a new tab", 0,
                            e -> studioWindow.addTab(server, null));
                }
                newPopupMenu.add(action);
            }
            newPopupMenu.add(new JSeparator());

            for (Component component: popupMenu.getComponents()) {
                if (component instanceof JSeparator) newPopupMenu.addSeparator();
                if (component instanceof JMenuItem) {
                    Action action = ((JMenuItem) component).getAction();
                    if (action != null) newPopupMenu.add(action);
                }
            }
        }

        for (Component component: popupMenu.getComponents()) {
            if (component instanceof JMenuItem) {
                Action action = ((JMenuItem) component).getAction();
                if (action instanceof TableUserAction) {
                    ((TableUserAction)action).setLocation(row, col);
                }
            }
        }

        return newPopupMenu;
    }

    private boolean isSearchContinue(SearchContext context) {
        if (lastSearchContext == null) return false;

        return Objects.equals(context.getSearchFor(), lastSearchContext.getSearchFor()) &&
                context.getWholeWord() == lastSearchContext.getWholeWord() &&
                context.isRegularExpression() == lastSearchContext.isRegularExpression() &&
                context.getMatchCase() == lastSearchContext.getMatchCase() &&
                ! context.getMarkAll() && ! lastSearchContext.getMarkAll();
    }

    @Override
    public void search(SearchContext context, SearchAction action) {
        boolean markAll = context.getMarkAll();
        int markCount = 0;

        boolean searchingInSelection = table.getSelectedColumns().length > 1 || table.getSelectedRows().length > 1;
        boolean continueSearch = isSearchContinue(context);
        if (!continueSearch) {
            lastSearchContext = context;
            lastSearchPos = null;
        }
        if (!markAll) {
            markers.clear();
            table.repaint();
        }

        TableIterator tableIterator = new TableIterator(table, context.getSearchForward(), lastSearchPos);
        SearchEngine searchEngine;
        try {
            searchEngine = new SearchEngine(context);
        } catch (PatternSyntaxException e) {
            studioWindow.getMainStatusBar().setTemporaryStatus("Error in regular expression: " + e.getMessage());
            return;
        }

        Position startPos = lastSearchPos;
        while (true) {
            lastSearchPos = tableIterator.next();
            if (startPos == null) startPos = lastSearchPos;
            else if (startPos.equals(lastSearchPos)) break;

            int modelRow = table.convertRowIndexToModel(lastSearchPos.getRow());
            int modelColumn = table.convertColumnIndexToModel(lastSearchPos.getColumn());
            K.KBase value = model.get(modelRow, modelColumn);
            String text = value.isNull() ? "" : value.toString(KFormatContext.NO_TYPE);

            if (searchEngine.containsIn(text)) {
                markers.mark(modelRow, modelColumn);
                if (markAll) {
                    markCount++;
                } else {
                    table.repaint();

                    Rectangle rectangle = table.getCellRect(lastSearchPos.getRow(), lastSearchPos.getColumn(), false);
                    table.scrollRectToVisible(rectangle);


                    studioWindow.getMainStatusBar().setTemporaryStatus(
                            (searchingInSelection ? "Found in selection: " : "Found: ")
                             + text);
                    studioWindow.getMainStatusBar().setRowColStatus(lastSearchPos);
                    return;
                }
            }
        }

        studioWindow.getMainStatusBar().resetStatuses();
        if (markAll) {
            studioWindow.getMainStatusBar().setTemporaryStatus(
                    (searchingInSelection ? "Marked in selection " : "Marked ")
                    + markCount + " cell(s)");
            table.repaint();
        } else {
            studioWindow.getMainStatusBar().setTemporaryStatus("Nothing was found" +
                    (searchingInSelection ? " in selection" : "") );
        }
    }

    @Override
    public void closeSearchPanel() {
        markers.clear();
        table.repaint();
        studioWindow.getResultSearchPanel().setVisible(false);
        table.requestFocus();
    }
}
