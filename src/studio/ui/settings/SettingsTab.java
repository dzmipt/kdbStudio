package studio.ui.settings;

import javax.swing.*;

abstract public class SettingsTab extends JPanel {

    abstract public void saveSettings(SettingsSaveResult result);
}
