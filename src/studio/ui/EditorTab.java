package studio.ui;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.Session;
import studio.ui.action.QueryExecutor;
import studio.ui.action.QueryResult;
import studio.ui.action.QueryTask;
import studio.ui.rstextarea.StudioRSyntaxTextArea;
import studio.ui.statusbar.EditorStatusBarCallback;
import studio.utils.Content;
import studio.utils.FileReaderWriter;
import studio.utils.FileWatcher;
import studio.utils.LineEnding;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class EditorTab implements FileWatcher.Listener, EditorStatusBarCallback {

    private String title;
    private String filename = null;
    private Server server = Server.NO_SERVER;
    private boolean modified = false;
    private LineEnding lineEnding = LineEnding.Unix;

    private static int scriptNumber = 0;

    private StudioWindow studioWindow;
    private EditorPane editorPane;

    private long modifiedTimeOnDisk = 0;
    private boolean watchFile = true;

    private Session session = null;

    private static final Logger log = LogManager.getLogger();

    private final static Cursor textCursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    private final static Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    public EditorTab(StudioWindow studioWindow) {
        this.studioWindow = studioWindow;
        init();
    }

    public Session getSession() {
        return session;
    }

    public void closing() {
        stopFileWatching();
        if (session != null) {
            session.removeTab(this);
        }
    }

    private void init() {
        if (editorPane != null) throw new IllegalStateException("The EditorTab has been already initialized");

        editorPane = new EditorPane(true, studioWindow.getEditorSearchPanel(), studioWindow.getMainStatusBar());
        editorPane.setEditorStatusBarCallback(this);
        editorPane.setFocusable(false);
        RSyntaxTextArea textArea = editorPane.getTextArea();
        textArea.setName("editor" + studioWindow.nextEditorNameIndex());

        textArea.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {
                Transferable t = dtde.getTransferable();
                if (!t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return;
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles = (java.util.List<File>)
                            t.getTransferData(DataFlavor.javaFileListFlavor);
                    File[] files = droppedFiles.toArray(new File[droppedFiles.size()]);
                    SwingUtilities.invokeLater(() -> dropFiles(files));
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    log.info("Error in drag and drop event processing", e);
                }
            }
        });


        JPopupMenu popupMenu = textArea.getPopupMenu();
        popupMenu.insert(studioWindow.getSplitAction(false), 0);
        popupMenu.insert(studioWindow.getSplitAction(true), 1);
        popupMenu.insert(new JPopupMenu.Separator(), 2);

        textArea.putClientProperty(QueryExecutor.class, new QueryExecutor(this));
    }

    public void init(Content content) {
        setContent(content);
        RSyntaxTextArea textArea = getTextArea();
        //@TODO do we need to discard all changes??
        textArea.discardAllEdits();
        textArea.requestFocus();
    }

    public void executeQuery(QueryTask queryTask) {
        if (server == Server.NO_SERVER) {
            log.info("Server is not set. Can't execute the query");
            return;
        }
        getTextArea().setCursor(waitCursor);
        editorPane.setEditorStatus("Executing: " + queryTask.getQueryText());
        getQueryExecutor().execute(queryTask);
        editorPane.startClock();
        studioWindow.refreshActionState();
    }

    // if the query is cancelled execTime=-1, result and error are null's
    public void queryExecutionComplete(QueryResult queryResult) {
        editorPane.stopClock();
        JTextComponent textArea = getTextArea();
        textArea.setCursor(textCursor);
        if (queryResult.isComplete()) {
            long execTime = queryResult.getExecutionTimeInMS();
            editorPane.setEditorStatus("Last execution time: " + (execTime > 0 ? "" + execTime : "<1") + " ms");
        } else {
            editorPane.setEditorStatus("Last query was cancelled");
        }

        try {
            if (queryResult.hasResultObject()) {
                studioWindow.addResultTab(queryResult, "Executed at server: " + queryResult.getServer().getDescription(true) );
            }
        } catch (Throwable error) {
            log.error("Error during result rendering", error);

            String message = error.getMessage();
            if ((message == null) || (message.length() == 0))
                message = "No message with exception. Exception is " + error;
            StudioOptionPane.showError(editorPane,
                    "\nAn unexpected error occurred whilst communicating with " +
                            server.getConnectionString() +
                            "\n\nError detail is\n\n" + message + "\n\n",
                    "Studio for kdb+");
        }

        studioWindow.refreshActionState();
    }

    @Override
    public void connect(String authMethod) {
        if (! server.getAuthenticationMechanism().equals(authMethod)) {
            Server newServer = Config.getInstance().getServerByConnectionString(server.getConnectionString(), authMethod);
            studioWindow.setServer(newServer);
        }

        executeQuery(QueryTask.connect());
    }

    @Override
    public void disconnect() {
        session.close();
    }

    private void dropFiles(File... files) {
        files = Stream.of(files)
                .filter(File::isFile)
                .toArray(File[]::new);

        if (files.length > 1) {
            files = Stream.of(files)
                    .filter(f -> FilenameUtils.getExtension(f.getName()).equals("q"))
                    .toArray(File[]::new);
        }

        if (files.length > 10) {
            int choice = StudioOptionPane.showYesNoDialog(studioWindow,
                    "" + files.length + " files are going to be opened. Are you sure?", "Are you sure?");
            if (choice != JOptionPane.YES_OPTION) return;
        }

        EditorsPanel editorsPanel = getEditorsPanel();
        for (File file: files) {
            String name = file.toString();
            editorsPanel.addTab(server).loadFile(name);
        }
    }

    private void setContent(Content content) {
        try {
            RSyntaxTextArea textArea = getTextArea();
            Document doc = textArea.getDocument();
            int caretPosition = textArea.getCaretPosition();
            doc.remove(0, textArea.getDocument().getLength());
            doc.insertString(0, content.getContent(),null);
            textArea.setCaretPosition(Math.min(caretPosition, doc.getLength()));
            setLineEnding(content.getLineEnding());
            setModified(content.isModified());
            modifiedTimeOnDisk = readModifiedTime();
        }
        catch (BadLocationException e) {
            log.error("Unexpected exception", e);
        }
    }

    public Content getContent() {
        return Content.newContent(getTextArea().getText(), lineEnding);
    }

    public StudioWindow getStudioWindow() {
        return studioWindow;
    }

    public void setSessionConnection(boolean connected) {
        editorPane.setSessionConnected(connected, server.getAuthenticationMechanism());
    }

    public void setStudioWindow(StudioWindow studioWindow) {
        this.studioWindow = studioWindow;
    }

    public EditorPane getPane() {
        return editorPane;
    }

    public StudioRSyntaxTextArea getTextArea() {
        return editorPane.getTextArea();
    }

    public String getFilename() {
        return filename;
    }

    public void loadFile(String filename) {
        setFilename(filename);

        Content content = filename == null ? Content.NO_CONTENT : EditorsPanel.loadFile(filename);
        init(content);
    }

    public void setFilename(String filename) {
        if (Objects.equals(this.filename, filename)) return;
        stopFileWatching();

        this.filename = filename;
        title = getTitleFromFilename();
        EditorsPanel.refreshEditorTitle(this);

        startFileWatching();
    }

    private String getTitleFromFilename() {
        if (filename == null) {
            return "Script" + scriptNumber++;
        } else {
            return new File(filename).getName();
        }

    }

    public String getTitle() {
        if (title == null) {
            title = getTitleFromFilename();
        }
        return title;
    }

    // If filename != null, returns baseName of the filename
    // If server == NO_SERVER, returns "Script"+index (is it possible??)
    // If server has a name, then server name
    // Server host:port
    private String getTabTitleInternal() {
        if (getFilename() != null) return getTitle();
        Server server = getServer();
        if (server == Server.NO_SERVER) return getTitle();

        if (server.getName().length() > 0) return server.getName();
        return server.getHost() + ":" + server.getPort();
    }

    public String getTabTitle() {
        String title = getTabTitleInternal();

        if (getFilename() == null) return title;
        if (isModified()) title = title + " *";
        return title;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean value) {
        if (modified == value) return;
        if (! value) {
            watchFile = true;
        }
        modified = value;
        EditorsPanel.refreshEditorTitle(this);
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        if (this.server == server) return;

        if (session != null) {
            session.removeTab(this);
        }
        this.server = server;
        session = Session.newSession(this);
        setSessionConnection(!session.isClosed());

        getTextArea().setBackground(server.getBackgroundColor());

        studioWindow.getMainStatusBar().setTemporaryStatus("Changed server: " + server.getDescription(true));
    }

    public LineEnding getLineEnding() {
        return lineEnding;
    }

    public void setLineEnding(LineEnding lineEnding) {
        if (this.lineEnding == lineEnding) return;

        this.lineEnding = lineEnding;
        setModified(true);
    }

    public QueryExecutor getQueryExecutor() {
        return (QueryExecutor) getTextArea().getClientProperty(QueryExecutor.class);
    }

    public void stopFileWatching() {
        if (filename == null) return;
        FileWatcher.removeListener(this);
    }

    public void startFileWatching() {
        if (filename == null) return;
        FileWatcher.addListener(new File(filename).toPath(), this);
    }

    private long readModifiedTime() {
        if (filename == null) return 0;

        return new File(filename).lastModified();
    }

    // returns true if saved, false if error or cancelled
    public boolean saveFileOnDisk(boolean autoSave) {
        if (filename == null) return false;

        if (autoSave && modifiedTimeOnDisk != readModifiedTime()) return false;

        try {
            FileReaderWriter.write(filename, getTextArea().getText(), lineEnding);
            setModified(false);
            modifiedTimeOnDisk = readModifiedTime();
            studioWindow.addToMruFiles(filename);
            return true;
        }
        catch (IOException e) {
            log.error("Error during saving file " + filename, e);
            studioWindow.getMainStatusBar().setTemporaryStatus("Error during saving file " + filename);
        }

        return false;
    }

    @Override
    public void fileModified(Path path) {
        long nowModifiedTimeOnDisk = readModifiedTime();
        if (modifiedTimeOnDisk == nowModifiedTimeOnDisk) return;

        if (!watchFile) return;

        if (nowModifiedTimeOnDisk == 0) {
            setModified(true);
            studioWindow.getMainStatusBar().setTemporaryStatus("File " + filename + " was removed on disk.");
            return;
        }

        if (modified) {
            watchFile = false; // prevent to show new dialogs until this one is answered
            int result = StudioOptionPane.reloadFileDialog(editorPane, "File " + filename +
                                " was modified on disk.\nReload and override local changes?\n\n" +
                    "If you select Cancel, then you will be notified about file modifications again.\n\n" +
                    "However if you select Ignore All, there will be no future notifications\n" +
                    "about file modifications until the file is saved in the Studio.", "Reload");

            if (result != StudioOptionPane.IGNOREALL_RESULT) {
                watchFile = true;
            }
            modifiedTimeOnDisk = readModifiedTime();

            if (result != StudioOptionPane.RELOAD_RESULT) {
                return;
            }
        }

        try {
            Content content = FileReaderWriter.read(filename);
            if (content.hasMixedLineEnding()) {
                log.warn("{} has mixing line ending. Line ending will be update to {}", filename, content.getLineEnding());
            }
            setContent(content);
            studioWindow.getMainStatusBar().setTemporaryStatus("Reloaded: " + filename);
        } catch (IOException e) {
            log.error("Can't reload {} with error {}", filename, e.getMessage());
            studioWindow.getMainStatusBar().setTemporaryStatus("Reload of " + filename + " failed");
        }
    }

    //@TODO quite hacky... needs to think of better design...
    public EditorsPanel getEditorsPanel() {
        if (!isAddedToPanel()) return null;
        return (EditorsPanel) getPane().getParent().getParent();
    }

    public void selectEditor() {
        getEditorsPanel().selectTab(this);
    }

    public boolean isAddedToPanel() {
        return getPane().getParent() != null;
    }
}
