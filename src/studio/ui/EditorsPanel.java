package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.Workspace;
import studio.ui.dndtabbedpane.DraggableTabbedPane;
import studio.utils.Content;
import studio.utils.FileReaderWriter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
    private final StudioPanel panel;
    private DraggableTabbedPane tabbedEditors;
    private JSplitPane splitPane;

    private static final Logger log = LogManager.getLogger();

    public EditorsPanel(StudioPanel panel, Workspace.Window workspaceWindow) {
        this.panel = panel;

        if (workspaceWindow != null && workspaceWindow.isSplit()) {
            left = loadWorkspaceWindow(panel, workspaceWindow.getLeft());
            right = loadWorkspaceWindow(panel, workspaceWindow.getRight());
            init(left, right, workspaceWindow.isVerticalSplit());
            return;
        }

        initTabbedEditors();

        if (workspaceWindow != null) {
            loadWorkspaceTabs(workspaceWindow);
            if (tabbedEditors.getTabCount() == 0) {
                log.warn("Workspace is corrupted. No tab found in a split. Creating empty tab");
                addTab(Server.NO_SERVER, null);
            }
        }
    }

    private void initTabbedEditors() {
        tabbedEditors = new DraggableTabbedPane("Editor");
        tabbedEditors.setDimSelectedTab(true);
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
            getEditorTab(index).getTextArea().requestFocus();
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
    }

    public EditorTab addTab(Server server, String filename) {
        EditorTab editor = new EditorTab(panel);
        editor.getTextArea().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                log.info("FocusGained: {}", editor.getTabTitle());
                panel.updateEditor(editor);
            }
        });
        JTextComponent textArea = editor.getTextArea();
        removeFocusChangeKeysForWindows(textArea);

        JComponent editorPane = editor.getPane();
        editorPane.putClientProperty(EditorTab.class, editor);

        editor.setServer(server);

        if (filename != null) {
            loadFile(editor, filename);
        } else {
            editor.setFilename(null);
            editor.init(Content.getEmpty());
        }

        addTab(editor);
        textArea.getDocument().addDocumentListener(new MarkingDocumentListener(editor));
        textArea.requestFocus();

        panel.setServer(server);
        panel.refreshActionState();
        refreshEditorTitle(editor);
        return editor;
    }

    private void addTab(EditorTab editorTab) {
        tabbedEditors.add(editorTab.getTabTitle(), editorTab.getPane());
        tabbedEditors.setSelectedIndex(tabbedEditors.getTabCount()-1);

        if (panel.getActiveEditor() == editorTab && panel == StudioPanel.getActivePanel()) {
            tabbedEditors.setDimSelectedTab(false);
        }
    }

    public void split(boolean vertically) {
        if (tabbedEditors == null) return;

        int selectedIndex = tabbedEditors.getSelectedIndex();
        if (selectedIndex == -1) return;

        List<EditorTab> editors = getAllEditors(false);

        remove(tabbedEditors);
        tabbedEditors = null;

        EditorsPanel aRight = new EditorsPanel(panel, null);
        //@TODO move modified content as well
        aRight.addTab(editors.get(selectedIndex).getServer(), editors.get(selectedIndex).getFilename());

        EditorsPanel aLeft = new EditorsPanel(panel, null);
        for(EditorTab editor: editors) {
            aLeft.addTab(editor);
        }
        aLeft.tabbedEditors.setSelectedIndex(selectedIndex);

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

    public void setDimEditors(boolean dimEditors) {
        if (tabbedEditors == null) return;

        tabbedEditors.setDimSelectedTab(dimEditors);
    }

    public static boolean loadFile(EditorTab editor, String filename) {
        Content content = Content.getEmpty();
        try {
            content = FileReaderWriter.read(filename);
            if (content.hasMixedLineEnding()) {
                StudioOptionPane.showMessage(editor.getPanel(), "The file " + filename + " has mixed line endings. Mixed line endings are not supported.\n\n" +
                                "All line endings are set to " + content.getLineEnding() + " style.",
                        "Mixed Line Ending");
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to load file {}", filename, e);
            StudioOptionPane.showError(editor.getPanel(), "Failed to load file "+filename + ".\n" + e.getMessage(),
                    "Error in file load");
        } finally {
            //@TODO check if this is correct.
            editor.setFilename(filename);
            editor.init(content);
        }
        return false;
    }

    public static boolean saveAsFile(EditorTab editor) {
        String filename = editor.getFilename();
        File file = StudioPanel.chooseFile(editor.getPanel(), Config.SAVE_FILE_CHOOSER, JFileChooser.SAVE_DIALOG, "Save script as",
                filename == null ? null : new File(filename),
                new FileNameExtensionFilter("q script", "q"));

        if (file == null) return false;

        filename = file.getAbsolutePath();
        if (file.exists()) {
            int choice = StudioOptionPane.showYesNoDialog(editor.getPanel(),
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

        int choice = StudioOptionPane.showYesNoCancelDialog(editor.getPane(),
                editor.getTitle() + " is changed. Save changes?","Save changes?");

        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return false;

        if (choice == JOptionPane.YES_OPTION) {
            return saveEditor(editor);
        }

        return true;
    }

    public boolean closeTab(EditorTab editorTab) {
        if (!checkAndSaveTab(editorTab)) return false;

        editorTab.closing();

        tabbedEditors.remove(editorTab.getPane());
        closeIfEmpty();
        return true;
    }

    private void closeIfEmpty() {
        if (tabbedEditors.getTabCount()>0) return;

        if (parent == null) {
            panel.closeFrame();
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
        EditorsPanel panel = editorTab.getEditorsPanel();
        if (panel == null) return; //while new tab is added
        JTabbedPane tabbedPane = panel.tabbedEditors;
        int count = tabbedPane.getTabCount();
        //@TODO: need to rework
        for(int index = 0; index<count; index++) {
            if (editorTab == panel.getEditorTab(index)) {
                tabbedPane.setTitleAt(index, editorTab.getTabTitle());
            }
        }
        //@TODO dirty
        panel.panel.refreshFrameTitle();
    }

    public void getWorkspace(Workspace.Window window) {
        if (tabbedEditors == null) {
            window.setVerticalSplit(isVerticalSplit());
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

    private static EditorsPanel loadWorkspaceWindow(StudioPanel panel, Workspace.Window window) {
        if (window == null) {
            log.warn("Workspace is corrupted. There is no one of the split. Creating empty one");
            EditorsPanel editorsPanel = new EditorsPanel(panel, null);
            editorsPanel.addTab(Server.NO_SERVER, null);
            return editorsPanel;
        }

        return new EditorsPanel(panel, window);
    }

    private void loadWorkspaceTabs(Workspace.Window window) {
        Workspace.Tab[] tabs = window.getTabs();
        for (Workspace.Tab tab: tabs) {
            try {
                EditorTab editor = addTab(getServer(tab), tab.getFilename());
                editor.init(Content.newContent(tab.getContent(), tab.getLineEnding()));
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
        Server server = null;
        String serverFullname = tab.getServerFullName();
        if (serverFullname != null) {
            server = config.getServer(serverFullname);
        }
        if (server != null) return server;

        String connectionString = tab.getServerConnection();
        if (connectionString != null) {
            server = config.getServerByConnectionString(connectionString);
        }
        if (server == null) server = Server.NO_SERVER;

        String auth = tab.getServerAuth();
        if (auth == null) return server;

        if (AuthenticationManager.getInstance().lookup(auth) != null) {
            server.setAuthenticationMechanism(auth);
        }
        return server;
    }

    private EditorTab getEditorTab(int index) {
        return (EditorTab) ((JComponent) tabbedEditors.getComponentAt(index)).getClientProperty(EditorTab.class);
    }

    public boolean execute(EditorTabAction action) {
        if (tabbedEditors != null) {
            int count = tabbedEditors.getTabCount();
            for (int index = 0; index<count; index++) {
                EditorTab editorTab = getEditorTab(index);
                if (!action.execute(editorTab)) return false;
            }
        } else {
            if (! left.execute(action) ) return false;
            if (! right.execute(action) ) return false;
        }

        return true;
    }

    public interface EditorTabAction {
        boolean execute(EditorTab editorTab); // return true means to continue
    }


    private class MarkingDocumentListener implements DocumentListener {
        private final EditorTab editor;
        public MarkingDocumentListener(EditorTab editor) {
            this.editor = editor;
        }
        private void update() {
            editor.setModified(true);
            panel.refreshActionState();
        }
        public void changedUpdate(DocumentEvent evt) { update(); }
        public void insertUpdate(DocumentEvent evt) {
            update();
        }
        public void removeUpdate(DocumentEvent evt) {
            update();
        }
    }

}