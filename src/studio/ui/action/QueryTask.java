package studio.ui.action;

import kx.K4Exception;
import kx.KMessage;
import kx.ProgressCallback;
import studio.kdb.K;
import studio.kdb.Session;
import studio.ui.StudioWindow;

import java.io.IOException;

public abstract class QueryTask {

    protected final StudioWindow studioWindow;

    protected QueryTask(StudioWindow studioWindow) {
        this.studioWindow = studioWindow;
    }

    public static QueryTask query(StudioWindow studioWindow, String queryText) {
        return new Query(studioWindow, queryText, false);
    }

    public static QueryTask queryAndChart(StudioWindow studioWindow, String queryText) {
        return new Query(studioWindow, queryText, true);
    }

    public static QueryTask upload(StudioWindow studioWindow, String varName, K.KBase kObject) {
        return new Upload(studioWindow, varName, kObject);
    }

    public static QueryTask connect(StudioWindow studioWindow) {
        return new Connect(studioWindow);
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

        Query (StudioWindow studioWindow, String query, boolean chartAfter) {
            super(studioWindow);
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
            return session.execute(studioWindow, new K.KString(query), progress);
        }
    }

    private static class Upload extends QueryTask {
        private String varName;
        private K.KBase kObject;

        Upload(StudioWindow studioWindow, String varName, K.KBase kObject) {
            super(studioWindow);
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
            return session.execute(studioWindow, query, progress);
        }
    }

    private static class Connect extends QueryTask {
        Connect(StudioWindow studioWindow) {
            super(studioWindow);
        }

        @Override
        public String getQueryText() {
            return "<connect>";
        }

        @Override
        public KMessage execute(Session session, ProgressCallback progress) throws IOException, K4Exception, InterruptedException {
            session.connect(studioWindow);
            return null;
        }

        @Override
        public boolean returnResult() {
            return false;
        }
    }
}
