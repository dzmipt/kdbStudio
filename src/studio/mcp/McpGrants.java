package studio.mcp;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Time-limited permissions to run queries without confirmation. Thread-safe.
 */
public class McpGrants {

    public static class Grant {
        private final String key;
        private final String label;
        private final Instant grantedAt;
        private final Instant expiresAt; // null - until Studio exits
        private int queryCount = 0;

        Grant(String key, String label, Instant grantedAt, Instant expiresAt) {
            this.key = key;
            this.label = label;
            this.grantedAt = grantedAt;
            this.expiresAt = expiresAt;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public Instant getGrantedAt() {
            return grantedAt;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public synchronized int getQueryCount() {
            return queryCount;
        }

        synchronized void incrementQueryCount() {
            queryCount++;
        }

        public boolean isExpired(Instant now) {
            return expiresAt != null && !now.isBefore(expiresAt);
        }
    }

    public interface Listener {
        void grantsChanged();
    }

    private final Clock clock;
    private final Map<String, Grant> grants = new LinkedHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public McpGrants() {
        this(Clock.systemUTC());
    }

    public McpGrants(Clock clock) {
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
            listener.grantsChanged();
        }
    }

    public static String serverKey(String fullName) {
        return "server:" + fullName;
    }

    public static String adHocKey(String host, int port) {
        return "adhoc:" + host + ":" + port;
    }

    /**
     * @param duration null - until Studio exits
     */
    public Grant grant(String key, String label, Duration duration) {
        Instant now = clock.instant();
        Grant grant = new Grant(key, label, now, duration == null ? null : now.plus(duration));
        synchronized (this) {
            grants.put(key, grant);
        }
        fire();
        return grant;
    }

    /**
     * Checks for an active grant; if found, increments its query counter.
     */
    public boolean useGrant(String key) {
        Grant grant;
        boolean expired = false;
        synchronized (this) {
            grant = grants.get(key);
            if (grant != null && grant.isExpired(clock.instant())) {
                grants.remove(key);
                grant = null;
                expired = true;
            }
        }
        if (expired) fire();
        if (grant == null) return false;
        grant.incrementQueryCount();
        fire();
        return true;
    }

    public boolean revoke(String key) {
        boolean removed;
        synchronized (this) {
            removed = grants.remove(key) != null;
        }
        if (removed) fire();
        return removed;
    }

    public void revokeAll() {
        boolean changed;
        synchronized (this) {
            changed = ! grants.isEmpty();
            grants.clear();
        }
        if (changed) fire();
    }

    /**
     * Active grants; expired grants are removed.
     */
    public List<Grant> getActive() {
        boolean changed = false;
        List<Grant> result;
        synchronized (this) {
            Instant now = clock.instant();
            changed = grants.values().removeIf(g -> g.isExpired(now));
            result = new ArrayList<>(grants.values());
        }
        if (changed) fire();
        return result;
    }

    public Instant now() {
        return clock.instant();
    }
}
