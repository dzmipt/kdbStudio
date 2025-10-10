package studio.ui.settings;

import studio.kdb.Config;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;

public class SettingsStyleTab extends SettingsTab {

    private FontSelectionPanel editorFontSelection, resultFontSelection;

    private static final Config CONFIG = Config.getInstance();

    public SettingsStyleTab(JDialog parentDialog) {
        editorFontSelection = new FontSelectionPanel(parentDialog, "Editor font: ", Config.FONT_EDITOR);
        resultFontSelection = new FontSelectionPanel(parentDialog, "Result table font: ", Config.FONT_TABLE);

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(editorFontSelection)
                        .addLineAndGlue(resultFontSelection)
        );

    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        boolean changed = editorFontSelection.saveSettings();
        result.setRefreshEditorsSettings(changed);

        changed = resultFontSelection.saveSettings();
        result.setRefreshResultSettings(changed);
    }

}
