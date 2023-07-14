package studio.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
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
        textArea.setFont(font);

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

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if ((e.getModifiers() & StudioPanel.menuShortcutKeyMask) == 0) return;

        Font font = Config.getInstance().getFont(Config.FONT_EDITOR);
        int newFontSize = font.getSize() + e.getWheelRotation();
        if (newFontSize < 6) return;
        font = font.deriveFont((float) newFontSize);

        Config.getInstance().setFont(Config.FONT_EDITOR, font);
        StudioPanel.refreshEditorsSettings();
        StudioPanel.refreshResultSettings();
    }

    public void hideSearchPanel() {
        searchPanel.setVisible(false);
        textArea.setHighlighter(null); // workaround to clear all marks
    }

    public void showSearchPanel(boolean showReplace) {
        searchPanel.setReplaceVisible(showReplace);
        searchPanel.setVisible(true);
    }

    public RSyntaxTextArea getTextArea() {
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

        int pos = context.getSearchForward() ? textArea.getSelectionEnd() : textArea.getSelectionStart();
        textArea.setSelectionStart(pos);
        textArea.setSelectionEnd(pos);
        SearchResult result;
        if (action == SearchAction.Find) {
            result = SearchEngine.find(textArea, context);
        } else {
            try {
                if (action == SearchAction.Replace) {
                    result = SearchEngine.replace(textArea, context);
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
