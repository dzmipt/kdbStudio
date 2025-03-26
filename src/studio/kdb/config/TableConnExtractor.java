package studio.kdb.config;

import studio.kdb.K;
import studio.kdb.KFormatContext;

import javax.swing.table.TableModel;
import java.util.*;
import java.util.regex.Pattern;

public class TableConnExtractor {

    private final int maxConn;
    private final List<String> connWords;
    private final List<String> hostWords;
    private final List<String> portWords;

    //@TODO: The regexp can't be supported.
    private static final String hostRegex = "(([a-zA-Z0-9][a-zA-Z0-9\\-]*((\\.[a-zA-Z0-9][a-zA-Z0-9\\-]*)*\\.[a-zA-Z][a-zA-Z0-9]*)?)|" +
                                            "([0-9]{1,3}(\\.[0-9]{1,3}){3,3}))";
    private static final String portRegex = "[0-9]{1,5}";

    private static final Pattern connectionPattern = Pattern.compile("`?:?" + hostRegex + ":" + portRegex + "(:[^:]*(:[^:]*)?)?");
    private static final Pattern hostPattern = Pattern.compile("`?:?" + hostRegex);
    private static final Pattern portPattern = Pattern.compile("`?:?" + portRegex);

    public final static TableConnExtractor DEFAULT = new TableConnExtractor(20,
            List.of("server", "host", "connection", "handle"),
            List.of("server", "host"),
            List.of("port"));

    public TableConnExtractor(int maxConn, List<String> connWords, List<String> hostWords, List<String> portWords) {
        this.maxConn = maxConn;
        this.connWords = connWords;
        this.hostWords = hostWords;
        this.portWords = portWords;
    }

    public List<String> getHostWords() {
        return hostWords;
    }

    public List<String> getPortWords() {
        return portWords;
    }

    public List<String> getConnWords() {
        return connWords;
    }

    public int getMaxConn() {
        return maxConn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableConnExtractor)) return false;
        TableConnExtractor that = (TableConnExtractor) o;
        return maxConn == that.maxConn && Objects.equals(hostWords, that.hostWords) && Objects.equals(portWords, that.portWords) && Objects.equals(connWords, that.connWords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostWords, portWords, connWords, maxConn);
    }

    private static boolean contains(String header, List<String> words) {
        for (String word:words) {
            if (header.contains(word)) return true;
        }
        return false;
    }

    private static boolean match(Pattern pattern, String value) {
        return pattern.matcher(value).matches();
    }

    private static String getValue(TableModel model, int row, int col) {
        return ((K.KBase)model.getValueAt(row, col)).toString(KFormatContext.NO_TYPE);
    }

    public String[] getConnections(TableModel model, int row, int col) {
        List<String> conns = new ArrayList<>();
        List<String> hosts = new ArrayList<>();
        List<String> ports = new ArrayList<>();

        int count = model.getColumnCount();
        for (int aCol = 0; aCol<count; aCol++) {
            String header = model.getColumnName(aCol).toLowerCase();
            String value = getValue(model, row, aCol);

            if (contains(header, connWords) && match(connectionPattern, value)) {
                conns.add(value);
            }

            if (contains(header, hostWords) && match(hostPattern, value)) {
                hosts.add(value);
            }

            if (contains(header, portWords) && match(portPattern, value)) {
                ports.add(value);
            }
        }

        Set<String> result = new LinkedHashSet<>();

        String value = getValue(model, row, col);
        if (match(connectionPattern, value)) {
            result.add(value);
        }

        result.addAll(conns);
        if (result.size() < maxConn) {
            for (String host: hosts) {
                if (result.size()>=maxConn) break;
                for (String port: ports) {
                    result.add( host + ":" + port);
                }
            }
        }

        return result.stream().limit(maxConn).toArray(String[]::new);
    }
}
