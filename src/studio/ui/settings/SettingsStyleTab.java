package studio.ui.settings;

import org.fife.ui.rtextarea.RTextScrollPane;
import studio.kdb.Config;
import studio.kdb.DictTableModel;
import studio.kdb.K;
import studio.kdb.KTableModel;
import studio.kdb.config.StyleSchema;
import studio.kdb.config.StyleSchemas;
import studio.ui.GroupLayoutSimple;
import studio.ui.StudioOptionPane;
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
    private final TokenStyleEditor colorTokenEditor;
    private final StudioRSyntaxTextArea preview;
    private final GridColorsEditor gridColorsEditor;
    private final QGridPanel grid;

    private final JComboBox<String> comboBoxSchema;
    private final JButton btnDuplicate, btnRename, btnDelete;
    private final JLabel lblReadOnly;

    private StyleSchemas schemas;
    private boolean muteEvents = false;

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
        if (lf != null) comboBoxLookAndFeel.setSelectedItem(lf);


        schemas = CONFIG.getStyleSchemas();
        StyleSchema schema = schemas.getSchema(getSelectedLookAndFeel());

        JLabel lblSchema = new JLabel("Style schema:");
        comboBoxSchema = new JComboBox<>();
        comboBoxSchema.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text = value == null ? "" : value.toString();
                if (value != null && schemas.isPredefined(text)) text = text + " (predefined)";
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        btnDuplicate = new JButton("Duplicate...");
        btnDuplicate.setToolTipText("Create a new style schema as a copy of the selected one");
        btnRename = new JButton("Rename...");
        btnRename.setToolTipText("Rename the selected style schema");
        btnDelete = new JButton("Delete");
        btnDelete.setToolTipText("Delete the selected style schema");
        lblReadOnly = new JLabel("<html><i>Predefined schemas are read-only. Use Duplicate to create a schema you can change.</i></html>");

        editorFontSelection = new FontSelectionPanel(parentDialog, "Editor font: ", CONFIG.getFont(Config.FONT_EDITOR));
        gridFontSelection = new FontSelectionPanel(parentDialog, "Result table font: ", CONFIG.getFont(Config.FONT_TABLE));
        colorTokenEditor = new TokenStyleEditor(schema.getEditorColors(), schema.getTokens());
        preview = RSTextAreaFactory.newTextArea(true);
        preview.setHighlightCurrentLine(true);
        preview.setSyntaxScheme(editorFontSelection.getSelectedFont(), colorTokenEditor.getTokenStyleMap());
        preview.setEditorColors(colorTokenEditor.getEditorColorConfig());
        preview.setText(SAMPLE);

        RTextScrollPane scrollPreview = new RTextScrollPane(preview);
        preview.setGutter(scrollPreview.getGutter());
        preview.setRows(12);

        scrollPreview.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPreview.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPreview.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new ResizeGrip(scrollPreview));

        editorFontSelection.addChangeListener(e -> {
            preview.setFont(editorFontSelection.getSelectedFont());
        });
        colorTokenEditor.addChangeListener(e -> {
            preview.setEditorColors(colorTokenEditor.getEditorColorConfig());
            preview.setSyntaxScheme(editorFontSelection.getSelectedFont(), colorTokenEditor.getTokenStyleMap());
            if (muteEvents) return;
            updateSelectedSchema(getSelectedSchema()
                    .newEditorColors(colorTokenEditor.getEditorColorConfig())
                    .newTokens(colorTokenEditor.getTokenStyleMap()));
        });

        gridColorsEditor = new GridColorsEditor(schema.getGridColors());

        grid = getGrid(schema);
        grid.setPreferredSize(new Dimension(600,300));
        Util.sizeToViewPort(grid);

        gridFontSelection.addChangeListener(e -> {
            grid.setFont(gridFontSelection.getSelectedFont());
        });
        gridColorsEditor.addChangeListener(e -> {
            grid.getTable().setGridColorConfig(gridColorsEditor.getConfig());
            grid.repaint();
            if (muteEvents) return;
            updateSelectedSchema(getSelectedSchema().newGridColors(gridColorsEditor.getConfig()));
        });

        comboBoxLookAndFeel.addActionListener(e -> lookAndFeelSelected());
        comboBoxSchema.addActionListener(e -> schemaSelected());
        btnDuplicate.addActionListener(e -> duplicateSchema());
        btnRename.addActionListener(e -> renameSchema());
        btnDelete.addActionListener(e -> deleteSchema());

        refreshSchemas(schemas.getSchemaName(getSelectedLookAndFeel()));

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblLookAndFeel, comboBoxLookAndFeel)
                        .addLineAndGlue(lblSchema, comboBoxSchema, btnDuplicate, btnRename, btnDelete)
                        .addLineAndGlue(lblReadOnly)
                        .addLine(Util.getLineInViewPort())
                        .addLineAndGlue(editorFontSelection)
                        .addLineAndGlue(new JLabel("<html><b><i>Colors for tokens:</i></b></html>"))
                        .addLineAndGlue(colorTokenEditor)
                        .addLineAndGlue(new JLabel("<html><b><i>Preview</i></b></html>"))
                        .addLine(scrollPreview)
                        .addLine(Util.getLineInViewPort())
                        .addLineAndGlue(gridFontSelection)
                        .addLineAndGlue(new JLabel("<html><b><i>Colors for result grid:</i></b></html>"))
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

    private String getSelectedLookAndFeel() {
        return ((CustomiszedLookAndFeelInfo)comboBoxLookAndFeel.getSelectedItem()).getClassName();
    }

    private String getSelectedSchemaName() {
        return (String) comboBoxSchema.getSelectedItem();
    }

    private StyleSchema getSelectedSchema() {
        return schemas.get(getSelectedSchemaName());
    }

    private void updateSelectedSchema(StyleSchema schema) {
        String name = getSelectedSchemaName();
        if (schemas.isPredefined(name)) return;
        schemas = schemas.set(name, schema);
    }

    private void refreshSchemas(String selectedName) {
        boolean oldMuteEvents = muteEvents;
        muteEvents = true;
        try {
            comboBoxSchema.setModel(new DefaultComboBoxModel<>(schemas.getNames().toArray(new String[0])));
            comboBoxSchema.setSelectedItem(selectedName);
        } finally {
            muteEvents = oldMuteEvents;
        }
        loadSelectedSchema();
    }

    private void loadSelectedSchema() {
        String name = getSelectedSchemaName();
        StyleSchema schema = schemas.get(name);
        boolean editable = !schemas.isPredefined(name);

        boolean oldMuteEvents = muteEvents;
        muteEvents = true;
        try {
            colorTokenEditor.set(schema.getEditorColors(), schema.getTokens());
            gridColorsEditor.set(schema.getGridColors());
        } finally {
            muteEvents = oldMuteEvents;
        }

        colorTokenEditor.setEditable(editable);
        gridColorsEditor.setEditable(editable);
        btnRename.setEnabled(editable);
        btnDelete.setEnabled(editable);
        lblReadOnly.setVisible(!editable);
    }

    private void lookAndFeelSelected() {
        if (muteEvents) return;
        String name = schemas.getSchemaName(getSelectedLookAndFeel());
        boolean oldMuteEvents = muteEvents;
        muteEvents = true;
        try {
            comboBoxSchema.setSelectedItem(name);
        } finally {
            muteEvents = oldMuteEvents;
        }
        loadSelectedSchema();
    }

    private void schemaSelected() {
        if (muteEvents) return;
        String name = getSelectedSchemaName();
        if (name == null) return;
        schemas = schemas.select(getSelectedLookAndFeel(), name);
        loadSelectedSchema();
    }

    private String askName(String title, String initialName, String currentName) {
        String name = initialName;
        while (true) {
            Object result = JOptionPane.showInputDialog(this, "Style schema name:", title,
                    JOptionPane.QUESTION_MESSAGE, null, null, name);
            if (result == null) return null;
            name = result.toString().trim();
            if (name.equals(currentName)) return null;

            String error = schemas.validateNewName(name);
            if (error == null) return name;

            StudioOptionPane.showError(this, error, title);
        }
    }

    private void duplicateSchema() {
        String name = getSelectedSchemaName();
        String newName = name + " copy";
        for (int index = 2; schemas.contains(newName); index++) {
            newName = name + " copy " + index;
        }

        newName = askName("Duplicate Style Schema", newName, null);
        if (newName == null) return;

        schemas = schemas.duplicate(name, newName)
                        .select(getSelectedLookAndFeel(), newName);
        refreshSchemas(newName);
    }

    private void renameSchema() {
        String name = getSelectedSchemaName();
        if (schemas.isPredefined(name)) return;

        String newName = askName("Rename Style Schema", name, name);
        if (newName == null) return;

        schemas = schemas.rename(name, newName);
        refreshSchemas(newName);
    }

    private void deleteSchema() {
        String name = getSelectedSchemaName();
        if (schemas.isPredefined(name)) return;

        int result = StudioOptionPane.showYesNoDialog(this,
                String.format("Do you want to delete the style schema \"%s\"?", name), "Delete Style Schema");
        if (result != JOptionPane.YES_OPTION) return;

        schemas = schemas.delete(name);
        refreshSchemas(schemas.getSchemaName(getSelectedLookAndFeel()));
    }

    private QGridPanel getGrid(StyleSchema schema) {
        QGridPanel grid = new QGridPanel(getGridPreviewModel());
        grid.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        grid.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        grid.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new ResizeGrip(grid));
        grid.getTable().setGridColorConfig(schema.getGridColors());
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

        StyleSchema oldSchema = CONFIG.getStyleSchema();

        String lfClassName = getSelectedLookAndFeel();
        changed = CONFIG.setString(Config.LOOK_AND_FEEL, lfClassName);
        result.setChangedLF(changed);

        CONFIG.setStyleSchemas(schemas);

        changed = ! oldSchema.equals(CONFIG.getStyleSchema());
        result.setRefreshEditorsSettings(changed);
        result.setRefreshResultSettings(changed);
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
