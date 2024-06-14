package studio.ui;

import kx.KMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import studio.core.AuthenticationManager;
import studio.core.Studio;
import studio.kdb.*;
import studio.kdb.config.ActionOnExit;
import studio.ui.action.ConnectionStats;
import studio.ui.action.QPadImport;
import studio.ui.action.QueryResult;
import studio.ui.action.WorkspaceSaver;
import studio.ui.chart.Chart;
import studio.ui.dndtabbedpane.DragEvent;
import studio.ui.dndtabbedpane.DraggableTabbedPane;
import studio.ui.rstextarea.ConvertTabsToSpacesAction;
import studio.ui.rstextarea.FindReplaceAction;
import studio.ui.rstextarea.RSTextAreaFactory;
import studio.ui.search.SearchPanel;
import studio.ui.statusbar.MainStatusBar;
import studio.utils.BrowserLaunch;
import studio.utils.Content;
import studio.utils.HistoricalList;
import studio.utils.LineEnding;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static javax.swing.JSplitPane.VERTICAL_SPLIT;
import static studio.ui.EscapeDialog.DialogResult.ACCEPTED;
import static studio.ui.EscapeDialog.DialogResult.CANCELLED;

public class StudioWindow extends JFrame implements WindowListener {

    private static final Logger log = LogManager.getLogger();
    private static final Action editorUndoAction;
    private static final Action editorRedoAction;
    private static final Action editorCutAction;
    private static final Action editorCopyAction;
    private static final Action editorPasteAction;
    private static final Action editorSelectAllAction;
    private static final Action editorFindAction;
    private static final Action editorReplaceAction;
    private static final Action editorConvertTabsToSpacesAction;

    static {
        // Action name will be used for text in menu items. Kit's actions have internal names.
        // We will create new actions for menu/toolbar and use kit's actions as action itself.
        editorCopyAction = RSTextAreaFactory.getAction(RSTextAreaFactory.rstaCopyAsStyledTextAction);
        editorCutAction = RSTextAreaFactory.getAction(RSTextAreaFactory.rstaCutAsStyledTextAction);
        editorPasteAction = RSTextAreaFactory.getAction(RSyntaxTextAreaEditorKit.pasteAction);
        editorSelectAllAction = RSTextAreaFactory.getAction(RSyntaxTextAreaEditorKit.selectAllAction);
        editorUndoAction = RSTextAreaFactory.getAction(RSyntaxTextAreaEditorKit.rtaUndoAction);
        editorRedoAction = RSTextAreaFactory.getAction(RSyntaxTextAreaEditorKit.rtaRedoAction);
        editorFindAction = RSTextAreaFactory.getAction(FindReplaceAction.findAction);
        editorReplaceAction = RSTextAreaFactory.getAction(FindReplaceAction.replaceAction);
        editorConvertTabsToSpacesAction = RSTextAreaFactory.getAction(ConvertTabsToSpacesAction.action);
    }


    private boolean loading = true;

    private JComboBox<String> comboServer;
    private JComboBox<String> comboAuthMethod;
    private JTextField txtServer;
    private String lastQuery = null;
    private JToolBar toolbar;
    private EditorsPanel rootEditorsPanel;
    private EditorTab editor; // should be NotNull
    private JSplitPane splitpane;
    private JPanel topPanel;
    private MainStatusBar mainStatusBar;
    private DraggableTabbedPane tabbedPane;
    private SearchPanel editorSearchPanel;
    private SearchPanel resultSearchPanel;
    private ServerList serverList;

    private UserAction arrangeAllAction;
    private UserAction closeFileAction;
    private UserAction closeTabAction;
    private UserAction cleanAction;
    private UserAction openFileAction;
    private UserAction openInExcel;
    private UserAction codeKxComAction;
    private UserAction serverListAction;
    private UserAction serverHistoryAction;
    private UserAction newWindowAction;
    private UserAction newTabAction;
    private UserAction saveFileAction;
    private UserAction saveAllFilesAction;
    private UserAction saveAsFileAction;
    private UserAction exportAction;
    private UserAction chartAction;
    private Action undoAction;
    private Action redoAction;
    private Action cutAction;
    private Action copyAction;
    private Action pasteAction;
    private Action selectAllAction;
    private Action findAction;
    private Action replaceAction;
    private Action convertTabsToSpacesAction;
    private UserAction stopAction;
    private UserAction executeAction;
    private UserAction executeCurrentLineAction;
    private UserAction refreshAction;
    private UserAction aboutAction;
    private UserAction exitAction;
    private UserAction settingsAction;
    private UserAction toggleDividerOrientationAction;
    private UserAction minMaxDividerAction;
    private UserAction importFromQPadAction;
    private UserAction connectionStatsAction;
    private UserAction editServerAction;
    private UserAction addServerAction;
    private UserAction removeServerAction;
    private UserAction toggleCommaFormatAction;
    private UserAction findInResultAction;
    private UserAction nextEditorTabAction;
    private UserAction prevEditorTabAction;
    private UserAction[] lineEndingActions;
    private UserAction wordWrapAction;
    private UserAction splitEditorRight;
    private UserAction splitEditorDown;

    private static int studioWindowNameIndex = 0;
    private int editorTabbedPaneNameIndex = 0;
    private int editorNameIndex = 0;
    private int resultNameIndex = 0;


    private static List<StudioWindow> allWindows = new ArrayList<>();
    private static StudioWindow activeWindow = null;

    private final List<Server> serverHistory;

    public final static int menuShortcutKeyMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private final static Cursor textCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    private final static Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    private final static int MAX_SERVERS_TO_CLONE = 20;

    private static final Config CONFIG = Config.getInstance();
    
    public int nextEditorNameIndex() {
        return editorNameIndex++;
    }

    public int nextResultNameIndex() {
        return resultNameIndex++;
    }

    public int nextEditorTabbedPaneNameIndex() {
        return editorTabbedPaneNameIndex++;
    }

    private String getCaption() {
        StringBuilder caption = new StringBuilder();
        caption.append(editor.getTitle());
        if (editor.isModified()) caption.append(" (not saved)");

        Server server = editor.getServer();
        if (server != Server.NO_SERVER) {
            caption.append(" @ ");
            String fullName = server.getFullName();
            if (fullName.length()>0) caption.append(fullName);
            else caption.append(server.getHost()).append(":").append(server.getPort());
        }

        return caption.toString();
    }

    public void refreshFrameTitle() {
        StringBuilder frameTitleBuilder = new StringBuilder(getCaption());
        frameTitleBuilder.append(" ");

        frameTitleBuilder.append("Studio for kdb+ ").append(Lm.version);

        String env = EnvConfig.getEnvironment();
        if (env != null) frameTitleBuilder.append(" [").append(env).append("]");

        String frameTitle = frameTitleBuilder.toString();

        if (frameTitle.equals(getTitle())) return;

        setTitle(frameTitle);
        refreshAllMenus();
    }

    private void setActionsEnabled(boolean value, Action... actions) {
        for (Action action: actions) {
            if (action != null) {
                action.setEnabled(value);
            }
        }
    }

    public boolean isQueryRunning() {
        return editor.getQueryExecutor().running();
    }

