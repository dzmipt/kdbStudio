package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.Workspace;
import studio.ui.dndtabbedpane.DraggableTabbedPane;
import studio.ui.dndtabbedpane.SelectedTabDecoration;
import studio.ui.rstextarea.StudioRSyntaxTextArea;
import studio.utils.Content;
import studio.utils.FileReaderWriter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EditorsPanel extends JPanel {

    private EditorsPanel parent, left, right;
    private final StudioWindow studioWindow;
    private DraggableTabbedPane tabbedEditors;
    private JSplitPane splitPane;

    private static final Logger log = LogManager.getLogger();

    public EditorsPanel(StudioWindow studioWindow, Workspace.Window workspaceWindow) {
        this.studioWindow = studioWindow;

        if (workspaceWindow != null && workspaceWindow.isSplit()) {
            left = loadWorkspaceWindow(studioWindow, workspaceWindow.getLeft());
            right = loadWorkspaceWindow(studioWindow, workspaceWindow.getRight());
            init(left, right, workspaceWindow.isVerticalSplit());
            return;
        }

        initTabbedEditors();

        if (workspaceWindow != null) {
            loadWorkspaceTabs(workspaceWindow);
            if (tabbedEditors.getTabCount() == 0) {
                log.warn("Workspace is corrupted. No tab found in a split. Creating empty tab");
                addTab(Server.NO_SERVER);
            }
        }
    }

    private void initTabbedEditors() {
        tabbedEditors = new DraggableTabbedPane("Editor");
        tabbedEditors.setName("editorTabbedPane" + studioWindow.nextEditorTabbedPaneNameIndex());
        removeFocusChangeKeysForWindows(tabbedEditors);
        ClosableTabbedPane.makeCloseable(tabbedEditors, (index, force) -> closeTab(getEditorTab(index)) );
        tabbedEditors.addChangeListener(e -> activateSelectedEditor());
        tabbedEditors.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                activateSelectedEditor();
            }
        });
        tabbedEditors.addDragCompleteListener(success -> closeIfEmpty() );

        setLayout(new BorderLayout());
        add(tabbedEditors, BorderLayout.CENTER);
    }

    private boolean isVerticalSplit() {
        return splitPane.getOrientation() == JSplitPane.VERTICAL_SPLIT;
    }

    private void activateSelectedEditor() {
        if (tabbedEditors == null) return; // during split

        int index = tabbedEditors.getSelectedIndex();
        if (index!=-1) {
            // need to schedule focus transfer, otherwise focus would be transferred to other UI components which are touched later
            SwingUtilities.invokeLater(()-> {
                if (index < tabbedEditors.getTabCount()) {
                    //to prevent IndexOutOfBoundException when tab is closed
                    getEditorTab(index).getTextArea().requestFocus();
                }
            } );
        }
    }

    public List<EditorTab> getAllEditors(boolean selected) {
        List<EditorTab> result = new ArrayList<>();
        if (tabbedEditors!=null) {
            if (selected) {
                result.add(getEditorTab(tabbedEditors.getSelectedIndex()));
            } else {
                int count = tabbedEditors.getTabCount();
                for (int index=0; index<count; index++) {
                    result.add(getEditorTab(index));
                }
            }
        } else {
            result.addAll(left.getAllEditors(selected));
            result.addAll(right.getAllEditors(selected));
        }
        return result;
    }

    private void init(EditorsPanel left, EditorsPanel right, boolean verticalSplit) {
        this.left = left;
        this.right = right;
        left.parent = this;
        right.parent = this;

        splitPane = new JSplitPane(verticalSplit ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(left);
        splitPane.setRightComponent(right);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        validate();
        splitPane.setDividerLocation(0.5);
    }

    public void loadDividerLocation(Workspace.Window window) {
        if (window == null) return;
        if (splitPane == null) return;

        if (window.isSplit()) {
            splitPane.setDividerLocation(window.getDividerLocation());
            left.loadDividerLocation(window.getLeft());
            right.loadDividerLocation(window.getRight());
        }
    }

    public EditorTab addTab(Server server) {
        EditorTab editor = new EditorTab(studioWindow);

        StudioRSyntaxTextArea textArea = editor.getTextArea();
        textArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                log.info("FocusGained: {}", editor.getTabTitle());
                editor.getEditorsPanel().studioWindow.updateEditor(editor);
            }
        });
        removeFocusChangeKeysForWindows(textArea);

        JComponent editorPane = editor.getPane();
        editorPane.putClientProperty(EditorTab.class, editor);

        editor.setServer(server);

        addTab(editor);
        textArea.getDocument().addDocumentListener(new MarkingDocumentListener(editor));
        textArea.requestFocus();
        textArea.setActionsUpdateListener(() -> editor.getStudioWindow().refreshActionState());

        studioWindow.refreshActionState();
        refreshEditorTitle(editor);
        return editor;
    }

    private void addTab(EditorTab editorTab) {
        tabbedEditors.add(editorTab.getTabTitle(), editorTab.getPane());
        tabbedEditors.setSelectedIndex(tabbedEditors.getTabCount()-1);

        if (studioWindow.getActiveEditor() == editorTab && studioWindow == StudioWindow.getActiveStudioWindow()) {
            tabbedEditors.setSelectedTabDecoration(SelectedTabDecoration.UNDERLINE);
        }
    }

    public void split(boolean vertically) {
        if (tabbedEditors == null) return;

        int selectedIndex = tabbedEditors.getSelectedIndex();
        if (selectedIndex == -1) return;

        List<EditorTab> editors = getAllEditors(false);
        EditorTab editorToCopy = editors.get(selectedIndex);

        remove(tabbedEditors);
        tabbedEditors = null;

        EditorsPanel aLeft = new EditorsPanel(studioWindow, null);
        for(EditorTab editor: editors) {
            aLeft.addTab(editor);
        }
        aLeft.tabbedEditors.setSelectedIndex(selectedIndex);

        EditorsPanel aRight = new EditorsPanel(studioWindow, null);

        EditorTab newEditor = aRight.addTab(editors.get(selectedIndex).getServer());
        newEditor.setFilename(editorToCopy.getFilename());
        newEditor.init(editorToCopy.getContent());

        init(aLeft, aRight, vertically);
    }

    public void unite() {
        if (left == null || right == null) return;

        boolean leftHasTabs = left.tabbedEditors != null;
        boolean rightHasTabs = right.tabbedEditors != null;

        // can't unite if both children are split
        if (!leftHasTabs && !rightHasTabs) return;

        if (leftHasTabs && rightHasTabs) {
            // both children have tabs
            List<EditorTab> editors = left.getAllEditors(false);
            editors.addAll(right.getAllEditors(false));

            left = null;
            right = null;
            remove(splitPane);
            splitPane = null;

            initTabbedEditors();
            for (EditorTab editor : editors) {
                addTab(editor);
            }

        } else {
            // one child is split and another has tabs
            EditorsPanel splitChild = leftHasTabs ? right : left;
            EditorsPanel tabChild = leftHasTabs ? left : right;

            // can't do if there are >0 tabs
            if (tabChild.tabbedEditors.getTabCount() > 0) return;

            remove(splitPane);
            init(splitChild.left, splitChild.right, splitChild.isVerticalSplit());
        }
    }

    public void setInFocusTabbedEditors(boolean inFocus) {
        if (tabbedEditors == null) return;

        tabbedEditors.setSelectedTabDecoration(inFocus ? SelectedTabDecoration.UNDERLINE :
                                                        SelectedTabDecoration.NONE);
    }

    public static Content loadFile(String filename) {
        Content content = Content.NO_CONTENT;
        try {
            content = FileReaderWriter.read(filename);
            if (content.hasMixedLineEnding()) {
                StudioOptionPane.showMessage(StudioWindow.getActiveStudioWindow(), "The file " + filename + " has mixed line endings. Mixed line endings are not supported.\n\n" +
                                "All line endings are set to " + content.getLineEnding() + " style.",
                        "Mixed Line Ending");
            }
        } catch (IOException e) {
            log.error("Failed to load file {}", filename, e);
            StudioOptionPane.showError(StudioWindow.getActiveStudioWindow(), "Failed to load file "+filename + ".\n" + e.getMessage(),
                    "Error in file load");
        }
        return content;
    }

    public static boolean saveAsFile(EditorTab editor) {
        String filename = editor.getFilename();
        File file = FileChooser.chooseFile(editor.getStudioWindow(), Config.SAVE_FILE_CHOOSER, JFileChooser.SAVE_DIALOG, "Save script as",
                filename == null ? null : new File(filename),
                new FileNameExtensionFilter("q script", "q"));

        if (file == null) return false;

        filename = file.getAbsolutePath();
        if (file.exists()) {
            int choice = StudioOptionPane.showYesNoDialog(editor.getStudioWindow(),
                    filename + " already exists.\nOverwrite?",
                    "Overwrite?");

            if (choice != JOptionPane.YES_OPTION)
                return false;
        }

        editor.setFilename(filename);
        return editor.saveFileOnDisk(false);
    }

    public static boolean saveEditor(EditorTab editor) {
        if (editor.getFilename() == null) {
            return saveAsFile(editor);
        } else {
            return editor.saveFileOnDisk(false);
        }
    }

    public static boolean checkAndSaveTab(EditorTab editor) {
        if (! editor.isModified()) return true;

        editor.selectEditor();
        int choice = StudioOptionPane.showYesNoCancelDialog(editor.getPane(),
                editor.getTitle() + " is changed. Save changes?","Save changes?");

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return false;

        if (choice == JOptionPane.YES_OPTION) {
            return saveEditor(editor);
        }

        return true;
    }

    public void closeTabForced(EditorTab editorTab) {
        editorTab.closing();

        tabbedEditors.remove(editorTab.getPane());
        closeIfEmpty();
    }

    public boolean closeTab(EditorTab editorTab) {
        if (!checkAndSaveTab(editorTab)) return false;
        closeTabForced(editorTab);
        return true;
    }

    private void closeIfEmpty() {
        if (tabbedEditors.getTabCount()>0) return;

        if (parent == null) {
            studioWindow.close();
        } else {
            parent.unite();
        }
    }

    public void selectNextTab(boolean forward) {
        int index = tabbedEditors.getSelectedIndex();
        int count = tabbedEditors.getTabCount();
        if (forward) {
            index++;
            if (index == count) index = 0;
        } else {
            index--;
            if (index == -1) index = count-1;
        }
        tabbedEditors.setSelectedIndex(index);
    }

    public void selectTab(EditorTab editorTab) {
        int count = tabbedEditors.getTabCount();
        for (int index = 0; index<count; index++) {
            if ( getEditorTab(index) == editorTab) {
                tabbedEditors.setSelectedIndex(index);
                editorTab.getStudioWindow().refreshFrameTitle();
                return;
            }
        }
    }

    private void removeFocusChangeKeysForWindows(JComponent component) {
        if (Util.MAC_OS_X) return;

        KeyStroke ctrlTab = KeyStroke.getKeyStroke("ctrl TAB");
        KeyStroke ctrlShiftTab = KeyStroke.getKeyStroke("ctrl shift TAB");

        // Remove ctrl-tab from normal focus traversal
        Set<AWTKeyStroke> forwardKeys = new HashSet<>(component.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forwardKeys.remove(ctrlTab);
        component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys);

        // Remove ctrl-shift-tab from normal focus traversal
        Set<AWTKeyStroke> backwardKeys = new HashSet<>(component.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        backwardKeys.remove(ctrlShiftTab);
        component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardKeys);
    }

    public static void refreshEditorTitle(EditorTab editorTab) {
        if (! editorTab.isAddedToPanel()) return;
        EditorsPanel panel = editorTab.getEditorsPanel();
        JTabbedPane tabbedPane = panel.tabbedEditors;
        int count = tabbedPane.getTabCount();
        //@TODO: need to rework
        for(int index = 0; index<count; index++) {
            if (editorTab == panel.getEditorTab(index)) {
                tabbedPane.setTitleAt(index, editorTab.getTabTitle());
            }
        }
        //@TODO dirty
        panel.studioWindow.refreshFrameTitle();
    }

    public void getWorkspace(Workspace.Window window) {
        if (tabbedEditors == null) {
            window.setVerticalSplit(isVerticalSplit());
            window.setDividerLocation(Util.getDividerLocation(splitPane));
            left.getWorkspace(window.addLeft());
            right.getWorkspace(window.addRight());
        } else {
            Config config = Config.getInstance();
            int count = tabbedEditors.getTabCount();
            for (int index = 0; index < count; index++) {
                EditorTab editor = getEditorTab(index);
                Server server = editor.getServer();
                String filename = editor.getFilename();
                boolean modified = editor.isModified();
                if (modified && config.getBoolean(Config.AUTO_SAVE)) {
                    editor.saveFileOnDisk(true);
                }
                JTextComponent textArea = editor.getTextArea();

                window.addTab(index == tabbedEditors.getSelectedIndex())
                        .addFilename(filename)
                        .addServer(server)
                        .addContent(textArea.getText())
                        .setCaret(textArea.getCaretPosition())
                        .setModified(modified)
                        .setLineEnding(editor.getLineEnding());
            }
        }
    }

    private static EditorsPanel loadWorkspaceWindow(StudioWindow studioWindow, Workspace.Window window) {
        if (window == null) {
            log.warn("Workspace is corrupted. There is no one of the split. Creating empty one");
            EditorsPanel editorsPanel = new EditorsPanel(studioWindow, null);
            editorsPanel.addTab(Server.NO_SERVER);
            return editorsPanel;
        }

        return new EditorsPanel(studioWindow, window);
    }

    private void loadWorkspaceTabs(Workspace.Window window) {
        Workspace.Tab[] tabs = window.getAllTabs();
        for (Workspace.Tab tab: tabs) {
            try {
                Content content = Content.newContent(tab.getContent(), tab.getLineEnding());
                EditorTab editor = addTab(getServer(tab));
                editor.setFilename(tab.getFilename());
                editor.init(content);
                editor.setModified(tab.isModified());
                int caretPosition = tab.getCaret();
                if (caretPosition >= 0 && caretPosition < editor.getTextArea().getDocument().getLength()) {
                    editor.getTextArea().setCaretPosition(caretPosition);
                }
                editor.getTextArea().discardAllEdits();
            } catch (RuntimeException e) {
                log.error("Failed to init tab with filename {}", tab.getFilename(), e);
            }
        }

        int selectedIndex = window.getSelectedTab();
        if (selectedIndex >= 0 && selectedIndex < tabbedEditors.getTabCount()) {
            tabbedEditors.setSelectedIndex(selectedIndex);
        }

    }
    private static Server getServer(Workspace.Tab tab) {
        Config config = Config.getInstance();
        Server server = Server.NO_SERVER;
        String serverFullname = tab.getServerFullName();
        if (serverFullname != null) {
            server = config.getServerConfig().getServer(serverFullname);
        }
        if (server != Server.NO_SERVER) return server;

        String connectionString = tab.getServerConnection();
        if (connectionString != null) {
            server = config.getServerByConnectionString(connectionString);
        }

        String auth = tab.getServerAuth();
        if (auth == null) return server;

        if (AuthenticationManager.getInstance().lookup(auth) != null) {
            server = server.newAuthMethod(auth);
        }
        return server;
    }

    private EditorTab getEditorTab(int index) {
        return (EditorTab) ((JComponent) tabbedEditors.getComponentAt(index)).getClientProperty(EditorTab.class);
    }

    // @returns true if action execution should continue
    public boolean execute(EditorTabAction action) {
        List<EditorTab> editors = getAllEditors(false);
        for(EditorTab editor: editors) {
            if (!action.execute(editor)) return false;
        }
        return true;
    }

    public interface EditorTabAction {
        boolean execute(EditorTab editorTab); // return true means to continue
    }


    private static class MarkingDocumentListener implements DocumentChangeListener {
        private final EditorTab editor;
        public MarkingDocumentListener(EditorTab editor) {
            this.editor = editor;
        }
        @Override
        public void documentChanged(DocumentEvent e) {
            editor.setModified(true);
        }
    }

}
