package studio.ui.settings;

import studio.kdb.config.ColorSchema;
import studio.kdb.config.ColorSets;
import studio.ui.ColorLabel;
import studio.ui.GroupLayoutSimple;
import studio.ui.Util;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class ColorSetsEditor extends JPanel {

    private ColorSets colorSets;

    private final ColorListComponent colorList;
    private final JComboBox<String> comboBoxColorSetName;
    private final JButton btnAddColorSet;
    private final JButton btnDeleteColorSet;
    private final ColorLabel colorBackground;
    private final ColorLabel colorGrid;


    public ColorSetsEditor(ColorSets colorSets) {
        this.colorSets = colorSets;

        JLabel lblColorSchema = new JLabel("Color schema: ");
        comboBoxColorSetName = new JComboBox<>();
        comboBoxColorSetName.addActionListener(this::colorSetNameSelected);
        btnAddColorSet = new JButton("new");
        btnAddColorSet.addActionListener(this::chartAddColorSetAction);
        btnDeleteColorSet = new JButton("delete");
        btnDeleteColorSet.addActionListener(this::chartDeleteColorSetAction);
        colorList = new ColorListComponent();
        colorList.setPreferredSize(new Dimension(0, 0));
        colorList.addActionListener(this::colorsChanged);
        colorList.setToolTipText("<html>Use drag&drop, double click, <code>INS</code>, <code>DEL</code> to edit</html>");

        JLabel lblBackground = new JLabel("Background:");
        colorBackground = new ColorLabel();
        colorBackground.addChangeListener(this::backgroundChanged);
        JLabel lblGrid = new JLabel("   Grid:");
        colorGrid = new ColorLabel();
        colorGrid.addChangeListener(this::gridChanged);
        JPanel pnlBgGrid = new JPanel(new FlowLayout());
        pnlBgGrid.add(lblBackground);
        pnlBgGrid.add(colorBackground);
        pnlBgGrid.add(lblGrid);
        pnlBgGrid.add(colorGrid);

        FocusDecorator.add(colorList);
        Util.sizeToViewPort(colorList);

        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblColorSchema, comboBoxColorSetName, btnAddColorSet, btnDeleteColorSet)
                        .addLine(colorList)
                        .addLineAndGlue(pnlBgGrid)
        );

        refreshColorSet();
    }

    public ColorSets getColorSets() {
        return colorSets;
    }

    private void refreshColorSet() {
        String[] names = colorSets.getNames().toArray(new String[0]);
        comboBoxColorSetName.setModel(new DefaultComboBoxModel<>(names));
        comboBoxColorSetName.setSelectedItem(colorSets.getDefaultName());

        ColorSchema colorSchema = colorSets.getColorSchema();
        colorList.setColors(colorSchema.getColors());
        colorBackground.setColor(colorSchema.getBackground());
        colorGrid.setColor(colorSchema.getGrid());
        btnDeleteColorSet.setEnabled(colorSets.getNames().size() > 1);
    }


    private void colorSetNameSelected(ActionEvent e) {
        colorSets = colorSets.newSelected((String)comboBoxColorSetName.getSelectedItem());
        refreshColorSet();
    }

    private void chartAddColorSetAction(ActionEvent e) {
        String name = JOptionPane.showInputDialog(this, "Enter name:", "Add Color Schema", JOptionPane.QUESTION_MESSAGE);
        if (name == null) return;

        if (! colorSets.getNames().contains(name)) {
            List<Color> colors = new ArrayList<>();
            colors.add(Color.BLACK);
            colorSets = colorSets.setColorSchema(name, new ColorSchema());
        }
        colorSets = colorSets.newSelected(name);
        refreshColorSet();
    }

    private void chartDeleteColorSetAction(ActionEvent e) {
        colorSets = colorSets.deleteColorSchema((String)comboBoxColorSetName.getSelectedItem());
        refreshColorSet();
    }

    private void colorsChanged(ActionEvent e) {
        ColorSchema colorSchema = colorSets.getColorSchema();
        colorSets = colorSets.setColorSchema(colorSets.getDefaultName(), colorSchema.newColors(colorList.getColors()) );
        refreshColorSet();
    }

    private void backgroundChanged(ChangeEvent e) {
        ColorSchema colorSchema = colorSets.getColorSchema();
        colorSets = colorSets.setColorSchema(colorSets.getDefaultName(), colorSchema.newBackgroundColor(colorBackground.getColor()) );
        refreshColorSet();
    }

    private void gridChanged(ChangeEvent e) {
        ColorSchema colorSchema = colorSets.getColorSchema();
        colorSets = colorSets.setColorSchema(colorSets.getDefaultName(), colorSchema.newGridColor(colorGrid.getColor()) );
        refreshColorSet();

    }

}
