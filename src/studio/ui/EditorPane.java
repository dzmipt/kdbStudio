package studio.ui;

import kx.ConnectionContext;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import studio.kdb.Config;
import studio.ui.rstextarea.RSTextAreaFactory;
import studio.ui.rstextarea.StudioRSyntaxTextArea;
import studio.ui.search.SearchAction;
import studio.ui.search.SearchPanel;
import studio.ui.search.SearchPanelListener;
import studio.ui.statusbar.EditorStatusBar;
import studio.ui.statusbar.EditorStatusBarCallback;
import studio.ui.statusbar.MainStatusBar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EditorPane extends JPanel implements MouseWheelListener, SearchPanelListener {

    private final StudioRSyntaxTextArea textArea;
    private final SearchPanel searchPanel;
    private final MainStatusBar mainStatusBar;
    private final EditorStatusBar editorStatusBar;

    public EditorPane(boolean editable, SearchPanel searchPanel, MainStatusBar mainStatusBar) {
        super(new BorderLayout());
        this.searchPanel = searchPanel;
        this.mainStatusBar = mainStatusBar;

        textArea = RSTextAreaFactory.newTextArea(editable);
        mainStatusBar.bindTextArea(textArea);


        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        textArea.setGutter(scrollPane.getGutter());

        scrollPane.addMouseWheelListener(this);

        Font font = Config.getInstance().getFont(Config.FONT_EDITOR);
        textArea.setSyntaxScheme(font, Config.getInstance().getTokenStyleConfig());
        textArea.setEditorColors(Config.getInstance().getEditorColors());

        hideSearchPanel();

        add(scrollPane, BorderLayout.CENTER);

        editorStatusBar = new EditorStatusBar();
        if (editable) {
            add(editorStatusBar, BorderLayout.SOUTH);
            editorStatusBar.setStatus("Ready");
        }
    }

    public void setEditorStatus(String status) {
        editorStatusBar.setStatus(status);
    }

    public void startClock() {
        editorStatusBar.startClock();
    }

    public void stopClock() {
        editorStatusBar.stopClock();
    }

    public void setSessionContext(ConnectionContext context) {
        editorStatusBar.setSessionContext(context);
    }

    public void setEditorStatusBarCallback(EditorStatusBarCallback callback) {
        editorStatusBar.setEditorStatusBarCallback(callback);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ((e.getModifiersEx() & Util.menuShortcutKeyMask) == 0) return;

        Font font = Config.getInstance().getFont(Config.FONT_EDITOR);
        int newFontSize = font.getSize() + e.getWheelRotation();
        if (newFontSize < 6) return;
        font = font.deriveFont((float) newFontSize);

        Config.getInstance().setFont(Config.FONT_EDITOR, font);
        SettingsDialog.refreshEditorsSettings();
        SettingsDialog.refreshResultSettings();
    }

    public void hideSearchPanel() {
        searchPanel.setVisible(false);
        textArea.setHighlighter(null); // workaround to clear all marks
    }

    public void showSearchPanel(boolean showReplace) {
        searchPanel.setReplaceVisible(showReplace);
        searchPanel.setVisible(true);
    }

    public StudioRSyntaxTextArea getTextArea() {
        return textArea;
    }

    @Override
    public void search(SearchContext context, SearchAction action) {
        if (context.isRegularExpression()) {
            try {
                Pattern.compile(context.getSearchFor());
            } catch (PatternSyntaxException e) {
                mainStatusBar.setTemporaryStatus("Error in regular expression: " + e.getMessage());
                return;
            }
        }

        SearchResult result;
        if (action == SearchAction.Find) {
            result = SearchEngine.find(textArea, context);
        } else if (action == SearchAction.FindContinues) {
            textArea.setSelectionEnd(textArea.getSelectionStart());
            result = SearchEngine.find(textArea, context);
        } else {
            try {
                if (action == SearchAction.Replace) {
                    int selStart = textArea.getSelectionStart();
                    int selEnd = textArea.getSelectionEnd();
                    textArea.setSelectionEnd(selStart);
                    result = SearchEngine.find(textArea, context);
                    if (selStart == textArea.getSelectionStart() && selEnd == textArea.getSelectionEnd()) {
                        result = SearchEngine.replace(textArea, context);
                    }
                } else { //ReplaceAll
                    result = SearchEngine.replaceAll(textArea, context);
                }
            } catch (IndexOutOfBoundsException e) {
                mainStatusBar.setTemporaryStatus("Error during replacement: " + e.getMessage());
                return;
            }
        }

        String status;
        if (! result.wasFound()) {
            status = "Nothing was found";
        } else if (result.getMarkedCount() > 0) {
            status = "Marked " + result.getMarkedCount() + " occurrence(s)";
        } else if (action == SearchAction.Find) {
            status = "Selected the first occurrence";
        } else {
            status = "Replaced " + result.getCount() + " occurrence(s)";
        }
        mainStatusBar.setTemporaryStatus(status);
    }

    @Override
    public void closeSearchPanel() {
        hideSearchPanel();
        textArea.requestFocus();
    }
}
