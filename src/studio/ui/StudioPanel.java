package studio.ui;

import kx.c;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import studio.core.Studio;
import studio.kdb.*;
import studio.kdb.config.ActionOnExit;
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
import javax.swing.filechooser.FileFilter;
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

public class StudioPanel extends JPanel implements WindowListener {

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
    private JFrame frame;

    private static Map<String, JFileChooser> fileChooserMap = new HashMap<>();

    private static List<StudioPanel> allPanels = new ArrayList<>();
    private static StudioPanel activePanel = null;

    private final List<Server> serverHistory;

    public final static int menuShortcutKeyMask = java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private final static Cursor textCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    private final static Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    private final static int MAX_SERVERS_TO_CLONE = 20;

    private static final Config CONFIG = Config.getInstance();
    
    public void refreshFrameTitle() {
        if (loading) return;

        StringBuilder frameTitleBuilder = new StringBuilder();
        frameTitleBuilder.append(editor.getTitle());
        if (editor.isModified()) frameTitleBuilder.append(" (not saved) ");

        Server server = editor.getServer();
        if (server != Server.NO_SERVER) frameTitleBuilder.append(" @").append(server);
        frameTitleBuilder.append(" ");

        frameTitleBuilder.append("Studio for kdb+ ").append(Lm.version);

        String env = EnvConfig.getEnvironment();
        if (env != null) frameTitleBuilder.append(" [").append(env).append("]");

        String frameTitle = frameTitleBuilder.toString();
        if (!frameTitle.equals(frame.getTitle())) {
            frame.setTitle(frameTitle);
        }
    }

    private void setActionsEnabled(boolean value, Action... actions) {
        for (Action action: actions) {
            if (action != null) {
                action.setEnabled(value);
            }
        }
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

        boolean queryRunning = editor.getQueryExecutor().running();
        stopAction.setEnabled(queryRunning);
        executeAction.setEnabled(!queryRunning);
        executeCurrentLineAction.setEnabled(!queryRunning);
        refreshAction.setEnabled(lastQuery != null && !queryRunning);

        TabPanel tab = (TabPanel) tabbedPane.getSelectedComponent();
        if (tab == null) {
            setActionsEnabled(false, exportAction, chartAction, openInExcel, refreshAction);
        } else {
            exportAction.setEnabled(tab.isTable());
            chartAction.setEnabled(tab.getType() == TabPanel.ResultType.TABLE);
            openInExcel.setEnabled(tab.isTable());
            refreshAction.setEnabled(true);
            tab.refreshActionState(queryRunning);
        }
    }

    public static File chooseFile(Component parent, String fileChooserType, int dialogType, String title, File defaultFile, FileFilter... filters) {
        JFileChooser fileChooser = fileChooserMap.get(fileChooserType);
        FileChooserConfig config = CONFIG.getFileChooserConfig(fileChooserType);
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooserMap.put(fileChooserType, fileChooser);

            if (title != null) fileChooser.setDialogTitle(title);
            fileChooser.setDialogType(dialogType);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            for (FileFilter ff: filters) {
                fileChooser.addChoosableFileFilter(ff);
            }
            if (filters.length == 1) fileChooser.setFileFilter(filters[0]);

            if (defaultFile == null && ! config.getFilename().equals("")) {
                defaultFile = new File(config.getFilename());
            }

        }

        if (defaultFile != null) {
            fileChooser.setCurrentDirectory(defaultFile.getParentFile());
            fileChooser.setSelectedFile(defaultFile);
            fileChooser.ensureFileIsVisible(defaultFile);
        }

        Dimension preferredSize = config.getPreferredSize();
        if (preferredSize.width > 0 && preferredSize.height > 0) {
            fileChooser.setPreferredSize(preferredSize);
        }

        int option;
        if (dialogType == JFileChooser.OPEN_DIALOG) option = fileChooser.showOpenDialog(parent);
        else option = fileChooser.showSaveDialog(parent);

        File selectedFile = fileChooser.getSelectedFile();
        String filename = "";
        if (selectedFile != null) {
            filename = selectedFile.getAbsolutePath();
        }

        if (dialogType == JFileChooser.SAVE_DIALOG && option == JFileChooser.APPROVE_OPTION) {
            FileFilter ff = fileChooser.getFileFilter();
            if (ff instanceof FileNameExtensionFilter) {
                String ext = "." + ((FileNameExtensionFilter) ff).getExtensions()[0];
                if (!filename.endsWith(ext)) {
                    filename = filename + ext;
                    selectedFile = new File(filename);
                }
            }
        }

