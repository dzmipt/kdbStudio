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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class EditorPane extends JPanel implements MouseWheelListener, SearchPanelListener {

    private final StudioRSyntaxTextArea textArea;
    private final SearchPanel searchPanel;
    private final MinSizeLabel lblRowCol;
    private final MinSizeLabel lblInsStatus;
    private final JLabel lblStatus;
    private final Box statusBar;

    private final Timer tempStatusTimer;
    private String oldStatus = "";

    private final int yGap;
    private final int xGap;


    public EditorPane(boolean editable, SearchPanel searchPanel) {
        super(new BorderLayout());
        this.searchPanel = searchPanel;

        tempStatusTimer =  new Timer(3000, this::tempStatusTimerAction);
        tempStatusTimer.setRepeats(false);

        FontMetrics fm = getFontMetrics(UIManager.getFont("Label.font"));
        yGap = Math.round(0.1f * fm.getHeight());
        xGap = Math.round(0.25f * SwingUtilities.computeStringWidth(fm, "x"));

        textArea = RSTextAreaFactory.newTextArea(editable);
        textArea.addCaretListener(e -> updateRowColStatus());
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateTextModeStatus();
            }
        });

        lblRowCol = new MinSizeLabel("");
        lblRowCol.setHorizontalAlignment(JLabel.CENTER);
        lblRowCol.setMinimumWidth("9999:9999");
        setBorder(lblRowCol);

        lblInsStatus = new MinSizeLabel("INS");
        lblInsStatus.setHorizontalAlignment(JLabel.CENTER);
        lblInsStatus.setMinimumWidth("INS", "OVR");
        setBorder(lblInsStatus);
        lblStatus = new JLabel("Ready");
        Box boxStatus = Box.createHorizontalBox();
        boxStatus.add(lblStatus);
        boxStatus.add(Box.createHorizontalGlue());
        setBorder(boxStatus);

        statusBar = Box.createHorizontalBox();
        statusBar.add(boxStatus);
        statusBar.add(lblInsStatus);
        statusBar.add(lblRowCol);
        statusBar.setVisible(editable);

        RTextScrollPane scrollPane = new RTextScrollPane(textArea);
        textArea.setGutter(scrollPane.getGutter());

        scrollPane.addMouseWheelListener(this);

        Font font = Config.getInstance().getFont(Config.FONT_EDITOR);
        textArea.setFont(font);

        hideSearchPanel();

        add(scrollPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
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

    public void setStatus(String status) {
        if (tempStatusTimer.isRunning()) {
            oldStatus = status;
        }
        lblStatus.setText(status);
    }

    public void setTemporaryStatus(String status) {
        if (!tempStatusTimer.isRunning()) {
            oldStatus = lblStatus.getText();
        }
        setStatus(status);
        tempStatusTimer.restart();
    }

    private void tempStatusTimerAction(ActionEvent event) {
        setStatus(oldStatus);
    }

    private void updateRowColStatus() {
        int row = textArea.getCaretLineNumber() + 1;
        int col = textArea.getCaretPosition() - textArea.getLineStartOffsetOfCurrentLine() + 1;
        lblRowCol.setText("" + row + ":" + col);
    }

    private void updateTextModeStatus() {
        String text = textArea.getTextMode() == RSyntaxTextArea.INSERT_MODE ? "INS" : "OVR";
        lblInsStatus.setText(text);
    }

    private void setBorder(JComponent component) {
        component.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(yGap,xGap,yGap,xGap),
                            BorderFactory.createLineBorder(Color.LIGHT_GRAY)
                    ),
                    BorderFactory.createEmptyBorder(2*yGap, 2*xGap, yGap, 2*xGap)
                )
        );
    }

    @Override
    public void search(SearchContext context, SearchAction action) {
        if (context.isRegularExpression()) {
            try {
                Pattern.compile(context.getSearchFor());
            } catch (PatternSyntaxException e) {
                setTemporaryStatus("Error in regular expression: " + e.getMessage());
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
                setTemporaryStatus("Error during replacement: " + e.getMessage());
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
        setTemporaryStatus(status);
    }

    @Override
    public void closeSearchPanel() {
        hideSearchPanel();
        textArea.requestFocus();
    }
}
