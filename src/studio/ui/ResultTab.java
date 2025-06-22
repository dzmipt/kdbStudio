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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class ResultTab extends JPanel {
    private StudioWindow studioWindow;

    private final BetterCardLayout cardLayout;
    private final JPanel pnlCards;
    private JToolBar toolbar = null;
    private JToggleButton tglBtnComma;
    private JButton uploadBtn = null;
    private JButton btnLeft, btnRight;
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
        if (uploadBtn != null) {
            uploadBtn.setEnabled(! studioWindow.isQueryRunning());
        }
    }

    private void upload() {
        String varName = StudioOptionPane.showInputDialog(studioWindow, "Enter variable name", "Upload to Server");
        if (varName == null) return;
        varName = varName.trim();
        studioWindow.getActiveEditor().executeQuery(QueryTask.upload(varName, getQueryResult().getResult()));
    }

    private void initComponents() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_J, StudioWindow.menuShortcutKeyMask);
        tglBtnComma = new JToggleButton(Util.COMMA_CROSSED_ICON);
        tglBtnComma.setSelectedIcon(Util.COMMA_ICON);

        tglBtnComma.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        tglBtnComma.setToolTipText(Util.getTooltipWithAccelerator("Add comma as thousands separators for numbers", keyStroke));
        tglBtnComma.setFocusable(false);
        tglBtnComma.addActionListener(e -> {
            updateFormatting();
        });

        uploadBtn = new JButton(Util.UPLOAD_ICON);
        uploadBtn.setName("UploadButton");
        uploadBtn.setToolTipText("Upload to server");
        uploadBtn.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        uploadBtn.setFocusable(false);
        uploadBtn.addActionListener(e -> upload());

        keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, StudioWindow.menuShortcutKeyMask | InputEvent.SHIFT_MASK);

        JButton findBtn = new JButton(Util.FIND_ICON);
        findBtn.setToolTipText(Util.getTooltipWithAccelerator("Find in result", keyStroke));
        findBtn.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        findBtn.setFocusable(false);
        findBtn.addActionListener(e -> studioWindow.getResultSearchPanel().setVisible(true));

        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(tglBtnComma);
        toolbar.add(Box.createRigidArea(new Dimension(16,16)));
        toolbar.add(uploadBtn);
        toolbar.add(Box.createRigidArea(new Dimension(16,16)));
        toolbar.add(findBtn);
        toolbar.setVisible(getType() != ResultType.ERROR);
        updateFormatting();

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

    private void updateFormatting() {
        formatContext.setShowThousandsComma(tglBtnComma.isSelected());
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
        if (tglBtnComma == null) return;
        tglBtnComma.doClick();
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
