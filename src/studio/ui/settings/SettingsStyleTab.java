package studio.ui.settings;

import org.fife.ui.rtextarea.RTextScrollPane;
import studio.kdb.Config;
import studio.kdb.DictTableModel;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.config.ColorMap;
import studio.kdb.config.GridColorConfig;
import studio.ui.GroupLayoutSimple;
import studio.ui.Util;
import studio.ui.grid.QGridPanel;
import studio.ui.rstextarea.RSTextAreaFactory;
import studio.ui.rstextarea.StudioRSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsStyleTab extends SettingsTab {

    private final JComboBox<CustomiszedLookAndFeelInfo> comboBoxLookAndFeel;
    private final FontSelectionPanel editorFontSelection, gridFontSelection;
    private final ColorTokenEditor colorTokenEditor;
    private final StudioRSyntaxTextArea preview;
    private final GridColorsEditor gridColorsEditor;
    private final QGridPanel grid;

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
        gridFontSelection = new FontSelectionPanel(parentDialog, "Result table font: ", CONFIG.getFont(Config.FONT_TABLE));
        colorTokenEditor = new ColorTokenEditor(CONFIG.getColor(Config.COLOR_BACKGROUND), CONFIG.getColorTokenConfig());
        preview = RSTextAreaFactory.newTextArea(true);
        preview.setSyntaxScheme(editorFontSelection.getSelectedFont(), colorTokenEditor.getColorTokenConfig());
        preview.setBackground(colorTokenEditor.getBgColor());
        preview.setText(SAMPLE);

        RTextScrollPane scrollPreview = new RTextScrollPane(preview);
        preview.setGutter(scrollPreview.getGutter());
        preview.setRows(10);

        scrollPreview.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPreview.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPreview.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new ResizeGrip(scrollPreview));

        editorFontSelection.addChangeListener(e -> {
            preview.setFont(editorFontSelection.getSelectedFont());
        });
        colorTokenEditor.addChangeListener(e -> {
            preview.setBackground(colorTokenEditor.getBgColor());
            preview.setSyntaxScheme(editorFontSelection.getSelectedFont(), colorTokenEditor.getColorTokenConfig());
        });

        gridColorsEditor = new GridColorsEditor(CONFIG.getGridColorConfig());

        grid = getGrid();
        grid.setPreferredSize(new Dimension(600,300));
        Util.sizeToViewPort(grid);

        gridFontSelection.addChangeListener(e -> {
            grid.setFont(gridFontSelection.getSelectedFont());
        });
        gridColorsEditor.addChangeListener(e -> {
            grid.getTable().setGridColorConfig(gridColorsEditor.getConfig());
            grid.repaint();
        });


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
                        .addLine(scrollPreview)
                        .addLine(Util.getLineInViewPort())
                        .addLineAndGlue(gridFontSelection)
                        .addLineAndGlue(new JLabel("<html><b><i>Colors for result grid:</i></b></html>"), btnGridColorReset)
                        .addLineAndGlue(gridColorsEditor)
                        .addLine(grid)

        );

        Util.sizeToViewPort(scrollPreview);
    }


    private KTableModel getGridPreviewModel() {
        long nill = K.KLong.NULL.toLong();;

        long[] array = new long[] {nill, nill, nill, 0, 1, 2, 3, 4, 5};
        String[] syms = new String[] {"", "MSFT", "NVDA", "APPL", "GOOG", "VOW3", "SIE", "005930.KS", "9988.HK"};

        K.Flip key = new K.Flip(new K.KSymbolVector("key"),
                                new K.KList(new K.KLongVector(array) ) );

        K.Flip value = new K.Flip(new K.KSymbolVector("colA", "colB"),
                new K.KList(new K.KLongVector(array), new K.KSymbolVector(syms)) );

        return new DictTableModel( new K.Dict(key, value) );
    }

    private QGridPanel getGrid() {
        QGridPanel grid = new QGridPanel(getGridPreviewModel());
        grid.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        grid.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        grid.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new ResizeGrip(grid));
        grid.getTable().setGridColorConfig(CONFIG.getGridColorConfig());
        grid.setFont(CONFIG.getFont(Config.FONT_TABLE));

        for (int row=2; row<=3; row++){
            for (int col=0; col<=1; col++) {
                grid.getTable().getMarkers().mark(row, col);
            }
        }

        grid.getTable().setRowSelectionInterval(3,4);
        grid.getTable().setColumnSelectionInterval(0,2);

        return grid;
    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        boolean changed = CONFIG.setFont(Config.FONT_EDITOR, editorFontSelection.getSelectedFont());
        result.setRefreshEditorsSettings(changed);
        result.setRefreshResultSettings(changed);

        changed = CONFIG.setFont(Config.FONT_TABLE, gridFontSelection.getSelectedFont());
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
