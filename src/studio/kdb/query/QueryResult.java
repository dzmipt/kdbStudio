package studio.kdb.query;

import kx.KMessage;
import studio.kdb.K;
import studio.kdb.Server;

public class QueryResult {

    private final QueryTask queryTask;
    private final Server server;
    private KMessage result = null;
    private Throwable error = null;
    private boolean complete = false;

    public QueryResult(K.KBase kObject) {
        this(QueryTask.queryResult(null, kObject), null);
        setResult(new KMessage(kObject));
    }

    public QueryResult(QueryTask queryTask, Server server) {
        this.queryTask = queryTask;
        this.server = server;
    }

    public boolean isChartAfter() {
        return isComplete() && queryTask.isChartAfter();
    }

    public QueryExecutedListener getQueryExecutedListener() {
        return queryTask.getQueryExecutedListener();
    }

    public void setResult(KMessage result) {
        this.result = result;
        if (result != null) {
            Throwable error = result.getError();
            if (error != null) setError(error);
        }
        complete = true;
    }

    public void setError(Throwable error) {
        this.error = error;
        complete = true;
    }

    public boolean hasResultObject() {
        return result != null || error != null;
    }

    public boolean isComplete() {
        return complete;
    }

    public String getQuery() {
        return queryTask.getQueryText();
    }

    public Server getServer() {
        return server;
    }

    public K.KBase getResult() {
        return result == null ? null : result.getObject();
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
