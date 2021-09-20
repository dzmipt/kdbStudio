package studio.ui;

import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.kdb.Config;

import javax.swing.*;
import javax.swing.text.NumberFormatter;

import org.jfree.chart.ui.FontChooserPanel;

import java.awt.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import static javax.swing.GroupLayout.PREFERRED_SIZE;

public class SettingsDialog extends EscapeDialog {
    private static class FontBox {
        final JLabel label;
        final JButton button;
        final String qualifier;
        Font currentFont;

        FontBox(String qualifier, Font currentFont) {
            this.qualifier = qualifier;
            this.currentFont = currentFont;
            this.label = new JLabel();
            this.button = new JButton("Change");
            this.button.addActionListener(a -> {
                FontChooserPanel panel = new FontChooserPanel(currentFont);
                int result = JOptionPane.showConfirmDialog(button, panel,
                        "Font Selection", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    this.currentFont = panel.getSelectedFont();
                    update();
                }
            });
            update();
        }

        void update() {
            label.setText(String.format("%s font: %s, %d", qualifier, currentFont.getName(), currentFont.getSize()));
        }

        Font getFont() {
            return currentFont;
        }
    }
    private JComboBox<CustomiszedLookAndFeelInfo> comboBoxLookAndFeel;
    private FontBox editorFont;
    private FontBox tableFont;
    private JComboBox<String> comboBoxAuthMechanism;
    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JCheckBox chBoxShowServerCombo;
    private JCheckBox chBoxAutoSave;
    private JCheckBox chBoxSaveOnExit;
    private JFormattedTextField txtTabsCount;
    private JFormattedTextField txtMaxCharsInResult;
    private JFormattedTextField txtMaxCharsInTableCell;
    private JFormattedTextField txtCellRightPadding;
    private JFormattedTextField txtCellMaxWidth;
    private JComboBox<Config.ExecAllOption> comboBoxExecAll;
    private JButton btnOk;
    private JButton btnCancel;

    private final static int FIELD_SIZE = 150;

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

    public boolean isSaveOnExit() {
        return chBoxSaveOnExit.isSelected();
    }

    public Font getEditorFont() {
        return editorFont.getFont();
    }

    public Font getTableFont() {
        return tableFont.getFont();
    }

    private void refreshCredentials() {
        Credentials credentials = Config.getInstance().getDefaultCredentials(getDefaultAuthenticationMechanism());

        txtUser.setText(credentials.getUsername());
        txtPassword.setText(credentials.getPassword());
    }

    @Override
    public void align() {
        super.align();
        btnOk.requestFocusInWindow();
    }

