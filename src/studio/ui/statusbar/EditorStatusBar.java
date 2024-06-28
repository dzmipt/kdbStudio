package studio.ui.statusbar;

import studio.ui.MinSizeLabel;
import studio.ui.UserAction;
import studio.ui.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EditorStatusBar extends StatusBar {

    private final MinSizeLabel lblConnection;

    private final Timer timer;
    private long clock;
    private boolean sessionConnected = false;
    private EditorStatusBarCallback editorStatusBarCallback = null;

    private UserAction actionConnect;
    private UserAction actionDisconnect;

    private final static Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final static String CONNECTED = "Connected";
    private final static String DISCONNECTED = "Disconnected";

    public EditorStatusBar() {
        lblConnection = new MinSizeLabel("");
        lblConnection.setHorizontalAlignment(JLabel.CENTER);
        lblConnection.setMinimumWidth("1:00:00", CONNECTED, DISCONNECTED);
        lblConnection.setCursor(cursor);
        addComponent(lblConnection);
        lblConnection.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (editorStatusBarCallback == null) return;

                JPopupMenu menu = new JPopupMenu();
                if (sessionConnected) {
                    menu.add(actionDisconnect);
                } else {
                    menu.add(actionConnect);
                }
                menu.show(lblConnection, e.getX(), e.getY());
            }
        });

        timer =  new Timer(500, this::timerClockAction);
        refreshConnectedLabel();
    }

    public void setEditorStatusBarCallback(EditorStatusBarCallback editorStatusBarCallback) {
        this.editorStatusBarCallback = editorStatusBarCallback;
        if (editorStatusBarCallback == null) return;

        actionConnect = UserAction.create("Connect", Util.CHECK_ICON,
                 e -> this.editorStatusBarCallback.connect()
        );

        actionDisconnect = UserAction.create("Disconnect", Util.ERROR_SMALL_ICON,
                e -> this.editorStatusBarCallback.disconnect()
        );

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
