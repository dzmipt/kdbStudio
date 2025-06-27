package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.ui.action.QueryResult;
import studio.ui.action.QueryTask;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class ResultTab extends JPanel {
    private StudioWindow studioWindow;

    private final BetterCardLayout cardLayout;
    private final JPanel pnlCards;
    private Toolbar toolbar = null;
    private UserAction formatAction;
    private UserAction uploadAction;
//    private JButton btnLeft, btnRight;
    private KFormatContext formatContext = new KFormatContext(KFormatContext.DEFAULT);
    private boolean pinned = false;

    private final static Logger log = LogManager.getLogger();

    public ResultTab(StudioWindow studioWindow, QueryResult queryResult) {
        super(new BorderLayout());
        setName("resultPanel" + studioWindow.nextResultNameIndex());

        this.studioWindow = studioWindow;

        pnlCards = new JPanel();
        cardLayout = new BetterCardLayout(pnlCards);
        pnlCards.add(new ResultPane(studioWindow, this, queryResult));
        add(pnlCards, BorderLayout.CENTER);

        initComponents();
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

    private void upload(ActionEvent evt) {
        String varName = StudioOptionPane.showInputDialog(studioWindow, "Enter variable name", "Upload to Server");
        if (varName == null) return;
        varName = varName.trim();
        studioWindow.getActiveEditor().executeQuery(QueryTask.upload(varName, getQueryResult().getResult()));
    }

    private void initComponents() {
        formatAction = UserAction.create(
                "Toggle decimal format", Util.COMMA_CROSSED_ICON, "Add comma as thousands separators for numbers",
                KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_J, StudioWindow.menuShortcutKeyMask), this::updateFormatting
        ).toggleButton(Util.COMMA_ICON);

        uploadAction = UserAction.create(
                "Upload", Util.UPLOAD_ICON, "Upload to server",
                KeyEvent.VK_U, null, this::upload );


        UserAction findAction = UserAction.create(
                "Find in result", Util.FIND_ICON, "Find in result",
                KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, StudioWindow.menuShortcutKeyMask | InputEvent.SHIFT_MASK),
                e -> studioWindow.getResultSearchPanel().setVisible(true) );

        toolbar = new Toolbar();
        toolbar.setFloatable(false);
        toolbar.setButtonBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        toolbar.setGap(16);
        toolbar.addAll(formatAction, uploadAction, findAction);

        toolbar.setVisible(getType() != ResultType.ERROR);
        updateFormatting(null);

        refreshFont();
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
        String title = makeTitle();
        tabbedPane.addTab(title, getType().getIcon(), this, tooltip);
        int tabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setSelectedIndex(tabIndex);
        updateToolbarLocation(tabbedPane);
    }

    public String makeTitle() {
        String title = (isPinned() ? "\u2191 " : "") + getType().getTitle();
        QGrid grid = getGrid();
        if (grid != null) {
            title = title + " [" + grid.getRowCount() + " rows] ";
        }
        return title;
    }

    public void updateTitle() {
        JTabbedPane parentPane = (JTabbedPane)getClientProperty(JTabbedPane.class);
        parentPane.setTitleAt(parentPane.indexOfComponent(this), makeTitle());
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

    public boolean isTable() {
        return getGrid() != null;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        updateTitle();
    }
}
