package studio.ui;

import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.kdb.Config;
import studio.kdb.KFormatContext;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.ColorSets;
import studio.kdb.config.ExecAllOption;
import studio.kdb.config.KdbMessageLimitAction;
import studio.ui.colorlist.ColorListComponent;
import studio.ui.settings.FontSelectionPanel;
import studio.utils.LineEnding;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsDialog extends EscapeDialog {
    private JComboBox<String> comboBoxAuthMechanism;
    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JCheckBox chBoxSessionInvalidation;
    private JFormattedTextField txtSessionInvalidation;
    private JCheckBox chBoxSessionReuse;
    private JFormattedTextField txtKdbMessageSizeLimit;
    private JComboBox<KdbMessageLimitAction> comboBoxKdbMessageSizeAction;
    private JCheckBox chBoxShowServerCombo;
    private JCheckBox chBoxAutoSave;
    private JComboBox<ActionOnExit> comboBoxActionOnExit;
    private JCheckBox chBoxRTSAAnimateBracketMatching;
    private JCheckBox chBoxRTSAHighlightCurrentLine;
    private JCheckBox chBoxRTSAWordWrap;
    private JCheckBox chBoxRTSAInsertPairedChar;
    private JComboBox<CustomiszedLookAndFeelInfo> comboBoxLookAndFeel;
    private JFormattedTextField txtTabsCount;
    private JFormattedTextField txtMaxCharsInResult;
    private JFormattedTextField txtMaxCharsInTableCell;
    private JFormattedTextField txtCellRightPadding;
    private JFormattedTextField txtCellMaxWidth;
    private JFormattedTextField txtMaxFractionDigits;
    private JFormattedTextField txtEmulateDoubleClickTimeout;
    private JComboBox<ExecAllOption> comboBoxExecAll;
    private JComboBox<LineEnding> comboBoxLineEnding;
    private JCheckBox chBoxEmulateTab;
    private JFormattedTextField txtEmulatedTabSize;
    private JCheckBox chBoxReplaceTabOnOpen;

    private ColorListComponent colorList;
    private JComboBox<String> comboBoxColorSetName;
    private JButton btnAddColorSet;
    private JButton btnDeleteColorSet;

    private JButton btnOk;
    private JButton btnCancel;

    private FontSelectionPanel editorFontSelection, resultFontSelection;
    private ColorSets chartColorSets;

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

    public ExecAllOption getExecAllOption() {
        return (ExecAllOption) comboBoxExecAll.getSelectedItem();
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
        CustomiszedLookAndFeelInfo lf = lookAndFeels.getLookAndFeel(CONFIG.getString(Config.LOOK_AND_FEEL));

        chBoxRTSAAnimateBracketMatching = new JCheckBox("Animate bracket matching");
        chBoxRTSAAnimateBracketMatching.setSelected(CONFIG.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));

        chBoxRTSAHighlightCurrentLine = new JCheckBox("Highlight current line");
        chBoxRTSAHighlightCurrentLine.setSelected(CONFIG.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));

        chBoxRTSAWordWrap = new JCheckBox("Word wrap");
        chBoxRTSAWordWrap.setSelected(CONFIG.getBoolean(Config.RSTA_WORD_WRAP));

        chBoxRTSAInsertPairedChar = new JCheckBox("Insert paired () {} [] \"\" on selection");
        chBoxRTSAInsertPairedChar.setSelected(CONFIG.getBoolean(Config.RSTA_INSERT_PAIRED_CHAR));

        NumberFormatter formatter = new NumberFormatter();
        formatter.setMinimum(1);

        chBoxSessionInvalidation = new JCheckBox("Kdb connections are invalidated every ");
        chBoxSessionInvalidation.setSelected(CONFIG.getBoolean(Config.SESSION_INVALIDATION_ENABLED));

        JLabel lblSessionInvalidationSuffix = new JLabel(" hour(s)");
        txtSessionInvalidation = new JFormattedTextField(formatter);
        txtSessionInvalidation.setValue(CONFIG.getInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS));

        chBoxSessionReuse = new JCheckBox("Reuse kdb connection between tabs");
        chBoxSessionReuse.setSelected(CONFIG.getBoolean(Config.SESSION_REUSE));

        JLabel lblKdbMessageSize = new JLabel("When the incoming message is greater than ");
        txtKdbMessageSizeLimit = new JFormattedTextField(formatter);
        txtKdbMessageSizeLimit.setValue(CONFIG.getInt(Config.KDB_MESSAGE_SIZE_LIMIT_MB));

        JLabel lblKdbMessageSizeSuffix = new JLabel("MB  ");
        comboBoxKdbMessageSizeAction = new JComboBox<>(KdbMessageLimitAction.values());
        comboBoxKdbMessageSizeAction.setSelectedItem(CONFIG.getEnum(Config.KDB_MESSAGE_SIZE_LIMIT_ACTION));

        chBoxEmulateTab = new JCheckBox("Emulate tab with spaces");
        chBoxEmulateTab.setSelected(CONFIG.getBoolean(Config.EDITOR_TAB_EMULATED));
        txtEmulatedTabSize = new JFormattedTextField(formatter);
        txtEmulatedTabSize.setValue(CONFIG.getInt(Config.EDITOR_TAB_SIZE));
        chBoxReplaceTabOnOpen = new JCheckBox("Automatically replace tabs on file load");
        chBoxReplaceTabOnOpen.setSelected(CONFIG.getBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN));

        comboBoxLookAndFeel.setSelectedItem(lf);
        JLabel lblResultTabsCount = new JLabel("Result tabs count");
        txtTabsCount = new JFormattedTextField(formatter);
        txtTabsCount.setValue(CONFIG.getInt(Config.RESULT_TAB_COUNTS));
        chBoxShowServerCombo = new JCheckBox("Show server drop down list in the toolbar");
        chBoxShowServerCombo.setSelected(CONFIG.getBoolean(Config.SHOW_SERVER_COMBOBOX));
        JLabel lblMaxCharsInResult = new JLabel("Max chars in result");
        txtMaxCharsInResult = new JFormattedTextField(formatter);
        txtMaxCharsInResult.setValue(CONFIG.getInt(Config.MAX_CHARS_IN_RESULT));
        JLabel lblMaxCharsInTableCell = new JLabel("Max chars in table cell");
        txtMaxCharsInTableCell = new JFormattedTextField(formatter);
        txtMaxCharsInTableCell.setValue(CONFIG.getInt(Config.MAX_CHARS_IN_TABLE_CELL));

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
        comboBoxExecAll = new JComboBox<>(ExecAllOption.values());
        comboBoxExecAll.setSelectedItem(CONFIG.getEnum(Config.EXEC_ALL));
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
                        .addLineAndGlue(lblKdbMessageSize, txtKdbMessageSizeLimit, lblKdbMessageSizeSuffix, comboBoxKdbMessageSizeAction)
                        .addLine(lblAuthMechanism, comboBoxAuthMechanism, lblUser, txtUser, lblPassword, txtPassword)
        );

        JPanel pnlEditor = new JPanel();
        layout = new GroupLayoutSimple(pnlEditor);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(chBoxRTSAAnimateBracketMatching, chBoxRTSAHighlightCurrentLine,
                                        chBoxRTSAWordWrap, chBoxRTSAInsertPairedChar)
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


        JLabel lblColorSchema = new JLabel("Color schema: ");
        comboBoxColorSetName = new JComboBox<>();
        comboBoxColorSetName.addActionListener(this::colorSetNameSelected);
        btnAddColorSet = new JButton("new");
        btnAddColorSet.addActionListener(this::chartAddColorSetAction);
        btnDeleteColorSet = new JButton("delete");
        btnDeleteColorSet.addActionListener(this::chartDeleteColorSetAction);
        colorList = new ColorListComponent();
        colorList.setPreferredSize(new Dimension(0, 0));
        colorList.addActionListener(this::colorsChanged);
        colorList.setToolTipText("<html>Use drag&drop, double click, <code>INS</code>, <code>DEL</code> to edit</html>");

        chartColorSets = CONFIG.getChartColorSets();
        refreshChartColorSet();

        JPanel pnlChart = new JPanel();
        layout = new GroupLayoutSimple(pnlChart);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblColorSchema, comboBoxColorSetName, btnAddColorSet, btnDeleteColorSet)
                        .addLine(colorList)
        );


        JScrollPane scrollChart = getTabComponent(pnlChart);
        JViewport viewport = scrollChart.getViewport();
        colorList.setPrefWidthComponent(viewport, 12);
        viewport.addChangeListener( e -> {
            colorList.revalidate();
            colorList.repaint();
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("General", getTabComponent(pnlGeneral));
        tabs.addTab("Editor", getTabComponent(pnlEditor));
        tabs.addTab("Result", getTabComponent(pnlResult));
        tabs.addTab("Style", getTabComponent(pnlStyle));
        tabs.addTab("Chart", scrollChart);

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.add(btnOk);
        pnlButtons.add(btnCancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(tabs, BorderLayout.CENTER);
        root.add(pnlButtons, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void refreshChartColorSet() {
        String[] names = chartColorSets.getNames().toArray(new String[0]);
        comboBoxColorSetName.setModel(new DefaultComboBoxModel<>(names));
        comboBoxColorSetName.setSelectedItem(chartColorSets.getDefaultName());
        colorList.setColors(chartColorSets.getColors(chartColorSets.getDefaultName()));
        btnDeleteColorSet.setEnabled(chartColorSets.getNames().size() > 1);
    }

    private void colorSetNameSelected(ActionEvent e) {
        chartColorSets = chartColorSets.newSelected((String)comboBoxColorSetName.getSelectedItem());
        refreshChartColorSet();
    }

    private void chartAddColorSetAction(ActionEvent e) {
        String name = JOptionPane.showInputDialog(this, "Enter name:", "Add Color Schema", JOptionPane.QUESTION_MESSAGE);
        if (name == null) return;

        if (chartColorSets.getNames().contains(name)) {
            chartColorSets = chartColorSets.newSelected(name);
        } else {
            List<Color> colors = new ArrayList<>();
            colors.add(Color.BLACK);
            chartColorSets = chartColorSets.setColorSet(name, colors);
        }
        refreshChartColorSet();
    }

    private void chartDeleteColorSetAction(ActionEvent e) {
        chartColorSets = chartColorSets.deleteColorSet((String)comboBoxColorSetName.getSelectedItem());
        refreshChartColorSet();
    }

    private void colorsChanged(ActionEvent e) {
        chartColorSets = chartColorSets.setColorSet(chartColorSets.getDefaultName(), colorList.getColors());
        refreshChartColorSet();
    }

    public void saveSettings() {
        String auth = getDefaultAuthenticationMechanism();
        CONFIG.setBoolean(Config.SESSION_INVALIDATION_ENABLED, chBoxSessionInvalidation.isSelected());
        CONFIG.setInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS, Math.max(1,(Integer)txtSessionInvalidation.getValue()));
        CONFIG.setBoolean(Config.SESSION_REUSE, chBoxSessionReuse.isSelected());
        CONFIG.setInt(Config.KDB_MESSAGE_SIZE_LIMIT_MB, Math.max(1, (Integer)txtKdbMessageSizeLimit.getValue()));
        CONFIG.setEnum(Config.KDB_MESSAGE_SIZE_LIMIT_ACTION, (KdbMessageLimitAction) comboBoxKdbMessageSizeAction.getSelectedItem());
        CONFIG.setDefaultAuthMechanism(auth);
        CONFIG.setDefaultCredentials(auth, new Credentials(getUser(), getPassword()));

        if (CONFIG.setBoolean(Config.SHOW_SERVER_COMBOBOX, isShowServerComboBox()) ) {
            StudioWindow.refreshComboServerVisibility();
        }

        CONFIG.setInt(Config.RESULT_TAB_COUNTS, getResultTabsCount());
        CONFIG.setInt(Config.MAX_CHARS_IN_RESULT, getMaxCharsInResult());
        CONFIG.setInt(Config.MAX_CHARS_IN_TABLE_CELL, getMaxCharsInTableCell());
        CONFIG.setDouble(Config.CELL_RIGHT_PADDING, getCellRightPadding());
        CONFIG.setInt(Config.CELL_MAX_WIDTH, getCellMaxWidth());
        CONFIG.setEnum(Config.EXEC_ALL, getExecAllOption());
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
        changedEditor |= CONFIG.setBoolean(Config.RSTA_INSERT_PAIRED_CHAR, chBoxRTSAInsertPairedChar.isSelected());
        changedEditor |= editorFontSelection.saveSettings();
        changedEditor |= CONFIG.setBoolean(Config.EDITOR_TAB_EMULATED, chBoxEmulateTab.isSelected());
        changedEditor |= CONFIG.setInt(Config.EDITOR_TAB_SIZE, (Integer)txtEmulatedTabSize.getValue());
        changedEditor |= CONFIG.setBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN, chBoxReplaceTabOnOpen.isSelected());;

        if (changedEditor) {
            StudioWindow.refreshEditorsSettings();
        }

        boolean changedResult = CONFIG.setInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT, getEmulatedDoubleClickTimeout());
        changedResult |= resultFontSelection.saveSettings();

        if (changedResult) {
            StudioWindow.refreshResultSettings();
        }

        CONFIG.setChartColorSets(chartColorSets);

        String lfClass = getLookAndFeelClassName();
        if (!lfClass.equals(UIManager.getLookAndFeel().getClass().getName())) {
            CONFIG.setString(Config.LOOK_AND_FEEL, lfClass);
            StudioOptionPane.showMessage(this, "Look and Feel was changed. " +
                    "New L&F will take effect on the next start up.", "Look and Feel Setting Changed");
        }
    }
    
    
    private JScrollPane getTabComponent(JComponent panel) {
        return new JScrollPane(panel);
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
