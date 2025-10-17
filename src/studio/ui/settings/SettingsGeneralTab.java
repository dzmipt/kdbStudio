package studio.ui.settings;

import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.kdb.Config;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.KdbMessageLimitAction;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsGeneralTab extends SettingsTab {

    private final JComboBox<String> comboBoxAuthMechanism;
    private final JTextField txtUser;
    private final JPasswordField txtPassword;
    private final JCheckBox chBoxSessionInvalidation;
    private final JFormattedTextField txtSessionInvalidation;
    private final JCheckBox chBoxSessionReuse;
    private final JFormattedTextField txtKdbMessageSizeLimit;
    private final JComboBox<KdbMessageLimitAction> comboBoxKdbMessageSizeAction;
    private final JCheckBox chBoxShowServerCombo;
    private final JCheckBox chBoxAutoSave;
    private final JComboBox<ActionOnExit> comboBoxActionOnExit;
    private final JComboBox<CustomiszedLookAndFeelInfo> comboBoxLookAndFeel;

    private static final Config CONFIG = Config.getInstance();

    public SettingsGeneralTab() {
        NumberFormatter formatter = new NumberFormatter();
        formatter.setMinimum(1);

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

        comboBoxLookAndFeel.setSelectedItem(lf);

        chBoxShowServerCombo = new JCheckBox("Show server drop down list in the toolbar");
        chBoxShowServerCombo.setSelected(CONFIG.getBoolean(Config.SHOW_SERVER_COMBOBOX));

        chBoxAutoSave = new JCheckBox("Auto save files");
        chBoxAutoSave.setSelected(CONFIG.getBoolean(Config.AUTO_SAVE));
        JLabel lblActionOnExit = new JLabel("Action on exit: ");
        comboBoxActionOnExit = new JComboBox<>(ActionOnExit.values());
        comboBoxActionOnExit.setSelectedItem(CONFIG.getEnum(Config.ACTION_ON_EXIT));

        JLabel lblAuthMechanism = new JLabel("Authentication:");
        JLabel lblUser = new JLabel("  User:");
        JLabel lblPassword = new JLabel("  Password:");

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
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

    }

    private void refreshCredentials() {
        Credentials credentials = CONFIG.getDefaultCredentials(getDefaultAuthenticationMechanism());

        txtUser.setText(credentials.getUsername());
        txtPassword.setText(credentials.getPassword());
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

    public boolean isAutoSave() {
        return chBoxAutoSave.isSelected();
    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        String auth = getDefaultAuthenticationMechanism();
        CONFIG.setBoolean(Config.SESSION_INVALIDATION_ENABLED, chBoxSessionInvalidation.isSelected());
        CONFIG.setInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS, Math.max(1,(Integer)txtSessionInvalidation.getValue()));
        CONFIG.setBoolean(Config.SESSION_REUSE, chBoxSessionReuse.isSelected());
        CONFIG.setInt(Config.KDB_MESSAGE_SIZE_LIMIT_MB, Math.max(1, (Integer)txtKdbMessageSizeLimit.getValue()));
        CONFIG.setEnum(Config.KDB_MESSAGE_SIZE_LIMIT_ACTION, (KdbMessageLimitAction) comboBoxKdbMessageSizeAction.getSelectedItem());
        CONFIG.setDefaultAuthMechanism(auth);
        CONFIG.setDefaultCredentials(auth, new Credentials(getUser(), getPassword()));

        boolean changed = CONFIG.setBoolean(Config.SHOW_SERVER_COMBOBOX, isShowServerComboBox());
        result.setRefreshComboServerVisibility(changed);

        CONFIG.setEnum(Config.ACTION_ON_EXIT, (ActionOnExit)comboBoxActionOnExit.getSelectedItem());
        CONFIG.setBoolean(Config.AUTO_SAVE, isAutoSave());

        changed = CONFIG.setString(Config.LOOK_AND_FEEL, getLookAndFeelClassName());
        result.setChangedLF(changed);

    }

    private static class LookAndFeels {
        private Map<String, CustomiszedLookAndFeelInfo> mapLookAndFeels;

        public LookAndFeels() {
            mapLookAndFeels = new LinkedHashMap<>();
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