        config = new FileChooserConfig(filename, fileChooser.getSize());
        CONFIG.setFileChooserConfig(fileChooserType, config);

        return option == JFileChooser.APPROVE_OPTION ? selectedFile : null;
    }

    private void exportAsExcel(final String filename) {
        new ExcelExporter().exportTableX(frame,getSelectedTable(),new File(filename),false);
    }

    private void exportAsDelimited(final TableModel model,final String filename,final char delimiter) {
        UIManager.put("ProgressMonitor.progressText","Studio for kdb+");
        final ProgressMonitor pm = new ProgressMonitor(frame,"Exporting data to " + filename,
                "0% complete",0,100);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = () -> {
            if (filename != null) {
                String lineSeparator = System.getProperty("line.separator");;

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
        final ProgressMonitor pm = new ProgressMonitor(frame,"Exporting data to " + filename,
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

        File file = chooseFile(this, Config.EXPORT_FILE_CHOOSER, JFileChooser.SAVE_DIALOG, "Export result set as",
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

        editor.setFilename(null);
        editor.init(Content.getEmpty());
    }

    private void openFile() {
        File file = chooseFile(this, Config.OPEN_FILE_CHOOSER, JFileChooser.OPEN_DIALOG, null, null,
                new FileNameExtensionFilter("q script", "q"));

        if (file == null) return;
        String filename = file.getAbsolutePath();
        addToMruFiles(filename);
        addTab(editor.getServer(), filename);
    }

    public void loadMRUFile(String filename) {
        if (!EditorsPanel.checkAndSaveTab(editor)) return;
        if (!EditorsPanel.loadFile(editor, filename)) return;

        addToMruFiles(filename);
        EditorsPanel.refreshEditorTitle(editor);
        rebuildAll();
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
        rebuildMenuBar();
    }

    private static void saveAll() {
        for (StudioPanel panel: allPanels) {
            panel.rootEditorsPanel.execute(editorTab -> editorTab.saveFileOnDisk(false));
        }
    }

    private void arrangeAll() {
        int noWins = allPanels.size();

        Iterator<StudioPanel> panelIterator = allPanels.iterator();

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
                StudioPanel panel = panelIterator.next();
                JFrame frame = panel.frame;

                frame.setSize(width,height);
                frame.setLocation(col * width,((noRows - 1) - row) * height);
                ensureDeiconified(frame);
            }
        }
    }

    public void setServer(Server server) {
        editor.setServer(server);
        if (!loading) {
            CONFIG.addServerToHistory(server);
            serverHistory.add(server);

            EditorsPanel.refreshEditorTitle(editor);
            rebuildAll();
        }
    }

    private void initActions() {
        cleanAction = UserAction.create("Clean", Util.NEW_DOCUMENT_ICON, "Clean editor script", KeyEvent.VK_N,
                null, e -> newFile());

        arrangeAllAction = UserAction.create(I18n.getString("ArrangeAll"), Util.BLANK_ICON, "Arrange all windows on screen",
                KeyEvent.VK_A, null, e -> arrangeAll());

        minMaxDividerAction = UserAction.create(I18n.getString("MaximizeEditorPane"), Util.BLANK_ICON, "Maximize editor pane",
                KeyEvent.VK_M, KeyStroke.getKeyStroke(KeyEvent.VK_M, menuShortcutKeyMask),
                e -> minMaxDivider());

        toggleDividerOrientationAction = UserAction.create(I18n.getString("ToggleDividerOrientation"), Util.BLANK_ICON,
                "Toggle the window divider's orientation", KeyEvent.VK_C, null, e -> toggleDividerOrientation());

        closeTabAction = UserAction.create("Close Tab", Util.BLANK_ICON, "Close current tab", KeyEvent.VK_W,
                KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutKeyMask), e -> editor.getEditorsPanel().closeTab(editor));

        closeFileAction = UserAction.create("Close Window", Util.BLANK_ICON, "Close current window (close all tabs)",
                KeyEvent.VK_C, null, e -> closePanel());

        openFileAction = UserAction.create(I18n.getString("Open"), Util.FOLDER_ICON, "Open a script", KeyEvent.VK_O,
                KeyStroke.getKeyStroke(KeyEvent.VK_O, menuShortcutKeyMask), e -> openFile());

        newWindowAction = UserAction.create(I18n.getString("NewWindow"), Util.BLANK_ICON, "Open a new window",
                KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> {
                    StudioPanel panel = new StudioPanel(editor.getServer(), null);
                    panel.rebuildMenuAndTooblar();
                } );

        newTabAction = UserAction.create("New Tab", Util.BLANK_ICON, "Open a new tab", KeyEvent.VK_T,
                KeyStroke.getKeyStroke(KeyEvent.VK_N, menuShortcutKeyMask),
                e -> addTab(editor.getServer(), null));

        serverListAction = UserAction.create(I18n.getString("ServerList"), Util.TEXT_TREE_ICON, "Show server list",
                KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_L, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> showServerList(false));

        serverHistoryAction = UserAction.create("Server History", null, "Recent selected servers", KeyEvent.VK_R,
                KeyStroke.getKeyStroke(KeyEvent.VK_R, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> showServerList(true));

        importFromQPadAction = UserAction.create("Import Servers from QPad...", null, "Import from Servers.cfg",
                KeyEvent.VK_I, null, e -> QPadImport.doImport(this));

        editServerAction = UserAction.create(I18n.getString("Edit"), Util.SERVER_INFORMATION_ICON, "Edit the server details",
                KeyEvent.VK_E, null, e -> {
                    Server s = new Server(editor.getServer());

                    EditServerForm f = new EditServerForm(frame, s);
                    f.alignAndShow();
                    if (f.getResult() == ACCEPTED) {
                        if (stopAction.isEnabled())
                            stopAction.actionPerformed(e);

                        ConnectionPool.getInstance().purge(editor.getServer());
                        CONFIG.removeServer(editor.getServer());

                        s = f.getServer();
                        CONFIG.addServer(s);
                        setServer(s);
                        rebuildAll();
                    }
                });


        addServerAction = UserAction.create(I18n.getString("Add"), Util.ADD_SERVER_ICON, "Configure a new server",
                KeyEvent.VK_A, null, e -> {
                    AddServerForm f = new AddServerForm(frame);
                    f.alignAndShow();
                    if (f.getResult() == ACCEPTED) {
                        Server s = f.getServer();
                        CONFIG.addServer(s);
                        ConnectionPool.getInstance().purge(s);   //?
                        setServer(s);
                        rebuildAll();
                    }
                });

        removeServerAction = UserAction.create(I18n.getString("Remove"), Util.DELETE_SERVER_ICON, "Remove this server",
                KeyEvent.VK_R, null, e -> {
                    int choice = JOptionPane.showOptionDialog(frame,
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

                        rebuildAll();
                    }
                });


        saveFileAction = UserAction.create(I18n.getString("Save"), Util.DISKS_ICON, "Save the script",
                KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutKeyMask),
                e -> EditorsPanel.saveEditor(editor));

        saveAllFilesAction = UserAction.create("Save All...", Util.BLANK_ICON, "Save all files",
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
                        new ExcelExporter().exportTableX(frame, getSelectedTable(), file, true);
                    } catch (IOException ex) {
                        log.error("Failed to create temporary file", ex);
                        StudioOptionPane.showError(frame, "Failed to Open in Excel " + ex.getMessage(),"Error");
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
                    TabPanel tab = (TabPanel) tabbedPane.getSelectedComponent();
                    if (tab != null) tab.toggleCommaFormatting();
                });

        findInResultAction = UserAction.create("Find in Result", Util.FIND_ICON, "Find in result tab",
                KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> resultSearchPanel.setVisible(true) );

        aboutAction = UserAction.create(I18n.getString("About"), Util.ABOUT_ICON, "About Studio for kdb+",
                KeyEvent.VK_E, null, e -> about());

        exitAction = UserAction.create(I18n.getString("Exit"), Util.BLANK_ICON, "Close this window",
                KeyEvent.VK_X, e -> quit());

        settingsAction = UserAction.create("Settings", Util.BLANK_ICON, "Settings",
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

        nextEditorTabAction = UserAction.create("Next tab", Util.BLANK_ICON,
                "Select next editor tab", KeyEvent.VK_N,
                    Util.MAC_OS_X ? KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, menuShortcutKeyMask | InputEvent.ALT_MASK ) :
                                    KeyStroke.getKeyStroke(KeyEvent.VK_TAB, menuShortcutKeyMask),
                e -> editor.getEditorsPanel().selectNextTab(true));

        prevEditorTabAction = UserAction.create("Previous tab", Util.BLANK_ICON,
                "Select previous editor tab", KeyEvent.VK_P,
                Util.MAC_OS_X ? KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, menuShortcutKeyMask | InputEvent.ALT_MASK ) :
                        KeyStroke.getKeyStroke(KeyEvent.VK_TAB, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> editor.getEditorsPanel().selectNextTab(false));

        lineEndingActions = new UserAction[LineEnding.values().length];
        for(LineEnding lineEnding: LineEnding.values()) {
            lineEndingActions[lineEnding.ordinal()] = UserAction.create(lineEnding.getDescription(),
                e -> {
                    editor.setLineEnding(lineEnding);
                    refreshActionState();
                } );
        }

        wordWrapAction = UserAction.create("Word wrap", Util.BLANK_ICON, "Word wrap for all tabs",
                KeyEvent.VK_W, KeyStroke.getKeyStroke(KeyEvent.VK_W, menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> toggleWordWrap());

        splitEditorRight = UserAction.create("Split right", Util.BLANK_ICON, "Split vertically",
                KeyEvent.VK_R, null,
                e-> editor.getEditorsPanel().split(false));
        splitEditorDown = UserAction.create("Split down", Util.BLANK_ICON, "Split horizontally",
                KeyEvent.VK_D, null,
                e-> editor.getEditorsPanel().split(true));
    }

    public static void settings() {
        SettingsDialog dialog = new SettingsDialog(activePanel.frame);
        dialog.alignAndShow();
        if (dialog.getResult() == CANCELLED) return;

        dialog.saveSettings();
        activePanel.rebuildToolbar();
    }

    private void toggleWordWrap() {
        boolean value = CONFIG.getBoolean(Config.RSTA_WORD_WRAP);
        CONFIG.setBoolean(Config.RSTA_WORD_WRAP, !value);
        refreshEditorsSettings();
        refreshActionState();
        rebuildAll();
    }

    public static void refreshEditorsSettings() {
        for (StudioPanel panel: allPanels) {
            panel.rootEditorsPanel.execute(editorTab -> {
                RSyntaxTextArea editor = editorTab.getTextArea();
                editor.setHighlightCurrentLine(CONFIG.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));
                editor.setAnimateBracketMatching(CONFIG.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));
                editor.setLineWrap(CONFIG.getBoolean(Config.RSTA_WORD_WRAP));
                editor.setFont(CONFIG.getFont(Config.FONT_EDITOR));

                editor.setTabSize(CONFIG.getInt(Config.EDITOR_TAB_SIZE));
                editor.setTabsEmulated(CONFIG.getBoolean(Config.EDITOR_TAB_EMULATED));
                return true;
            });
        }
    }

    public static void refreshResultSettings() {
        long doubleClickTimeout = CONFIG.getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT);
        for (StudioPanel panel: allPanels) {
            int count = panel.tabbedPane.getTabCount();
            for (int index=0; index<count; index++) {
                TabPanel tabPanel = panel.getResultPane(index);
                tabPanel.setDoubleClickTimeout(doubleClickTimeout);
                tabPanel.refreshFont();
            }
        }
    }

    public static StudioPanel[] getPanels() {
        return allPanels.toArray(new StudioPanel[0]);
    }

    public static void about() {
        HelpDialog help = new HelpDialog(activePanel.frame);
        Util.centerChildOnParent(help,activePanel.frame);
        // help.setTitle("About Studio for kdb+");
        help.pack();
        help.setVisible(true);
    }

    public static void quit() {
        WorkspaceSaver.setEnabled(false);
        try {
            ActionOnExit action = CONFIG.getEnum(Config.ACTION_ON_EXIT);
            if (action != ActionOnExit.NOTHING) {
                for (StudioPanel panel : allPanels.toArray(new StudioPanel[0])) {
                    panel.getFrame().toFront();
                    boolean complete = panel.rootEditorsPanel.execute(editorTab -> {
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
            if (allPanels.size() > 0) {
                activePanel.frame.toFront();
            }
            WorkspaceSaver.setEnabled(true);
        }
        WorkspaceSaver.save(getWorkspace());
        log.info("Shutting down");
        System.exit(0);
    }

    private void closePanel() {
        // If this is the last window, we need to properly persist workspace
        if (allPanels.size() == 1) {
            quit();
        } else {
            rootEditorsPanel.execute(editorTab -> editorTab.getEditorsPanel().closeTab(editorTab));
            //closing the last tab would dispose the frame
        }
    }


    public void closeFrame() {
        if (allPanels.size() == 1) {
            quit();
        } else {
            frame.dispose();
            allPanels.remove(this);
            rebuildAll();
        }
    }

    public static void rebuildAll() {
        for (StudioPanel panel: allPanels) {
            panel.rebuildMenuAndTooblar();
        }
    }

    private void rebuildMenuAndTooblar() {
        rebuildMenuBar();
        rebuildToolbar();
    }

    private void rebuildMenuBar() {
        if (loading) return;

        JMenuBar menubar = createMenuBar();
        frame.setJMenuBar(menubar);
        menubar.validate();
        menubar.repaint();
        frame.validate();
        frame.repaint();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menubar = new JMenuBar();
        JMenu menu = new JMenu(I18n.getString("File"));
        menu.setMnemonic(KeyEvent.VK_F);
        menu.add(new JMenuItem(newWindowAction));
        menu.add(new JMenuItem(newTabAction));
        menu.add(new JMenuItem(openFileAction));
        menu.add(new JMenuItem(saveFileAction));
        menu.add(new JMenuItem(saveAsFileAction));
        menu.add(new JMenuItem(saveAllFilesAction));

        menu.add(new JMenuItem(closeTabAction));
        menu.add(new JMenuItem(closeFileAction));

        if (!Studio.hasMacOSSystemMenu()) {
            menu.add(new JMenuItem(settingsAction));
        }
        menu.addSeparator();
//        menu.add(new JMenuItem(importAction));
        menu.add(new JMenuItem(openInExcel));
        menu.addSeparator();
        menu.add(new JMenuItem(exportAction));
        menu.addSeparator();
        menu.add(new JMenuItem(chartAction));

        String[] mru = CONFIG.getMRUFiles();

        if (mru.length > 0) {
            menu.addSeparator();
            char[] mnems = "123456789".toCharArray();

            for (int i = 0;i < (mru.length > mnems.length ? mnems.length : mru.length);i++) {
                final String filename = mru[i];

                JMenuItem item = new JMenuItem("" + (i + 1) + " " + filename);
                item.setMnemonic(mnems[i]);
                item.setIcon(Util.BLANK_ICON);
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        loadMRUFile(filename);
                    }
                });
                menu.add(item);
            }
        }

        if (!Studio.hasMacOSSystemMenu()) {
            menu.addSeparator();
            menu.add(new JMenuItem(exitAction));
        }
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Edit"));
        menu.setMnemonic(KeyEvent.VK_E);
        menu.add(new JMenuItem(undoAction));
        menu.add(new JMenuItem(redoAction));
        menu.addSeparator();
        menu.add(new JMenuItem(cutAction));
        menu.add(new JMenuItem(copyAction));
        menu.add(new JMenuItem(pasteAction));
        menu.addSeparator();

        menu.add(new JCheckBoxMenuItem(wordWrapAction));

        JMenu lineEndingSubMenu = new JMenu("Line Ending");
        lineEndingSubMenu.setIcon(Util.BLANK_ICON);
        for (Action action: lineEndingActions) {
            lineEndingSubMenu.add(new JCheckBoxMenuItem(action));
        }
        menu.add(lineEndingSubMenu);

        menu.add(new JMenuItem(cleanAction));
        menu.add(new JMenuItem(selectAllAction));
        menu.addSeparator();
        menu.add(new JMenuItem(findAction));
        menu.add(new JMenuItem(replaceAction));
        menu.add(new JMenuItem(convertTabsToSpacesAction));
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Server"));
        menu.setMnemonic(KeyEvent.VK_S);
        menu.add(new JMenuItem(addServerAction));
        menu.add(new JMenuItem(editServerAction));
        menu.add(new JMenuItem(removeServerAction));

        Server server = editor.getServer();
        Server[] servers = CONFIG.getServers();
        if (servers.length > 0) {
            JMenu subMenu = new JMenu(I18n.getString("Clone"));
            subMenu.setIcon(Util.DATA_COPY_ICON);

            int count = MAX_SERVERS_TO_CLONE;
            for (int i = 0;i < servers.length;i++) {
                final Server s = servers[i];
                if (!s.equals(server) && count <= 0) continue;
                count--;
                JMenuItem item = new JMenuItem(s.getFullName());
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        Server clone = new Server(s);
                        clone.setName("Clone of " + clone.getName());

                        EditServerForm f = new EditServerForm(frame,clone);
                        f.alignAndShow();

                        if (f.getResult() == ACCEPTED) {
                            clone = f.getServer();
                            CONFIG.addServer(clone);
                            //ebuildToolbar();
                            setServer(clone);
                            ConnectionPool.getInstance().purge(clone); //?
                            rebuildAll();
                        }
                    }
                });

                subMenu.add(item);
            }

            menu.add(subMenu);
        }

        menu.addSeparator();
        menu.add(new JMenuItem(serverListAction));
        menu.add(new JMenuItem(serverHistoryAction));
        menu.add(new JMenuItem(importFromQPadAction));

        menubar.add(menu);

        menu = new JMenu(I18n.getString("Query"));
        menu.setMnemonic(KeyEvent.VK_Q);
        menu.add(new JMenuItem(executeCurrentLineAction));
        menu.add(new JMenuItem(executeAction));
        menu.add(new JMenuItem(stopAction));
        menu.add(new JMenuItem(refreshAction));
        menu.add(new JMenuItem(toggleCommaFormatAction));
        menu.add(new JMenuItem(findInResultAction));
        menubar.add(menu);

        menu = new JMenu(I18n.getString("Window"));
        menu.setMnemonic(KeyEvent.VK_W);

        menu.add(new JMenuItem(splitEditorRight));
        menu.add(new JMenuItem(splitEditorDown));
        menu.addSeparator();
        menu.add(new JMenuItem(minMaxDividerAction));
        menu.add(new JMenuItem(toggleDividerOrientationAction));
        menu.add(new JMenuItem(arrangeAllAction));
        menu.add(new JMenuItem(nextEditorTabAction));
        menu.add(new JMenuItem(prevEditorTabAction));

        if (allPanels.size() > 0) {
            menu.addSeparator();

            int i = 0;
            for (StudioPanel panel: allPanels) {
                EditorTab editor = panel.editor;
                String t = "unknown";
                String filename = editor.getFilename();

                if (filename != null)
                    t = filename.replace('\\', '/');

                if (editor.getServer() != Server.NO_SERVER)
                    t = t + "[" + editor.getServer().getFullName() + "]";
                else
                    t = t + "[no server]";

                JMenuItem item = new JMenuItem("" + (i + 1) + " " + t);
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        ensureDeiconified(panel.frame);
                    }
                });

                if (panel == this)
                    item.setIcon(Util.CHECK_ICON);
                else
                    item.setIcon(Util.BLANK_ICON);

                menu.add(item);
                i++;
            }
        }
        menubar.add(menu);
        menu = new JMenu(I18n.getString("Help"));
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(new JMenuItem(codeKxComAction));
        if (!Studio.hasMacOSSystemMenu())
            menu.add(new JMenuItem(aboutAction));
        menubar.add(menu);

        return menubar;
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

            rebuildToolbar();
            toolbar.validate();
            toolbar.repaint();
        } catch (IllegalArgumentException e) {
            refreshConnection();
        }
    }

    private void showServerList(boolean selectHistory) {
        if (serverList == null) {
            serverList = new ServerList(frame);
        }
        Rectangle bounds = Config.getInstance().getBounds(Config.SERVER_LIST_BOUNDS);
        serverList.setBounds(bounds);

        serverList.updateServerTree(CONFIG.getServerTree(), editor.getServer());
        serverList.updateServerHistory(serverHistory);
        serverList.selectHistoryTab(selectHistory);
        serverList.setVisible(true);

        bounds = serverList.getBounds();
        CONFIG.setBounds(Config.SERVER_LIST_BOUNDS, bounds);

        Server selectedServer = serverList.getSelectedServer();
        if (selectedServer == null || selectedServer.equals(editor.getServer())) return;

        setServer(selectedServer);
        rebuildToolbar();
    }


    private void selectServerName() {
        String selection = comboServer.getSelectedItem().toString();
        if(! CONFIG.getServerNames().contains(selection)) return;

        setServer(CONFIG.getServer(selection));
        rebuildToolbar();
        toolbar.validate();
        toolbar.repaint();
    }

    private void refreshConnection() {
        Server server = editor.getServer();
        if (server == Server.NO_SERVER) {
            txtServer.setText("");
            txtServer.setToolTipText("Select connection details");
        } else {
            txtServer.setText(server.getConnectionString());
            txtServer.setToolTipText(server.getConnectionStringWithPwd());
        }
    }

    private void toolbarAddServerSelection() {
        Server server = editor.getServer();
        Collection<String> names = CONFIG.getServerNames();
        String name = server == Server.NO_SERVER ? "" : server.getFullName();
        if (!names.contains(name)) {
            List<String> newNames = new ArrayList<>();
            newNames.add(name);
            newNames.addAll(names);
            names = newNames;
        }
        comboServer = new JComboBox<>(names.toArray(new String[0]));
        comboServer.setToolTipText("Select the server context");
        comboServer.setSelectedItem(name);
        comboServer.addActionListener(e->selectServerName());
        // Cut the width if it is too wide.
        comboServer.setMinimumSize(new Dimension(0, 0));
        comboServer.setVisible(CONFIG.isShowServerComboBox());

        txtServer = new JTextField(32);
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
        refreshConnection();

        toolbar.add(new JLabel(I18n.getString("Server")));
        toolbar.add(comboServer);
        toolbar.add(txtServer);
        toolbar.add(serverListAction);
        toolbar.addSeparator();
    }

    private void rebuildToolbar() {
        if (loading) return;

        toolbar.removeAll();
        toolbarAddServerSelection();

        toolbar.add(stopAction);
        toolbar.add(executeAction);
        toolbar.add(refreshAction);
        toolbar.addSeparator();

        toolbar.add(openFileAction);
        toolbar.add(saveFileAction);
        toolbar.add(saveAsFileAction);
        toolbar.addSeparator();
        toolbar.add(openInExcel);
        toolbar.addSeparator();
        toolbar.add(exportAction);
        toolbar.addSeparator();

        toolbar.add(chartAction);
        toolbar.addSeparator();

        toolbar.add(undoAction);
        toolbar.add(redoAction);
        toolbar.addSeparator();

        toolbar.add(cutAction);
        toolbar.add(copyAction);
        toolbar.add(pasteAction);

        toolbar.addSeparator();
        toolbar.add(findAction);

        toolbar.add(replaceAction);
        toolbar.addSeparator();
        toolbar.add(codeKxComAction);

        for (int j = 0;j < toolbar.getComponentCount();j++) {
            Component c = toolbar.getComponentAtIndex(j);

            if (c instanceof JButton) {
                JButton btn = (JButton)c;
                btn.setFocusable(false);
                btn.setMnemonic(KeyEvent.VK_UNDEFINED);
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

    public EditorTab addTab(Server server, String filename) {
        return editor.getEditorsPanel().addTab(server, filename);
    }

    public EditorTab getActiveEditor() {
        return editor;
    }

    public static StudioPanel getActivePanel() {
        return activePanel;
    }

    public void updateEditor(EditorTab newEditor) {
        if (editor == newEditor) return;

        log.info("Update editor with server {} and filename {}",newEditor.getServer(), newEditor.getFilename());

        if (editor.isAddedToPanel()) { // during initialization editor is not in the panel
            editor.getEditorsPanel().setDimEditors(true);
        }

        newEditor.getEditorsPanel().setDimEditors(false);

        editor = newEditor;
        editor.setPanel(this);
        mainStatusBar.updateStatuses(editor.getTextArea());
        setServer(editor.getServer());
        lastQuery = null;
        refreshFrameTitle();
        refreshActionState();
    }

    private void refreshResultTab() {
        refreshActionState();

        TabPanel tab = (TabPanel) tabbedPane.getSelectedComponent();
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
        StudioPanel targetPanel = (StudioPanel) targetPane.getClientProperty(StudioPanel.class);
        ((TabPanel)targetPane.getComponentAt(event.getTargetIndex())).setPanel(targetPanel);
    }

    public StudioPanel(Server server, String filename) {
        this(new Workspace.Window(server, filename));
    }

    public StudioPanel(Workspace.Window workspaceWindow) {
        loading = true;

        allPanels.add(this);
        if (activePanel == null) activePanel = this;

        serverHistory = new HistoricalList<>(CONFIG.getServerHistoryDepth(),
                CONFIG.getServerHistory());
        initActions();

        toolbar = initToolbar();
        editorSearchPanel = new SearchPanel( () -> editor.getPane() );
        mainStatusBar = new MainStatusBar();
        tabbedPane = initResultPane();
        resultSearchPanel = initResultSearchPanel();

        // We need to have some editor initialize to prevent NPE
        editor = new EditorTab(this);
        topPanel = new JPanel(new BorderLayout());
        frame = new JFrame();

        rootEditorsPanel = new EditorsPanel(this, workspaceWindow);

        List<EditorTab> editors = rootEditorsPanel.getAllEditors(true);
        boolean first = true;
        for (EditorTab editor: editors) {
            if (first) {
                updateEditor(editor);
                first = false;
            } else {
                editor.getEditorsPanel().setDimEditors(true);
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

        initFrame(frame, toolbar, splitpane, mainStatusBar);
        splitpane.setDividerLocation(0.5);

        loading = false;
    }

    private Toolbar initToolbar() {
        Toolbar toolbar = new Toolbar();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0,2,0,1));
        return toolbar;
    }

    private DraggableTabbedPane initResultPane() {
        DraggableTabbedPane tabbedPane = new DraggableTabbedPane("Result", JTabbedPane.TOP);
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
        tabbedPane.putClientProperty(StudioPanel.class, this);
        tabbedPane.addDragListener( evt -> resultTabDragged(evt));
        return tabbedPane;
    }

    private SearchPanel initResultSearchPanel() {
        SearchPanel resultSearchPanel = new SearchPanel(() -> {
            if (tabbedPane.getTabCount() == 0) return null;
            TabPanel resultTab = getResultPane(tabbedPane.getSelectedIndex());
            EditorPane editorPane = resultTab.getEditor();
            if (editorPane != null) return editorPane;

            return resultTab.getGrid(); // the Grid or null
        });

        resultSearchPanel.setReplaceVisible(false);

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

    private void initFrame(JFrame frame, JComponent toolbar, JComponent central, JComponent statusBar) {
        frame.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                StudioPanel newPanel = StudioPanel.this;
                if (StudioPanel.activePanel == newPanel) return;

                StudioPanel.activePanel.editor.getEditorsPanel().setDimEditors(true);
                newPanel.editor.getEditorsPanel().setDimEditors(false);
                StudioPanel.activePanel = newPanel;
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
            }
        });

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(toolbar, BorderLayout.NORTH);
        contentPane.add(central, BorderLayout.CENTER);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        frame.setContentPane(contentPane);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(this);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize((int) (0.8 * screenSize.width),
                (int) (0.8 * screenSize.height));

        frame.setLocation(((int) Math.max(0, (screenSize.width - frame.getWidth()) / 2.0)),
                (int) (Math.max(0, (screenSize.height - frame.getHeight()) / 2.0)));

        frame.setIconImage(Util.LOGO_ICON.getImage());

        frame.setVisible(true);
    }

    public SearchPanel getEditorSearchPanel() {
        return editorSearchPanel;
    }

    public SearchPanel getResultSearchPanel() {
        return resultSearchPanel;
    }

    public static void loadWorkspace(Workspace workspace) {
        for (Workspace.Window window: workspace.getWindows()) {
            new StudioPanel(window);
        }

        int index = workspace.getSelectedWindow();
        if (index >= 0 && index < allPanels.size()) {
            allPanels.get(index).frame.toFront();
        }

        if (allPanels.size() == 0) {
            new StudioPanel(Server.NO_SERVER, null);
        }

        for (StudioPanel panel: allPanels) {
            panel.refreshFrameTitle();
        }
        rebuildAll();
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

    public JFrame getFrame() {
        return frame;
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
        int result = StudioOptionPane.showYesNoDialog(frame, "Nothing is selected. Execute the whole script?",
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

    // if the query is cancelled execTime=-1, result and error are null's
    public static void queryExecutionComplete(EditorTab editor, QueryResult queryResult) {
        editor.getPane().stopClock();
        JTextComponent textArea = editor.getTextArea();
        textArea.setCursor(textCursor);
        Throwable error = queryResult.getError();
        if (queryResult.isComplete()) {
            long execTime = queryResult.getExecutionTime();
            editor.getPane().setEditorStatus("Last execution time: " + (execTime > 0 ? "" + execTime : "<1") + " mS");
        } else {
            editor.getPane().setEditorStatus("Last query was cancelled");
        }

        StudioPanel panel = editor.getPanel();
        if (error == null || error instanceof c.K4Exception) {
            try {
                if (queryResult.isComplete()) {
                    JTabbedPane tabbedPane = panel.tabbedPane;
                    if (tabbedPane.getTabCount() >= CONFIG.getResultTabsCount()) {
                        for (int index = 0; index < tabbedPane.getTabCount(); index++) {
                            TabPanel tab = (TabPanel)tabbedPane.getComponentAt(index);
                            if (!tab.isPinned()) {
                                tabbedPane.removeTabAt(index);
                                break;
                            }
                        }
                    }
                    TabPanel tab = new TabPanel(panel, queryResult);
                    tab.addInto(tabbedPane);
                    tab.setToolTipText(editor.getServer().getConnectionString());
                }
                error = null;
            } catch (Throwable exc) {
                error = new RuntimeException("Error during result rendering", exc);
                log.error("Error during result rendering", exc);
            }
        }

        if (error != null) {
            String message = error.getMessage();
            if ((message == null) || (message.length() == 0))
                message = "No message with exception. Exception is " + error;
            StudioOptionPane.showError(editor.getPane(),
                    "\nAn unexpected error occurred whilst communicating with " +
                            editor.getServer().getConnectionString() +
                            "\n\nError detail is\n\n" + message + "\n\n",
                    "Studio for kdb+");
        }

        panel.refreshActionState();
    }

    public static Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

        for (StudioPanel panel : allPanels) {
            Workspace.Window window = workspace.addWindow(panel.getFrame() == activeWindow);
            panel.rootEditorsPanel.getWorkspace(window);
        }
        return workspace;
    }

    public void windowClosing(WindowEvent e) {
        closePanel();
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
        this.invalidate();
        SwingUtilities.updateComponentTreeUI(this);
    }


    public void windowDeactivated(WindowEvent e) {
    }

}
