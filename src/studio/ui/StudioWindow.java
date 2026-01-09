package studio.ui;

import kx.KMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import studio.kdb.*;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.ExecAllOption;
import studio.ui.action.*;
import studio.ui.chart.Chart;
import studio.ui.dndtabbedpane.DragEvent;
import studio.ui.dndtabbedpane.DraggableTabbedPane;
import studio.ui.search.SearchPanel;
import studio.ui.statusbar.MainStatusBar;
import studio.utils.Content;
import studio.utils.LineEnding;
import studio.utils.log4j.EnvConfig;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static javax.swing.JSplitPane.VERTICAL_SPLIT;
import static studio.ui.EscapeDialog.DialogResult.ACCEPTED;
import static studio.ui.EscapeDialog.DialogResult.CANCELLED;

public class StudioWindow extends StudioFrame {

    private static final Logger log = LogManager.getLogger();

    private boolean loading = true;

    private StringComboBox comboServer;
    private JTextField txtServer;
    private String lastQuery = null;
    private Toolbar toolbar;
    private EditorsPanel rootEditorsPanel;
    private EditorTab editor; // should be NotNull
    private JSplitPane splitpane;
    private MainStatusBar mainStatusBar;
    private DraggableTabbedPane resultsPane;
    private SearchPanel editorSearchPanel;
    private SearchPanel resultSearchPanel;
    private ServerList serverList;

    private StudioActionMap studioActionMap;
    private MenuFactory menuFactory;

    private StudioAction serverBackAction;
    private StudioAction serverForwardAction;
    private StudioAction openInExcel;
    private StudioAction codeKxComAction;
    private StudioAction serverListAction;
    private StudioAction exportAction;
    private StudioAction chartAction;
    private StudioAction undoAction;
    private StudioAction redoAction;
    private StudioAction cutAction;
    private StudioAction copyAction;
    private StudioAction pasteAction;
    private StudioAction findAction;
    private StudioAction replaceAction;
    private StudioAction stopAction;
    private StudioAction executeAction;
    private StudioAction executeCurrentLineAction;
    private StudioAction refreshAction;
    private StudioAction minMaxDividerAction;
    private StudioAction editServerAction;
    private StudioAction removeServerAction;
    private StudioAction uploadAction;
    private StudioAction prevResultAction;
    private StudioAction nextResultAction;
    private StudioAction[] lineEndingActions;
    private StudioAction wordWrapAction;
    private StudioAction splitEditorRight;
    private StudioAction splitEditorDown;

    private static int studioWindowNameIndex = 0;
    private int editorTabbedPaneNameIndex = 0;
    private int editorNameIndex = 0;
    private int resultNameIndex = 0;

    private final static int MAX_SERVERS_TO_CLONE = 20;

    public static final Config CONFIG = Config.getInstance();
    private final List<Server> serverHistory = CONFIG.getServerHistory();

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
            if (!fullName.isEmpty()) caption.append(fullName);
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

    private void disableActions(Action... actions) {
        for (Action action: actions) {
            if (action != null) {
                action.setEnabled(false);
            }
        }
    }

    public boolean isQueryRunning() {
        return editor.getQueryExecutor().running();
    }

    public void refreshActionState() {
        RSyntaxTextArea textArea = editor.getTextArea();
        Server server = editor.getServer();
        serverBackAction.setEnabled(editor.hasPreviousServerInHistory());
        serverForwardAction.setEnabled(editor.hasNextServerInHistory());
        editServerAction.setEnabled(server != Server.NO_SERVER);
        removeServerAction.setEnabled(server != Server.NO_SERVER);

        undoAction.setEnabled(textArea.canUndo());
        redoAction.setEnabled(textArea.canRedo());

        wordWrapAction.setSelected(textArea.getLineWrap());

        for (LineEnding lineEnding: LineEnding.values() ) {
            lineEndingActions[lineEnding.ordinal()].setSelected(editor.getLineEnding() == lineEnding);
        }

        boolean queryRunning = isQueryRunning();
        stopAction.setEnabled(queryRunning);
        executeAction.setEnabled(!queryRunning);
        executeCurrentLineAction.setEnabled(!queryRunning);
        refreshAction.setEnabled(lastQuery != null && !queryRunning);

        ResultTab tab = getSelectedResultTab();
        if (tab == null) {
            disableActions(exportAction, chartAction, openInExcel, refreshAction, uploadAction, prevResultAction, nextResultAction);
        } else {
            ResultType type = tab.getType();
            exportAction.setEnabled(type.isTable());
            chartAction.setEnabled(type == ResultType.TABLE);
            openInExcel.setEnabled(type.isTable());
            refreshAction.setEnabled(true);
            uploadAction.setEnabled(type != ResultType.ERROR);
            prevResultAction.setEnabled(tab.hasPreviousResult());
            nextResultAction.setEnabled(tab.hasNextResult());
            tab.refreshActionState();
        }
    }

