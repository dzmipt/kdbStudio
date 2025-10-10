package studio.ui.settings;

import studio.kdb.Config;
import studio.kdb.config.ExecAllOption;
import studio.ui.GroupLayoutSimple;
import studio.utils.LineEnding;

import javax.swing.*;
import javax.swing.text.NumberFormatter;

public class SettingsEditorTab extends SettingsTab {

    private final JCheckBox chBoxRTSAAnimateBracketMatching;
    private final JCheckBox chBoxRTSAHighlightCurrentLine;
    private final JCheckBox chBoxRTSAWordWrap;
    private final JCheckBox chBoxRTSAInsertPairedChar;
    private final JCheckBox chBoxEmulateTab;
    private final JFormattedTextField txtEmulatedTabSize;
    private final JCheckBox chBoxReplaceTabOnOpen;
    private final JComboBox<ExecAllOption> comboBoxExecAll;
    private final JComboBox<LineEnding> comboBoxLineEnding;

    private static final Config CONFIG = Config.getInstance();

    public SettingsEditorTab() {
        NumberFormatter formatter = new NumberFormatter();
        formatter.setMinimum(1);

        chBoxRTSAAnimateBracketMatching = new JCheckBox("Animate bracket matching");
        chBoxRTSAAnimateBracketMatching.setSelected(CONFIG.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));

        chBoxRTSAHighlightCurrentLine = new JCheckBox("Highlight current line");
        chBoxRTSAHighlightCurrentLine.setSelected(CONFIG.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));

        chBoxRTSAWordWrap = new JCheckBox("Word wrap");
        chBoxRTSAWordWrap.setSelected(CONFIG.getBoolean(Config.RSTA_WORD_WRAP));

        chBoxRTSAInsertPairedChar = new JCheckBox("Insert paired () {} [] \"\" on selection");
        chBoxRTSAInsertPairedChar.setSelected(CONFIG.getBoolean(Config.RSTA_INSERT_PAIRED_CHAR));

        chBoxEmulateTab = new JCheckBox("Emulate tab with spaces");
        chBoxEmulateTab.setSelected(CONFIG.getBoolean(Config.EDITOR_TAB_EMULATED));
        txtEmulatedTabSize = new JFormattedTextField(formatter);
        txtEmulatedTabSize.setValue(CONFIG.getInt(Config.EDITOR_TAB_SIZE));
        chBoxReplaceTabOnOpen = new JCheckBox("Automatically replace tabs on file load");
        chBoxReplaceTabOnOpen.setSelected(CONFIG.getBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN));

        JLabel lblDefaultLineEnding = new JLabel ("Default line ending:");
        comboBoxLineEnding = new JComboBox<>(LineEnding.values());
        comboBoxLineEnding.setSelectedItem(CONFIG.getEnum(Config.DEFAULT_LINE_ENDING));

        JLabel lblExecAll = new JLabel ("Execute the script when nothing is selected:");
        comboBoxExecAll = new JComboBox<>(ExecAllOption.values());
        comboBoxExecAll.setSelectedItem(CONFIG.getEnum(Config.EXEC_ALL));

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(chBoxRTSAAnimateBracketMatching, chBoxRTSAHighlightCurrentLine,
                                chBoxRTSAWordWrap, chBoxRTSAInsertPairedChar)
                        .addLineAndGlue(chBoxEmulateTab, txtEmulatedTabSize, chBoxReplaceTabOnOpen)
                        .addLineAndGlue(lblDefaultLineEnding, comboBoxLineEnding)
                        .addLineAndGlue(lblExecAll, comboBoxExecAll)
        );
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

    public ExecAllOption getExecAllOption() {
        return (ExecAllOption) comboBoxExecAll.getSelectedItem();
    }

    public LineEnding getDefaultLineEnding() {
        return (LineEnding) comboBoxLineEnding.getSelectedItem();
    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        CONFIG.setEnum(Config.EXEC_ALL, getExecAllOption());
        CONFIG.setEnum(Config.DEFAULT_LINE_ENDING, getDefaultLineEnding());

        boolean changedEditor = CONFIG.setBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING, isAnimateBracketMatching());
        changedEditor |= CONFIG.setBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE, isHighlightCurrentLine());
        changedEditor |= CONFIG.setBoolean(Config.RSTA_WORD_WRAP, isWordWrap());
        changedEditor |= CONFIG.setBoolean(Config.RSTA_INSERT_PAIRED_CHAR, chBoxRTSAInsertPairedChar.isSelected());
        changedEditor |= CONFIG.setBoolean(Config.EDITOR_TAB_EMULATED, chBoxEmulateTab.isSelected());
        changedEditor |= CONFIG.setInt(Config.EDITOR_TAB_SIZE, (Integer)txtEmulatedTabSize.getValue());
        changedEditor |= CONFIG.setBoolean(Config.AUTO_REPLACE_TAB_ON_OPEN, chBoxReplaceTabOnOpen.isSelected());;

        result.setRefreshEditorsSettings(changedEditor);

    }
}
