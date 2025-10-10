package studio.ui.settings;

import studio.kdb.Config;
import studio.ui.GroupLayoutSimple;
import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

public class SettingsChartTab extends SettingsTab {

    private final ColorSetsEditor colorSetsEditor;
    private final StrokeStyleEditor strokeStyleEditor;
    private final StrokeWidthEditor strokeWidthEditor;

    private static final Config CONFIG = Config.getInstance();

    public SettingsChartTab() {
        colorSetsEditor = new ColorSetsEditor(CONFIG.getChartColorSets());

        strokeStyleEditor = new StrokeStyleEditor(135, 140);
        strokeWidthEditor = new StrokeWidthEditor(70, 140);

        colorSetsEditor.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.GRAY));
        strokeStyleEditor.setBorder(BorderFactory.createMatteBorder(0,0,1,0,Color.GRAY));

        Box boxChart = Box.createVerticalBox();
        boxChart.add(colorSetsEditor);
        boxChart.add(strokeStyleEditor);
        boxChart.add(strokeWidthEditor);

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(new GroupLayoutSimple.Stack()
                .addLine(boxChart)
        );

        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                JScrollPane scrollChart = Util.findParent(SettingsChartTab.this, JScrollPane.class);
                if (scrollChart == null) return;
                colorSetsEditor.setExternalViewport(scrollChart.getViewport());
            }
        });
    }

    @Override
    public void saveSettings(SettingsSaveResult result) {
        CONFIG.setChartColorSets(colorSetsEditor.getColorSets());
        strokeStyleEditor.saveSettings();
        strokeWidthEditor.saveSettings();

    }
}