    private void exportAsExcel(final String filename) {
        ExcelExporter.exportTableX(this, getSelectedResultTab(),new File(filename),false);
    }

    private void exportAsDelimited(final KTableModel model,final String filename,final char delimiter) {
        if (model == null) return;
        UIManager.put("ProgressMonitor.progressText","Studio for kdb+");
        final ProgressMonitor pm = new ProgressMonitor(this,"Exporting data to " + filename,
                "0% complete",0,100);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = () -> {
            if (filename != null) {
                String lineSeparator = System.lineSeparator();

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
                        String note = progress + "% complete";
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

    private void exportAsXml(final KTableModel model,final String filename) {
        UIManager.put("ProgressMonitor.progressText","Studio for kdb+");
        final ProgressMonitor pm = new ProgressMonitor(this,"Exporting data to " + filename,
                "0% complete",0,100);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = () -> {
            if (filename != null) {
                String lineSeparator = System.lineSeparator();

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
                        String note = progress + "% complete";
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
        exportAsDelimited(getSelectedTableModel(), filename, '\t');
    }

    private void exportAsCSV(String filename) {
        exportAsDelimited(getSelectedTableModel(), filename, ',');
    }

    public void export() {
        if (getSelectedTableModel() == null) return;

        File file = FileChooser.chooseFile(this, Config.EXPORT_FILE_CHOOSER, JFileChooser.SAVE_DIALOG, "Export result set as",
                null,
                FileChooser.CSV_FF, FileChooser.TXT_FF, FileChooser.XML_FF, FileChooser.XLS_FF );

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
                exportAsXml(getSelectedTableModel(), filename);
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

    public void openFile() {
        File file = FileChooser.openFile(this, FileChooser.Q_FF);

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
        List<String> mruFiles = CONFIG.getStringArray(Config.MRU_FILES);

        List<String> list = new ArrayList<>();
        list.add(filename);
        for (String mruFile: mruFiles) {
            if (list.size() == 9) break;

            if (!filename.equals(mruFile)) list.add(mruFile);
        }

        CONFIG.setStringArray(Config.MRU_FILES, list);
        refreshAllMenus();
    }

    public boolean execute(EditorsPanel.EditorTabAction action) {
        return rootEditorsPanel.execute(action);
    }


    public static void saveAll() {
        WindowFactory.forEachEditors(editorTab -> {
            if ( !editorTab.saveFileOnDisk(false)) {
                throw new WindowFactory.StopIteration();
            }
        });
    }

    public void arrangeAll() {
        List<StudioWindow> windows = WindowFactory.allStudioWindows();
        int noWins = windows.size();
        int noRows = Math.min(noWins, 3);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int height = screenSize.height / noRows;

        int row = -1;
        int col = 0, noCols = 0, width = 0;
        for (StudioWindow window: windows) {
            if (col == noCols) {
                row++;
                col = 0;
                noCols = noWins / noRows + (row < noWins % noRows ? 1 : 0);
                width = screenSize.width / noCols;
            }
            window.setSize(width,height);
            window.setLocation(col * width,((noRows - 1) - row) * height);
            WindowFactory.activate(window);
            col++;
        }
    }

    public void setServer(Server server) {
        editor.setServer(server);
        if (!loading) {
            serverHistory.add(server);
            CONFIG.refreshServerToHistory();

            EditorsPanel.refreshEditorTitle(editor);
            refreshServer();
            editor.getTextArea().requestFocus();
        }
    }

    public void editServer() {
        EditServerForm f = new EditServerForm(this, editor.getServer());
        f.alignAndShow();
        if (f.getResult() == ACCEPTED) {
            if (stopAction.isEnabled())
                stopAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, Actions.stop));

            Server newServer = f.getServer();
            if (newServer.inServerTree()) {
                CONFIG.getServerConfig().replaceServer(editor.getServer(), newServer);
            }
            setServer(newServer);
            refreshAll();
        }
    }

    public void addServer() {
        AddServerForm f = new AddServerForm(this, editor.getServer());
        f.alignAndShow();
        if (f.getResult() == ACCEPTED) {
            Server server = f.getServer();
            if (server.inServerTree()) {
                CONFIG.getServerConfig().addServer(server);
            }
            setServer(server);
            refreshAll();
        }
    }

    public void removeServer() {
        int choice = JOptionPane.showOptionDialog(this,
                "Remove server " + editor.getServer().getFullName() + " from list?",
                "Remove server?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                Util.QUESTION_ICON,
                null, // use standard button titles
                null);      // no default selection

        if (choice == 0) {
            CONFIG.getServerConfig().removeServer(editor.getServer());

            Server[] servers = CONFIG.getServerConfig().getServers();

            if (servers.length > 0)
                setServer(servers[0]);

            refreshAll();
        }
    }

    public void openInExcel() {
        try {
            File file = File.createTempFile("studioExport", ".xlsx");
            ExcelExporter.exportTableX(this, getSelectedResultTab(), file, true);
        } catch (IOException ex) {
            log.error("Failed to create temporary file", ex);
            StudioOptionPane.showError(this, "Failed to Open in Excel " + ex.getMessage(),"Error");
        }
    }

    public void setLineEnding(LineEnding lineEnding) {
        editor.setLineEnding(lineEnding);
        refreshActionState();
    }

    private void initActions() {
        studioActionMap = ActionRegistry.getStudioActionMap(this);
        serverBackAction = studioActionMap.get(Actions.serverBack);
        serverForwardAction = studioActionMap.get(Actions.serverForward);
        minMaxDividerAction = studioActionMap.get(Actions.minMaxDivider);
        serverListAction = studioActionMap.get(Actions.serverList);
        editServerAction = studioActionMap.get(Actions.editServer);
        removeServerAction = studioActionMap.get(Actions.removeServer);
        exportAction = studioActionMap.get(Actions.export);
        chartAction = studioActionMap.get(Actions.chart);
        stopAction = studioActionMap.get(Actions.stop);
        openInExcel = studioActionMap.get(Actions.openInExcel);
        executeAction = studioActionMap.get(Actions.execute);
        executeCurrentLineAction = studioActionMap.get(Actions.executeCurrentLine);
        refreshAction = studioActionMap.get(Actions.refresh);
        uploadAction = studioActionMap.get(Actions.upload);
        prevResultAction = studioActionMap.get(Actions.prevResult);
        nextResultAction = studioActionMap.get(Actions.nextResult);
        codeKxComAction = studioActionMap.get(Actions.codeKxCom);
        copyAction = studioActionMap.get(Actions.copy);
        cutAction = studioActionMap.get(Actions.cut);
        pasteAction = studioActionMap.get(Actions.paste);
        undoAction = studioActionMap.get(Actions.undo);
        redoAction = studioActionMap.get(Actions.redo);
        findAction = studioActionMap.get(Actions.find);
        replaceAction = studioActionMap.get(Actions.replace);

        lineEndingActions = new StudioAction[LineEnding.values().length];
        lineEndingActions[LineEnding.Unix.ordinal()] = studioActionMap.get(Actions.lineEndingUnix);
        lineEndingActions[LineEnding.Windows.ordinal()] = studioActionMap.get(Actions.lineEndingWindows);
        lineEndingActions[LineEnding.MacOS9.ordinal()] = studioActionMap.get(Actions.lineEndingMacOS9);

        wordWrapAction = studioActionMap.get(Actions.wordWrap);

        splitEditorRight = studioActionMap.get(Actions.splitEditorRight);
        splitEditorDown = studioActionMap.get(Actions.splitEditorDown);
    }

    public Action getSplitAction(boolean vertically) {
        return vertically ? splitEditorDown : splitEditorRight;
    }

    public static void settings() {
        SettingsDialog dialog = new SettingsDialog(WindowFactory.getActiveWindow());
        dialog.alignAndShow();
        if (dialog.getResult() == CANCELLED) return;

        dialog.saveSettings();
    }

    public void toggleWordWrap() {
        boolean newValue = wordWrapAction.isSelected();
        getActiveEditor().getTextArea().setLineWrap(newValue);
    }

    public static void about() {
        HelpDialog help = new HelpDialog(WindowFactory.getActiveWindow());
        help.alignAndShow();
    }

    private static volatile boolean quitting = false;

    public static void quit() {
        if (quitting) return;
        quitting = true;

        WorkspaceSaver.setEnabled(false);
        try {
            ActionOnExit action = CONFIG.getEnum(Config.ACTION_ON_EXIT);
            if (action != ActionOnExit.NOTHING) {
                for (StudioWindow window : WindowFactory.allStudioWindows().toArray(new StudioWindow[0])) {
                    window.toFront();
                    boolean complete = window.execute(editorTab -> {
                        if (editorTab.isModified()) {
                            if (!EditorsPanel.checkAndSaveTab(editorTab)) {
                                return false;
                            }

                            if (editorTab.isModified()) {
                                if (action == ActionOnExit.CLOSE_ANONYMOUS_NOT_SAVED && editorTab.getFilename()==null) {
                                    editorTab.getEditorsPanel().closeTabForced(editorTab);
                                }
                            }
                        }
                        return true;
                    });
                    if (!complete) {
                        quitting = false;
                        return;
                    }
                }
            }
        } finally {
            StudioWindow window = WindowFactory.getLastActiveWindow();
            if (window != null) {
                WindowFactory.activate(window);
            }
            WorkspaceSaver.setEnabled(true);
        }
        WorkspaceSaver.save(getWorkspace());
        CONFIG.exit();
    }

    public void close() {
        // If this is the last window, we need to properly persist workspace
        if (WindowFactory.allStudioWindows().size() == 1) {
            quit();
        } else {
            boolean result = execute(editorTab -> editorTab.getEditorsPanel().closeTab(editorTab));
            if (!result) return;
            dispose();
        }

    }

    public static void refreshAll() {
        refreshAllMenus();
        WindowFactory.forEach(StudioWindow::refreshServerList);
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

    public static void refreshAllMenus() {
        WindowFactory.forEach(StudioWindow::refreshMenu);
    }

    private void cloneServer(Server server) {
        Server clone = server.newName("Clone of " + server.getName());
        EditServerForm f = new EditServerForm(StudioWindow.this, clone);
        f.alignAndShow();

        if (f.getResult() == ACCEPTED) {
            clone = f.getServer();
            if (clone.inServerTree()) {
                CONFIG.getServerConfig().addServer(clone);
            }
            setServer(clone);
            refreshAll();
        }
    }

    private void refreshMenu() {
        menuFactory.cleanAtMarker(MenuFactory.openMRU_MARK);
        int index = 1;
        for (String filename: CONFIG.getStringArray(Config.MRU_FILES)) {
            menuFactory.addAtMarker(MenuFactory.openMRU_MARK,
                    BaseAction.build((index++) + " " + filename, null, filename, this::loadMRUFile) );
        }

        menuFactory.cleanAtMarker(MenuFactory.clone_MARK);
        Server[] servers = CONFIG.getServerConfig().getServers();
        int count = Math.min(MAX_SERVERS_TO_CLONE, servers.length);
        for (int i = 0; i < count; i++) {
            menuFactory.addAtMarker(MenuFactory.clone_MARK,
                    BaseAction.build(servers[i].getFullName(), null, servers[i], this::cloneServer) );
        }


        menuFactory.cleanAtMarker(MenuFactory.windows_MARK);

        List<StudioWindow> windows = WindowFactory.allStudioWindows();
        if (!windows.isEmpty()) {
            menuFactory.addAtMarker(MenuFactory.windows_MARK, null);

            index = 1;
            for (StudioWindow window: windows) {
                menuFactory.addAtMarker(MenuFactory.windows_MARK,
                        BaseAction.build((index++) + " " + window.getCaption(),
                                window == this ? Util.CHECK_ICON : Util.BLANK_ICON,
                                window, WindowFactory::activate )
                        );
            }
        }

        List<Chart> charts = WindowFactory.allCharts();
        if (!charts.isEmpty()) {
            menuFactory.addAtMarker(MenuFactory.windows_MARK, null);

            index = 1;
            for(Chart chart: charts) {
                menuFactory.addAtMarker(MenuFactory.windows_MARK,
                        BaseAction.build((index++) + " " + chart.getChartTitle(),
                                chart.getFrame(), WindowFactory::activate )
                );
            }
        }

    }

    private void createMenuBar() {
        menuFactory = ActionRegistry.getMenuFactory(studioActionMap);
        JMenuBar menuBar = menuFactory.buildMenuBar();
        setNamesInMenu(menuBar);
        setJMenuBar(menuBar);
    }

    private void selectConnectionString() {
        String connection = txtServer.getText().trim();
        if (connection.isEmpty()) return;
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

    public void showServerList(boolean selectHistory) {
        log.info("Show server list from {}", getTitle());
        Server selectedServer = serverList.showServerTree(editor.getServer(), serverHistory, selectHistory);

        if (selectedServer == null || selectedServer.equals(editor.getServer())) return;

        setServer(selectedServer);

        refreshAllMenus();
        refreshServerListAllWindows();
    }

    private void selectServerName() {
        String selection = comboServer.getSelectedItem();
        if (selection == null) return;
        if(! CONFIG.getServerConfig().getServerNames().contains(selection)) return;

        setServer(CONFIG.getServerConfig().getServer(selection));
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

    public StringComboBox getComboServer() {
        return comboServer;
    }

    public static void refreshServerListAllWindows() {
        WindowFactory.forEach(StudioWindow::refreshServerList);
    }

    private void refreshServerList() {
        Server server = editor.getServer();
        Collection<String> names = CONFIG.getServerConfig().getServerNames();
        String name = server == Server.NO_SERVER ? "" : server.getFullName();
        if (!names.contains(name)) {
            List<String> newNames = new ArrayList<>();
            newNames.add(name);
            newNames.addAll(names);
            names = newNames;
        }

        comboServer.setItems(names);

        comboServer.getModel().setSelectedItem(name);
    }


    private void refreshServer() {
        Server server = editor.getServer();
        String name = server == Server.NO_SERVER ? "" : server.getFullName();
            comboServer.setSelectedItem(name);

        refreshConnectionText();
        refreshActionState();
    }


    private void initToolbar() {
        comboServer = new StringComboBox();
        comboServer.setVisible(CONFIG.getBoolean(Config.SHOW_SERVER_COMBOBOX));
        comboServer.setName("serverDropDown");
        comboServer.setToolTipText("Select the server context");
        comboServer.addActionListener(e->selectServerName());
        // Cut the width if it is too wide.
        comboServer.setMinimumSize(new Dimension(0, 0));

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

        Object[] actions = new Object[] {
                Box.createRigidArea(new Dimension(3,0)),
                new JLabel("Server: "),
                comboServer, txtServer,
                serverBackAction, serverForwardAction,
                serverListAction, null,
                stopAction, executeAction, refreshAction, null,
                openInExcel, null, exportAction, null, chartAction, null, undoAction, redoAction, null,
                cutAction, copyAction, pasteAction, null, findAction, replaceAction, null, codeKxComAction };

        toolbar.addAll(actions);

        addNavigationList(toolbar.getButton(serverBackAction), false);
        addNavigationList(toolbar.getButton(serverForwardAction), true);

        refreshActionState();
    }

    private void addNavigationList(AbstractButton button, boolean forward) {
        button.addMouseListener(new MouseAdapter() {

            private void checkPopup(MouseEvent e) {
                if (! e.isPopupTrigger()) return;

                List<String> list = editor.getNavigationList(forward);
                int count = list.size();
                if (count == 0) return;

                JPopupMenu menu = new JPopupMenu();
                for (int index = 0; index <count; index++) {
                    JMenuItem menuItem = new JMenuItem(list.get(index));
                    final int shift = index + 1;
                    menuItem.addActionListener(actionEvent-> {
                        editor.navigateHistoryServer(shift, forward);
                    });

                    menu.add(menuItem);
                }

                menu.show(button, e.getX(), e.getY());
            }


            @Override
            public void mousePressed(MouseEvent e) {
                checkPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                checkPopup(e);
            }
        });
    }

    private int dividerLastPosition; // updated from property change listener
    public void minMaxDivider(){
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

    public void toggleDividerOrientation() {
        if (splitpane.getOrientation() == JSplitPane.VERTICAL_SPLIT) {
            splitpane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
            resultsPane.setTabPlacement(JTabbedPane.LEFT);
        } else {
            splitpane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            resultsPane.setTabPlacement(JTabbedPane.TOP);
        }

        int count = resultsPane.getTabCount();
        for (int index = 0; index<count; index++) {
            ((ResultTab) resultsPane.getComponent(index)).updateToolbarLocation(resultsPane);
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

    public EditorsPanel getRootEditorsPanel() {
        return rootEditorsPanel;
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

        ResultTab tab = getSelectedResultTab();
        if (tab == null) return;

        if (tab.getEditor() != null) {
            mainStatusBar.updateStatuses(tab.getEditor().getTextArea());
        } else if (tab.getGrid() != null) {
            mainStatusBar.updateStatuses(tab.getGrid().getTable());
        } else if (tab.getType() == ResultType.ERROR) {
            mainStatusBar.resetStatuses();
        }
    }

    private void resultTabDragged(DragEvent event) {
        DraggableTabbedPane targetPane = event.getTargetPane();
        StudioWindow targetStudiowWindow = (StudioWindow) targetPane.getClientProperty(StudioWindow.class);
        ((ResultTab)targetPane.getComponentAt(event.getTargetIndex())).setStudioWindow(targetStudiowWindow);
    }

    StudioWindow() {}

    void init(Workspace.TopWindow workspaceWindow) {
        setName("studioWindow" + (++studioWindowNameIndex));

        loading = true;

        initActions();
        createMenuBar();

        toolbar = createToolbar();
        editorSearchPanel = new SearchPanel( () -> editor.getPane() );
        editorSearchPanel.setName("SearchPanel");
        mainStatusBar = new MainStatusBar();
        resultsPane = initResultPane();
        resultSearchPanel = initResultSearchPanel();

        // We need to have some editor initialized to prevent NPE
        editor = new EditorTab(this);
        initToolbar();
        JPanel topPanel = new JPanel(new BorderLayout());

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
        bottomPanel.add(resultsPane, BorderLayout.CENTER);
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
                ResultTab panel = (ResultTab)tabbedPane.getComponentAt(index);
                return panel.isPinned();
            }
            @Override
            public void setPinned(int index, boolean pinned) {
                ResultTab panel = (ResultTab)tabbedPane.getComponentAt(index);
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
        tabbedPane.addDragListener(this::resultTabDragged);
        tabbedPane.addChangeListener(e -> {
            ResultTab resultTab = getSelectedResultTab();
            if (resultTab != null) resultTab.refreshActionState();
        });
        return tabbedPane;
    }

    private SearchPanel initResultSearchPanel() {
        SearchPanel resultSearchPanel = new SearchPanel(() -> {
            if (resultsPane.getTabCount() == 0) return null;
            ResultTab resultTab = getSelectedResultTab();
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
            public void propertyChange(PropertyChangeEvent pce) {
                int dividerLocation = splitpane.getDividerLocation();
                String actionName =
                        dividerLocation >= splitpane.getMaximumDividerLocation() ?
                                "Minimize Editor Pane" :
                                dividerLocation <= splitpane.getMinimumDividerLocation() ?
                                    "Restore Editor Pane" :
                                    "Maximize Editor Pane" ;
                minMaxDividerAction.putValue(Action.SHORT_DESCRIPTION, actionName);
                minMaxDividerAction.putValue(Action.NAME, actionName);
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
                EditorsPanel editorsPanel = editor.getEditorsPanel();
                if (editorsPanel == null) return;
                editorsPanel.setInFocusTabbedEditors(true);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                EditorsPanel editorsPanel = editor.getEditorsPanel();
                if (editorsPanel == null) return;
                editorsPanel.setInFocusTabbedEditors(false);
            }
        });

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(toolbar, BorderLayout.NORTH);
        contentPane.add(central, BorderLayout.CENTER);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        setContentPane(contentPane);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
            public void windowClosed(WindowEvent e) {
                refreshAllMenus();
            }
        });

        pack();
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
            WindowFactory.newStudioWindow(window);
        }

        List<StudioWindow> windows = WindowFactory.allStudioWindows();
        int index = workspace.getSelectedWindow();
        if (index >= 0 && index < windows.size()) {
            WindowFactory.activate(windows.get(index));
        }

        if (windows.isEmpty()) {
            WindowFactory.newStudioWindow(Server.NO_SERVER, null);
        }

        WindowFactory.forEach(StudioWindow::refreshFrameTitle);
    }

    public void refreshQuery() {
        editor.executeQuery(QueryTask.query(this, lastQuery));
    }

    public void executeQueryCurrentLine(boolean chart) {
        executeQuery(getCurrentLineEditorText(editor.getTextArea()), chart);
    }

    public void executeQuery(boolean chart) {
        executeQuery(getEditorText(editor.getTextArea()), chart);
    }

    private void executeQuery(String text, boolean chart) {
        if (text == null) {
            return;
        }
        text = text.trim();
        if (text.isEmpty()) {
            log.info("Nothing to execute - got empty string");
            return;
        }

        QueryTask queryTask = chart ? QueryTask.queryAndChart(this, text) : QueryTask.query(this, text);
        editor.executeQuery(queryTask);
        lastQuery = text;
    }

    public void chart() {
        KTableModel tableModel = getSelectedTableModel();
        if (tableModel == null) {
            StudioOptionPane.showWarning(this, "Can only chart from table result", "Table expected");
            return;
        }

        WindowFactory.newChart(tableModel);
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

        ExecAllOption option = CONFIG.getEnum(Config.EXEC_ALL);
        if (option == ExecAllOption.Ignore) {
            log.info("Nothing is selected. Ignore execution of the whole script");
            return null;
        }
        if (option == ExecAllOption.Execute) {
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
        catch (BadLocationException ignored) {
        }

        if (text != null) {
            text = text.trim();

            if (text.isEmpty())
                text = null;
        }

        return text;
    }

    private KTableModel getSelectedTableModel() {
        ResultTab tab = (ResultTab) resultsPane.getSelectedComponent();
        if (tab == null) return null;

        ResultGrid grid = tab.getGrid();
        if (grid == null) return null;

        return (KTableModel) grid.getTable().getModel();
    }

    public int countResultTabs() {
        return resultsPane.getTabCount();
    }

    public ResultTab getResultTab(int index) {
        return (ResultTab) resultsPane.getComponentAt(index);
    }

    public ResultTab getSelectedResultTab() {
        return (ResultTab) resultsPane.getSelectedComponent();
    }

    public void addResultTab(QueryResult queryResult, String tooltip) {
        ResultTab tab = new ResultTab(this, queryResult);
        tab.addInto(resultsPane, getTooltipText(tooltip, queryResult.getKMessage()));
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

    public static Workspace getWorkspace() {
        Workspace workspace = new Workspace();
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

        WindowFactory.forEach(window -> {
            Workspace.TopWindow workspaceWindow = workspace.addWindow(window == activeWindow);
            workspaceWindow.setResultDividerLocation(Util.getDividerLocation(window.splitpane));
            workspaceWindow.setLocation(window.getBounds());
            workspaceWindow.setServerListBounds(window.getServerList().getBounds());
            window.rootEditorsPanel.getWorkspace(workspaceWindow);
        });
        return workspace;
    }

}