    private void initComponents() {
        JPanel root = new JPanel();

        txtUser = new JTextField();
        txtPassword = new JPasswordField();
        comboBoxAuthMechanism = new JComboBox<>(AuthenticationManager.getInstance().getAuthenticationMechanisms());
        comboBoxAuthMechanism.getModel().setSelectedItem(Config.getInstance().getDefaultAuthMechanism());
        comboBoxAuthMechanism.addItemListener(e -> refreshCredentials());
        refreshCredentials();

        JLabel lblLookAndFeel = new JLabel("Look and Feel:");

        LookAndFeels lookAndFeels = new LookAndFeels();
        comboBoxLookAndFeel = new JComboBox<>(lookAndFeels.getLookAndFeels());
        CustomiszedLookAndFeelInfo lf = lookAndFeels.getLookAndFeel(Config.getInstance().getLookAndFeel());
        if (lf == null) {
            lf = lookAndFeels.getLookAndFeel(UIManager.getLookAndFeel().getClass().getName());
        }
        comboBoxLookAndFeel.setSelectedItem(lf);
        JLabel lblResultTabsCount = new JLabel("Result tabs count");
        NumberFormatter formatter = new NumberFormatter();
        formatter.setMinimum(1);
        formatter.setAllowsInvalid(false);
        txtTabsCount = new JFormattedTextField(formatter);
        txtTabsCount.setValue(Config.getInstance().getResultTabsCount());
        chBoxShowServerCombo = new JCheckBox("Show server drop down list in the toolbar");
        chBoxShowServerCombo.setSelected(Config.getInstance().isShowServerComboBox());
        JLabel lblMaxCharsInResult = new JLabel("Max chars in result");
        txtMaxCharsInResult = new JFormattedTextField(formatter);
        txtMaxCharsInResult.setValue(Config.getInstance().getMaxCharsInResult());
        JLabel lblMaxCharsInTableCell = new JLabel("Max chars in table cell");
        txtMaxCharsInTableCell = new JFormattedTextField(formatter);
        txtMaxCharsInTableCell.setValue(Config.getInstance().getMaxCharsInTableCell());

        JLabel lblCellRightPadding = new JLabel("Right padding in table cell");

        NumberFormat doubleFormat = DecimalFormat.getInstance();
        doubleFormat.setMaximumFractionDigits(1);
        doubleFormat.setRoundingMode(RoundingMode.HALF_UP);
        NumberFormatter doubleFormatter = new NumberFormatter(doubleFormat);
        doubleFormatter.setMinimum(0.0);
        txtCellRightPadding = new JFormattedTextField(doubleFormatter);
        txtCellRightPadding.setValue(Config.getInstance().getDouble(Config.CELL_RIGHT_PADDING));

        JLabel lblCellMaxWidth = new JLabel("Max width of table columns");
        NumberFormatter maxWidthFormatter = new NumberFormatter();
        maxWidthFormatter.setMinimum(10);
        maxWidthFormatter.setAllowsInvalid(false);
        txtCellMaxWidth = new JFormattedTextField(maxWidthFormatter);
        txtCellMaxWidth.setValue(Config.getInstance().getInt(Config.CELL_MAX_WIDTH));


        JLabel lblExecAll = new JLabel ("Execute the script when nothing is selected");
        comboBoxExecAll = new JComboBox<>(Config.ExecAllOption.values());
        comboBoxExecAll.setSelectedItem(Config.getInstance().getExecAllOption());
        chBoxAutoSave = new JCheckBox("Auto save files");
        chBoxAutoSave.setSelected(Config.getInstance().getBoolean(Config.AUTO_SAVE));
        chBoxSaveOnExit = new JCheckBox("Ask save file on exit");
        chBoxSaveOnExit.setSelected(Config.getInstance().getBoolean(Config.SAVE_ON_EXIT));
        JLabel lblAuthMechanism = new JLabel("Authentication:");
        JLabel lblUser = new JLabel("  User:");
        JLabel lblPassword = new JLabel("  Password:");

        editorFont = new FontBox("Editor", Config.getInstance().getFont(Config.FontKind.EDITOR));
        tableFont = new FontBox("Table", Config.getInstance().getFont(Config.FontKind.TABLE));

        Component glue = Box.createGlue();
        Component glue1 = Box.createGlue();
        Component glue2 = Box.createGlue();
        Component glue3 = Box.createGlue();
        Component glue4 = Box.createGlue();

        btnOk = new JButton("OK");
        btnCancel = new JButton("Cancel");

        btnOk.addActionListener(e->accept());
        btnCancel.addActionListener(e->cancel());

        GroupLayout layout = new GroupLayout(root);
        root.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createParallelGroup()
                        .addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(lblLookAndFeel)
                                        .addComponent(comboBoxLookAndFeel)
                                        .addComponent(glue2)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(editorFont.label)
                                        .addComponent(editorFont.button)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(tableFont.label)
                                        .addComponent(tableFont.button)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(lblResultTabsCount)
                                        .addComponent(txtTabsCount)
                                        .addComponent(chBoxShowServerCombo)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(lblMaxCharsInResult)
                                        .addComponent(txtMaxCharsInResult)
                                        .addComponent(lblMaxCharsInTableCell)
                                        .addComponent(txtMaxCharsInTableCell)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(lblCellRightPadding)
                                        .addComponent(txtCellRightPadding)
                                        .addComponent(lblCellMaxWidth)
                                        .addComponent(txtCellMaxWidth)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(lblExecAll)
                                        .addComponent(comboBoxExecAll)
                                        .addComponent(glue3)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(chBoxAutoSave)
                                        .addComponent(chBoxSaveOnExit)
                                        .addComponent(glue4)
                        ).addGroup(
                            layout.createSequentialGroup()
                                        .addComponent(lblAuthMechanism)
                                        .addComponent(comboBoxAuthMechanism, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)
                                        .addComponent(lblUser)
                                        .addComponent(txtUser, FIELD_SIZE, FIELD_SIZE, FIELD_SIZE)
                                        .addComponent(lblPassword)
                                        .addComponent(txtPassword, FIELD_SIZE, FIELD_SIZE, FIELD_SIZE)
                        ).addComponent(glue)
                        .addGroup(
                            layout.createSequentialGroup()
                                    .addComponent(glue1)
                                    .addComponent(btnOk)
                                    .addComponent(btnCancel)
                        )
        );


        layout.setVerticalGroup(
                layout.createSequentialGroup()
                    .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblLookAndFeel)
                                .addComponent(comboBoxLookAndFeel)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(editorFont.label)
                                .addComponent(editorFont.button)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(tableFont.label)
                                .addComponent(tableFont.button)
                                .addComponent(glue2)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblResultTabsCount)
                                .addComponent(txtTabsCount)
                                .addComponent(chBoxShowServerCombo)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMaxCharsInResult)
                                .addComponent(txtMaxCharsInResult)
                                .addComponent(lblMaxCharsInTableCell)
                                .addComponent(txtMaxCharsInTableCell)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCellRightPadding)
                                .addComponent(txtCellRightPadding)
                                .addComponent(lblCellMaxWidth)
                                .addComponent(txtCellMaxWidth)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblExecAll)
                                .addComponent(comboBoxExecAll)
                                .addComponent(glue3)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(chBoxAutoSave)
                                .addComponent(chBoxSaveOnExit)
                                .addComponent(glue4)
                    ).addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblAuthMechanism)
                                .addComponent(comboBoxAuthMechanism)
                                .addComponent(lblUser)
                                .addComponent(txtUser)
                                .addComponent(lblPassword)
                                .addComponent(txtPassword)
                    ).addComponent(glue)
                    .addGroup(
                        layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(glue1)
                                .addComponent(btnOk)
                                .addComponent(btnCancel)
                    )
        );
        layout.linkSize(SwingConstants.HORIZONTAL, txtUser, txtPassword, txtTabsCount, txtMaxCharsInResult, txtMaxCharsInTableCell);
        layout.linkSize(SwingConstants.HORIZONTAL, btnOk, btnCancel);
        setContentPane(root);
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
