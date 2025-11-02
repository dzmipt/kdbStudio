package studio.ui;

import com.formdev.flatlaf.FlatLaf;
import studio.core.Studio;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.kdb.config.ServerConfig;
import studio.ui.rstextarea.StudioRSyntaxTextArea;
import studio.ui.settings.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsDialog extends EscapeDialog {

    private final Map<String, SettingsTab> pages = new LinkedHashMap<>();

    private final JButton btnOk;
    private final JButton btnCancel;

    private static final Config CONFIG = Config.getInstance();

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

        tabs.setPreferredSize(new Dimension(705, 705));

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
        Color oldBgColor = CONFIG.getColor(Config.COLOR_BACKGROUND);

        SettingsSaveResult result = new SettingsSaveResult();
        for(SettingsTab tab: pages.values() ) {
            tab.saveSettings(result);
        }

        Color newBgColor = CONFIG.getColor(Config.COLOR_BACKGROUND);
        if (! newBgColor.equals(oldBgColor) ) {
            int res = StudioOptionPane.showYesNoDialog(this, "Background color is changed.\n" +
                    "Do you want to update background for all servers with old background color?", "Background color changed");
            if (res == JOptionPane.YES_OPTION) {
                updateBgColor(oldBgColor, newBgColor);
            }
        }

        if (result.isRefreshComboServerVisibility()) {
            refreshComboServerVisibility();
        }
        if (result.isRefreshEditorsSettings()) {
            refreshEditorsSettings();
        }
        if (result.isRefreshResultSettings()) {
            refreshResultSettings();
        }
        if (result.isChangedLF()) {
            Studio.initLF();
            FlatLaf.updateUI();
        }
    }

    public static void refreshComboServerVisibility() {
        for (StudioWindow window: StudioWindow.getAllStudioWindows()) {
            window.getComboServer().setVisible(CONFIG.getBoolean(Config.SHOW_SERVER_COMBOBOX));
        }
    }

    public static void refreshEditorsSettings() {
        StudioWindow.executeForAllEditors(editorTab -> {
            StudioRSyntaxTextArea editor = editorTab.getTextArea();
            editor.setHighlightCurrentLine(CONFIG.getBoolean(Config.RSTA_HIGHLIGHT_CURRENT_LINE));
            editor.setAnimateBracketMatching(CONFIG.getBoolean(Config.RSTA_ANIMATE_BRACKET_MATCHING));
            editor.setLineWrap(CONFIG.getBoolean(Config.RSTA_WORD_WRAP));
            editor.setSyntaxScheme(CONFIG.getFont(Config.FONT_EDITOR), CONFIG.getColorTokenConfig());

            editor.setTabSize(CONFIG.getInt(Config.EDITOR_TAB_SIZE));
            editor.setTabsEmulated(CONFIG.getBoolean(Config.EDITOR_TAB_EMULATED));

            editor.setInsertPairedCharacters(CONFIG.getBoolean(Config.RSTA_INSERT_PAIRED_CHAR));
            return true;
        });
    }

    public static void refreshResultSettings() {
        long doubleClickTimeout = CONFIG.getInt(Config.EMULATED_DOUBLE_CLICK_TIMEOUT);
        StudioWindow.executeForAllResultTabs(resultTab -> {
            resultTab.setDoubleClickTimeout(doubleClickTimeout);
            resultTab.refreshFont();

            resultTab.forEachResultPane(resultPane -> {
                EditorPane editorPane = resultPane.getEditor();
                if (editorPane != null) {
                    StudioRSyntaxTextArea editor = editorPane.getTextArea();
                    editor.setBackground(CONFIG.getColor(Config.COLOR_BACKGROUND));
                    editor.setSyntaxScheme(CONFIG.getFont(Config.FONT_EDITOR), CONFIG.getColorTokenConfig());
                }
                ResultGrid grid = resultPane.getGrid();
                if (grid != null) {
                    grid.getTable().setGridColorConfig(CONFIG.getGridColorConfig());
                }
            });
            return true;
        });
    }


    public static void updateBgColor(Color oldColor, Color newColor) {
        ServerConfig serverConfig = CONFIG.getServerConfig();
        serverConfig.updateBgColor(oldColor, newColor);
        StudioWindow.executeForAllEditors(editor -> {
           Server server = editor.getServer();
           if (server.inServerTree()) {
               editor.setServer(serverConfig.getServer(server.getFullName()));
           } else if (server.getBackgroundColor().equals(oldColor)){
               Server newServer = new Server(server.getName(), server.getConnection(), server.getAuthenticationMechanism(), newColor);
               editor.setServer(newServer);
           }
           return true;
        });
    }

    public static void main(String... args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
        SettingsDialog dialog = new SettingsDialog(null);
        dialog.alignAndShow();
    }
}
