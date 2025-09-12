package studio.kdb.query;

import studio.kdb.Server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoCompletionCache {

    private final static Map<Server, Schema> cache = new ConcurrentHashMap<>();

    public static Schema getSchema(Server server) {
        return cache.get(server);
    }

    public static void addSchema(Server server, Schema schema) {
        cache.put(server, schema);
    }
}