    public void refreshActionState() {
        RSyntaxTextArea textArea = editor.getTextArea();
        Server server = editor.getServer();
        editServerAction.setEnabled(server != Server.NO_SERVER);
        removeServerAction.setEnabled(server != Server.NO_SERVER);

        undoAction.setEnabled(textArea.canUndo());
        redoAction.setEnabled(textArea.canRedo());

        wordWrapAction.setSelected(CONFIG.getBoolean(Config.RSTA_WORD_WRAP));

        for (LineEnding lineEnding: LineEnding.values() ) {
            lineEndingActions[lineEnding.ordinal()].setSelected(editor.getLineEnding() == lineEnding);
        }

        boolean queryRunning = isQueryRunning();
        stopAction.setEnabled(queryRunning);
        executeAction.setEnabled(!queryRunning);
        executeCurrentLineAction.setEnabled(!queryRunning);
        refreshAction.setEnabled(lastQuery != null && !queryRunning);

        TabPanel tab = getSelectedResultPane();
        if (tab == null) {
            setActionsEnabled(false, exportAction, chartAction, openInExcel, refreshAction);
        } else {
            exportAction.setEnabled(tab.isTable());
            chartAction.setEnabled(tab.getType() == TabPanel.ResultType.TABLE);
            openInExcel.setEnabled(tab.isTable());
            refreshAction.setEnabled(true);
            tab.refreshActionState();
        }
    }

    private void exportAsExcel(final String filename) {
        ExcelExporter.exportTableX(this,getSelectedResultPane(),new File(filename),false);
    }

    private void exportAsDelimited(final TableModel model,final String filename,final char delimiter) {
        UIManager.put("ProgressMonitor.progressText","Studio for kdb+");
        final ProgressMonitor pm = new ProgressMonitor(this,"Exporting data to " + filename,
                "0% complete",0,100);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = () -> {
            if (filename != null) {
                String lineSeparator = System.getProperty("line.separator");

                try (BufferedWriter fw = new BufferedWriter(new FileWriter(filename))) {
                    for(int col = 0; col < model.getColumnCount(); col++) {
                        if (col > 0)
                            fw.write(delimiter);
                        fw.write(model.getColumnName(col));
                    }
                    fw.write(lineSeparator);
                    int maxRow = model.getRowCount();
                    for(int r = 1; r <= maxRow; r++) {
                        for (int col = 0;col < model.getColumnCount();col++) {
                            if (col > 0) fw.write(delimiter);

                            K.KBase o = (K.KBase) model.getValueAt(r - 1,col);
                            if (!o.isNull())
                                fw.write(o.toString(KFormatContext.NO_TYPE));
                        }
                        fw.write(lineSeparator);
                        if (pm.isCanceled()) break;
                        int progress = (100 * r) / maxRow;
                        String note = "" + progress + "% complete";
                        SwingUtilities.invokeLater( () -> {
                            pm.setProgress(progress);
                            pm.setNote(note);
                        } );
                    }

                }
                catch (IOException e) {
                    log.error("Error in writing to file {}", filename, e);
                }
                finally {
                    pm.close();
                }
            }
        };

        Thread t = new Thread(runner,"Exporter");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void exportAsXml(final TableModel model,final String filename) {
        UIManager.put("ProgressMonitor.progressText","Studio for kdb+");
        final ProgressMonitor pm = new ProgressMonitor(this,"Exporting data to " + filename,
                "0% complete",0,100);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = () -> {
            if (filename != null) {
                String lineSeparator = System.getProperty("line.separator");;

                try (BufferedWriter fw = new BufferedWriter(new FileWriter(filename))) {
                    fw.write("<R>");
                    int maxRow = model.getRowCount();
                    fw.write(lineSeparator);

                    String[] columns = new String[model.getColumnCount()];
                    for (int col = 0; col < model.getColumnCount(); col++)
                        columns[col] = model.getColumnName(col);

                    for (int r = 1; r <= maxRow; r++) {
                        fw.write("<r>");
                        for (int col = 0; col < columns.length; col++) {
                            fw.write("<" + columns[col] + ">");

                            K.KBase o = (K.KBase) model.getValueAt(r - 1,col);
                            if (!o.isNull())
                                fw.write(o.toString(KFormatContext.NO_TYPE));

                            fw.write("</" + columns[col] + ">");
                        }
                        fw.write("</r>");
                        fw.write(lineSeparator);

                        if (pm.isCanceled()) break;
                        int progress = (100 * r) / maxRow;
                        String note = "" + progress + "% complete";
                        SwingUtilities.invokeLater(() -> {
                            pm.setProgress(progress);
                            pm.setNote(note);
                        });
                    }
                    fw.write("</R>");
                }
                catch (IOException e) {
                    log.error("Error in writing to file {}", filename, e);
                }
                finally {
                    pm.close();
                }
            }
        };

        Thread t = new Thread(runner, "Exporter");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void exportAsTxt(String filename) {
        exportAsDelimited(getSelectedTable().getModel(),filename,'\t');
    }

    private void exportAsCSV(String filename) {
        exportAsDelimited(getSelectedTable().getModel(),filename,',');
    }

    private void export() {
        if (getSelectedTable() == null) return;

        File file = FileChooser.chooseFile(this, Config.EXPORT_FILE_CHOOSER, JFileChooser.SAVE_DIALOG, "Export result set as",
                null,
                new FileNameExtensionFilter("csv (Comma delimited)", "csv"),
                new FileNameExtensionFilter("txt (Tab delimited)", "txt"),
                new FileNameExtensionFilter("xml", "xml"),
                new FileNameExtensionFilter("xls (Microsoft Excel)", "xls"));

        if (file == null) return;

        try {
            String filename = file.getAbsolutePath();

            if (filename.endsWith(".xls"))
                exportAsExcel(filename);
            else if (filename.endsWith(".csv"))
                exportAsCSV(filename);
            else if (filename.endsWith(".txt"))
                exportAsTxt(filename);
            else if (filename.endsWith(".xml"))
                exportAsXml(getSelectedTable().getModel(),filename);
            else
                StudioOptionPane.showWarning(this,
                        "You did not specify what format to export the file as.\n Cancelling data export",
                        "Warning");
        } catch (Exception e) {
            StudioOptionPane.showError(this,
                    "Error",
                    "An error occurred whilst writing the export file.\n Details are: " + e.getMessage());
        }
    }

    public void newFile() {
        if (!EditorsPanel.checkAndSaveTab(editor)) return;

        editor.loadFile(null);
    }

    private void openFile() {
        File file = FileChooser.chooseFile(this, Config.OPEN_FILE_CHOOSER, JFileChooser.OPEN_DIALOG, null, null,
                new FileNameExtensionFilter("q script", "q"));

        if (file == null) return;
        String filename = file.getAbsolutePath();
        addTab(filename);
        addToMruFiles(filename);
    }

    public void loadMRUFile(String filename) {
        if (!EditorsPanel.checkAndSaveTab(editor)) return;

        editor.loadFile(filename);
        addToMruFiles(filename);
        EditorsPanel.refreshEditorTitle(editor);
        refreshAllMenus();
    }

    public void addToMruFiles(String filename) {
        if (filename == null)
            return;

        Vector v = new Vector();
        v.add(filename);
        String[] mru = CONFIG.getMRUFiles();
        for (int i = 0;i < mru.length;i++)
            if (!v.contains(mru[i]))
                v.add(mru[i]);
        CONFIG.saveMRUFiles((String[]) v.toArray(new String[0]));
        refreshAllMenus();
    }

    public static void executeAll(EditorsPanel.EditorTabAction action) {
        for (StudioWindow studioWindow: allWindows) {
            studioWindow.execute(action);
        }
    }

    public boolean execute(EditorsPanel.EditorTabAction action) {
        return rootEditorsPanel.execute(action);
    }


    private static void saveAll() {
        executeAll(editorTab -> editorTab.saveFileOnDisk(false));
    }

    private void arrangeAll() {
        int noWins = allWindows.size();

        Iterator<StudioWindow> windowIterator = allWindows.iterator();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        int noRows = Math.min(noWins, 3);
        int height = screenSize.height / noRows;

        for (int row = 0;row < noRows;row++) {
            int noCols = (noWins / 3);

            if ((row == 0) && ((noWins % 3) > 0))
                noCols++;
            else if ((row == 1) && ((noWins % 3) > 1))
                noCols++;

            int width = screenSize.width / noCols;

            for (int col = 0;col < noCols;col++) {
                StudioWindow window = windowIterator.next();

                window.setSize(width,height);
                window.setLocation(col * width,((noRows - 1) - row) * height);
                ensureDeiconified(window);
            }
        }
    }

    public void setServer(Server server) {
        editor.setServer(server);
        if (!loading) {
            CONFIG.addServerToHistory(server);
            serverHistory.add(server);

            EditorsPanel.refreshEditorTitle(editor);
            refreshServer();
        }
    }

    private void initActions() {
        cleanAction = UserAction.create("Clean", Util.NEW_DOCUMENT_ICON, "Clean editor script", KeyEvent.VK_N,
                null, e -> newFile());

        arrangeAllAction = UserAction.create(I18n.getString("ArrangeAll"),  "Arrange all windows on screen",
                KeyEvent.VK_A, null, e -> arrangeAll());

        minMaxDividerAction = UserAction.create(I18n.getString("MaximizeEditorPane"), "Maximize editor pane",
                KeyEvent.VK_M, KeyStroke.getKeyStroke(KeyEvent.VK_M, menuShortcutKeyMask),
                e -> minMaxDivider());

        toggleDividerOrientationAction = UserAction.create(I18n.getString("ToggleDividerOrientation"),
                "Toggle the window divider's orientation", KeyEvent.VK_C, null, e -> toggleDividerOrientation());

        closeTabAction = UserAction.create("Close Tab",  "Close current tab", KeyEvent.VK_W,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutKeyMask), e -> editor.getEditorsPanel().closeTab(editor));

        closeFileAction = UserAction.create("Close Window",  "Close current window (close all tabs)",
                KeyEvent.VK_C, null, e -> close());

        openFileAction = UserAction.create(I18n.getString("Open"), Util.FOLDER_ICON, "Open a script", KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutKeyMask), e -> openFile());

        newWindowAction = UserAction.create(I18n.getString("NewWindow"),  "Open a new window",
                KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> new StudioWindow(editor.getServer(), null) );

        newTabAction = UserAction.create("New Tab",  "Open a new tab", KeyEvent.VK_T,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask),
                e -> addTab(null));

