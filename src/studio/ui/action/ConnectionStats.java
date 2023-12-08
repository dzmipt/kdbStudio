package studio.ui.action;

import kx.KConnectionStats;
import kx.KMessage;
import studio.kdb.K;
import studio.kdb.Session;
import studio.ui.StudioWindow;

import java.util.HashMap;
import java.util.Map;

public class ConnectionStats {

    public static void getStats(StudioWindow studioWindow) {
        QueryResult queryResult = new QueryResult(null, "");
        queryResult.setResult(new KMessage(getTable()));
        studioWindow.addResultTab(queryResult, "Connection statistics");
    }

    private static K.Flip getTable() {
        Map<Session, Integer> sessionsCount = new HashMap<>();

        StudioWindow.executeAll(editor -> {
            Session session = editor.getSession();
            if (session == null) return true;

            Integer count = sessionsCount.get(session);
            if (count == null) {
                sessionsCount.put(session, 1);
            } else {
                sessionsCount.put(session, count + 1);
            }

            return true;
        });

        final int DELTA = 4;
        String[] statsHeaders = KConnectionStats.getStatsHeaders();
        String[] names = new String[DELTA + statsHeaders.length];
        names[0] = "name";
        names[1] = "server";
        names[2] = "status";
        names[3] = "numberOfTabs";
        System.arraycopy(statsHeaders, 0, names, DELTA, statsHeaders.length);

        int count = sessionsCount.size();

        Object[] cols = new Object[names.length];
        Class<? extends K.KBase>[] types = (Class<? extends K.KBase>[]) new Class[names.length];

        String[] serverNames = new String[count];
        String[] servers = new String[count];
        String[] statuses = new String[count];
        int[] numbers = new int[count];

        cols[0] = serverNames;
        cols[1] = servers;
        cols[2] = statuses;
        cols[3] = numbers;
        types[0] = K.KSymbol.class;
        types[1] = K.KSymbol.class;
        types[2] = K.KSymbol.class;
        types[3] = K.KInteger.class;

        int index = 0;
        for (Session session: sessionsCount.keySet()) {
            serverNames[index] = session.getServer().getFullName();
            servers[index] = session.getServer().getConnectionString().substring(1);
            statuses[index] = session.isClosed() ? "Disconnected" : "Connected";
            numbers[index] = sessionsCount.get(session);

            K.KBase[] values = session.getConnectionStats().getValues();
            if (index == 0) {
                for (int i=0; i<values.length; i++) {
                    types[i+DELTA] = values[i].getClass();

                    if (values[i] instanceof K.KLongBase) {
                        cols[i+DELTA] = new long[count];
                    } else if (values[i] instanceof K.KIntBase) {
                        cols[i+DELTA] = new int[count];
                    } else {
                        throw new IllegalStateException("Internal error. This shouldn't happen");
                    }
                }
            }

            for (int i = 0; i<values.length; i++) {
                if (values[i] instanceof K.KLongBase) {
                    ((long[])cols[i+DELTA])[index] = ((K.KLongBase)values[i]).toLong();
                } else if (values[i] instanceof K.KIntBase) {
                    ((int[])cols[i+DELTA])[index] = ((K.KIntBase)values[i]).toInt();
                } else {
                    throw new IllegalStateException("Internal error. This shouldn't happen");
                }
            }

            index++;
        }


        K.KBaseVector[] kCols = new K.KBaseVector[cols.length];
        for (int i=0; i<cols.length; i++) {
            if (types[i] == null) {
                kCols[i] = new K.KList();
            } else if (types[i] == K.KSymbol.class) {
                kCols[i] = new K.KSymbolVector((String[])cols[i]);
            } else if (types[i] == K.KInteger.class) {
                kCols[i] = new K.KIntVector((int[])cols[i]);
            } else if (types[i] == K.KLong.class) {
                kCols[i] = new K.KLongVector((long[])cols[i]);
            } else if (types[i] == K.KTimestamp.class) {
                kCols[i] = new K.KTimestampVector((long[])cols[i]);
            } else if (types[i] == K.KTimespan.class) {
                kCols[i] = new K.KTimespanVector((long[])cols[i]);
            } else {
                throw new IllegalStateException("Internal error. This shouldn't happen");
            }
        }

        K.Flip table = new K.Flip(new K.KSymbolVector(names), new K.KList(kCols));
        return table;
    }
}
