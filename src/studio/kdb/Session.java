package studio.kdb;

import kx.ConnectionStateListener;
import kx.K4Exception;
import kx.KConnection;
import kx.ProgressCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.core.IAuthenticationMechanism;
import studio.ui.EditorTab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session implements ConnectionStateListener {
    private KConnection kConn;
    private long created;
    private final Server server;

    private boolean busy = false;

    private static final long HOUR = 1000*3600;
    private static SessionCreator sessionCreator = new SessionCreator();
    private final List<EditorTab> editors = new ArrayList<>();

    private static final Logger log = LogManager.getLogger();

    private final static Map<Server, Session> sessions = new HashMap<>();

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
            editor.setSessonConnection(connected);
        }
    }

    private Session(Server server) {
        this.server = server;
        init();
    }

    private void init() {
        log.info("Connecting to server " + server.getDescription(true));
        kConn = createConnection(server);
        if (kConn == null) throw new RuntimeException("Failure in the authentication plugin");
        created = System.currentTimeMillis();
        kConn.setConnectionStateListener(this);
    }

    static void mock(SessionCreator sessionCreator) {
        log.info("Mocking kdb session creator");
        Session.sessionCreator = sessionCreator;
    }

    private static KConnection createConnection(Server s) {
        return sessionCreator.createConnection(s);
    }

    public boolean isBusy() {
        return busy;
    }

    public void setFree() {
        busy = false;
    }

    public void setBusy() {
        busy = true;
    }

    public K.KBase execute(K.KBase x, ProgressCallback progress) throws K4Exception, IOException, InterruptedException {
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
        if (created + hours * HOUR < System.currentTimeMillis()) {
            log.info("Closing session to stale server: " + server.getDescription(true));
            kConn.close();
            init();
        }
    }

    public static class SessionCreator {
        public KConnection createConnection(Server s) {
            try {
                Class<?> clazz = AuthenticationManager.getInstance().lookup(s.getAuthenticationMechanism());
                IAuthenticationMechanism authenticationMechanism = (IAuthenticationMechanism) clazz.newInstance();

                authenticationMechanism.setProperties(s.getAsProperties());
                Credentials credentials = authenticationMechanism.getCredentials();

                KConnection kConn;
                if (credentials.getUsername().length() > 0) {
                    String p = credentials.getPassword();
                    kConn = new KConnection(s.getHost(), s.getPort(), credentials.getUsername() + ((p.length() == 0) ? "" : ":" + p), s.getUseTLS());
                } else {
                    kConn = new KConnection(s.getHost(), s.getPort(), "", s.getUseTLS());
                }
                return kConn;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
                log.error("Failed to initialize connection", ex);
                return null;
            }
        }
    }

}
