package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.ListModel;
import studio.kdb.*;
import studio.ui.action.QueryResult;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class TabPanel extends JPanel {
    private StudioWindow studioWindow;

    private JToolBar toolbar = null;
    private JToggleButton tglBtnComma;
    private JButton uploadBtn = null;
    private QueryResult queryResult;
    private K.KBase result;
    private JTextComponent textArea = null;
    private EditorPane editor = null;
    private QGrid grid = null;
    private KFormatContext formatContext = new KFormatContext(KFormatContext.DEFAULT);
    private ResultType type;
    private boolean pinned = false;

    private final static Logger log = LogManager.getLogger();

    public TabPanel(StudioWindow studioWindow, QueryResult queryResult) {
        this.studioWindow = studioWindow;
        this.queryResult = queryResult;
        if (queryResult != null && queryResult.getError() == null ) {
            this.result = queryResult.getResult();
        }
        initComponents();
    }

    public void setStudioWindow(StudioWindow studioWindow) {
        this.studioWindow = studioWindow;
        if (grid != null) {
            grid.setStudioWindow(studioWindow);
        }
    }

    public ResultType getType() {
        return type;
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
        studioWindow.executeK4Query(new K.KList(new K.Function("{x set y}"), new K.KSymbol(varName), result), "<upload to server>");
    }

    private void initComponents() {
        JComponent component;
        if (result != null) {
            KTableModel model = KTableModel.getModel(result);
            if (model != null) {
                grid = new QGrid(studioWindow, model);
                component = grid;
                if (model instanceof ListModel) {
                    type = ResultType.LIST;
                } else {
                    type = ResultType.TABLE;
                }
            } else {
                editor = new EditorPane(false, studioWindow.getResultSearchPanel(), studioWindow.getMainStatusBar());
                textArea = editor.getTextArea();
                component = editor;
                type = ResultType.TEXT;
            }

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
            updateFormatting();
        } else {
            textArea = new JTextPane();
            String hint = QErrors.lookup(queryResult.getError().getMessage());
            hint = hint == null ? "" : "\nStudio Hint: Possibly this error refers to " + hint;
            textArea.setText("An error occurred during execution of the query.\nThe server sent the response:\n" + queryResult.getError().getMessage() + hint);
            textArea.setForeground(Color.RED);
            textArea.setEditable(false);
            component = new JScrollPane(textArea);
            type = ResultType.ERROR;
        }

        refreshFont();
        setLayout(new BorderLayout());
        add(component, BorderLayout.CENTER);
        setName("resultPanel" + studioWindow.nextResultNameIndex());
    }

    public void refreshFont() {
        if (grid != null) {
            grid.setFont(Config.getInstance().getFont(Config.FONT_TABLE));
        }
        if (editor != null) {
            editor.getTextArea().setFont(Config.getInstance().getFont(Config.FONT_EDITOR));
        }
    }

    private void ensureTabLimit(JTabbedPane tabbedPane) {
        int limit = Config.getInstance().getResultTabsCount();
        int index = 0;

        while (tabbedPane.getTabCount() >= limit && index < tabbedPane.getTabCount()) {
            TabPanel tab = (TabPanel)tabbedPane.getComponentAt(index);
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
        tabbedPane.addTab(title, type.icon, this);
        int tabIndex = tabbedPane.getTabCount() - 1;
        tabbedPane.setSelectedIndex(tabIndex);
        tabbedPane.setToolTipTextAt(tabIndex, tooltip);
        updateToolbarLocation(tabbedPane);
    }

    public String makeTitle() {
        String title = (isPinned() ? "\u2191 " : "") + type.title;
        if (isTable()) {
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
        if (grid != null) {
            grid.setFormatContext(formatContext);
        }
        if (type == ResultType.TEXT) {
            String text;
            if ((result instanceof K.UnaryPrimitive) && ((K.UnaryPrimitive)result).isIdentity() ) text = "";
            else {
                text = Util.limitString(result.toString(formatContext), Config.getInstance().getMaxCharsInResult());
            }
            textArea.setText(text);
        }
    }

    public void toggleCommaFormatting() {
        if (tglBtnComma == null) return;
        tglBtnComma.doClick();
    }

    public void setDoubleClickTimeout(long doubleClickTimeout) {
        if (grid == null) return;
        grid.setDoubleClickTimeout(doubleClickTimeout);
    }

    public QGrid getGrid() {
        return grid;
    }

    public EditorPane getEditor() {
        return editor;
    }

    public boolean isTable() {
        return grid != null;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        updateTitle();
    }

    public enum ResultType {
        ERROR("Error Details ", Util.ERROR_SMALL_ICON),
        TEXT(I18n.getString("ConsoleView"), Util.CONSOLE_ICON),
        LIST("List", Util.TABLE_ICON),
        TABLE("Table", Util.TABLE_ICON);

        private final String title;
        private final Icon icon;
        ResultType(String title, Icon icon) {
            this.title = title;
            this.icon = icon;
        }
    };
}