        serverListAction = UserAction.create(I18n.getString("ServerList"), Util.TEXT_TREE_ICON, "Show server list",
                KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_L, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> showServerList(false));

        serverHistoryAction = UserAction.create("Server History", "Recent selected servers", KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_R, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> showServerList(true));

        importFromQPadAction = UserAction.create("Import Servers from QPad...", "Import from Servers.cfg",
                KeyEvent.VK_I, null, e -> QPadImport.doImport(this));

        connectionStatsAction = UserAction.create("Get Connection Statistics", "Details about all connections for all opened tabs",
                KeyEvent.VK_S, null, e -> ConnectionStats.getStats(this));


        editServerAction = UserAction.create(I18n.getString("Edit"), Util.SERVER_INFORMATION_ICON, "Edit the server details",
                KeyEvent.VK_E, null, e -> {
                    EditServerForm f = new EditServerForm(this, editor.getServer());
                    f.alignAndShow();
                    if (f.getResult() == ACCEPTED) {
                        if (stopAction.isEnabled())
                            stopAction.actionPerformed(e);

                        Server newServer = f.getServer();
                        CONFIG.replaceServer(editor.getServer(), newServer);
                        setServer(newServer);
                        refreshAll();
                    }
                });


        addServerAction = UserAction.create(I18n.getString("Add"), Util.ADD_SERVER_ICON, "Configure a new server",
                KeyEvent.VK_A, null, e -> {
                    AddServerForm f = new AddServerForm(this, editor.getServer());
                    f.alignAndShow();
                    if (f.getResult() == ACCEPTED) {
                        Server s = f.getServer();
                        CONFIG.addServer(s);
                        setServer(s);
                        refreshAll();
                    }
                });

        removeServerAction = UserAction.create(I18n.getString("Remove"), Util.DELETE_SERVER_ICON, "Remove this server",
                KeyEvent.VK_R, null, e -> {
                    int choice = JOptionPane.showOptionDialog(this,
                            "Remove server " + editor.getServer().getFullName() + " from list?",
                            "Remove server?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            Util.QUESTION_ICON,
                            null, // use standard button titles
                            null);      // no default selection

                    if (choice == 0) {
                        CONFIG.removeServer(editor.getServer());

                        Server[] servers = CONFIG.getServers();

                        if (servers.length > 0)
                            setServer(servers[0]);

                        refreshAll();
                    }
                });


