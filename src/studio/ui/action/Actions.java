package studio.ui.action;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import studio.kdb.Config;
import studio.kdb.ServerTreeNode;
import studio.kdb.config.ServerTreeNodeSerializer;
import studio.ui.*;
import studio.ui.rstextarea.ConvertTabsToSpacesAction;
import studio.ui.rstextarea.FindReplaceAction;
import studio.ui.rstextarea.RSTextAreaFactory;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

public class Actions {

    public final static String serverBack = "serverBack";
    public final static String serverForward = "serverForward";
    public final static String clean = "clean";
    public final static String arrangeAll = "arrangeAll";
    public final static String minMaxDivider = "minMaxDivider";
    public final static String toggleDividerOrientation = "toggleDividerOrientation";
    public final static String closeTab = "closeTab";
    public final static String closeFile = "closeFile";
    public final static String openFile = "openFile";
    public final static String newWindow = "newWindow";
    public final static String newTab = "newTab";
    public final static String serverList = "serverList";
    public final static String serverHistory = "serverHistory";
    public final static String loadServerTree = "loadServerTree";
    public final static String exportServerTree = "exportServerTree";
    public final static String importFromQPad = "importFromQPad";
    public final static String connectionStats = "connectionStats";
    public final static String editServer = "editServer";
    public final static String addServer = "addServer";
    public final static String removeServer = "removeServer";
    public final static String saveFile = "saveFile";
    public final static String saveAllFiles = "saveAllFiles";
    public final static String saveAsFile = "saveAsFile";
    public final static String export = "export";
    public final static String chart = "chart";
    public final static String executeAndChart = "executeAndChart";
    public final static String executeCurrentLineAndChart = "executeCurrentLineAndChart";
    public final static String stop = "stop";
    public final static String openInExcel = "openInExcel";
    public final static String execute = "execute";
    public final static String executeCurrentLine = "executeCurrentLine";
    public final static String refresh = "refresh";
    public final static String toggleCommaFormat = "toggleCommaFormat";
    public final static String upload = "upload";
    public final static String findInResult = "findInResult";
    public final static String prevResult = "prevResult";
    public final static String nextResult = "nextResult";
    public final static String about = "about";
    public final static String exit = "exit";
    public final static String settings = "settings";
    public final static String codeKxCom = "codeKxCom";
    public final static String copy = "copy";
    public final static String cut = "cut";
    public final static String paste = "paste";
    public final static String selectAll = "selectAll";
    public final static String undo = "undo";
    public final static String redo = "redo";
    public final static String find = "find";
    public final static String replace = "replace";
    public final static String convertTabsToSpaces = "convertTabsToSpaces";
    public final static String nextEditorTab = "nextEditorTab";
    public final static String prevEditorTab = "prevEditorTab";
    public final static String wordWrap = "wordWrap";
    public final static String splitEditorRight = "splitEditorRight";
    public final static String splitEditorDown = "splitEditorDown";

    public final static Map<String, StudioWindowAction> studioWindowActions = new HashMap<>();

    private static void add(String actionName, StudioWindowAction action) {
        studioWindowActions.put(actionName, action);
    }

    private static void addEditorAction(String actionName, StudioWindowAction.EditorAction action) {
        studioWindowActions.put(actionName, action);
    }

    private static void addResultTabAction(String actionName, StudioWindowAction.ResultTabAction action) {
        studioWindowActions.put(actionName, action);
    }

    private static void addStaticAction(String actionName, StudioWindowAction.StaticAction action) {
        studioWindowActions.put(actionName, action);
    }

