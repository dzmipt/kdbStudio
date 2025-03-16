package studio.kdb.config;

import studio.kdb.K;
import studio.kdb.KFormatContext;

import javax.swing.table.TableModel;
import java.util.*;
import java.util.regex.Pattern;

public class TableConnExtractor {

    private String[] hostWords = new String[0];
    private String[] portWords = new String[0];
    private String[] connWords = new String[0];

    //@TODO: The regexp can't be supported.
    private static final String hostRegex = "(([a-zA-Z0-9][a-zA-Z0-9\\-]*((\\.[a-zA-Z0-9][a-zA-Z0-9\\-]*)*\\.[a-zA-Z][a-zA-Z0-9]*)?)|" +
                                            "([0-9]{1,3}(\\.[0-9]{1,3}){3,3}))";
    private static final String portRegex = "[0-9]{1,5}";

    private static final Pattern connectionPattern = Pattern.compile("`?:?" + hostRegex + ":" + portRegex + "(:[^:]*(:[^:]*)?)?");
    private static final Pattern hostPattern = Pattern.compile("`?:?" + hostRegex);
    private static final Pattern portPattern = Pattern.compile("`?:?" + portRegex);

    private final static int MAX_CONNECTIONS = 20;
    private int maxConn = MAX_CONNECTIONS;

    public void setHostWords(String[] hostWords) {
        this.hostWords = hostWords;
    }

    public void setPortWords(String[] portWords) {
        this.portWords = portWords;
    }

    public void setConnWords(String[] connWords) {
        this.connWords = connWords;
    }

    public void setMaxConn(int maxConn) {
        this.maxConn = maxConn<=0 ? MAX_CONNECTIONS : maxConn;
    }

    public String[] getHostWords() {
        return hostWords;
    }

    public String[] getPortWords() {
        return portWords;
    }

    public String[] getConnWords() {
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
        return maxConn == that.maxConn && Arrays.equals(hostWords, that.hostWords) && Arrays.equals(portWords, that.portWords) && Arrays.equals(connWords, that.connWords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(hostWords), Arrays.hashCode(portWords), Arrays.hashCode(connWords), maxConn);
    }

    private static boolean contains(String header, String[] words) {
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
