package studio.kdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionPool {

    private static final Logger log = LogManager.getLogger();

    private final static ConnectionPool instance = new ConnectionPool();

    private final Map<Server,List<Session>> sessionMap = new HashMap<>();

    public static ConnectionPool getInstance() {
        return instance;
    }

    private ConnectionPool() {}

    private static void cleanupSessions(List<Session> sessions) {
        sessions.removeIf(Session::isClosed);
    }

    public synchronized Session leaseConnection(Server server) {
        List<Session> sessions = sessionMap.computeIfAbsent(server, k-> new ArrayList<>());
        cleanupSessions(sessions);

        for (Session session: sessions) {
            if (! session.isBusy()) {
                session.validate();
                session.setBusy();
                return session;
            }
        }
        Session session = new Session(server);

        sessions.add(session);
        return session;
    }

    public synchronized void purge(Server server) {
        List<Session> sessions = sessionMap.computeIfAbsent(server, k-> new ArrayList<>());
        for(Session session: sessions) {
            session.close();
        }
        sessions.clear();
    }

}
