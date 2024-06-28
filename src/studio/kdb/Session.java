package studio.kdb;

import kx.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.core.IAuthenticationMechanism;
import studio.kdb.config.KdbMessageLimitAction;
import studio.ui.EditorTab;
import studio.ui.StudioOptionPane;
import studio.ui.StudioWindow;

import javax.swing.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Session implements ConnectionStateListener, KAuthentication {
    private KConnection kConn;
    private final Server server;

    private static final long HOUR_NS = 1_000_000_000L*3600;
    private static SessionCreator sessionCreator = new SessionCreator();
    private final List<EditorTab> editors = new ArrayList<>();

    private static final Logger log = LogManager.getLogger();

    private final static Map<Server, Session> sessions = new HashMap<>();

    private final static NumberFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    public static Session newSession(EditorTab editor) {
        Server server = editor.getServer();
        Session session;
        if (Config.getInstance().getBoolean(Config.SESSION_REUSE) ) {
            session = sessions.get(server);
            if (session == null) {
                session = new Session(server);
                sessions.put(server, session);
            }
        } else {
            session = new Session(server);
        }

        session.editors.add(editor);
        return session;
    }

    public void removeTab(EditorTab editor) {
        editors.remove(editor);
        if (editors.size() == 0) {
            close();
        }
    }

    @Override
    public void connectionStateChange(boolean connected) {
        for(EditorTab editor: editors) {
            editor.setSessionConnection(connected);
        }
    }

    @Override
    public void checkIncomingLimit(long msgLength) throws IOException {
        KdbMessageLimitAction action = Config.getInstance().getEnum(Config.KDB_MESSAGE_SIZE_LIMIT_ACTION);
        long limit = 1_000_000L * Config.getInstance().getInt(Config.KDB_MESSAGE_SIZE_LIMIT_MB);
        if (msgLength < limit) return;
        final String msg = "Incoming message size " + NUMBER_FORMAT.format(msgLength) + " breached the limit of " + NUMBER_FORMAT.format(limit) + ".";
        if (action == KdbMessageLimitAction.BLOCK) {
            throw new IOException(msg);
        }

        AtomicInteger choice = new AtomicInteger();
        try {
            SwingUtilities.invokeAndWait(() -> {
                choice.set(StudioOptionPane.showYesNoDialog(StudioWindow.getActiveStudioWindow(),
                        msg + "\n\nDownloading large amount of data can result in OutOfMemoryError.\n" +
                                "Note: you can change the limit as well as action in the Settings menu.\n\n" +
                                "Download?",
                        "Too Big Incoming Message"
                ));
            });
        } catch (Exception e) {
            throw new IOException("Error during showing incoming message limit dialog", e);
        }

        if (choice.get() != JOptionPane.YES_OPTION) {
            throw new IOException(msg);
        }
    }

    private Session(Server server) {
        this.server = server;
        init();
    }

    private void init() {
        kConn = sessionCreator.createConnection(server, this);
        if (kConn == null) throw new RuntimeException("Failure in the authentication plugin");
        kConn.setConnectionStateListener(this);
    }

    public KConnectionStats getConnectionStats() {
        return kConn.getStats();
    }

    static void mock(SessionCreator sessionCreator) {
        log.info("Mocking kdb session creator");
        Session.sessionCreator = sessionCreator;
    }

    public KMessage execute(K.KBase x, ProgressCallback progress) throws K4Exception, IOException, InterruptedException {
        return kConn.k(x, progress);
    }

    public boolean isClosed() {
        return kConn.isClosed();
    }

    public void close() {
        kConn.close();
    }

    public Server getServer() {
        return server;
    }

    public void validate() {
        if (!Config.getInstance().getBoolean(Config.SESSION_INVALIDATION_ENABLED)) return;

        int hours = Config.getInstance().getInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS);

        K.KTimestamp created = kConn.getStats().getLastConnectedTime();
        if (created.isNull()) return;
        K.KTimespan duration = K.KTimespan.period(created, K.KTimestamp.now());
        if (duration.toLong() > hours * HOUR_NS) {
            log.info("Closing session to stale server: " + server.getDescription(true));
            kConn.close();
        }
    }

    @Override
    public String getUserPassword() throws IOException {
        try {
            log.info("Getting authentication credential for {} with auth.method {}",
                    server.getDescription(true), server.getAuthenticationMechanism());

            Class<?> clazz = AuthenticationManager.getInstance().lookup(server.getAuthenticationMechanism());
            IAuthenticationMechanism authenticationMechanism = (IAuthenticationMechanism) clazz.newInstance();

            authenticationMechanism.setProperties(server.getAsProperties());
            Credentials credentials = authenticationMechanism.getCredentials();

            if (credentials.getUsername().length() > 0) {
                String p = credentials.getPassword();
                return credentials.getUsername() + ((p.length() == 0) ? "" : ":" + p);
            } else {
                return "";
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
            log.error("Failed to initialize connection", ex);
            throw new IOException("Failed to get credentials", ex);
        }
    }

    public static class SessionCreator {
        public KConnection createConnection(Server s, KAuthentication authentication) {
            return new KConnection(s.getHost(), s.getPort(), s.getUseTLS(), authentication);
        }
    }

}
