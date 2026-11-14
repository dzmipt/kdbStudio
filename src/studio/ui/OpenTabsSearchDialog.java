package studio.ui;

import org.fife.ui.rtextarea.SearchContext;
import studio.ui.search.SearchEngine;
import studio.ui.search.SearchResult;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/** A modeless, live-search dialog for all editors belonging to one Studio window. */
public class OpenTabsSearchDialog extends EscapeDialog {

    private static OpenTabsSearchDialog instance = null;

    private List<EditorTab> editors;
    private final JTextField searchField = new JTextField();
    private final JCheckBox caseSensitive = new JCheckBox("Case sensitive");
    private final JCheckBox wholeWord = new JCheckBox("Whole word");
    private final JCheckBox regex = new JCheckBox("Regular expression");
    private final JLabel status = new JLabel(" ");
    private final MatchesTableModel model = new MatchesTableModel();
    private final JTable table = new JTable(model);

    private Window pendingWindow = null;
    private WindowFocusListener pendingListener = null;

    private OpenTabsSearchDialog(StudioWindow owner, List<EditorTab> editors) {
        super(null, "Find in Open Tabs", ModalityType.MODELESS);
        this.editors = editors;

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        options.add(caseSensitive);
        options.add(wholeWord);
        options.add(regex);

        JPanel top = new JPanel(new BorderLayout(6, 6));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        top.add(new JLabel("Find:"), BorderLayout.WEST);
        top.add(searchField, BorderLayout.CENTER);
        top.add(options, BorderLayout.SOUTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setMaxWidth(70);
        table.getColumnModel().getColumn(2).setMaxWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(480);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                            boolean focused, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setToolTipText(value == null ? null : value.toString());
                return component;
            }
        });

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        bottom.add(status, BorderLayout.WEST);
        JButton close = new JButton("Close");
        close.addActionListener(e -> cancel());
        bottom.add(close, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(760, 480));

        DocumentChangeListener listener = new DocumentChangeListener() {
            @Override public void documentChanged(javax.swing.event.DocumentEvent e) {
                search();
            }
        };
        searchField.getDocument().addDocumentListener(listener);
        caseSensitive.addActionListener(e -> search());
        wholeWord.addActionListener(e -> search());
        regex.addActionListener(e -> search());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount()<2 || !SwingUtilities.isLeftMouseButton(e)) return;

                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row < 0 || column < 0) return;

                openSelectedMatch(true);
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && e.getFirstIndex() >= 0 && table.getSelectedRow() >= 0) {
                openSelectedMatch(false);
            }
        });
    }

    // Only one dialog exists at a time. If it is already open, it is refreshed with the current tabs
    // and brought to front.
    public static void show(StudioWindow owner, List<EditorTab> editors) {
        if (instance != null) {
            instance.editors = editors;
            instance.search();
            instance.toFront();
            instance.searchField.requestFocusInWindow();
            return;
        }

        instance = new OpenTabsSearchDialog(owner, editors);
        instance.align();
        instance.setVisible(true);
        instance.searchField.requestFocusInWindow();
    }

    private void search() {
        String query = searchField.getText();
        model.clear();
        if (query.isEmpty()) {
            status.setText("Enter text to search all open tabs");
            return;
        }

        SearchContext context = new SearchContext();
        context.setSearchFor(query);
        context.setMatchCase(caseSensitive.isSelected());
        context.setWholeWord(wholeWord.isSelected());
        context.setRegularExpression(regex.isSelected());
        try {
            SearchEngine engine = new SearchEngine(context);
            for (EditorTab editor : editors) addMatches(engine, editor);
            status.setText(model.getRowCount() + " match(es) in " + editors.size() + " open tab(s)");
        } catch (PatternSyntaxException e) {
            status.setText("Invalid regular expression: " + e.getDescription());
        }
    }

    private void addMatches(SearchEngine engine, EditorTab editor) {
        String text = editor.getTextArea().getText();
        int offset = 0;
        while (offset < text.length()) {
            SearchResult result = engine.search(text, offset);
            if (!result.found()) return;
            int start = result.matcher().start();
            int end = result.matcher().end();
            model.add(new Match(editor, start, end, lineNumber(text, start), columnNumber(text, start), preview(text, start, end)));
            offset = Math.max(end, start + 1);
        }
    }

    private static int lineNumber(String text, int offset) {
        int line = 1;
        for (int index = 0; index < offset; index++) if (text.charAt(index) == '\n') line++;
        return line;
    }

    private static int columnNumber(String text, int offset) {
        int lineStart = text.lastIndexOf('\n', Math.max(0, offset - 1));
        return offset - lineStart;
    }

    private static String preview(String text, int start, int end) {
        int lineStart = text.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        int lineEnd = text.indexOf('\n', end);
        if (lineEnd < 0) lineEnd = text.length();
        String line = text.substring(lineStart, lineEnd).replace('\t', ' ');
        return line.length() <= 240 ? line : line.substring(0, 237) + "...";
    }

    private void openSelectedMatch(boolean closeDialog) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) return;
        Match match = model.get(table.convertRowIndexToModel(selectedRow));
        if (match.end > match.editor.getTextArea().getDocument().getLength()) {
            search();
            status.setText("Search results were refreshed after the document changed");
            return;
        }

        StudioWindow studioWindow = match.editor.getStudioWindow();
        if (closeDialog) {
            accept();
        } else {
            keepOnTopOf(studioWindow);
        }

        studioWindow.toFront();

        match.editor.selectEditor();
        JTextArea textArea = match.editor.getTextArea();
        textArea.setCaretPosition(match.end);
        textArea.moveCaretPosition(match.start);
        try {
            textArea.scrollRectToVisible(textArea.modelToView(match.start));
        } catch (BadLocationException ignored) {
            // The document may have changed since the result was created.
        }
        textArea.requestFocusInWindow();
    }

    // We need to bring the studioWindow to front and only after that the dialog.
    // The dialog is brought to front after the studioWindow has fully got focus
    // (including the focus transfer to the text area). Calling toFront() earlier, e.g. from
    // windowActivated(), doesn't work on Windows: the subsequent focus transfer to the text area
    // re-activates the studioWindow natively, and it ends up above the dialog.
    private void keepOnTopOf(Window window) {
        clearPendingListener();
        if (window.isFocused()) {
            SwingUtilities.invokeLater(this::toFront);
            return;
        }

        pendingWindow = window;
        pendingListener = new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                clearPendingListener();
                SwingUtilities.invokeLater(() -> {
                    if (isVisible()) toFront();
                });
            }
        };
        window.addWindowFocusListener(pendingListener);
    }

    private void clearPendingListener() {
        if (pendingWindow != null) pendingWindow.removeWindowFocusListener(pendingListener);
        pendingWindow = null;
        pendingListener = null;
    }

    @Override
    public void dispose() {
        if (instance == this) instance = null;
        clearPendingListener();
        super.dispose();
    }

    private static class Match {
        private final EditorTab editor;
        private final int start;
        private final int end;
        private final int line;
        private final int column;
        private final String preview;

        private Match(EditorTab editor, int start, int end, int line, int column, String preview) {
            this.editor = editor;
            this.start = start;
            this.end = end;
            this.line = line;
            this.column = column;
            this.preview = preview;
        }
    }

    private static class MatchesTableModel extends AbstractTableModel {
        private final List<Match> matches = new ArrayList<>();
        private static final String[] COLUMNS = {"Tab", "Line", "Column", "Preview"};

        void clear() { matches.clear(); fireTableDataChanged(); }
        void add(Match match) { matches.add(match); fireTableRowsInserted(matches.size() - 1, matches.size() - 1); }
        Match get(int row) { return matches.get(row); }
        @Override public int getRowCount() { return matches.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Object getValueAt(int row, int column) {
            Match match = matches.get(row);
            if (column == 0) return match.editor.getTabTitle();
            if (column == 1) return match.line;
            if (column == 2) return match.column;
            return match.preview;
        }
    }
}
