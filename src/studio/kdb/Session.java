package studio.kdb;

import kx.ProgressCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.core.IAuthenticationMechanism;

import java.io.IOException;

public class Session {
    private kx.c c;
    private long created;
    private final Server server;

    private boolean busy = false;

    private static final long HOUR = 1000*3600;
    private static SessionCreator sessionCreator = new SessionCreator();

    private static final Logger log = LogManager.getLogger();

    public Session(Server server) {
        this.server = server;
        init();
    }

    private void init() {
        log.info("Connecting to server " + server.getDescription(true));
        c = createConnection(server);
        if (c == null) throw new RuntimeException("Failure in the authentication plugin");
        created = System.currentTimeMillis();
    }

    static void mock(SessionCreator sessionCreator) {
        log.info("Mocking kdb session creator");
        Session.sessionCreator = sessionCreator;
    }

    private static kx.c createConnection(Server s) {
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

    public K.KBase execute(K.KBase x, ProgressCallback progress) throws kx.c.K4Exception, IOException {
        return c.k(x, progress);
    }

    public boolean isClosed() {
        return c.isClosed();
    }

    public void close() {
        c.close();
    }

    public Server getServer() {
        return server;
    }

    public void validate() {
        if (!Config.getInstance().getBoolean(Config.SESSION_INVALIDATION_ENABLED)) return;

        int hours = Config.getInstance().getInt(Config.SESSION_INVALIDATION_TIMEOUT_IN_HOURS);
        if (created + hours * HOUR < System.currentTimeMillis()) {
            log.info("Closing session to stale server: " + server.getDescription(true));
            c.close();
            init();
        }
    }

    public static class SessionCreator {
        public kx.c createConnection(Server s) {
            try {
                Class<?> clazz = AuthenticationManager.getInstance().lookup(s.getAuthenticationMechanism());
                IAuthenticationMechanism authenticationMechanism = (IAuthenticationMechanism) clazz.newInstance();

                authenticationMechanism.setProperties(s.getAsProperties());
                Credentials credentials = authenticationMechanism.getCredentials();

                kx.c c;
                if (credentials.getUsername().length() > 0) {
                    String p = credentials.getPassword();
                    c = new kx.c(s.getHost(), s.getPort(), credentials.getUsername() + ((p.length() == 0) ? "" : ":" + p), s.getUseTLS());
                } else {
                    c = new kx.c(s.getHost(), s.getPort(), "", s.getUseTLS());
                }
                return c;
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
                log.error("Failed to initialize connection", ex);
                return null;
            }
        }
    }

}
