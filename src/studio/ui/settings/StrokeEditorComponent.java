package studio.ui.settings;

import studio.ui.DocumentChangeListener;
import studio.ui.GroupLayoutSimple;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

abstract class StrokeEditorComponent extends JPanel implements DocumentChangeListener {

    private static final int HEIGHT = 20;

    private final int iconWidth;

    private final DndList list;
    private final JTextField txtField;
    protected final JLabel lblInfo;

    protected final static NumberFormat f2 = new DecimalFormat("#.##");

    StrokeEditorComponent(int iconWidth, int prefWidth) {
        this.iconWidth = iconWidth;

        list = new DndList(getListItem(new BasicStroke(1)));
        for (BasicStroke stroke: getInitStrokes() ) {
            list.add(getListItem(stroke));
        }
        list.setPrefWidth(prefWidth);
        FocusDecorator.add(list);

        txtField = new JTextField(12);

        list.addListSelectionListener(this::listSelectionChanges);
        txtField.getDocument().addDocumentListener(this);

        lblInfo = new JLabel();
        GroupLayoutSimple layout = new GroupLayoutSimple(this);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLineAndGlue(lblInfo)
                        .addLineAndGlue(list, txtField)
        );

    }

    protected static float round(float value) {
        return Math.round(100*value) / 100.0f;
    }

    protected StrokeIcon getIcon(BasicStroke stroke) {
        return new StrokeIcon(stroke, Color.BLACK, iconWidth, HEIGHT);
    }

    abstract protected List<BasicStroke> getInitStrokes();
    abstract protected DndList.ListItem getListItem(BasicStroke stroke);
    abstract protected String getText(BasicStroke stroke);
    abstract protected BasicStroke getStroke(String text);
    abstract public void saveSettings();

    private void listSelectionChanges(ListSelectionEvent e) {
        StrokeIcon icon = (StrokeIcon) list.getSelectedIcon();
        String text = "";
        if (icon != null) {
            BasicStroke stroke = icon.getStroke();
            text = getText(stroke);
        }
        txtField.setText(text);
        txtField.setForeground(Color.BLACK);
    }

    @Override
    public void documentChanged(DocumentEvent e) {
        try {
            BasicStroke stroke = getStroke(txtField.getText());
            list.setSelected(getListItem(stroke));
            txtField.setForeground(Color.BLACK);
        } catch (IllegalArgumentException exception) {
            txtField.setForeground(Color.RED);
        }
    }

    protected List<BasicStroke> getStrokes() {
        int count = list.getModel().getSize();
        List<BasicStroke> strokes = new ArrayList<>(count);
        for (int i=0; i<count; i++) {
            StrokeIcon icon = (StrokeIcon) list.getModel().getElementAt(i).getIcon();
            strokes.add(icon.getStroke());
        }
        return strokes;
    }

}
