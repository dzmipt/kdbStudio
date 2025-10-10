package studio.ui.settings;

import studio.kdb.Config;
import studio.kdb.KFormatContext;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class SettingsResultTab extends SettingsTab {

    private final JFormattedTextField txtTabsCount;
    private final JFormattedTextField txtMaxCharsInResult;
    private final JFormattedTextField txtMaxCharsInTableCell;
    private final JFormattedTextField txtCellRightPadding;
    private final JFormattedTextField txtCellMaxWidth;
    private final JFormattedTextField txtMaxFractionDigits;
    private final JFormattedTextField txtEmulateDoubleClickTimeout;
    private final JCheckBox chBoxOpenServerInCurrentTab;
    private final JCheckBox chBoxInspectResultInCurrentTab;

    private static final Config CONFIG = Config.getInstance();

    public SettingsResultTab() {
        NumberFormatter formatter = new NumberFormatter();
        formatter.setMinimum(1);
        JLabel lblResultTabsCount = new JLabel("Result tabs count");
        txtTabsCount = new JFormattedTextField(formatter);
        txtTabsCount.setValue(CONFIG.getInt(Config.RESULT_TAB_COUNTS));
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

        chBoxOpenServerInCurrentTab = new JCheckBox("Open servers from popup menu in the current editor tab");
        chBoxOpenServerInCurrentTab.setSelected(CONFIG.getBoolean(Config.SERVER_FROM_RESULT_IN_CURRENT));
        chBoxInspectResultInCurrentTab = new JCheckBox("Inspect result in the current tab");
        chBoxInspectResultInCurrentTab.setSelected(CONFIG.getBoolean(Config.INSPECT_RESULT_IN_CURRENT));

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblMaxFractionDigits, txtMaxFractionDigits)
                        .addLineAndGlue(lblEmulatedDoubleClickTimeout, txtEmulateDoubleClickTimeout)
                        .addLineAndGlue(lblResultTabsCount, txtTabsCount)
                        .addLine(lblMaxCharsInResult, txtMaxCharsInResult, lblMaxCharsInTableCell, txtMaxCharsInTableCell)
                        .addLine(lblCellRightPadding, txtCellRightPadding, lblCellMaxWidth, txtCellMaxWidth)
                        .addLineAndGlue(chBoxOpenServerInCurrentTab)
                        .addLineAndGlue(chBoxInspectResultInCurrentTab)

        );
        layout.linkSize(SwingConstants.HORIZONTAL, lblCellRightPadding, txtMaxFractionDigits, txtEmulateDoubleClickTimeout, txtTabsCount,
                txtMaxCharsInResult, txtMaxCharsInTableCell, txtCellRightPadding, txtCellMaxWidth);

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

    public int getMaxFractionDigits() {
        return (Integer) txtMaxFractionDigits.getValue();
    }

    public int getEmulatedDoubleClickTimeout() {
        return (Integer) txtEmulateDoubleClickTimeout.getValue();
    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        CONFIG.setInt(Config.RESULT_TAB_COUNTS, getResultTabsCount());
        CONFIG.setInt(Config.MAX_CHARS_IN_RESULT, getMaxCharsInResult());
        CONFIG.setInt(Config.MAX_CHARS_IN_TABLE_CELL, getMaxCharsInTableCell());
        CONFIG.setDouble(Config.CELL_RIGHT_PADDING, getCellRightPadding());
        CONFIG.setInt(Config.CELL_MAX_WIDTH, getCellMaxWidth());
        CONFIG.setBoolean(Config.SERVER_FROM_RESULT_IN_CURRENT, chBoxOpenServerInCurrentTab.isSelected());
        CONFIG.setBoolean(Config.INSPECT_RESULT_IN_CURRENT, chBoxInspectResultInCurrentTab.isSelected());

        int maxFractionDigits = getMaxFractionDigits();
        CONFIG.setInt(Config.MAX_FRACTION_DIGITS, maxFractionDigits);
        //Looks like a hack??
        KFormatContext.setMaxFractionDigits(maxFractionDigits);

        boolean changedResult = CONFIG.setInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT, getEmulatedDoubleClickTimeout());
        result.setRefreshResultSettings(changedResult);
    }
}
