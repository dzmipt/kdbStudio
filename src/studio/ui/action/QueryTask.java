package studio.ui.action;

import kx.K4Exception;
import kx.KMessage;
import kx.ProgressCallback;
import studio.kdb.K;
import studio.kdb.Session;

import java.io.IOException;

public abstract class QueryTask {

    public static QueryTask query(String queryText) {
        return new Query(queryText, false);
    }

    public static QueryTask queryAndChart(String queryText) {
        return new Query(queryText, true);
    }

    public static QueryTask upload(String varName, K.KBase kObject) {
        return new Upload(varName, kObject);
    }

    public static QueryTask connect() {
        return new Connect();
    }

    public abstract String getQueryText();

    public abstract KMessage execute(Session session, ProgressCallback progress) throws IOException, K4Exception, InterruptedException;

    public boolean returnResult() {
        return true;
    }

    public boolean isChartAfter() {
        return false;
    }

    private static class Query extends QueryTask {
        private final String query;
        private final boolean chartAfter;

        Query (String query, boolean chartAfter) {
            this.query = query;
            this.chartAfter = chartAfter;
        }

        @Override
        public String getQueryText() {
            return query;
        }

        @Override
        public boolean isChartAfter() {
            return chartAfter;
        }

        @Override
        public KMessage execute(Session session, ProgressCallback progress) throws IOException, K4Exception, InterruptedException {
            return session.execute(new K.KCharacterVector(query), progress);
        }
    }

    private static class Upload extends QueryTask {
        private String varName;
        private K.KBase kObject;

        Upload(String varName, K.KBase kObject) {
            this.varName = varName;
            this.kObject = kObject;
        }

        @Override
        public String getQueryText() {
            return "<upload to server>";
        }

        @Override
        public KMessage execute(Session session, ProgressCallback progress) throws IOException, K4Exception, InterruptedException {
            K.KBase query = new K.KList(new K.Function("{x set y}"), new K.KSymbol(varName), kObject);
            return session.execute(query, progress);
        }
    }

    private static class Connect extends QueryTask {
        Connect() {}

        @Override
        public String getQueryText() {
            return "<connect>";
        }

        @Override
        public KMessage execute(Session session, ProgressCallback progress) throws IOException, K4Exception, InterruptedException {
            session.connect();
            return null;
        }

        @Override
        public boolean returnResult() {
            return false;
        }
    }
}
