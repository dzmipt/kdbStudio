package kx;

import studio.kdb.K;

public class KConnectionStats {

    private K.KInteger numberOfConnections = K.KInteger.ZERO;
    private K.KTimestamp lastConnectedTime = K.KTimestamp.NULL;
    private K.KTimestamp lastDisconnectedTime = K.KTimestamp.NULL;

    private K.KLong totalBytesSent = K.KLong.ZERO;
    private K.KLong totalBytesSentInCurrentSession = K.KLong.ZERO;
    private K.KLong lastBytesSent = K.KLong.ZERO;

    private K.KLong totalBytesReceived = K.KLong.ZERO;
    private K.KLong totalBytesReceivedInCurrentSession = K.KLong.ZERO;
    private K.KLong lastBytesReceived = K.KLong.ZERO;

    private K.KTimestamp lastQueryTimestamp = K.KTimestamp.NULL;
    private K.KTimespan totalQueryTime = K.KTimespan.NULL;
    private K.KTimespan totalQueryTimeInCurrentSession = K.KTimespan.NULL;
    private K.KTimespan lastQueryTime = K.KTimespan.NULL;

    private K.KInteger totalNumberQueries = K.KInteger.ZERO;
    private K.KInteger totalNumberQueriesInCurrentSession = K.KInteger.ZERO;

    KConnectionStats() {}

    public synchronized void connected() {
        numberOfConnections = numberOfConnections.add(1);
        lastConnectedTime = K.KTimestamp.now();
        totalBytesSentInCurrentSession = K.KLong.ZERO;
        totalBytesReceivedInCurrentSession = K.KLong.ZERO;
        totalQueryTimeInCurrentSession = K.KTimespan.NULL;
        totalNumberQueriesInCurrentSession = K.KInteger.ZERO;
    }

    public synchronized void disconnected() {
        lastDisconnectedTime = K.KTimestamp.now();
    }

    public synchronized void sentBytes(long count) {
        lastQueryTimestamp = K.KTimestamp.now();
        totalBytesSent = totalBytesSent.add(count);
        totalBytesSentInCurrentSession = totalBytesSentInCurrentSession.add(count);
        lastBytesSent = new K.KLong(count);

        totalNumberQueries = totalNumberQueries.add(1);
        totalNumberQueriesInCurrentSession = totalNumberQueriesInCurrentSession.add(1);
    }

    public synchronized void receivedBytes(long count) {
        lastQueryTime = K.KTimespan.period(lastQueryTimestamp, K.KTimestamp.now());
        totalQueryTime = totalQueryTime.isNull() ? lastQueryTime : totalQueryTime.add(lastQueryTime);
        totalQueryTimeInCurrentSession = totalQueryTimeInCurrentSession.isNull() ?
                                            lastQueryTime : totalQueryTimeInCurrentSession.add(lastQueryTime);

        totalBytesReceived = totalBytesReceived.add(count);
        totalBytesReceivedInCurrentSession = totalBytesReceivedInCurrentSession.add(count);
        lastBytesReceived = new K.KLong(count);
    }

    public synchronized K.KTimestamp getLastConnectedTime() {
        return lastConnectedTime;
    }

    public static String[] getStatsHeaders() {
        Pair[] pairs = new KConnectionStats().pairs();
        String[] headers = new String[pairs.length];
        for (int i=0; i < headers.length; i++) {
            headers[i] = pairs[i].column;
        }
        return headers;
    }

    public synchronized K.KBase[] getValues() {
        Pair[] pairs = pairs();
        K.KBase[] values = new K.KBase[pairs.length];
        for (int i=0; i < values.length; i++) {
            values[i] = pairs[i].value;
        }
        return values;
    }

    private Pair[] pairs() {
        return new Pair[] {
                Pair.of("numberConnections", numberOfConnections),
                Pair.of("lastConnected", lastConnectedTime),
                Pair.of("lastDisconnected", lastDisconnectedTime),
                Pair.of("lastQuerySent", lastQueryTimestamp),

                Pair.of("totalNumberOfQueries", totalNumberQueries),
                Pair.of("totalBytesSent", totalBytesSent),
                Pair.of("totalBytesReceived", totalBytesReceived),
                Pair.of("totalQueriesDuration", totalQueryTime),

                Pair.of("totalNumberOfQueriesInLastSession", totalNumberQueriesInCurrentSession),
                Pair.of("totalBytesSentInLastSession", totalBytesSentInCurrentSession),
                Pair.of("totalBytesReceivedInLastSession", totalBytesReceivedInCurrentSession),
                Pair.of("totalQueriesDurationInLastSession", totalQueryTimeInCurrentSession),

                Pair.of("lastBytesSent", lastBytesSent),
                Pair.of("lastBytesReceived", lastBytesReceived),
                Pair.of("lastQueryDuration", lastQueryTime),
        };
    }

    private static class Pair {
        String column;
        K.KBase value;
        static Pair of(String column, K.KBase value) {
            Pair pair = new Pair();
            pair.column = column;
            pair.value = value;
            return pair;
        }
    }

}
