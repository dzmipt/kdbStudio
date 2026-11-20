package studio.mcp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MCP sessions (connected agents). Thread-safe.
 */
public class McpSessions {

    // An agent is shown as connected if it was active recently
    public static final Duration ACTIVE_PERIOD = Duration.ofMinutes(5);
    // Sessions idle for longer are forgotten (a client then gets 404 and initializes a new session)
    public static final Duration IDLE_TIMEOUT = Duration.ofDays(1);
    // The maximum number of sessions; the least recently active ones are removed
    public static final int MAX_SESSIONS = 200;

    public static class Session {
        private final String id;
        private final String clientName;
        private final String clientVersion;
        private final String remoteAddress;
        private final Instant created;
        private volatile Instant lastActivity;

        Session(String id, String clientName, String clientVersion, String remoteAddress, Instant created) {
            this.id = id;
            this.clientName = clientName;
            this.clientVersion = clientVersion;
            this.remoteAddress = remoteAddress;
            this.created = created;
            this.lastActivity = created;
        }

        public String getId() {
            return id;
        }

        public String getClientName() {
            return clientName;
        }

        public String getClientVersion() {
            return clientVersion;
        }

        public String getRemoteAddress() {
            return remoteAddress;
        }

        public Instant getCreated() {
            return created;
        }

        public Instant getLastActivity() {
            return lastActivity;
        }

        public String getDisplayName() {
            String name = clientName == null || clientName.isEmpty() ? "unknown agent" : clientName;
            return clientVersion == null || clientVersion.isEmpty() ? name : name + " " + clientVersion;
        }
    }

    public interface Listener {
        void sessionsChanged();
    }

    private final Clock clock;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public McpSessions() {
        this(Clock.systemUTC());
    }

    public McpSessions(Clock clock) {
        this.clock = clock;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void fire() {
        for (Listener listener: listeners) {
            listener.sessionsChanged();
        }
    }

    public Session create(String clientName, String clientVersion, String remoteAddress) {
        Session session = new Session(UUID.randomUUID().toString(), clientName, clientVersion, remoteAddress, clock.instant());
        sessions.put(session.getId(), session);
        removeIdle();
        fire();
        return session;
    }

    // Removes sessions idle for longer than IDLE_TIMEOUT, and the least recently active ones above MAX_SESSIONS
    private synchronized void removeIdle() {
        Instant now = clock.instant();
        sessions.values().removeIf(s -> Duration.between(s.getLastActivity(), now).compareTo(IDLE_TIMEOUT) > 0);
        int extra = sessions.size() - MAX_SESSIONS;
        if (extra <= 0) return;
        List<Session> list = new ArrayList<>(sessions.values());
        list.sort((s1, s2) -> s1.getLastActivity().compareTo(s2.getLastActivity()));
        for (int i = 0; i < extra; i++) sessions.remove(list.get(i).getId());
    }

    public int size() {
        return sessions.size();
    }

    public Session get(String id) {
        if (id == null) return null;
        return sessions.get(id);
    }

    public void touch(Session session) {
        boolean wasActive = isActive(session);
        session.lastActivity = clock.instant();
        if (!wasActive) fire();
    }

    public Session remove(String id) {
        Session session = sessions.remove(id);
        if (session != null) fire();
        return session;
    }

    public void clear() {
        if (sessions.isEmpty()) return;
        sessions.clear();
        fire();
    }

    public boolean isActive(Session session) {
        return Duration.between(session.getLastActivity(), clock.instant()).compareTo(ACTIVE_PERIOD) < 0;
    }

    public List<Session> getActive() {
        List<Session> result = new ArrayList<>();
        for (Session session: sessions.values()) {
            if (isActive(session)) result.add(session);
        }
        result.sort((s1, s2) -> s1.getCreated().compareTo(s2.getCreated()));
        return result;
    }
}
