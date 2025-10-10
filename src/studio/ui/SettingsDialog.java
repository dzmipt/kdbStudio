package studio.ui;

import studio.ui.settings.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDialog extends EscapeDialog {

    private final Map<String, SettingsTab> pages = new LinkedHashMap<>();

    private final JButton btnOk;
    private final JButton btnCancel;

    public SettingsDialog(JFrame owner) {
        super(owner, "Settings");

        pages.put("General", new SettingsGeneralTab());
        pages.put("Editor", new SettingsEditorTab());
        pages.put("Result", new SettingsResultTab());
        pages.put("Style", new SettingsStyleTab(this));
        pages.put("Chart", new SettingsChartTab());

        JTabbedPane tabs = new JTabbedPane();
        for (String title: pages.keySet()) {
            tabs.addTab(title, new JScrollPane(pages.get(title)));
        }

        btnOk = new JButton("OK");
        btnCancel = new JButton("Cancel");

        btnOk.addActionListener(e->accept());
        btnCancel.addActionListener(e->cancel());

        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.add(btnOk);
        pnlButtons.add(btnCancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(tabs, BorderLayout.CENTER);
        root.add(pnlButtons, BorderLayout.SOUTH);
        setContentPane(root);
    }


    @Override
    public void align() {
        super.align();
        btnOk.requestFocusInWindow();
    }

    public void saveSettings() {
        SettingsSaveResult result = new SettingsSaveResult();

        for(SettingsTab tab: pages.values() ) {
            tab.saveSettings(result);
        }

        if (result.isRefreshComboServerVisibility()) {
            StudioWindow.refreshComboServerVisibility();
        }
        if (result.isRefreshEditorsSettings()) {
            StudioWindow.refreshEditorsSettings();
        }
        if (result.isRefreshResultSettings()) {
            StudioWindow.refreshResultSettings();
        }
        if (result.isChangedLF()) {
            StudioOptionPane.showMessage(this, "Look and Feel was changed. " +
                    "New L&F will take effect on the next start up.", "Look and Feel Setting Changed");
        }

    }

    public static void main(String... args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
        SettingsDialog dialog = new SettingsDialog(null);
        dialog.alignAndShow();
    }
}
