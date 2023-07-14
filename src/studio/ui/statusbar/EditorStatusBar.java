package studio.ui.statusbar;

import studio.ui.MinSizeLabel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EditorStatusBar extends StatusBar {

    private final MinSizeLabel lblClock;

    private final Timer timer;
    private long clock;

    public EditorStatusBar() {
        lblClock  = new MinSizeLabel("");
        lblClock.setHorizontalAlignment(JLabel.CENTER);
        lblClock.setMinimumWidth("1:00:00");
        addComponent(lblClock);
        lblClock.setVisible(false);

        timer =  new Timer(500, this::timerClockAction);
    }

    public void startClock() {
        clock = System.currentTimeMillis();
        timer.start();
    }

    public void stopClock() {
        timer.stop();
        lblClock.setVisible(false);
    }

    private void timerClockAction(ActionEvent event) {
        long time = (System.currentTimeMillis() - clock) / 1000;

        if (time < 1) return;

        long sec = time % 60;
        time /= 60;
        long min = time % 60;
        long hour = time / 60;

        lblClock.setText(String.format("%d:%02d:%02d",hour, min, sec));
        lblClock.setVisible(true);
    }

}