    static {
        addEditorAction(serverBack, editor ->
                editor.navigateHistoryServer(false) );
        addEditorAction(serverForward, editor ->
                editor.navigateHistoryServer(true) );

        add(clean, StudioWindow::newFile);
        add(arrangeAll, StudioWindow::arrangeAll);
        add(minMaxDivider, StudioWindow::minMaxDivider);
        add(toggleDividerOrientation, StudioWindow::toggleDividerOrientation);

        addEditorAction(closeTab, editor ->
            editor.getEditorsPanel().closeTab(editor) );

        add(closeFile, StudioWindow::close);
        add(openFile, StudioWindow::openFile);

        addEditorAction(newWindow, editor ->
            WindowFactory.newStudioWindow(editor.getServer(), null) );

        add(newTab, studioWindow ->
                studioWindow.addTab(null) );

        add(serverList, studioWindow ->
                studioWindow.showServerList(false) );
        add(serverHistory, studioWindow ->
                studioWindow.showServerList(true) );

        add(loadServerTree, studioWindow -> {
            ServerTreeNode importTree = ServerTreeNodeSerializer.openImportDialog(studioWindow);
            if (importTree == null) return;
            Config.getInstance().getServerConfig().setRoot(importTree);
        });

        add(exportServerTree, studioWindow ->
                ServerTreeNodeSerializer.openExportDialog(studioWindow, Config.getInstance().getServerTree()) );

        add(importFromQPad, QPadImport::doImport);
        add(connectionStats, ConnectionStats::getStats);
        add(editServer, StudioWindow::editServer);
        add(addServer, StudioWindow::addServer);
        add(removeServer, StudioWindow::removeServer);
        addEditorAction(saveFile, EditorsPanel::saveEditor);
        addStaticAction(saveAllFiles, StudioWindow::saveAll);
        addEditorAction(saveAsFile, EditorsPanel::saveAsFile);
        add(export, StudioWindow::export);
        add(chart, StudioWindow::chart);

        add(executeAndChart, studioWindow ->
                studioWindow.executeQuery(true) );

        add(executeCurrentLineAndChart, studioWindow ->
                studioWindow.executeQueryCurrentLine(true) );

        addEditorAction(stop, editor ->
                editor.getQueryExecutor().cancel() );

        add(openInExcel, StudioWindow::openInExcel);

        add(execute, studioWindow ->
                studioWindow.executeQuery(false) );

        add(executeCurrentLine, studioWindow ->
                studioWindow.executeQueryCurrentLine(false) );

        add(refresh, StudioWindow::refreshQuery);

        addResultTabAction(toggleCommaFormat, ResultTab::toggleCommaFormatting);

        addResultTabAction(upload, tab ->
            tab.upload(new ActionEvent(tab, ActionEvent.ACTION_PERFORMED, upload)) );

        add(findInResult, studioWindow ->
                studioWindow.getResultSearchPanel().setVisible(true) );

        addResultTabAction(prevResult, tab ->
            tab.navigateCard(false) );

        addResultTabAction(nextResult, tab ->
            tab.navigateCard(true) );

        addStaticAction(about, StudioWindow::about);
        addStaticAction(exit, StudioWindow::quit);
        addStaticAction(settings, StudioWindow::settings);

        add(codeKxCom, studioWindow -> {
            try {
                Util.openURL("https://code.kx.com/q/ref/");
            } catch (Exception ex) {
                StudioOptionPane.showError("Error attempting to launch web browser:\n" + ex.getMessage(), "Error");
            }
        });

        addEditorAction(copy, new StudioWindowAction.RSTAAction(RSTextAreaFactory.rstaCopyAsStyledTextAction));
        addEditorAction(cut, new StudioWindowAction.RSTAAction(RSTextAreaFactory.rstaCutAsStyledTextAction));
        addEditorAction(paste, new StudioWindowAction.RSTAAction(RSyntaxTextAreaEditorKit.pasteAction));
        addEditorAction(selectAll, new StudioWindowAction.RSTAAction(RSyntaxTextAreaEditorKit.selectAllAction));
        addEditorAction(undo, new StudioWindowAction.RSTAAction(RSyntaxTextAreaEditorKit.rtaUndoAction));
        addEditorAction(redo, new StudioWindowAction.RSTAAction(RSyntaxTextAreaEditorKit.rtaRedoAction));
        addEditorAction(find, new StudioWindowAction.RSTAAction(FindReplaceAction.findAction));
        addEditorAction(replace, new StudioWindowAction.RSTAAction(FindReplaceAction.replaceAction));
        addEditorAction(convertTabsToSpaces, new StudioWindowAction.RSTAAction(ConvertTabsToSpacesAction.action));

        addEditorAction(nextEditorTab, editor ->
                editor.getEditorsPanel().selectNextTab(true) );
        addEditorAction(prevEditorTab, editor ->
                editor.getEditorsPanel().selectNextTab(true) );

        add(wordWrap, StudioWindow::toggleWordWrap);

        addEditorAction(splitEditorRight, editor ->
                editor.getEditorsPanel().split(false) );
        addEditorAction(splitEditorDown, editor ->
                editor.getEditorsPanel().split(true) );

    }

}
