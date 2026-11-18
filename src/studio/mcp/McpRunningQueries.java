package studio.mcp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Queries from AI agents which are being executed. Thread-safe.
 */
public class McpRunningQueries {

    public static class RunningQuery {
        private final int id;
        private final String agent;
        private final String server;
        private final String target;
        private final String query;
        private final Instant started = Instant.now();
        private final Runnable canceller;
        private volatile boolean cancelled = false;

        RunningQuery(int id, String agent, String server, String target, String query, Runnable canceller) {
            this.id = id;
            this.agent = agent;
            this.server = server;
            this.target = target;
            this.query = query;
            this.canceller = canceller;
        }

        public int getId() {
            return id;
        }

        public String getAgent() {
            return agent;
        }

        public String getServer() {
            return server;
        }

        // where the query runs: a tab or a background connection
        public String getTarget() {
            return target;
        }

        public String getQuery() {
            return query;
        }

        public Instant getStarted() {
            return started;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    public interface Listener {
        void runningQueriesChanged();
    }

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final List<RunningQuery> queries = new CopyOnWriteArrayList<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    private void fire() {
        for (Listener listener: listeners) listener.runningQueriesChanged();
    }

    /**
     * @param canceller called (from any thread) when the user cancels the query; should close the connection
     */
    public RunningQuery start(String agent, String server, String target, String query, Runnable canceller) {
        RunningQuery runningQuery = new RunningQuery(nextId.getAndIncrement(), agent, server, target, query, canceller);
        queries.add(runningQuery);
        fire();
        return runningQuery;
    }

    public void finish(RunningQuery runningQuery) {
        if (queries.remove(runningQuery)) fire();
    }

    public void cancel(RunningQuery runningQuery) {
        if (runningQuery.cancelled) return;
        runningQuery.cancelled = true;
        McpLog.info("[{}] query on {} is cancelled by the user", runningQuery.agent, runningQuery.server);
        runningQuery.canceller.run();
    }

    public void cancelAll() {
        for (RunningQuery runningQuery: getAll()) cancel(runningQuery);
    }

    public List<RunningQuery> getAll() {
        return new ArrayList<>(queries);
    }
}
