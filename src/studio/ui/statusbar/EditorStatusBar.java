package studio.ui.statusbar;

import studio.ui.MinSizeLabel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EditorStatusBar extends StatusBar {

    private final MinSizeLabel lblConnection;

    private final Timer timer;
    private long clock;
    private boolean sessionConnected = false;

    private final static String CONNECTED = "Connected";
    private final static String DISCONNECTED = "Disconnected";

    public EditorStatusBar() {
        lblConnection = new MinSizeLabel("");
        lblConnection.setHorizontalAlignment(JLabel.CENTER);
        lblConnection.setMinimumWidth("1:00:00", CONNECTED, DISCONNECTED);
        addComponent(lblConnection);

        timer =  new Timer(500, this::timerClockAction);
        refreshConnectedLabel();
    }

    public void startClock() {
        clock = System.currentTimeMillis();
        timer.start();
    }

    public void stopClock() {
        timer.stop();
        refreshConnectedLabel();
    }

    public void setSessionConnected(boolean connected) {
        sessionConnected = connected;
        refreshConnectedLabel();
    }

    private void refreshConnectedLabel() {
        lblConnection.setText(sessionConnected ? CONNECTED : DISCONNECTED);
    }

    private void timerClockAction(ActionEvent event) {
        long time = (System.currentTimeMillis() - clock) / 1000;

        if (time < 1) return;

        long sec = time % 60;
        time /= 60;
        long min = time % 60;
        long hour = time / 60;

        lblConnection.setText(String.format("%d:%02d:%02d",hour, min, sec));
    }

}
