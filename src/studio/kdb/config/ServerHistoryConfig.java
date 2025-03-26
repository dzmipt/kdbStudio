package studio.kdb.config;

import org.apache.commons.collections4.list.UnmodifiableList;
import studio.kdb.Server;
import studio.utils.HistoricalList;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class ServerHistoryConfig {
    private final int depth;
    private final List<String> list;

    public ServerHistoryConfig(int depth, List<String> list) {
        this.depth = depth;
        this.list = UnmodifiableList.unmodifiableList(list);
    }

    public int getDepth() {
        return depth;
    }

    public List<String> get() {
        return list;
    }

    public HistoricalList<Server> toServerHistory(ServerConfig serverConfig) {
        HistoricalList<Server> serverHistory = new HistoricalList<>(depth);
        for (int i = list.size()-1; i>=0; i--) {
            serverHistory.add(serverConfig.getServer(list.get(i)));
        }
        return serverHistory;
    }

    public static ServerHistoryConfig fromServerHistory(HistoricalList<Server> serverHistory) {
        List<String> list = serverHistory
                                .stream()
                                .map(Server::getFullName)
                                .collect(Collectors.toList());
        return new ServerHistoryConfig(serverHistory.getDepth(), list);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerHistoryConfig)) return false;
        ServerHistoryConfig that = (ServerHistoryConfig) o;
        return depth == that.depth && Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), depth, list);
    }
}
