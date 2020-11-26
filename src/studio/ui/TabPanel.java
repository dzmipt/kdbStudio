package studio.ui;

import org.netbeans.editor.Utilities;
import studio.kdb.*;
import studio.kdb.ListModel;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;

public class TabPanel extends JPanel {
    private Icon icon;
    private String title;

    private JComponent component = null;

    private JToolBar toolbar = null;
    private JToggleButton tglBtnComma;
    private K.KBase result = null;
    private JEditorPane textArea = null;
    private QGrid grid = null;
    private KFormatContext formatContext = new KFormatContext(KFormatContext.DEFAULT);

    public TabPanel(String title,Icon icon,JComponent component) {
        this.title = title;
        this.icon = icon;
        this.component = component;
        initComponents();
    }

    public TabPanel(K.KBase result) {
        this.result = result;
        initComponents();
    }

    private void initComponents() {
        if (result != null) {
            KTableModel model = KTableModel.getModel(result);
            if (model != null) {
                grid = new QGrid(model);
                component = grid;

                boolean dictModel = model instanceof DictModel;
                boolean listModel = model instanceof ListModel;
                boolean tableModel = !(dictModel || listModel);
                title = tableModel ? "Table" : (dictModel ? "Dict" : "List");
                title = title + " [" + grid.getRowCount() + " rows] ";
                icon = Util.TABLE_ICON;
            } else {
                textArea = new JEditorPane("text/q", "");
                textArea.setEditable(false);
                component = Utilities.getEditorUI(textArea).getExtComponent();
                title = I18n.getString("ConsoleView");
                icon = Util.CONSOLE_ICON;
            }

            tglBtnComma = new JToggleButton(Util.COMMA_CROSSED_ICON);
            tglBtnComma.setSelectedIcon(Util.COMMA_ICON);

            tglBtnComma.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            tglBtnComma.setToolTipText("Add comma as thousands separators for numbers");
            tglBtnComma.setFocusable(false);
            tglBtnComma.addActionListener(e -> {
                updateFormatting();
            });
            toolbar = new JToolBar();
            toolbar.setFloatable(false);
            toolbar.add(tglBtnComma);
            updateFormatting();
        }

        setLayout(new BorderLayout());
        add(component, BorderLayout.CENTER);
    }

    public void addInto(JTabbedPane tabbedPane) {
        tabbedPane.addTab(title,icon, this);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount()-1);
        updateToolbarLocation(tabbedPane);

        tabbedPane.addPropertyChangeListener(
                event -> updateToolbarLocation((JTabbedPane)event.getSource())
        );
    }

    private void updateToolbarLocation(JTabbedPane tabbedPane) {
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
        if (textArea != null) {
            String text;
            if ((result instanceof K.UnaryPrimitive&&0==((K.UnaryPrimitive)result).getPrimitiveAsInt())) text = "";
            else {
                text = Util.limitString(result.toString(formatContext), Config.getInstance().getMaxCharsInResult());
            }
            textArea.setText(text);
        }
    }

    public JTable getTable() {
        if (grid == null) return null;
        return grid.getTable();
    }

}

