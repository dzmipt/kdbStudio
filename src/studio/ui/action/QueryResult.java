package studio.ui.action;

import kx.KMessage;
import studio.kdb.K;
import studio.kdb.Server;

public class QueryResult {

    private String query;
    private Server server;

    private KMessage result = null;
    private Throwable error = null;
    private boolean complete = false;

    public QueryResult(Server server, String query) {
        this.server = server;
        this.query = query;
    }

    public void setResult(KMessage result) {
        this.result = result;
        Throwable error = result.getError();
        if (error != null) setError(error);
        complete = true;
    }

    public void setError(Throwable error) {
        this.error = error;
        complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getQuery() {
        return query;
    }

    public Server getServer() {
        return server;
    }

    public K.KBase getResult() {
        return result.getObject();
    }

    public KMessage getKMessage() {
        return result;
    }

    public Throwable getError() {
        return error;
    }

    public long getExecutionTimeInMS() {
        if (result == null) return 0;
        return (result.getFinished().toLong() - result.getStarted().toLong()) / 1_000_000;
    }
}
