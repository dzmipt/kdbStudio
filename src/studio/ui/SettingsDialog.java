package studio.ui;

import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.kdb.Config;
import studio.kdb.KFormatContext;
import studio.kdb.config.ActionOnExit;
import studio.ui.settings.FontSelectionPanel;
import studio.utils.LineEnding;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class SettingsDialog extends EscapeDialog {
    private JComboBox<String> comboBoxAuthMechanism;
    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JCheckBox chBoxSessionInvalidation;
    private JFormattedTextField txtSessionInvalidation;
    private JCheckBox chBoxSessionReuse;
    private JCheckBox chBoxShowServerCombo;
    private JCheckBox chBoxAutoSave;
    private JComboBox<ActionOnExit> comboBoxActionOnExit;
    private JCheckBox chBoxRTSAAnimateBracketMatching;
    private JCheckBox chBoxRTSAHighlightCurrentLine;
    private JCheckBox chBoxRTSAWordWrap;
    private JComboBox<CustomiszedLookAndFeelInfo> comboBoxLookAndFeel;
    private JFormattedTextField txtTabsCount;
    private JFormattedTextField txtMaxCharsInResult;
    private JFormattedTextField txtMaxCharsInTableCell;
    private JFormattedTextField txtCellRightPadding;
    private JFormattedTextField txtCellMaxWidth;
    private JFormattedTextField txtMaxFractionDigits;
    private JFormattedTextField txtEmulateDoubleClickTimeout;
    private JComboBox<Config.ExecAllOption> comboBoxExecAll;
    private JComboBox<LineEnding> comboBoxLineEnding;
    private JCheckBox chBoxEmulateTab;
    private JFormattedTextField txtEmulatedTabSize;
    private JCheckBox chBoxReplaceTabOnOpen;
    private JButton btnOk;
    private JButton btnCancel;

    private FontSelectionPanel editorFontSelection, resultFontSelection;

    private static final Config CONFIG = Config.getInstance();

    public SettingsDialog(JFrame owner) {
        super(owner, "Settings");
        initComponents();
    }

    public String getDefaultAuthenticationMechanism() {
        return comboBoxAuthMechanism.getModel().getSelectedItem().toString();
    }

    public String getUser() {
        return txtUser.getText().trim();
    }

    public String getPassword() {
        return new String(txtPassword.getPassword());
    }

    public boolean isShowServerComboBox() {
        return chBoxShowServerCombo.isSelected();
    }

    public String getLookAndFeelClassName() {
        return ((CustomiszedLookAndFeelInfo)comboBoxLookAndFeel.getSelectedItem()).getClassName();
    }

    public int getResultTabsCount() {
        return (Integer) txtTabsCount.getValue();
    }

    public int getMaxCharsInResult() {
        return (Integer) txtMaxCharsInResult.getValue();
    }

    public int getMaxCharsInTableCell() {
        return (Integer) txtMaxCharsInTableCell.getValue();
    }

    public double getCellRightPadding() {
        return (Double) txtCellRightPadding.getValue();
    }

    public int getCellMaxWidth() {
        return (Integer) txtCellMaxWidth.getValue();
    }

    public Config.ExecAllOption getExecAllOption() {
        return (Config.ExecAllOption) comboBoxExecAll.getSelectedItem();
    }

    public boolean isAutoSave() {
        return chBoxAutoSave.isSelected();
    }

    public boolean isAnimateBracketMatching() {
        return chBoxRTSAAnimateBracketMatching.isSelected();
    }

    public boolean isHighlightCurrentLine() {
        return chBoxRTSAHighlightCurrentLine.isSelected();
    }

    public boolean isWordWrap() {
        return chBoxRTSAWordWrap.isSelected();
    }

    public LineEnding getDefaultLineEnding() {
        return (LineEnding) comboBoxLineEnding.getSelectedItem();
    }

    public int getMaxFractionDigits() {
        return (Integer) txtMaxFractionDigits.getValue();
    }

    public int getEmulatedDoubleClickTimeout() {
        return (Integer) txtEmulateDoubleClickTimeout.getValue();
    }

    private void refreshCredentials() {
        Credentials credentials = CONFIG.getDefaultCredentials(getDefaultAuthenticationMechanism());

        txtUser.setText(credentials.getUsername());
        txtPassword.setText(credentials.getPassword());
    }

    @Override
    public void align() {
        super.align();
        btnOk.requestFocusInWindow();
    }

    private void initComponents() {
        txtUser = new JTextField(12);
        txtPassword = new JPasswordField(12);
        comboBoxAuthMechanism = new JComboBox<>(AuthenticationManager.getInstance().getAuthenticationMechanisms());
        comboBoxAuthMechanism.getModel().setSelectedItem(CONFIG.getDefaultAuthMechanism());
        comboBoxAuthMechanism.addItemListener(e -> refreshCredentials());
        refreshCredentials();

        JLabel lblLookAndFeel = new JLabel("Look and Feel:");

        LookAndFeels lookAndFeels = new LookAndFeels();
        comboBoxLookAndFeel = new JComboBox<>(lookAndFeels.getLookAndFeels());
        CustomiszedLookAndFeelInfo lf = lookAndFeels.getLookAndFeel(CONFIG.getLookAndFeel());
        if (lf == null) {
            lf = lookAndFeels.getLookAndFeel(UIManager.getLookAndFeel().getClass().getName());
        }

        chBoxRTSAAnimateBracketMatching = new JCheckBox("Animate bracket matching");
        chBoxRTSAAnimateBracketMatching.setSelected(CONFIG.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));

        chBoxRTSAHighlightCurrentLine = new JCheckBox("Highlight current line");
        chBoxRTSAHighlightCurrentLine.setSelected(CONFIG.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));

        chBoxRTSAWordWrap = new JCheckBox("Word wrap");
        chBoxRTSAWordWrap.setSelected(CONFIG.getBoolean(Config.RSTA_WORD_WRAP));

        NumberFormatter formatter = new NumberFormatter();
        formatter.setMinimum(1);

        chBoxSessionInvalidation = new JCheckBox("Kdb connections are invalidated every ");
        chBoxSessionInvalidation.setSelected(CONFIG.getBoolean(Config.SESSION_INVALIDATION_ENABLED));

        JLabel lblSessionInvalidationSuffix = new JLabel(" hour(s)");
        txtSessionInvalidation = new JFormattedTextField(formatter);
        txtSessionInvalidation.setValue(CONFIG.getInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS));

        chBoxSessionReuse = new JCheckBox("Reuse kdb connection between tabs");
        chBoxSessionReuse.setSelected(CONFIG.getBoolean(Config.SESSION_REUSE));

        chBoxEmulateTab = new JCheckBox("Emulate tab with spaces");
        chBoxEmulateTab.setSelected(CONFIG.getBoolean(Config.EDITOR_TAB_EMULATED));
        txtEmulatedTabSize = new JFormattedTextField(formatter);
        txtEmulatedTabSize.setValue(CONFIG.getInt(Config.EDITOR_TAB_SIZE));
        chBoxReplaceTabOnOpen = new JCheckBox("Automatically replace tabs on file load");
        chBoxReplaceTabOnOpen.setSelected(CONFIG.getBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN));

        comboBoxLookAndFeel.setSelectedItem(lf);
        JLabel lblResultTabsCount = new JLabel("Result tabs count");
        txtTabsCount = new JFormattedTextField(formatter);
        txtTabsCount.setValue(CONFIG.getResultTabsCount());
        chBoxShowServerCombo = new JCheckBox("Show server drop down list in the toolbar");
        chBoxShowServerCombo.setSelected(CONFIG.isShowServerComboBox());
        JLabel lblMaxCharsInResult = new JLabel("Max chars in result");
        txtMaxCharsInResult = new JFormattedTextField(formatter);
        txtMaxCharsInResult.setValue(CONFIG.getMaxCharsInResult());
        JLabel lblMaxCharsInTableCell = new JLabel("Max chars in table cell");
        txtMaxCharsInTableCell = new JFormattedTextField(formatter);
        txtMaxCharsInTableCell.setValue(CONFIG.getMaxCharsInTableCell());

        JLabel lblMaxFractionDigits = new JLabel("Max number of fraction digits in output");
        formatter = new NumberFormatter();
        formatter.setMinimum(1);
        formatter.setMaximum(20);
        txtMaxFractionDigits = new JFormattedTextField(formatter);
        txtMaxFractionDigits.setValue(CONFIG.getInt(Config.MAX_FRACTION_DIGITS));

        JLabel lblEmulatedDoubleClickTimeout = new JLabel("Emulated double-click speed (for copy action), ms");
        formatter = new NumberFormatter();
        formatter.setMinimum(0);
        formatter.setMaximum(2000);
        txtEmulateDoubleClickTimeout = new JFormattedTextField(formatter);
        txtEmulateDoubleClickTimeout.setValue(CONFIG.getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT));

        JLabel lblCellRightPadding = new JLabel("Right padding in table cell");

        NumberFormat doubleFormat = DecimalFormat.getInstance();
        doubleFormat.setMaximumFractionDigits(1);
        doubleFormat.setRoundingMode(RoundingMode.HALF_UP);
        NumberFormatter doubleFormatter = new NumberFormatter(doubleFormat);
        doubleFormatter.setMinimum(0.0);
        txtCellRightPadding = new JFormattedTextField(doubleFormatter);
        txtCellRightPadding.setValue(CONFIG.getDouble(Config.CELL_RIGHT_PADDING));

        JLabel lblCellMaxWidth = new JLabel("Max width of table columns");
        NumberFormatter maxWidthFormatter = new NumberFormatter();
        maxWidthFormatter.setMinimum(10);
        txtCellMaxWidth = new JFormattedTextField(maxWidthFormatter);
        txtCellMaxWidth.setValue(CONFIG.getInt(Config.CELL_MAX_WIDTH));

        JLabel lblExecAll = new JLabel ("Execute the script when nothing is selected:");
        comboBoxExecAll = new JComboBox<>(Config.ExecAllOption.values());
        comboBoxExecAll.setSelectedItem(CONFIG.getExecAllOption());
        chBoxAutoSave = new JCheckBox("Auto save files");
        chBoxAutoSave.setSelected(CONFIG.getBoolean(Config.AUTO_SAVE));
        JLabel lblActionOnExit = new JLabel("Action on exit: ");
        comboBoxActionOnExit = new JComboBox<>(ActionOnExit.values());
        comboBoxActionOnExit.setSelectedItem(CONFIG.getEnum(Config.ACTION_ON_EXIT));

        JLabel lblDefaultLineEnding = new JLabel ("Default line ending:");
        comboBoxLineEnding = new JComboBox<>(LineEnding.values());
        comboBoxLineEnding.setSelectedItem(CONFIG.getEnum(Config.DEFAULT_LINE_ENDING));

        editorFontSelection = new FontSelectionPanel(this, "Editor font: ", Config.FONT_EDITOR);
        resultFontSelection = new FontSelectionPanel(this, "Result table font: ", Config.FONT_TABLE);

        JLabel lblAuthMechanism = new JLabel("Authentication:");
        JLabel lblUser = new JLabel("  User:");
        JLabel lblPassword = new JLabel("  Password:");

        btnOk = new JButton("OK");
        btnCancel = new JButton("Cancel");

        btnOk.addActionListener(e->accept());
        btnCancel.addActionListener(e->cancel());


        JPanel pnlGeneral = new JPanel();
        GroupLayoutSimple layout = new GroupLayoutSimple(pnlGeneral);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblLookAndFeel, comboBoxLookAndFeel)
                        .addLineAndGlue(chBoxShowServerCombo, chBoxAutoSave)
                        .addLineAndGlue(lblActionOnExit, comboBoxActionOnExit)
                        .addLineAndGlue(chBoxSessionInvalidation, txtSessionInvalidation, lblSessionInvalidationSuffix)
                        .addLineAndGlue(chBoxSessionReuse)
                        .addLine(lblAuthMechanism, comboBoxAuthMechanism, lblUser, txtUser, lblPassword, txtPassword)
        );

        JPanel pnlEditor = new JPanel();
        layout = new GroupLayoutSimple(pnlEditor);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(chBoxRTSAAnimateBracketMatching, chBoxRTSAHighlightCurrentLine, chBoxRTSAWordWrap)
                        .addLineAndGlue(chBoxEmulateTab, txtEmulatedTabSize, chBoxReplaceTabOnOpen)
                        .addLineAndGlue(lblDefaultLineEnding, comboBoxLineEnding)
                        .addLineAndGlue(lblExecAll, comboBoxExecAll)
        );

        JPanel pnlResult = new JPanel();
        layout = new GroupLayoutSimple(pnlResult);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblMaxFractionDigits, txtMaxFractionDigits)
                        .addLineAndGlue(lblEmulatedDoubleClickTimeout, txtEmulateDoubleClickTimeout)
                        .addLineAndGlue(lblResultTabsCount, txtTabsCount)
                        .addLine(lblMaxCharsInResult, txtMaxCharsInResult, lblMaxCharsInTableCell, txtMaxCharsInTableCell)
                        .addLine(lblCellRightPadding, txtCellRightPadding, lblCellMaxWidth, txtCellMaxWidth)
        );
        layout.linkSize(SwingConstants.HORIZONTAL, lblCellRightPadding, txtMaxFractionDigits, txtEmulateDoubleClickTimeout, txtTabsCount,
                txtMaxCharsInResult, txtMaxCharsInTableCell, txtCellRightPadding, txtCellMaxWidth);

        JPanel pnlStyle = new JPanel();
        layout = new GroupLayoutSimple(pnlStyle);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(editorFontSelection)
                        .addLineAndGlue(resultFontSelection)
        );

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", getTabComponent(pnlGeneral));
        tabs.addTab("Editor", getTabComponent(pnlEditor));
        tabs.addTab("Result", getTabComponent(pnlResult));
        tabs.addTab("Style", getTabComponent(pnlStyle));

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.add(btnOk);
        pnlButtons.add(btnCancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(tabs, BorderLayout.CENTER);
        root.add(pnlButtons, BorderLayout.SOUTH);
        setContentPane(root);
    }

    public void saveSettings() {
        String auth = getDefaultAuthenticationMechanism();
        CONFIG.setBoolean(Config.SESSION_INVALIDATION_ENABLED, chBoxSessionInvalidation.isSelected());
        CONFIG.setInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS, Math.max(1,(Integer)txtSessionInvalidation.getValue()));
        CONFIG.setBoolean(Config.SESSION_REUSE, chBoxSessionReuse.isSelected());
        CONFIG.setDefaultAuthMechanism(auth);
        CONFIG.setDefaultCredentials(auth, new Credentials(getUser(), getPassword()));
        CONFIG.setShowServerComboBox(isShowServerComboBox());
        CONFIG.setResultTabsCount(getResultTabsCount());
        CONFIG.setMaxCharsInResult(getMaxCharsInResult());
        CONFIG.setMaxCharsInTableCell(getMaxCharsInTableCell());
        CONFIG.setDouble(Config.CELL_RIGHT_PADDING, getCellRightPadding());
        CONFIG.setInt(Config.CELL_MAX_WIDTH, getCellMaxWidth());
        CONFIG.setExecAllOption(getExecAllOption());
        CONFIG.setEnum(Config.ACTION_ON_EXIT, (ActionOnExit)comboBoxActionOnExit.getSelectedItem());
        CONFIG.setBoolean(Config.AUTO_SAVE, isAutoSave());
        CONFIG.setEnum(Config.DEFAULT_LINE_ENDING, getDefaultLineEnding());

        int maxFractionDigits = getMaxFractionDigits();
        CONFIG.setInt(Config.MAX_FRACTION_DIGITS, maxFractionDigits);
        //Looks like a hack??
        KFormatContext.setMaxFractionDigits(maxFractionDigits);

        boolean changedEditor = CONFIG.setBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING, isAnimateBracketMatching());
        changedEditor |= CONFIG.setBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE, isHighlightCurrentLine());
        changedEditor |= CONFIG.setBoolean(Config.RSTA_WORD_WRAP, isWordWrap());
        changedEditor |= editorFontSelection.saveSettings();
        changedEditor |= CONFIG.setBoolean(Config.EDITOR_TAB_EMULATED, chBoxEmulateTab.isSelected());
        changedEditor |= CONFIG.setInt(Config.EDITOR_TAB_SIZE, (Integer)txtEmulatedTabSize.getValue());
        changedEditor |= CONFIG.setBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN, chBoxReplaceTabOnOpen.isSelected());;

        if (changedEditor) {
            StudioPanel.refreshEditorsSettings();
        }

        boolean changedResult = CONFIG.setInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT, getEmulatedDoubleClickTimeout());
        changedResult |= resultFontSelection.saveSettings();

        if (changedResult) {
            StudioPanel.refreshResultSettings();
        }

        String lfClass = getLookAndFeelClassName();
        if (!lfClass.equals(UIManager.getLookAndFeel().getClass().getName())) {
            CONFIG.setLookAndFeel(lfClass);
            StudioOptionPane.showMessage(StudioPanel.getActivePanel().getFrame(), "Look and Feel was changed. " +
                    "New L&F will take effect on the next start up.", "Look and Feel Setting Changed");
        }
    }
    
    
    private JComponent getTabComponent(JComponent panel) {
        Box container = Box.createVerticalBox();
        container.add(panel);
        container.add(Box.createGlue());
        container.setBackground(panel.getBackground());
        container.setOpaque(true);
        return new JScrollPane(container);
    }

    
    private static class LookAndFeels {
        private Map<String, CustomiszedLookAndFeelInfo> mapLookAndFeels;

        public LookAndFeels() {
            mapLookAndFeels = new HashMap<>();
            for (UIManager.LookAndFeelInfo lf: UIManager.getInstalledLookAndFeels()) {
                mapLookAndFeels.put(lf.getClassName(), new CustomiszedLookAndFeelInfo(lf));
            }
        }
        public CustomiszedLookAndFeelInfo[] getLookAndFeels() {
            return mapLookAndFeels.values().toArray(new CustomiszedLookAndFeelInfo[0]);
        }
        public CustomiszedLookAndFeelInfo getLookAndFeel(String className) {
            return mapLookAndFeels.get(className);
        }
    }

    private static class CustomiszedLookAndFeelInfo extends UIManager.LookAndFeelInfo {
        public CustomiszedLookAndFeelInfo(UIManager.LookAndFeelInfo lfInfo) {
            super(lfInfo.getName(), lfInfo.getClassName());
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
