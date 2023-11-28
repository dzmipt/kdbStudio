package studio.kdb;

import kx.*;
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

public class Session implements ConnectionStateListener, KAuthentication {
    private KConnection kConn;
    private long created;
    private final Server server;

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
        kConn = sessionCreator.createConnection(server, this);
        if (kConn == null) throw new RuntimeException("Failure in the authentication plugin");
        created = System.currentTimeMillis();
        kConn.setConnectionStateListener(this);
    }

    static void mock(SessionCreator sessionCreator) {
        log.info("Mocking kdb session creator");
        Session.sessionCreator = sessionCreator;
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
