package studio.ui.chart;

import studio.ui.chart.event.LegendChangeEvent;
import studio.ui.chart.event.LegendChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.List;

public class LegendListPanel extends Box implements LegendChangeListener {

    private final boolean line, shape, bar;
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private final List<LegendButton> buttons = new ArrayList<>();
    private final JPanel panel;
    private final EventListenerList listenerList = new EventListenerList();


    public LegendListPanel(String labelText, boolean line, boolean shape, boolean bar) {
        super(BoxLayout.Y_AXIS);
        this.line = line;
        this.shape = shape;
        this.bar = bar;

        JCheckBox chkAll = new JCheckBox("All", true);
        chkAll.addActionListener(e -> {
            for (JCheckBox checkBox: checkBoxes) {
                checkBox.setSelected(chkAll.isSelected());
            }
            notifyListeners();
        });

        panel = new JPanel();

        Box boxTop = Box.createHorizontalBox();
        boxTop.add(new JLabel(labelText));
        boxTop.add(Box.createHorizontalGlue());
        boxTop.add(chkAll);

        Box boxCentral = Box.createHorizontalBox();
        boxCentral.add(panel);
        boxCentral.add(Box.createHorizontalGlue());

        add(boxTop);
        add(boxCentral);

        updateLayout();
    }

    public void add(String title, LegendIcon icon) {
        add(title, icon, null);
    }

    public void add(String title, LegendIcon icon, Action ... additionalActions) {
        JCheckBox checkBox = new JCheckBox(title, true);
        checkBox.addActionListener(e -> notifyListeners() );
        LegendButton button = new LegendButton(icon, line, shape, bar);
        button.addChangeListener(this);
        if (additionalActions != null) {
            button.setAdditionalActions(additionalActions);
        }

        checkBoxes.add(checkBox);
        buttons.add(button);
        updateLayout();
    }

    public int getListSize() {
        return buttons.size();
    }

    public LegendIcon getIcon(int index) {
        return buttons.get(index).getLegendIcon();
    }

    public boolean isSelected(int index) {
        return checkBoxes.get(index).isSelected();
    }

    public void setEnabled(int index, boolean enabled) {
        checkBoxes.get(index).setEnabled(enabled);
        buttons.get(index).setEnabled(enabled);
        buttons.get(index).invalidate();
    }

    private void updateLayout() {
        GroupLayout layout = new GroupLayout(panel);

        GroupLayout.ParallelGroup chkGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.ParallelGroup iconGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        GroupLayout.SequentialGroup rowsGroup = layout.createSequentialGroup();

        for (int i=0; i < buttons.size(); i++) {
            JCheckBox chk = checkBoxes.get(i);
            LegendButton button = buttons.get(i);
            chkGroup.addComponent(chk);
            iconGroup.addComponent(button);

            rowsGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(chk)
                    .addComponent(button) );
        }

        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(chkGroup)
                .addGroup(iconGroup) );

        layout.setVerticalGroup(rowsGroup);

        panel.setLayout(layout);
    }

    @Override
    public void legendChanged(LegendChangeEvent event) {
        notifyListeners();
    }

    @Override
    public void changeAllStrokes(LegendChangeEvent event) {
        for (int i=0; i < buttons.size(); i++) {
            buttons.get(i).getLegendIcon().setStroke(event.getIcon().getStroke());
        }
        notifyListeners();
    }

    @Override
    public void changeAllShapes(LegendChangeEvent event) {
        for (int i=0; i < buttons.size(); i++) {
            buttons.get(i).getLegendIcon().setShape(event.getIcon().getShape());
        }
        notifyListeners();
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    private void notifyListeners() {
        this.repaint();
        ChangeEvent event = new ChangeEvent(this);
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ChangeListener.class) {
                ((ChangeListener) listeners[i + 1]).stateChanged(event);
            }
        }
    }
}