        saveFileAction = UserAction.create(I18n.getString("Save"), Util.DISKS_ICON, "Save the script",
                KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutKeyMask),
                e -> EditorsPanel.saveEditor(editor));

        saveAllFilesAction = UserAction.create("Save All...",  "Save all files",
                KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> saveAll());

        saveAsFileAction = UserAction.create(I18n.getString("SaveAs"), Util.SAVE_AS_ICON, "Save script as",
                KeyEvent.VK_A, null, e -> EditorsPanel.saveAsFile(editor));

        exportAction = UserAction.create(I18n.getString("Export"), Util.EXPORT_ICON, "Export result set",
                KeyEvent.VK_E, null, e -> export());

        chartAction = UserAction.create(I18n.getString("Chart"), Util.CHART_ICON, "Chart current data set",
                KeyEvent.VK_E, null, e -> new Chart((KTableModel) getSelectedTable().getModel()));

        stopAction = UserAction.create(I18n.getString("Stop"), Util.STOP_ICON, "Stop the query",
                KeyEvent.VK_S, null, e -> editor.getQueryExecutor().cancel());

        openInExcel = UserAction.create(I18n.getString("OpenInExcel"), Util.EXCEL_ICON, "Open in Excel",
                KeyEvent.VK_O, null, e -> {
                    try {
                        File file = File.createTempFile("studioExport", ".xlsx");
                        ExcelExporter.exportTableX(this, getSelectedResultPane(), file, true);
                    } catch (IOException ex) {
                        log.error("Failed to create temporary file", ex);
                        StudioOptionPane.showError(this, "Failed to Open in Excel " + ex.getMessage(),"Error");
                    }
                });

        executeAction = UserAction.create(I18n.getString("Execute"), Util.TABLE_SQL_RUN_ICON, "Execute the full or highlighted text as a query",
                KeyEvent.VK_E, KeyStroke.getKeyStroke(KeyEvent.VK_E, menuShortcutKeyMask), e -> executeQuery());

        executeCurrentLineAction = UserAction.create(I18n.getString("ExecuteCurrentLine"), Util.RUN_ICON, "Execute the current line as a query",
                KeyEvent.VK_ENTER, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuShortcutKeyMask), e -> executeQueryCurrentLine());

        refreshAction = UserAction.create(I18n.getString("Refresh"), Util.REFRESH_ICON, "Refresh the result set",
                KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuShortcutKeyMask | InputEvent.SHIFT_MASK), e -> refreshQuery());

        toggleCommaFormatAction = UserAction.create("Toggle Comma Format", Util. COMMA_ICON, "Add/remove thousands separator in selected result",
                KeyEvent.VK_J, KeyStroke.getKeyStroke(KeyEvent.VK_J, menuShortcutKeyMask),
                e -> {
                    TabPanel tab = getSelectedResultPane();
                    if (tab != null) tab.toggleCommaFormatting();
                });

        findInResultAction = UserAction.create("Find in Result", Util.FIND_ICON, "Find in result tab",
                KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> resultSearchPanel.setVisible(true) );

        aboutAction = UserAction.create(I18n.getString("About"), Util.ABOUT_ICON, "About Studio for kdb+",
                KeyEvent.VK_E, null, e -> about());

        exitAction = UserAction.create(I18n.getString("Exit"), "Close this window",
                KeyEvent.VK_X, e -> quit());

        settingsAction = UserAction.create("Settings",  "Settings",
                KeyEvent.VK_S, null, e -> settings());

        codeKxComAction = UserAction.create("code.kx.com", Util.TEXT_ICON, "Open code.kx.com",
                KeyEvent.VK_C, null, e -> {
                    try {
                        BrowserLaunch.openURL("http://code.kx.com/q/");
                    } catch (Exception ex) {
                        StudioOptionPane.showError("Error attempting to launch web browser:\n" + ex.getLocalizedMessage(), "Error");
                    }
                });

        copyAction = UserAction.create(I18n.getString("Copy"), Util.COPY_ICON, "Copy the selected text to the clipboard",
                KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C,menuShortcutKeyMask), editorCopyAction);

        cutAction = UserAction.create(I18n.getString("Cut"), Util.CUT_ICON, "Cut the selected text",
                KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_X,menuShortcutKeyMask), editorCutAction);

        pasteAction = UserAction.create(I18n.getString("Paste"), Util.PASTE_ICON, "Paste text from the clipboard",
                KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_V,menuShortcutKeyMask), editorPasteAction);

        findAction = UserAction.create(I18n.getString("Find"), Util.FIND_ICON, "Find text in the document",
                KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F,menuShortcutKeyMask), editorFindAction);

        replaceAction = UserAction.create(I18n.getString("Replace"), Util.REPLACE_ICON, "Replace text in the document",
                KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_R,menuShortcutKeyMask), editorReplaceAction);

        convertTabsToSpacesAction = UserAction.create("Convert tabs to spaces", editorConvertTabsToSpacesAction);

        selectAllAction = UserAction.create(I18n.getString("SelectAll"), "Select all text in the document",
                KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_A,menuShortcutKeyMask), editorSelectAllAction);

        undoAction = UserAction.create(I18n.getString("Undo"), Util.UNDO_ICON, "Undo the last change to the document",
                KeyEvent.VK_U, KeyStroke.getKeyStroke(KeyEvent.VK_Z,menuShortcutKeyMask), editorUndoAction);

        redoAction = UserAction.create(I18n.getString("Redo"), Util.REDO_ICON, "Redo the last change to the document",
                KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_Y,menuShortcutKeyMask), editorRedoAction);

        nextEditorTabAction = UserAction.create("Next tab",
                "Select next editor tab", KeyEvent.VK_N,
                    Util.MAC_OS_X ? KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, menuShortcutKeyMask | InputEvent.ALT_MASK ) :
                                    KeyStroke.getKeyStroke(KeyEvent.VK_TAB, menuShortcutKeyMask),
                e -> editor.getEditorsPanel().selectNextTab(true));

        prevEditorTabAction = UserAction.create("Previous tab",
                "Select previous editor tab", KeyEvent.VK_P,
                Util.MAC_OS_X ? KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, menuShortcutKeyMask | InputEvent.ALT_MASK ) :
                        KeyStroke.getKeyStroke(KeyEvent.VK_TAB, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> editor.getEditorsPanel().selectNextTab(false));

        lineEndingActions = new UserAction[LineEnding.values().length];
        for(LineEnding lineEnding: LineEnding.values()) {
             UserAction action = UserAction.create(lineEnding.getDescription(),
                e -> {
                    editor.setLineEnding(lineEnding);
                    refreshActionState();
                } );
             lineEndingActions[lineEnding.ordinal()] = action;
        }

        wordWrapAction = UserAction.create("Word wrap",  "Word wrap for all tabs",
                KeyEvent.VK_W, KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> toggleWordWrap());

        splitEditorRight = UserAction.create("Split right",  "Split vertically",
                KeyEvent.VK_R, null,
                e-> editor.getEditorsPanel().split(false));
        splitEditorDown = UserAction.create("Split down",  "Split horizontally",
                KeyEvent.VK_D, null,
                e-> editor.getEditorsPanel().split(true));
    }

    public UserAction getSplitAction(boolean vertically) {
        return vertically ? splitEditorDown : splitEditorRight;
    }

    public static void settings() {
        SettingsDialog dialog = new SettingsDialog(activeWindow);
        dialog.alignAndShow();
        if (dialog.getResult() == CANCELLED) return;

        dialog.saveSettings();
    }

    private void toggleWordWrap() {
        boolean value = CONFIG.getBoolean(Config.RSTA_WORD_WRAP);
        CONFIG.setBoolean(Config.RSTA_WORD_WRAP, !value);
        refreshEditorsSettings();
        refreshActionState();
    }

    public static void refreshServerSettingsForAllWindows() {
        for (StudioWindow window: allWindows) {
            window.refreshServer();
        }
    }

    public static void refreshEditorsSettings() {
        executeAll(editorTab -> {
                RSyntaxTextArea editor = editorTab.getTextArea();
                editor.setHighlightCurrentLine(CONFIG.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));
                editor.setAnimateBracketMatching(CONFIG.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));
                editor.setLineWrap(CONFIG.getBoolean(Config.RSTA_WORD_WRAP));
                editor.setFont(CONFIG.getFont(Config.FONT_EDITOR));

                editor.setTabSize(CONFIG.getInt(Config.EDITOR_TAB_SIZE));
                editor.setTabsEmulated(CONFIG.getBoolean(Config.EDITOR_TAB_EMULATED));

                editor.setInsertPairedCharacters(CONFIG.getBoolean(Config.RSTA_INSERT_PAIRED_CHAR));
                return true;
            });
    }

    public static void refreshResultSettings() {
        long doubleClickTimeout = CONFIG.getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT);
        for (StudioWindow window: allWindows) {
            int count = window.tabbedPane.getTabCount();
            for (int index=0; index<count; index++) {
                TabPanel tabPanel = window.getResultPane(index);
                tabPanel.setDoubleClickTimeout(doubleClickTimeout);
                tabPanel.refreshFont();
            }
        }
    }

    public static StudioWindow[] getAllStudioWindows() {
        return allWindows.toArray(new StudioWindow[0]);
    }

    public static void about() {
        HelpDialog help = new HelpDialog(activeWindow);
        Util.centerChildOnParent(help, activeWindow);
        // help.setTitle("About Studio for kdb+");
        help.pack();
        help.setVisible(true);
    }

    public static void quit() {
        WorkspaceSaver.setEnabled(false);
        try {
            ActionOnExit action = CONFIG.getEnum(Config.ACTION_ON_EXIT);
            if (action != ActionOnExit.NOTHING) {
                for (StudioWindow window : allWindows.toArray(new StudioWindow[0])) {
                    window.toFront();
                    boolean complete = window.execute(editorTab -> {
                        if (editorTab.isModified()) {
                            if (!EditorsPanel.checkAndSaveTab(editorTab)) {
                                return false;
                            }

                            if (editorTab.isModified()) {
                                if (action == ActionOnExit.CLOSE_ANONYMOUS_NOT_SAVED && editorTab.getFilename()==null) {
                                    editorTab.getEditorsPanel().closeTab(editorTab);
                                }
                            }
                        }
                        return true;
                    });
                    if (!complete) return;
                }
            }
        } finally {
            if (allWindows.size() > 0) {
                activeWindow.toFront();
            }
            WorkspaceSaver.setEnabled(true);
        }
        WorkspaceSaver.save(getWorkspace());
        CONFIG.exit();
    }

    public void close() {
        // If this is the last window, we need to properly persist workspace
        if (allWindows.size() == 1) {
            quit();
        } else {
            boolean result = execute(editorTab -> editorTab.getEditorsPanel().closeTab(editorTab));
            if (!result) return;
            //  closing the last tab would trigger this code again
            if (allWindows.contains(this)) {
                allWindows.remove(this);
                dispose();
                refreshAllMenus();
            }
        }

    }

    public static void refreshAll() {
        for (StudioWindow window: allWindows) {
            window.refreshMenu();
            window.refreshServerList();
        }
    }

    private void addToMenu(JMenu menu, Action... actions) {
        for (Action action: actions) {
            if (action == null) {
                menu.addSeparator();
            } else {
                if (action.getValue(Action.SMALL_ICON) == null) {
                    action.putValue(Action.SMALL_ICON, Util.BLANK_ICON);
                }
                menu.add(action);
            }
        }
    }

    private void setNamesInMenu(JMenu menu) {
        menu.setName(menu.getText());
        for (int index = 0; index < menu.getMenuComponentCount(); index++) {
            Component c = menu.getMenuComponent(index);
            if (c instanceof JMenuItem) {
                JMenuItem menuItem = (JMenuItem) c;
                menuItem.setName(menuItem.getText());
            }
            if (c instanceof JMenu) {
                setNamesInMenu((JMenu) c);
            }
        }
    }

    private void setNamesInMenu(JMenuBar menuBar) {
        for (int index = 0; index < menuBar.getMenuCount(); index++) {
            setNamesInMenu(menuBar.getMenu(index));
        }
    }

    private static void refreshAllMenus() {
        for(StudioWindow window: allWindows) {
            window.refreshMenu();
        }
    }

    private JMenu openMRUMenu, cloneMenu, windowMenu;
    private int windowMenuWindowIndex;

    private void refreshMenu() {
        openMRUMenu.removeAll();

        String[] mru = CONFIG.getMRUFiles();
        String mnems = "123456789";
        for (int i = 0; i < mru.length; i++) {
            final String filename = mru[i];

            JMenuItem item = new JMenuItem("" + (i + 1) + " " + filename);
            if (i<mnems.length()) {
                item.setMnemonic(mnems.charAt(i));
            }
            item.setIcon(Util.BLANK_ICON);
            item.addActionListener(e -> loadMRUFile(filename));
            openMRUMenu.add(item);
        }

        cloneMenu.removeAll();
        Server[] servers = CONFIG.getServers();
        int count = Math.min(MAX_SERVERS_TO_CLONE, servers.length);
        for (int i = 0; i < count; i++) {
            final Server s = servers[i];
            JMenuItem item = new JMenuItem(s.getFullName());
            item.addActionListener(e -> {
                Server clone = s.newName("Clone of " + s.getName()) ;
                EditServerForm f = new EditServerForm(StudioWindow.this,clone);
                f.alignAndShow();

                if (f.getResult() == ACCEPTED) {
                    clone = f.getServer();
                    CONFIG.addServer(clone);
                    setServer(clone);
                    refreshAll();
                }
            });

            cloneMenu.add(item);
        }


        for (int index=windowMenu.getMenuComponentCount()-1; index>=windowMenuWindowIndex; index--) {
            windowMenu.remove(index);
        }

        count = allWindows.size();
        UserAction[] windowMenuActions = new UserAction[count];
        if (count > 0) {
            windowMenu.addSeparator();

            for (int index = 0; index < count; index++) {
                StudioWindow window = allWindows.get(index);
                windowMenuActions[index] = UserAction.create("" + (index + 1) + " " + window.getCaption(),
                        window == this ? Util.CHECK_ICON : Util.BLANK_ICON, "", 0 , null,
                        e -> ensureDeiconified(window));
            }

            addToMenu(windowMenu, windowMenuActions);
        }
    }

    private void createMenuBar() {
        openMRUMenu = new JMenu("Open Recent");
        openMRUMenu.setIcon(Util.BLANK_ICON);

        cloneMenu = new JMenu(I18n.getString("Clone"));
        cloneMenu.setIcon(Util.DATA_COPY_ICON);

        windowMenu = new JMenu(I18n.getString("Window"));
        windowMenu.setMnemonic(KeyEvent.VK_W);


        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu(I18n.getString("File"));
        menu.setMnemonic(KeyEvent.VK_F);

        addToMenu(menu, newWindowAction, newTabAction, openFileAction);
        menu.add(openMRUMenu);
        addToMenu(menu,saveFileAction, saveAsFileAction, saveAllFilesAction, closeTabAction, closeFileAction, null);

        if (!Studio.hasMacOSSystemMenu()) {
            addToMenu(menu, settingsAction);
        }

        addToMenu(menu, openInExcel, exportAction, chartAction);

        if (!Studio.hasMacOSSystemMenu()) {
            addToMenu(menu, null, exitAction);
        }
        menubar.add(menu);


        menu = new JMenu(I18n.getString("Edit"));
        menu.setMnemonic(KeyEvent.VK_E);

        addToMenu(menu, undoAction, redoAction, null, cutAction, copyAction, pasteAction, null);

        menu.add(new JCheckBoxMenuItem(wordWrapAction));

        JMenu lineEndingSubMenu = new JMenu("Line Ending");
        lineEndingSubMenu.setIcon(Util.BLANK_ICON);
        for (Action action: lineEndingActions) {
            lineEndingSubMenu.add(new JCheckBoxMenuItem(action));
        }
        menu.add(lineEndingSubMenu);

        addToMenu(menu, cleanAction, selectAllAction, null, findAction, replaceAction, convertTabsToSpacesAction);
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Server"));
        menu.setMnemonic(KeyEvent.VK_S);

        addToMenu(menu, addServerAction, editServerAction, removeServerAction);
        menu.add(cloneMenu);

        addToMenu(menu, null, serverListAction, serverHistoryAction, importFromQPadAction, null, connectionStatsAction);

        menubar.add(menu);

        menu = new JMenu(I18n.getString("Query"));
        menu.setMnemonic(KeyEvent.VK_Q);

        addToMenu(menu, executeCurrentLineAction, executeAction, stopAction, refreshAction,
                toggleCommaFormatAction, findInResultAction);
        menubar.add(menu);

        //Window menu
        addToMenu(windowMenu, splitEditorRight, splitEditorDown, null, minMaxDividerAction, toggleDividerOrientationAction,
                arrangeAllAction, nextEditorTabAction, prevEditorTabAction);

        windowMenuWindowIndex = windowMenu.getMenuComponentCount();
        menubar.add(windowMenu);

        menu = new JMenu(I18n.getString("Help"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(new JMenuItem(codeKxComAction));
        if (!Studio.hasMacOSSystemMenu())
            menu.add(new JMenuItem(aboutAction));
        menubar.add(menu);

        setNamesInMenu(menubar);

        setJMenuBar(menubar);
    }

    private void ensureDeiconified(JFrame f) {
        int state = f.getExtendedState();
        state = state & ~Frame.ICONIFIED;
        f.setExtendedState(state);
        f.show();
    }

    private void selectConnectionString() {
        String connection = txtServer.getText().trim();
        if (connection.length() == 0) return;
        Server server = editor.getServer();
        if (server != Server.NO_SERVER && server.getConnectionString().equals(connection)) return;

        try {
            setServer(CONFIG.getServerByConnectionString(connection));
            refreshServer();
        } catch (IllegalArgumentException e) {
            refreshConnectionText();
        }
    }

    public ServerList getServerList() {
        return serverList;
    }

    private void showServerList(boolean selectHistory) {
        Server selectedServer = serverList.showServerTree(editor.getServer(), serverHistory, selectHistory);

        if (selectedServer == null || selectedServer.equals(editor.getServer())) return;

        setServer(selectedServer);

        refreshAllMenus();
        refreshServerListAllWindows();
    }


    private void selectAuthMethod() {
        Object selectedItem = comboAuthMethod.getSelectedItem();
        String newAuthMethod =  selectedItem == null ? "" : selectedItem.toString();
        if (editor.getServer().getAuthenticationMechanism().equals(newAuthMethod)) return;

        Server server = CONFIG.getServerByConnectionString(txtServer.getText().trim(), newAuthMethod);
        setServer(server);
        refreshServer();

    }

    private void selectServerName() {
        String selection = comboServer.getSelectedItem().toString();
        if(! CONFIG.getServerNames().contains(selection)) return;

        setServer(CONFIG.getServer(selection));
        refreshServer();
    }

    private void refreshConnectionText() {
        Server server = editor.getServer();
        if (server == Server.NO_SERVER) {
            txtServer.setText("");
            txtServer.setToolTipText("Select connection details");
        } else {
            txtServer.setText(server.getConnectionString());
            txtServer.setToolTipText(server.getConnectionStringWithPwd());
        }
    }

    public static void refreshComboServerVisibility() {
        for (StudioWindow window: allWindows) {
            window.comboServer.setVisible(CONFIG.getBoolean(Config.SHOW_SERVER_COMBOBOX));
        }
    }

    public static void refreshServerListAllWindows() {
        for (StudioWindow window: allWindows) {
            window.refreshServerList();
        }
    }

    private void refreshServerList() {
        Server server = editor.getServer();
        Collection<String> names = CONFIG.getServerNames();
        String name = server == Server.NO_SERVER ? "" : server.getFullName();
        if (!names.contains(name)) {
            List<String> newNames = new ArrayList<>();
            newNames.add(name);
            newNames.addAll(names);
            names = newNames;
        }
        comboServer.setModel(new DefaultComboBoxModel<>(names.toArray(new String[0])));

        comboServer.setSelectedItem(name);
    }


    private void refreshServer() {
        Server server = editor.getServer();
        String name = server == Server.NO_SERVER ? "" : server.getFullName();
        comboServer.setSelectedItem(name);
        comboAuthMethod.getModel().setSelectedItem(server.getAuthenticationMechanism());

        refreshConnectionText();
        refreshActionState();
    }


    private void initToolbar() {
        comboServer = new JComboBox<>();
        comboServer.setVisible(CONFIG.getBoolean(Config.SHOW_SERVER_COMBOBOX));
        comboServer.setName("serverDropDown");
        comboServer.setToolTipText("Select the server context");
        comboServer.addActionListener(e->selectServerName());
        // Cut the width if it is too wide.
        comboServer.setMinimumSize(new Dimension(0, 0));

        comboAuthMethod = new JComboBox<>(AuthenticationManager.getInstance().getAuthenticationMechanisms());
        comboAuthMethod.addActionListener(e->selectAuthMethod());

        txtServer = new JTextField(32);
        txtServer.setName("serverEntryTextField");
        txtServer.addActionListener(e -> {
            selectConnectionString();
            editor.getTextArea().requestFocus();
        });
        txtServer.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                selectConnectionString();
            }
        });

        refreshServerList();
        refreshServer();

        toolbar.add(Box.createRigidArea(new Dimension(3,0)));
        toolbar.add(new JLabel(I18n.getString("Server")));
        toolbar.add(comboServer);
        toolbar.add(comboAuthMethod);
        toolbar.add(txtServer);

        Action[] actions = new Action[] {
                serverListAction, null,
                stopAction, executeAction, refreshAction, null, openFileAction, saveFileAction, saveAsFileAction, null,
                openInExcel, null, exportAction, null, chartAction, null, undoAction, redoAction, null,
                cutAction, copyAction, pasteAction, null, findAction, replaceAction, null, codeKxComAction };

        for (Action action: actions) {
            if (action == null) {
                toolbar.addSeparator();
            } else {
                JButton button = toolbar.add(action);
                button.setFocusable(false);
                button.setMnemonic(KeyEvent.VK_UNDEFINED);

                String name = (String) action.getValue(Action.NAME);
                button.setName("toolbar" + name);
            }
        }

        refreshActionState();
    }

    private int dividerLastPosition; // updated from property change listener
    private void minMaxDivider(){
        //BasicSplitPaneDivider divider = ((BasicSplitPaneUI)splitpane.getUI()).getDivider();
        //((JButton)divider.getComponent(0)).doClick();
        //((JButton)divider.getComponent(1)).doClick();
        if(splitpane.getDividerLocation()>=splitpane.getMaximumDividerLocation()){
            // Minimize editor pane
            splitpane.getTopComponent().setMinimumSize(new Dimension());
            splitpane.getBottomComponent().setMinimumSize(null);
            splitpane.setDividerLocation(0.);
            splitpane.setResizeWeight(0.);
        }
        else if(splitpane.getDividerLocation()<=splitpane.getMinimumDividerLocation()){
            // Restore editor pane
            splitpane.getTopComponent().setMinimumSize(null);
            splitpane.getBottomComponent().setMinimumSize(null);
            splitpane.setResizeWeight(0.);
            // Could probably catch resize edge-cases etc in pce too
            if(dividerLastPosition>=splitpane.getMaximumDividerLocation()||dividerLastPosition<=splitpane.getMinimumDividerLocation())
                dividerLastPosition=splitpane.getMaximumDividerLocation()/2;
            splitpane.setDividerLocation(dividerLastPosition);
        }
        else{
            // Maximize editor pane
            splitpane.getBottomComponent().setMinimumSize(new Dimension());
            splitpane.getTopComponent().setMinimumSize(null);
            splitpane.setDividerLocation(splitpane.getOrientation()==VERTICAL_SPLIT?splitpane.getHeight()-splitpane.getDividerSize():splitpane.getWidth()-splitpane.getDividerSize());
            splitpane.setResizeWeight(1.);
        }
    }

    private void toggleDividerOrientation() {
        if (splitpane.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
            splitpane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            tabbedPane.setTabPlacement(JTabbedPane.LEFT);
        } else {
            splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            tabbedPane.setTabPlacement(JTabbedPane.TOP);
        }

        int count = tabbedPane.getTabCount();
        for (int index = 0; index<count; index++) {
            ((TabPanel)tabbedPane.getComponent(index)).updateToolbarLocation(tabbedPane);
        }

        splitpane.setDividerLocation(0.5);
    }

    public void addTab(String filename) {
        addTab(editor.getServer(), filename);
    }

    public void addTab(Server server, String filename) {
        if (filename != null && EditorsPanel.loadFile(filename)== Content.NO_CONTENT) return;

        editor.getEditorsPanel().addTab(server).loadFile(filename);
    }

    public EditorTab getActiveEditor() {
        return editor;
    }

    public static StudioWindow getActiveStudioWindow() {
        return activeWindow;
    }

    public void updateEditor(EditorTab newEditor) {
        if (editor == newEditor) return;

        log.info("Update editor with server {} and filename {}",newEditor.getServer(), newEditor.getFilename());

        if (editor.isAddedToPanel()) { // during initialization editor is not in the panel
            editor.getEditorsPanel().setInFocusTabbedEditors(false);
        }

        newEditor.getEditorsPanel().setInFocusTabbedEditors(true);

        editor = newEditor;
        editor.setStudioWindow(this);
        mainStatusBar.updateStatuses(editor.getTextArea());
        setServer(editor.getServer());
        lastQuery = null;
        refreshFrameTitle();
        refreshActionState();
    }

    private void refreshResultTab() {
        refreshActionState();

        TabPanel tab = getSelectedResultPane();
        if (tab == null) return;

        if (tab.getEditor() != null) {
            mainStatusBar.updateStatuses(tab.getEditor().getTextArea());
        } else if (tab.getGrid() != null) {
            mainStatusBar.updateStatuses(tab.getGrid().getTable());
        } else if (tab.getType() == TabPanel.ResultType.ERROR) {
            mainStatusBar.resetStatuses();
        }
    }

    private void resultTabDragged(DragEvent event) {
        DraggableTabbedPane targetPane = event.getTargetPane();
        StudioWindow targetStudiowWindow = (StudioWindow) targetPane.getClientProperty(StudioWindow.class);
        ((TabPanel)targetPane.getComponentAt(event.getTargetIndex())).setStudioWindow(targetStudiowWindow);
    }

    public StudioWindow(Server server, String filename) {
        this(new Workspace.TopWindow(server, filename));
    }

    public StudioWindow(Workspace.TopWindow workspaceWindow) {
        setName("studioWindow" + (++studioWindowNameIndex));

        loading = true;

        allWindows.add(this);
        if (activeWindow == null) activeWindow = this;

        serverHistory = new HistoricalList<>(CONFIG.getServerHistoryDepth(),
                CONFIG.getServerHistory());
        initActions();
        createMenuBar();

        toolbar = createToolbar();
        editorSearchPanel = new SearchPanel( () -> editor.getPane() );
        editorSearchPanel.setName("SearchPanel");
        mainStatusBar = new MainStatusBar();
        tabbedPane = initResultPane();
        resultSearchPanel = initResultSearchPanel();

        // We need to have some editor initialized to prevent NPE
        editor = new EditorTab(this);
        initToolbar();
        topPanel = new JPanel(new BorderLayout());

        rootEditorsPanel = new EditorsPanel(this, workspaceWindow);

        List<EditorTab> editors = rootEditorsPanel.getAllEditors(true);
        boolean first = true;
        for (EditorTab editor: editors) {
            if (first) {
                updateEditor(editor);
                first = false;
            } else {
                editor.getEditorsPanel().setInFocusTabbedEditors(false);
            }
        }

        topPanel.add(editorSearchPanel, BorderLayout.NORTH);
        topPanel.setMinimumSize(new Dimension(0,0));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(tabbedPane, BorderLayout.CENTER);
        bottomPanel.add(resultSearchPanel, BorderLayout.NORTH);
        bottomPanel.setMinimumSize(new Dimension(0,0));
        splitpane = initSplitPane(topPanel, bottomPanel);

        topPanel.add(rootEditorsPanel, BorderLayout.CENTER);

        initFrame(workspaceWindow.getLocation(), toolbar, splitpane, mainStatusBar);
        serverList = new ServerList(this, workspaceWindow.getServerListBounds());
        splitpane.setDividerLocation(workspaceWindow.getResultDividerLocation());

        rootEditorsPanel.loadDividerLocation(workspaceWindow);

        loading = false;

        EditorsPanel.refreshEditorTitle(editor);
        refreshServer();
        refreshAllMenus();
    }

    private Toolbar createToolbar() {
        Toolbar toolbar = new Toolbar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0,2,0,1));
        return toolbar;
    }

    private DraggableTabbedPane initResultPane() {
        DraggableTabbedPane tabbedPane = new DraggableTabbedPane("Result", JTabbedPane.TOP);
        tabbedPane.setName("ResultTabbedPane");
        ClosableTabbedPane.makeCloseable(tabbedPane, new ClosableTabbedPane.PinTabAction() {
            @Override
            public boolean close(int index, boolean force) {
                if (force || !isPinned(index)) {
                    tabbedPane.removeTabAt(index);
                }
                return true;
            }
            @Override
            public boolean isPinned(int index) {
                TabPanel panel = (TabPanel)tabbedPane.getComponentAt(index);
                return panel.isPinned();
            }
            @Override
            public void setPinned(int index, boolean pinned) {
                TabPanel panel = (TabPanel)tabbedPane.getComponentAt(index);
                panel.setPinned(pinned);
            }
        });
        tabbedPane.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                refreshResultTab();
            }
        });
        tabbedPane.putClientProperty(StudioWindow.class, this);
        tabbedPane.addDragListener( evt -> resultTabDragged(evt));
        tabbedPane.addChangeListener(e -> {
            TabPanel tabPanel = getSelectedResultPane();
            if (tabPanel != null) tabPanel.refreshActionState();
        });
        return tabbedPane;
    }

    private SearchPanel initResultSearchPanel() {
        SearchPanel resultSearchPanel = new SearchPanel(() -> {
            if (tabbedPane.getTabCount() == 0) return null;
            TabPanel resultTab = getSelectedResultPane();
            EditorPane editorPane = resultTab.getEditor();
            if (editorPane != null) return editorPane;

            return resultTab.getGrid(); // the Grid or null
        });

        resultSearchPanel.setReplaceVisible(false);
        resultSearchPanel.setName("ResultSearchPanel");

        return resultSearchPanel;
    }

    private JSplitPane initSplitPane(JComponent top, JComponent bottom) {
        JSplitPane splitpane = new JSplitPane();
        splitpane.setTopComponent(top);
        splitpane.setBottomComponent(bottom);

        splitpane.setOneTouchExpandable(true);
        splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        if (splitpane.getUI() instanceof  BasicSplitPaneUI) {
            Component divider = ((BasicSplitPaneUI) splitpane.getUI()).getDivider();
            divider.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 2)
                        toggleDividerOrientation();
                }
            });
        }
        splitpane.setContinuousLayout(true);

        splitpane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent pce){
                String s = splitpane.getDividerLocation()>=splitpane.getMaximumDividerLocation() ?
                        I18n.getString("MinimizeEditorPane")
                        : splitpane.getDividerLocation()<=splitpane.getMinimumDividerLocation() ?
                        I18n.getString("RestoreEditorPane"):
                        I18n.getString("MaximizeEditorPane");
                minMaxDividerAction.putValue(Action.SHORT_DESCRIPTION,s);
                minMaxDividerAction.putValue(Action.NAME,s);
                if(splitpane.getDividerLocation()<splitpane.getMaximumDividerLocation()&&splitpane.getDividerLocation()>splitpane.getMinimumDividerLocation())
                    dividerLastPosition=splitpane.getDividerLocation();
            }
        });
        dividerLastPosition = splitpane.getDividerLocation();

        return splitpane;
    }

    private void initFrame(Rectangle location, JComponent toolbar, JComponent central, JComponent statusBar) {
        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (StudioWindow.activeWindow == StudioWindow.this) return;

                log.info("Window focus is changed from {} to {} ", StudioWindow.activeWindow.getCaption(), StudioWindow.this.getCaption());
                if (StudioWindow.allWindows.contains(StudioWindow.activeWindow)) {
                    StudioWindow.activeWindow.editor.getEditorsPanel().setInFocusTabbedEditors(false);
                }
                editor.getEditorsPanel().setInFocusTabbedEditors(true);
                StudioWindow.activeWindow = StudioWindow.this;
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(toolbar, BorderLayout.NORTH);
        contentPane.add(central, BorderLayout.CENTER);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        setContentPane(contentPane);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(this);

        if (Util.fitToScreen(location)) {
            setBounds(location);
        } else {
            //@TODO: should be defaulted in the Workspace.TopWindow
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setSize((int) (0.8 * screenSize.width), (int) (0.8 * screenSize.height));

            setLocation(((int) Math.max(0, (screenSize.width - getWidth()) / 2.0)),
                    (int) (Math.max(0, (screenSize.height - getHeight()) / 2.0)));
        }

        setIconImage(Util.LOGO_ICON.getImage());

        setVisible(true);
    }

    public SearchPanel getEditorSearchPanel() {
        return editorSearchPanel;
    }

    public SearchPanel getResultSearchPanel() {
        return resultSearchPanel;
    }

    public static void loadWorkspace(Workspace workspace) {
        for (Workspace.TopWindow window: workspace.getWindows()) {
            new StudioWindow(window);
        }

        int index = workspace.getSelectedWindow();
        if (index >= 0 && index < allWindows.size()) {
            allWindows.get(index).toFront();
        }

        if (allWindows.size() == 0) {
            new StudioWindow(Server.NO_SERVER, null);
        }

        for (StudioWindow window: allWindows) {
            window.refreshFrameTitle();
        }
    }

    public void refreshQuery() {
        executeK4Query(lastQuery);
    }

    public void executeQueryCurrentLine() {
        executeQuery(getCurrentLineEditorText(editor.getTextArea()));
    }

    public void executeQuery() {
        executeQuery(getEditorText(editor.getTextArea()));
    }

    private void executeQuery(String text) {
        if (text == null) {
            return;
        }
        text = text.trim();
        if (text.length() == 0) {
            log.info("Nothing to execute - got empty string");
            return;
        }

        executeK4Query(text);
        lastQuery = text;
    }

    public MainStatusBar getMainStatusBar() {
        return mainStatusBar;
    }

    public Server getServer() {
        return editor.getServer();
    }

    private String getEditorText(JTextComponent editor) {
        String text = editor.getSelectedText();
        if (text != null) return text;

        Config.ExecAllOption option = CONFIG.getExecAllOption();
        if (option == Config.ExecAllOption.Ignore) {
            log.info("Nothing is selected. Ignore execution of the whole script");
            return null;
        }
        if (option == Config.ExecAllOption.Execute) {
            return editor.getText();
        }
        //Ask
        int result = StudioOptionPane.showYesNoDialog(this, "Nothing is selected. Execute the whole script?",
                "Execute All?");

        if (result == JOptionPane.YES_OPTION ) {
            return editor.getText();
        }

        return null;
    }

    private String getCurrentLineEditorText(JTextComponent editor) {
        String newLine = "\n";
        String text = null;

        try {
            int pos = editor.getCaretPosition();
            int max = editor.getDocument().getLength();


            if ((max > pos) && (!editor.getText(pos,1).equals("\n"))) {
                String toeol = editor.getText(pos,max - pos);
                int eol = toeol.indexOf('\n');

                if (eol > 0)
                    pos = pos + eol;
                else
                    pos = max;
            }

            text = editor.getText(0,pos);

            int lrPos = text.lastIndexOf(newLine);

            if (lrPos >= 0) {
                lrPos += newLine.length(); // found it so skip it
                text = text.substring(lrPos,pos).trim();
            }
        }
        catch (BadLocationException e) {
        }

        if (text != null) {
            text = text.trim();

            if (text.length() == 0)
                text = null;
        }

        return text;
    }

    private JTable getSelectedTable() {
        TabPanel tab = (TabPanel) tabbedPane.getSelectedComponent();
        if (tab == null) return null;

        QGrid grid = tab.getGrid();
        if (grid == null) return null;

        return grid.getTable();
    }

    private void executeK4Query(String text) {
        executeK4Query(new K.KCharacterVector(text), text);
    }

    void executeK4Query(K.KBase query, String queryText) {
        if (editor.getServer() == Server.NO_SERVER) {
            log.info("Server is not set. Can't execute the query");
            return;
        }
        editor.getTextArea().setCursor(waitCursor);
        editor.getPane().setEditorStatus("Executing: " + queryText);
        editor.getQueryExecutor().execute(query, queryText);
        editor.getPane().startClock();
        refreshActionState();
    }

    private TabPanel getResultPane(int index) {
        return (TabPanel)tabbedPane.getComponentAt(index);
    }

    private TabPanel getSelectedResultPane() {
        return (TabPanel) tabbedPane.getSelectedComponent();
    }

    public void addResultTab(QueryResult queryResult, String tooltip) {
        TabPanel tab = new TabPanel(this, queryResult);
        tab.addInto(tabbedPane, getTooltipText(tooltip, queryResult.getKMessage()));
    }

    private static String getTooltipText(String tooltip, KMessage message) {
        StringBuilder res = new StringBuilder();
        res.append("<html>").append(tooltip);
        if (message != null) {
            res.append("<br/>Bytes sent: ").append(message.getBytesSent());
            res.append("<br/>Bytes received: ").append(message.getBytesReceived());

            K.KTimestamp started = message.getStarted();
            K.KTimestamp finished = message.getFinished();
            if (! started.isNull()) {
                res.append("<br/>Query sent at ").append(started);
            }
            if (! finished.isNull()) {
                res.append("<br/>Result received at ").append(finished);
            }
            if (!started.isNull() && !finished.isNull()) {
                res.append("<br/>Duration: ").append((finished.toLong() - started.toLong()) / 1_000_000).append("ms");
            }
        }
        res.append("</html>");
        return res.toString();
    }

    // if the query is cancelled execTime=-1, result and error are null's
    public static void queryExecutionComplete(EditorTab editor, QueryResult queryResult) {
        editor.getPane().stopClock();
        JTextComponent textArea = editor.getTextArea();
        textArea.setCursor(textCursor);
        if (queryResult.isComplete()) {
            long execTime = queryResult.getExecutionTimeInMS();
            editor.getPane().setEditorStatus("Last execution time: " + (execTime > 0 ? "" + execTime : "<1") + " ms");
        } else {
            editor.getPane().setEditorStatus("Last query was cancelled");
        }

        StudioWindow window = editor.getStudioWindow();
        try {
            if (queryResult.isComplete()) {
                window.addResultTab(queryResult, "Executed at server: " + queryResult.getServer().getDescription(true) );
            }
        } catch (Throwable error) {
            log.error("Error during result rendering", error);

            String message = error.getMessage();
            if ((message == null) || (message.length() == 0))
                message = "No message with exception. Exception is " + error;
            StudioOptionPane.showError(editor.getPane(),
                    "\nAn unexpected error occurred whilst communicating with " +
                            editor.getServer().getConnectionString() +
                            "\n\nError detail is\n\n" + message + "\n\n",
                    "Studio for kdb+");
        }

        window.refreshActionState();
    }

    public static Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

        for (StudioWindow window : allWindows) {
            Workspace.TopWindow workspaceWindow = workspace.addWindow(window == activeWindow);
            workspaceWindow.setResultDividerLocation(Util.getDividerLocation(window.splitpane));
            workspaceWindow.setLocation(window.getBounds());
            workspaceWindow.setServerListBounds(window.getServerList().getBounds());
            window.rootEditorsPanel.getWorkspace(workspaceWindow);
        }
        return workspace;
    }

    public void windowClosing(WindowEvent e) {
        close();
    }


    public void windowClosed(WindowEvent e) {
    }


    public void windowOpened(WindowEvent e) {
    }
    // ctrl-alt spacebar to minimize window

    public void windowIconified(WindowEvent e) {
    }


    public void windowDeiconified(WindowEvent e) {
    }


    public void windowActivated(WindowEvent e) {
    }


    public void windowDeactivated(WindowEvent e) {
    }

}
