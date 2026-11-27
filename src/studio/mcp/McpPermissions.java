package studio.mcp;

import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import java.util.*;

/**
 * Servers and folders permitted for AI agents: selected nodes of the server tree and a filter.
 * Entries are stored as "folder:&lt;path&gt;" (the root folder is "folder:") or "server:&lt;full name&gt;".
 * A server is permitted if its own entry or an entry of any of its ancestor folders is present,
 * and it matches the filter. The filter is the same as the filter of the server tree: words separated by spaces;
 * every word (case-insensitive) should be found in a folder name on the path to the server or in "name (host:port)"
 * of the server. The filter is evaluated on the current server tree, so it applies to servers added later too.
 */
public class McpPermissions {

    public static final String FOLDER = "folder:";
    public static final String SERVER = "server:";

    private final Set<String> entries;
    private final String filter;
    private final List<String> words;

    public McpPermissions(Collection<String> entries) {
        this(entries, "");
    }

    public McpPermissions(Collection<String> entries, String filter) {
        this.entries = Collections.unmodifiableSet(new LinkedHashSet<>(entries));
        this.filter = filter == null ? "" : filter.trim();
        words = this.filter.isEmpty() ? List.of() :
                Arrays.stream(this.filter.toLowerCase().split("\\s+")).collect(java.util.stream.Collectors.toList());
    }

    public Set<String> getEntries() {
        return entries;
    }

    public String getFilter() {
        return filter;
    }

    /**
     * Does the server match the filter (the same rule as ServerTreeNode.filter())
     */
    public boolean matchesFilter(Server server) {
        if (words.isEmpty()) return true;
        List<String> texts = new ArrayList<>();
        List<String> folderPath = server.getFolderPath();
        for (int i = 1; i < folderPath.size(); i++) texts.add(folderPath.get(i).toLowerCase());
        texts.add(server.getDescription(false).toLowerCase());
        for (String word: words) {
            boolean found = false;
            for (String text: texts) {
                if (text.contains(word)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static String entry(ServerTreeNode node) {
        if (node.isFolder()) return folderEntry(node.getFolderPath());
        return serverEntry(node.getServer());
    }

    public static String serverEntry(Server server) {
        return SERVER + server.getFullName();
    }

    // folderPath as returned by ServerTreeNode.getFolderPath(): the first element is the root
    public static String folderEntry(List<String> folderPath) {
        return FOLDER + String.join("/", folderPath.subList(Math.min(1, folderPath.size()), folderPath.size()));
    }

    public boolean contains(ServerTreeNode node) {
        return entries.contains(entry(node));
    }

    /**
     * Checks whether the server (which should be from the server tree) is permitted.
     * Servers outside the server tree are never permitted by this method.
     */
    public boolean isAllowed(Server server) {
        if (server == null || !server.inServerTree()) return false;
        if (!matchesFilter(server)) return false;
        return isSelected(server);
    }

    // The server or one of its ancestor folders is selected (the filter is not checked)
    private boolean isSelected(Server server) {
        if (entries.contains(serverEntry(server))) return true;

        List<String> folderPath = server.getFolderPath();
        for (int i = 1; i <= folderPath.size(); i++) {
            if (entries.contains(folderEntry(folderPath.subList(0, i)))) return true;
        }
        return false;
    }

    /**
     * For servers: is the server permitted. For folders: is the folder selected directly or via an ancestor
     */
    public boolean isAllowed(ServerTreeNode node) {
        if (! node.isFolder()) return isAllowed(node.getServer());
        List<String> folderPath = node.getFolderPath();
        for (int i = 1; i <= folderPath.size(); i++) {
            if (entries.contains(folderEntry(folderPath.subList(0, i)))) return true;
        }
        return false;
    }

    /**
     * Permitted servers from the tree in the depth-first order
     */
    public List<Server> allowedServers(ServerTreeNode root) {
        List<Server> result = new ArrayList<>();
        for (Enumeration<?> e = root.preorderEnumeration(); e.hasMoreElements();) {
            ServerTreeNode node = (ServerTreeNode) e.nextElement();
            if (node.isFolder()) continue;
            Server server = withParent(node);
            if (isAllowed(server)) result.add(server);
        }
        return result;
    }

    private static Server withParent(ServerTreeNode node) {
        Server server = node.getServer();
        ServerTreeNode parent = (ServerTreeNode) node.getParent();
        if (parent != null && server.getParent() != parent) {
            server = server.newParent(parent);
        }
        return server;
    }

    private static Set<String> allEntries(ServerTreeNode root) {
        Set<String> all = new HashSet<>();
        for (Enumeration<?> e = root.preorderEnumeration(); e.hasMoreElements();) {
            ServerTreeNode node = (ServerTreeNode) e.nextElement();
            if (node.isFolder()) all.add(entry(node));
            else all.add(serverEntry(withParent(node)));
        }
        return all;
    }

    /**
     * Removes entries for servers and folders which are no longer in the tree.
     * @return this instance if nothing is changed
     */
    public McpPermissions cleanup(ServerTreeNode root) {
        Set<String> all = allEntries(root);
        List<String> left = new ArrayList<>();
        for (String entry: entries) {
            if (all.contains(entry)) left.add(entry);
        }
        if (left.size() == entries.size()) return this;
        return new McpPermissions(left, filter);
    }

    public List<String> toList() {
        return new ArrayList<>(entries);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof McpPermissions)) return false;
        McpPermissions that = (McpPermissions) o;
        return entries.equals(that.entries) && filter.equals(that.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, filter);
    }

    @Override
    public String toString() {
        return filter.isEmpty() ? entries.toString() : entries + " filter '" + filter + "'";
    }
}
