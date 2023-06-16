package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.core.AuthenticationManager;
import studio.core.Credentials;
import studio.core.IAuthenticationMechanism;

public class Session {
    private kx.c c;
    private long created;
    private final Server server;

    private boolean busy = false;

    private static final long HOUR = 1000*3600;

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

    private static kx.c createConnection(Server s) {
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
            c.setEncoding(Config.getInstance().getEncoding());
            return c;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException ex) {
            log.error("Failed to initialize connection", ex);
            return null;
        }
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

    public kx.c getKdbConnection() {
        return c;
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
}
