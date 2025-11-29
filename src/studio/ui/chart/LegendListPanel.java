package studio.ui.chart;

import studio.ui.GroupLayoutSimple;
import studio.ui.UserAction;
import studio.ui.Util;
import studio.ui.chart.event.LegendChangeEvent;
import studio.ui.chart.event.LegendChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LegendListPanel extends JPanel implements LegendChangeListener {

    private final ChartConfigPanel chartConfigPanel;
    private final boolean isLines;
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private final List<LegendButton> buttons = new ArrayList<>();
    private final List<UserAction> extraAxisActions = new ArrayList<>();
    private final JLabel lblTitle = new JLabel();
    private final JComboBox<String> comboX = new JComboBox<>();
    private final JPanel panel = new JPanel();
    private boolean blockLayoutUpdate = true;

    private PlotConfig plotConfig;

    private final EventListenerList listenerList = new EventListenerList();

    public LegendListPanel(ChartConfigPanel chartConfigPanel, PlotConfig plotConfig, boolean visibleTitle) {
        this.chartConfigPanel = chartConfigPanel;
        this.plotConfig = plotConfig;
        isLines = false;
        lblTitle.setVisible(visibleTitle);
        lblTitle.setText(plotConfig.getTitle());
        Font font = lblTitle.getFont();
        if (font != null) lblTitle.setFont(font.deriveFont(Font.ITALIC | Font.BOLD));
        comboX.setModel(new DefaultComboBoxModel<>(plotConfig.getNames()));
        comboX.setSelectedIndex(plotConfig.getDomainIndex());
        comboX.addActionListener(e -> validateState() );

        for (int index = 0; index < plotConfig.size(); index++) {
            final int theIndex = index;
            UserAction extraAxis = UserAction.create("", ()-> extraAxisActionClick(theIndex));
            extraAxisActions.add(extraAxis);

            add(plotConfig.getColumn(index).getName(), plotConfig.getIcon(index), extraAxis);
            JCheckBox checkBox = checkBoxes.get(index);
            checkBox.setSelected(plotConfig.getEnabled(index));
            checkBox.setEnabled(index != plotConfig.getDomainIndex());
        }

        for (int index=0; index<plotConfig.size(); index++) {
            refreshExtraAxisAction(index);
        }
        initComponents();
    }

    public LegendListPanel(ChartConfigPanel chartConfigPanel) {
        this.chartConfigPanel = chartConfigPanel;
        isLines = true;
        initComponents();
    }

    private void initComponents() {
        JLabel domainLabel = new JLabel("Domain axis: ");
        JLabel captionLabel = new JLabel(isLines ? "Lines:" : "Series:");
        Component glue = Box.createGlue();
        JCheckBox chkAll = new JCheckBox("All", true);
        chkAll.addActionListener(e -> {
            for (JCheckBox checkBox: checkBoxes) {
                checkBox.setSelected(chkAll.isSelected());
            }
            notifyListeners();
        });

        GroupLayoutSimple layout = new GroupLayoutSimple(this, comboX, glue);
        layout.setAutoCreateContainerGaps(false);
        layout.setStacks(
                new GroupLayoutSimple.Stack()
                        .addLine(lblTitle)
                        .addLine(domainLabel,comboX)
                        .addLine(captionLabel, glue, chkAll)
                        .addLine(panel)
        );

        if (isLines) {
            lblTitle.setVisible(false);
            domainLabel.setVisible(false);
            comboX.setVisible(false);
        }

        blockLayoutUpdate = false;
        updateLayout();
    }

    public int getAxes(boolean extra) {
        return plotConfig.getAxes(extra);
    }

    private void refreshPlotConfig() {
        if (plotConfig == null) return;
        plotConfig.setDomainIndex(getDomainIndex());
        for (int index = 0; index < plotConfig.size(); index++) {
            plotConfig.setEnabled(index, isSelected(index));
            plotConfig.setIcon(index, getIcon(index));
        }
    }

    public PlotConfig getPlotConfig() {
        refreshPlotConfig();
        return new PlotConfig(plotConfig);
    }

    public void setPlotTitle(String title) {
        plotConfig.setTitle(title);
    }

    private int getDomainIndex() {
        return comboX.getSelectedIndex();
    }

    private void validateState() {
        int domainIndex = getDomainIndex();
        for (int index = 0; index < buttons.size(); index++) {
            setEnabled(index, index !=domainIndex);
        }
        notifyListeners();
    }

    public void add(String title, LegendIcon icon) {
        add(title, icon, (Action[]) null);
    }

    public int add(String title, LegendIcon icon, Action ... additionalActions) {
        JCheckBox checkBox = new JCheckBox(title, true);
        checkBox.addActionListener(e -> notifyListeners() );
        LegendButton button = new LegendButton(icon, true, !isLines, !isLines);
        button.addChangeListener(this);
        if (additionalActions != null) {
            button.setAdditionalActions(additionalActions);
        }

        checkBoxes.add(checkBox);
        buttons.add(button);
        updateLayout();
        return checkBoxes.size()-1;
    }

    public void extraAxisActionClick(int index) {
        plotConfig.setExtraAxis(index, !plotConfig.getExtraAxis(index));
        if (chartConfigPanel.getAxes(false) == 0) {
            plotConfig.setExtraAxis(index, false);
        }
        refreshExtraAxisAction(index);
        notifyListeners();
    }

    private void refreshExtraAxisAction(int index) {
        UserAction action = extraAxisActions.get(index);
        boolean extra = plotConfig.getExtraAxis(index);
        if (extra) {
            action.putValue(Action.NAME,"Return to left axis");
            action.putValue(Action.SMALL_ICON, Util.LEFT_ICON);
        } else {
            action.putValue(Action.NAME,"To extra axis");
            action.putValue(Action.SMALL_ICON, Util.RIGHT_ICON);
        }

        buttons.get(index).setText(extra ? " >>": "");

    }

    public void updateTitle(int index, String title) {
        checkBoxes.get(index).setText(title);
    }

    public int getListSize() {
        return buttons.size();
    }

    public LegendIcon getIcon(int index) {
        return buttons.get(index).getLegendIcon();
    }

    public void setChartType(ChartType chartType) {
        for (LegendButton btn : buttons) {
            btn.getLegendIcon().setChartType(chartType);
        }
    }

    public boolean isSelected(int index) {
        return checkBoxes.get(index).isSelected();
    }

    private void setEnabled(int index, boolean enabled) {
        checkBoxes.get(index).setEnabled(enabled);
        buttons.get(index).setEnabled(enabled);
        buttons.get(index).invalidate();
    }

    private void updateLayout() {
        if (blockLayoutUpdate) return;

        GroupLayoutSimple layout = new GroupLayoutSimple(panel);
        layout.setBaseline(false);
        layout.setPadding(3);
        GroupLayoutSimple.Stack chkBoxStack = new GroupLayoutSimple.Stack();
        GroupLayoutSimple.Stack btnStack = new GroupLayoutSimple.Stack();
        for (int i=0; i<buttons.size(); i++) {
            chkBoxStack.addLine(checkBoxes.get(i));
            btnStack.addLineAndGlue(buttons.get(i));
        }
        layout.setStacks(chkBoxStack, btnStack);
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
        refreshPlotConfig();
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
