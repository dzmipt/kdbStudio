package studio.ui.statusbar;

import kx.ConnectionContext;
import studio.core.AuthenticationManager;
import studio.ui.MinSizeLabel;
import studio.ui.UserAction;
import studio.ui.Util;
import studio.ui.tls.CertChainInfoDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;

public class EditorStatusBar extends StatusBar {

    private final JLabel lblTLS;
    private final MinSizeLabel lblConnection;

    private final Timer timer;
    private long clock;
    private ConnectionContext connectionContext = new ConnectionContext();
    private EditorStatusBarCallback editorStatusBarCallback = null;

    private final static Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final static String DISCONNECTED = "Disconnected";

    private Action disconnectAction;
    private Map<String, Action> authMethodMapActions;
    private Action tlsConnectAction;
    private Action nonTlsConnectAction;
    private Action tlsTrustedDisconnectAction;
    private Action tlsUntrustedDisconnectAction;
    private Action nonTlsDisconnectAction;
    private Action showTlsChainAction;

    public EditorStatusBar() {
        lblTLS = new JLabel(Util.UNLOCK_ICON);
        lblTLS.setCursor(cursor);
        addComponent(lblTLS);
        lblTLS.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (editorStatusBarCallback == null) return;
                getLblTLSPopupMenu().show(lblTLS, e.getX(), e.getY());
            }
        });

        lblConnection = new MinSizeLabel("");
        lblConnection.setHorizontalAlignment(JLabel.CENTER);
        lblConnection.setMinimumWidth("1:00:00", DISCONNECTED);
        lblConnection.setCursor(cursor);
        addComponent(lblConnection);
        lblConnection.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (editorStatusBarCallback == null) return;
                getLblConnectionPopupMenu().show(lblConnection, e.getX(), e.getY());
            }
        });

        timer =  new Timer(500, this::timerClockAction);
        refreshConnectedLabel();
    }

    private JPopupMenu getLblConnectionPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        if (connectionContext.isConnected()) {
            menu.add(disconnectAction);
            if (authMethodMapActions.size()>1) {
                menu.addSeparator();
            }
        }
        for (String authMethod: authMethodMapActions.keySet()) {
            if (connectionContext.isConnected() && authMethod.equals(connectionContext.getAuthMethod())) continue;
            menu.add(authMethodMapActions.get(authMethod));
        }

        return menu;
    }

    private JPopupMenu getLblTLSPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        if (connectionContext.isConnected()) {
            if (connectionContext.isSecure()) {
                if (connectionContext.isTrusted()) menu.add(tlsTrustedDisconnectAction);
                else menu.add(tlsUntrustedDisconnectAction);

                menu.add(nonTlsConnectAction);
            } else {
                menu.add(tlsConnectAction);
                menu.add(nonTlsDisconnectAction);
            }
        } else {
            menu.add(tlsConnectAction);
            menu.add(nonTlsConnectAction);
        }

        menu.addSeparator();
        showTlsChainAction.setEnabled(connectionContext.getCertificate() != null);
        menu.add(showTlsChainAction);

        return menu;
    }



    private void initActions() {
        if (editorStatusBarCallback == null) return;

        disconnectAction = UserAction.create("Disconnect", editorStatusBarCallback::disconnect);
        String[] authMethods = AuthenticationManager.getInstance().getAuthenticationMechanisms();
        authMethodMapActions = new LinkedHashMap<>();
        for (String authMethod: authMethods) {
            authMethodMapActions.put(authMethod,
                    UserAction.create(
                            String.format("<html>Connect with <i>%s</i></html>", authMethod),
                            () -> editorStatusBarCallback.connect(authMethod)
                    ));
        }

        tlsConnectAction = UserAction.create("Connect secure",
                Util.LOCK_ICON,
                () -> editorStatusBarCallback.connectTLS(true)
        );
        nonTlsConnectAction = UserAction.create("Connect insecure",
                Util.UNLOCK_ICON,
                () -> editorStatusBarCallback.connectTLS(false)
        );

        tlsTrustedDisconnectAction = UserAction.create("Disconnect",
                Util.LOCK_ICON, editorStatusBarCallback::disconnect );
        tlsUntrustedDisconnectAction = UserAction.create("Disconnect",
                Util.LOCK_CROSSED_ICON, editorStatusBarCallback::disconnect );
        nonTlsDisconnectAction = UserAction.create("Disconnect",
                Util.UNLOCK_ICON, editorStatusBarCallback::disconnect );

        showTlsChainAction = UserAction.create("Show certificate chain",
                    () -> new CertChainInfoDialog(EditorStatusBar.this, connectionContext.getCertChain())
                );
    }

    public void setEditorStatusBarCallback(EditorStatusBarCallback editorStatusBarCallback) {
        this.editorStatusBarCallback = editorStatusBarCallback;
        initActions();
    }

    public void startClock() {
        clock = System.currentTimeMillis();
        timer.start();
    }

    public void stopClock() {
        timer.stop();
        refreshConnectedLabel();
    }

    public void setSessionContext(ConnectionContext context) {
        this.connectionContext = context;
        refreshConnectedLabel();
    }

    private void refreshConnectedLabel() {
        String authMethod = connectionContext.getAuthMethod();
        lblConnection.setText(connectionContext.isConnected() ? authMethod : DISCONNECTED);
        if (connectionContext.isConnected()) {
            lblConnection.setToolTipText(String.format("<html>Connected with <i>%s</i></html>", authMethod));
        } else {
            lblConnection.setToolTipText(null);
        }


        if (connectionContext.isSecure()) {
            if (connectionContext.isTrusted()) {
                lblTLS.setIcon(Util.LOCK_ICON);
            } else {
                lblTLS.setIcon(Util.LOCK_CROSSED_ICON);
            }
        } else {
            lblTLS.setIcon(Util.UNLOCK_ICON);
        }

        X509Certificate certificate = connectionContext.getCertificate();
        if (certificate == null) {
            lblTLS.setToolTipText(null);
        } else {
            lblTLS.setToolTipText(certificate.getSubjectDN().toString());
        }
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
