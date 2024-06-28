package studio.ui.statusbar;

import studio.core.AuthenticationManager;
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
    private String authMethod = "";
    private EditorStatusBarCallback editorStatusBarCallback = null;

    private String[] knonwAuthMethods;
    private UserAction[] actionsConnect;
    private UserAction actionUnkownAuthConnect = null;

    private UserAction actionDisconnect;

    private final static Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final static String CONNECTED = "Connected";
    private final static String DISCONNECTED = "Disconnected";

    public EditorStatusBar() {
        initActions();

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
                JMenuItem menuItem = menu.add(actionDisconnect);
                menuItem.setDisabledIcon(menuItem.getIcon());
                menu.addSeparator();
                if (actionUnkownAuthConnect != null) menu.add(actionUnkownAuthConnect);
                for (Action action: actionsConnect) {
                    menuItem = menu.add(action);
                    menuItem.setDisabledIcon(menuItem.getIcon());
                }

                menu.show(lblConnection, e.getX(), e.getY());
            }
        });

        timer =  new Timer(500, this::timerClockAction);
        refreshConnectedLabel();
    }

    private void initActions() {
        knonwAuthMethods = AuthenticationManager.getInstance().getAuthenticationMechanisms();
        actionsConnect = new UserAction[knonwAuthMethods.length];
        for (int index = 0; index < knonwAuthMethods.length; index++) {
            String auth = knonwAuthMethods[index];
            actionsConnect[index] = UserAction.create(auth,
                    auth.equals(authMethod) ? Util.CHECK_ICON : Util.BLANK_ICON,
                    e -> this.editorStatusBarCallback.connect(auth) );
        }
        actionUnkownAuthConnect = null;

        actionDisconnect = UserAction.create("Disconnect", Util.ERROR_SMALL_ICON,
                e -> this.editorStatusBarCallback.disconnect()
        );
    }

    public void setEditorStatusBarCallback(EditorStatusBarCallback editorStatusBarCallback) {
        this.editorStatusBarCallback = editorStatusBarCallback;
    }

    public void startClock() {
        clock = System.currentTimeMillis();
        timer.start();
    }

    public void stopClock() {
        timer.stop();
        refreshConnectedLabel();
    }

    public void setSessionConnected(boolean connected, String authMethod) {
        sessionConnected = connected;
        this.authMethod = authMethod;

        refreshConnectedLabel();
    }

    private void refreshConnectedLabel() {
        lblConnection.setText(sessionConnected ? CONNECTED : DISCONNECTED);
        if (sessionConnected) {
            lblConnection.setToolTipText("Connected with '"+authMethod+"'");
        } else {
            lblConnection.setToolTipText("Disconnected");
        }

        boolean found = false;
        for (int index = 0; index < knonwAuthMethods.length; index++) {
            UserAction action = actionsConnect[index];
            String auth = knonwAuthMethods[index];
            if (auth.equals(authMethod)) {
                action.setIcon(Util.CHECK_ICON);
                action.setText(authMethod + (sessionConnected ? " (Connected)" : "") );
                action.setEnabled(!sessionConnected);
                found = true;
            } else {
                action.setIcon(Util.BLANK_ICON);
                action.setText(auth);
                action.setEnabled(true);
            }
        }

        if (found) {
            actionUnkownAuthConnect = null;
        } else {
            actionUnkownAuthConnect = UserAction.create(authMethod + (sessionConnected ? " (Connected)" : ""), Util.CHECK_ICON,
                    e -> this.editorStatusBarCallback.connect(authMethod) );
        }

        actionDisconnect.setText(sessionConnected ? "Disconnect" : "(Disconnected)");
        actionDisconnect.setEnabled(sessionConnected);

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
