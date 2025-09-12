package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.kdb.query.QueryResult;
import studio.kdb.query.QueryTask;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class ResultTab extends JPanel {
    private StudioWindow studioWindow;

    private final BetterCardLayout cardLayout;
    private final JPanel pnlCards;
    private Toolbar toolbar = null;
    private UserAction formatAction;
    private UserAction uploadAction;
    private UserAction previousCardAction;
    private UserAction nextCardAction;
    private final KFormatContext formatContext = new KFormatContext(KFormatContext.DEFAULT);
    private boolean pinned = false;

    private final static Logger log = LogManager.getLogger();

    public ResultTab(StudioWindow studioWindow, QueryResult queryResult) {
        super(new BorderLayout());
        setName("resultPanel" + studioWindow.nextResultNameIndex());

        this.studioWindow = studioWindow;

        pnlCards = new JPanel();
        cardLayout = new BetterCardLayout(pnlCards);
        add(pnlCards, BorderLayout.CENTER);
        initComponents();

        addResultInside(queryResult);
    }

    private void addDeepMouseListener(Component comp, MouseListener listener) {
        comp.addMouseListener(listener);
        if (! (comp instanceof Container)) return;
        for (Component child: ((Container)comp).getComponents()) {
            addDeepMouseListener(child, listener);
        }
    }

    public void addResult(QueryResult queryResult, String tooltip) {
        boolean reuse = Config.getInstance().getBoolean(Config.INSPECT_RESULT_IN_CURRENT);
        if (reuse) addResultInside(queryResult);
        else studioWindow.addResultTab(queryResult, tooltip);
        studioWindow.refreshActionState();
    }

    private void addResultInside(QueryResult queryResult) {
        cardLayout.removeAllAfterSelected();
        ResultPane resultPane = new ResultPane(studioWindow, this, queryResult);
        addDeepMouseListener(resultPane, new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == 4) navigateCard(false);
                else if (e.getButton() == 5) navigateCard(true);
            }
        });
        pnlCards.add(resultPane);
        toolbar.setVisible(resultPane.getType() != ResultType.ERROR);
        refreshFont();
        updateFormatting(null);
        refresh();
        resultPane.requestFocus();
    }

    public void navigateCard(boolean next) {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean hasFocus = focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, this);
        if (next) {
            cardLayout.next();
        } else {
            cardLayout.previous();
        }
        refresh();
        if (hasFocus) requestFocus();
    }

    public boolean hasPreviousResult() {
        return cardLayout.hasPrevious();
    }

    public boolean hasNextResult() {
        return cardLayout.hasNext();
    }

    private void refresh() {
        previousCardAction.setEnabled(hasPreviousResult());
        nextCardAction.setEnabled(hasNextResult());
        updateTitle();
        studioWindow.refreshActionState();
    }

    private ResultPane getResultPane() {
        return (ResultPane) cardLayout.getSelectedComponent();
    }

    private void forEachResultPane(Consumer<ResultPane> action) {
        for (Component component: pnlCards.getComponents()) {
            action.accept((ResultPane) component);
        }
    }

    public QueryResult getQueryResult() {
        return getResultPane().getQueryResult();
    }

    public void setStudioWindow(StudioWindow studioWindow) {
        this.studioWindow = studioWindow;
        forEachResultPane(pane -> {
            QGrid grid = pane.getGrid();
            if (grid != null) {
                grid.setStudioWindow(studioWindow);
            }
        });
    }

    public ResultType getType() {
        return getResultPane().getType();
    }

    public void refreshActionState() {
        uploadAction.setEnabled(! studioWindow.isQueryRunning());
    }

    public void upload(ActionEvent evt) {
        K.KBase obj = getQueryResult().getResult();
        if (obj == null) return;
        String varName = StudioOptionPane.showInputDialog(studioWindow, "Enter variable name", "Upload to Server");
        if (varName == null) return;
        varName = varName.trim();
        studioWindow.getActiveEditor().executeQuery(QueryTask.upload(studioWindow, varName, obj));
    }

    private void initComponents() {
        formatAction = UserAction.create(
                "Toggle decimal format", Util.COMMA_CROSSED_ICON, "Add comma as thousands separators for numbers",
                KeyEvent.VK_T, Util.getMenuShortcut(KeyEvent.VK_J), this::updateFormatting
        ).toggleButton(Util.COMMA_ICON);

        uploadAction = UserAction.create(
                "Upload", Util.UPLOAD_ICON, "Upload to current server",
                KeyEvent.VK_U, Util.getMenuShortcut(KeyEvent.VK_U), this::upload );

        UserAction findAction = UserAction.create(
                "Find in result", Util.FIND_ICON, "Find in result",
                KeyEvent.VK_F, Util.getMenuShortcut(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK),
                e -> studioWindow.getResultSearchPanel().setVisible(true) );

        previousCardAction = UserAction.create(
                "Previous result", Util.LEFT_ICON, "Show previous result",
                KeyEvent.VK_Q, Util.getMenuShortcut(KeyEvent.VK_COMMA, InputEvent.ALT_DOWN_MASK),
                e -> navigateCard(false)
        );

        nextCardAction = UserAction.create(
                "Next result", Util.RIGHT_ICON, "Show next result",
                KeyEvent.VK_W, Util.getMenuShortcut(KeyEvent.VK_PERIOD, InputEvent.ALT_DOWN_MASK),
                e -> navigateCard(true)
        );

        toolbar = new Toolbar();
        toolbar.setFloatable(false);
        toolbar.setButtonBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        toolbar.setGap(16);
        toolbar.addAll(formatAction, uploadAction, findAction, previousCardAction, nextCardAction);

        updateFormatting(null);
    }

    public void refreshFont() {
        forEachResultPane(resultPane -> {
            QGrid grid = resultPane.getGrid();
            if (grid != null) {
                grid.setFont(Config.getInstance().getFont(Config.FONT_TABLE));
            }
            EditorPane editor = resultPane.getEditor();
            if (editor != null) {
                editor.getTextArea().setFont(Config.getInstance().getFont(Config.FONT_EDITOR));
            }
        });
    }

    private void ensureTabLimit(JTabbedPane tabbedPane) {
        int limit = Config.getInstance().getInt(Config.RESULT_TAB_COUNTS);
        int index = 0;

        while (tabbedPane.getTabCount() >= limit && index < tabbedPane.getTabCount()) {
            ResultTab tab = (ResultTab)tabbedPane.getComponentAt(index);
            if (!tab.isPinned()) {
                tabbedPane.removeTabAt(index);
            } else {
                index++;
            }
        }
    }

    public void addInto(JTabbedPane tabbedPane, String tooltip) {
        ensureTabLimit(tabbedPane);
        putClientProperty(JTabbedPane.class, tabbedPane);
        String title = getTitle();
        tabbedPane.addTab(title, getType().getIcon(), this, tooltip);
        int tabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setSelectedIndex(tabIndex);
        updateToolbarLocation(tabbedPane);
    }

    public String getTitle() {
        StringBuilder title = new StringBuilder();
        if (isPinned()) title.append("↑ ");
        title.append(getType().getTitle()).append(' ');
        QGrid grid = getGrid();
        if (grid != null) {
            title.append('[').append(grid.getRowCount()).append(" rows]");
        }
        int index = cardLayout.getSelected();
        if (index > 0) {
            title.append(" - ").append(index);
        }
        title.append(' ');
        return title.toString();
    }

    public void updateTitle() {
        JTabbedPane parentPane = (JTabbedPane)getClientProperty(JTabbedPane.class);
        if (parentPane == null) return;
        int index = parentPane.indexOfComponent(this);
        if (index == -1) return;
        parentPane.setTitleAt(index, getTitle());
        parentPane.setIconAt(index, getType().getIcon());
    }

    public void updateToolbarLocation(JTabbedPane tabbedPane) {
        if (toolbar == null) return;

        remove(toolbar);
        if (tabbedPane.getTabPlacement() == JTabbedPane.TOP) {
            toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
            add(toolbar, BorderLayout.WEST);
        } else {
            toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
            add(toolbar, BorderLayout.NORTH);
        }
    }

    private void updateFormatting(ActionEvent evt) {
        formatContext.setShowThousandsComma(formatAction.isSelected());
        forEachResultPane(resultPane -> {
            QGrid grid = resultPane.getGrid();
            if (grid != null) {
                grid.setFormatContext(formatContext);
                grid.resizeColumns();
            }

            EditorPane editor = resultPane.getEditor();
            if (editor != null) {
                K.KBase result = resultPane.getQueryResult().getResult();
                String text;
                if ((result instanceof K.UnaryPrimitive) && ((K.UnaryPrimitive) result).isIdentity()) text = "";
                else {
                    text = Util.limitString(result.toString(formatContext), Config.getInstance().getInt(Config.MAX_CHARS_IN_RESULT));
                }
                JTextComponent textArea = editor.getTextArea();
                textArea.setText(text);
                textArea.setCaretPosition(0);
                textArea.scrollRectToVisible(new Rectangle(0, 0, 1, 1));  // Scroll to top
            }
        });
    }

    public void toggleCommaFormatting() {
        formatAction.click();
    }

    public void setDoubleClickTimeout(long doubleClickTimeout) {
        forEachResultPane(resultPane -> {
            QGrid grid = resultPane.getGrid();
            if (grid == null) return;
            grid.setDoubleClickTimeout(doubleClickTimeout);
        });
    }

    public QGrid getGrid() {
        return getResultPane().getGrid();
    }

    public EditorPane getEditor() {
        return getResultPane().getEditor();
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        updateTitle();
    }
}
