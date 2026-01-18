package studio.ui.statusbar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StatusBar extends JPanel {

    private final GroupLayout.Group vLayoutGroup, hLayoutGroup;
    private final JLabel lblStatus;

    private final Timer tempStatusTimer;

    private String oldStatus = "";

    public StatusBar() {
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        hLayoutGroup = layout.createSequentialGroup();
        vLayoutGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        layout.setHorizontalGroup(hLayoutGroup);
        layout.setVerticalGroup(vLayoutGroup);

        tempStatusTimer =  new Timer(3000, this::tempStatusTimerAction);
        tempStatusTimer.setRepeats(false);

        lblStatus = new JLabel(" ");
        addComponent(lblStatus, false);
    }

    protected void addComponent(JComponent component) {
        addComponent(component, true);
    }

    private void addComponent(JComponent component, boolean widget) {
        setBorder(component, widget);
        hLayoutGroup.addComponent(component, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                widget ? GroupLayout.DEFAULT_SIZE : Integer.MAX_VALUE);
        vLayoutGroup.addComponent(component, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Integer.MAX_VALUE);
    }

    public void setStatus(String status) {
        if (tempStatusTimer.isRunning()) {
            oldStatus = status;
        }
        lblStatus.setText(status);
    }

    public void setTemporaryStatus(String status) {
        if (!tempStatusTimer.isRunning()) {
            oldStatus = lblStatus.getText();
        }
        lblStatus.setText(status);
        tempStatusTimer.restart();
    }

    private void tempStatusTimerAction(ActionEvent event) {
        setStatus(oldStatus);
    }

    private void setBorder(JComponent component, boolean widget) {
        component.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1,
                                widget ? 0:1, 1, 1, Color.LIGHT_GRAY),
                        BorderFactory.createEmptyBorder(0,2,0,2)
                )
        );

    }

}
