package studio.ui.settings;

import org.fife.ui.rtextarea.RTextScrollPane;
import studio.kdb.Config;
import studio.kdb.config.ColorMap;
import studio.kdb.config.GridColorConfig;
import studio.ui.GroupLayoutSimple;
import studio.ui.Util;
import studio.ui.rstextarea.RSTextAreaFactory;
import studio.ui.rstextarea.StudioRSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsStyleTab extends SettingsTab {

    private final JComboBox<CustomiszedLookAndFeelInfo> comboBoxLookAndFeel;
    private final FontSelectionPanel editorFontSelection, resultFontSelection;
    private final ColorTokenEditor colorTokenEditor;
    private final StudioRSyntaxTextArea preview;
    private final RTextScrollPane scrollPane;
    private final GridColorsEditor gridColorsEditor;
//    private final QGridPanel grid;

    private static final Config CONFIG = Config.getInstance();

    private final static String SAMPLE =
            "/ Sample to check syntax highlight. The sample is editable.\n" +
            "\\pwd\n" +
            "func: {\n" +
            "     vals:  (-100; 23i; 17h; 2.9979e8; 3.14e; 0xFF; 1b; 101b);\n" +
            "     str:  (\"c\"; \"string and\"; `symbol);\n" +
            "     temp: (2025.10.18; 2025.10.18D20:08:35.189; 0D01:15:00; 205.10.18T20:08;\n" +
            "                    20:09; 20:10:45; 20:15:31.963; 2025.10m);\n" +
            "     : count each (vals; str; temp);\n" +
            " };\n" +
            "\"\\error\"\n";

    public SettingsStyleTab(JDialog parentDialog) {
        JLabel lblLookAndFeel = new JLabel("Look and Feel:");

        LookAndFeels lookAndFeels = new LookAndFeels();
        comboBoxLookAndFeel = new JComboBox<>(lookAndFeels.getLookAndFeels());
        CustomiszedLookAndFeelInfo lf = lookAndFeels.getLookAndFeel(CONFIG.getString(Config.LOOK_AND_FEEL));
        comboBoxLookAndFeel.setSelectedItem(lf);


        editorFontSelection = new FontSelectionPanel(parentDialog, "Editor font: ", CONFIG.getFont(Config.FONT_EDITOR));
        resultFontSelection = new FontSelectionPanel(parentDialog, "Result table font: ", CONFIG.getFont(Config.FONT_TABLE));
        colorTokenEditor = new ColorTokenEditor(CONFIG.getColor(Config.COLOR_BACKGROUND), CONFIG.getColorTokenConfig());
        preview = RSTextAreaFactory.newTextArea(true);
        preview.setSyntaxScheme(editorFontSelection.getSelectedFont(), colorTokenEditor.getColorTokenConfig());
        preview.setBackground(colorTokenEditor.getBgColor());
        preview.setText(SAMPLE);

        scrollPane = new RTextScrollPane(preview);
        preview.setGutter(scrollPane.getGutter());
        preview.setRows(10);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new ResizeGrip(scrollPane));

        editorFontSelection.addChangeListener(e -> {
            preview.setFont(editorFontSelection.getSelectedFont());
        });
        colorTokenEditor.addChangeListener(e -> {
            preview.setBackground(colorTokenEditor.getBgColor());
            preview.setSyntaxScheme(editorFontSelection.getSelectedFont(), colorTokenEditor.getColorTokenConfig());
        });

        gridColorsEditor = new GridColorsEditor(CONFIG.getGridColorConfig());


        JButton btnColorTokenReset = new JButton("Reset to default");
        btnColorTokenReset.addActionListener(e -> {
            Color bgColor = (Color) CONFIG.getDefault(Config.COLOR_BACKGROUND);
            ColorMap tokensColor = (ColorMap) CONFIG.getDefault(Config.COLOR_TOKEN_CONFIG);
            tokensColor = tokensColor.filterColorToken(); // May be this should be reworked?
            colorTokenEditor.set(bgColor, tokensColor);
        });

        JButton btnGridColorReset = new JButton("Reset to default");
        btnGridColorReset.addActionListener(e -> {
            GridColorConfig config = (GridColorConfig) CONFIG.getDefault(Config.GRID_COLOR_CONFIG);
            gridColorsEditor.set(config);
        });

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblLookAndFeel, comboBoxLookAndFeel)
                        .addLine(Util.getLineInViewPort())
                        .addLineAndGlue(editorFontSelection)
                        .addLineAndGlue(new JLabel("<html><b><i>Colors for tokens:</i></b></html>"), btnColorTokenReset)
                        .addLineAndGlue(colorTokenEditor)
                        .addLineAndGlue(new JLabel("<html><b><i>Preview</i></b></html>"))
                        .addLine(scrollPane)
                        .addLine(Util.getLineInViewPort())
                        .addLineAndGlue(resultFontSelection)
                        .addLineAndGlue(new JLabel("<html><b><i>Colors for result grid:</i></b></html>"), btnGridColorReset)
                        .addLineAndGlue(gridColorsEditor)

        );

        Util.sizeToViewPort(scrollPane);
    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        boolean changed = CONFIG.setFont(Config.FONT_EDITOR, editorFontSelection.getSelectedFont());
        result.setRefreshEditorsSettings(changed);
        result.setRefreshResultSettings(changed);

        changed = CONFIG.setFont(Config.FONT_TABLE, resultFontSelection.getSelectedFont());
        result.setRefreshResultSettings(changed);

        changed = CONFIG.setColorTokenConfig(colorTokenEditor.getColorTokenConfig());
        result.setRefreshEditorsSettings(changed);
        result.setRefreshResultSettings(changed);

        changed = CONFIG.setColor(Config.COLOR_BACKGROUND, colorTokenEditor.getBgColor());
        result.setRefreshEditorsSettings(changed);
        result.setRefreshResultSettings(changed);

        changed = CONFIG.setGridColorConfig(gridColorsEditor.getConfig());
        result.setRefreshResultSettings(changed);

        String lfClassName = ((CustomiszedLookAndFeelInfo)comboBoxLookAndFeel.getSelectedItem()).getClassName();
        changed = CONFIG.setString(Config.LOOK_AND_FEEL, lfClassName);
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
