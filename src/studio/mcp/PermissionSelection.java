package studio.mcp;

import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import java.util.*;

/**
 * Editing of permissions with check boxes in a (possibly filtered) view of the server tree.
 * View nodes are nodes of the tree shown to the user: the full tree or a filtered copy of it.
 * A folder entry is stored only when the whole folder is visible; when a folder is checked in a filtered
 * view, only the visible servers (or fully visible sub-folders) are permitted.
 */
public class PermissionSelection {

    private final ServerTreeNode root; // the full tree
    private final Set<String> entries = new LinkedHashSet<>();

    public PermissionSelection(ServerTreeNode root, McpPermissions permissions) {
        this.root = root;
        entries.addAll(permissions.cleanup(root).getEntries());
    }

    public McpPermissions getPermissions() {
        return new McpPermissions(entries);
    }

    // The node of the full tree for the view node; null if not found
    private ServerTreeNode fullNode(ServerTreeNode viewNode) {
        if (viewNode.isFolder()) return root.findPath(viewNode.getFolderPath(), false);
        ServerTreeNode viewParent = (ServerTreeNode) viewNode.getParent();
        if (viewParent == null) return null;
        ServerTreeNode fullParent = root.findPath(viewParent.getFolderPath(), false);
        if (fullParent == null) return null;
        return fullParent.getChild(viewNode.getServer());
    }

    private static int countNodes(ServerTreeNode node) {
        int count = 1;
        for (ServerTreeNode child: node.childNodes()) count += countNodes(child);
        return count;
    }

    // Is the whole subtree of the full node visible in the view
    private boolean isComplete(ServerTreeNode viewNode) {
        ServerTreeNode full = fullNode(viewNode);
        return full != null && countNodes(full) == countNodes(viewNode);
    }

    private static List<ServerTreeNode> visibleServers(ServerTreeNode viewNode) {
        List<ServerTreeNode> result = new ArrayList<>();
        for (Enumeration<?> e = viewNode.preorderEnumeration(); e.hasMoreElements();) {
            ServerTreeNode node = (ServerTreeNode) e.nextElement();
            if (!node.isFolder()) result.add(node);
        }
        return result;
    }

    private static Server serverOf(ServerTreeNode node) {
        Server server = node.getServer();
        ServerTreeNode parent = (ServerTreeNode) node.getParent();
        if (parent != null && server.getParent() != parent) server = server.newParent(parent);
        return server;
    }

    private boolean isServerAllowed(McpPermissions permissions, ServerTreeNode serverNode) {
        return permissions.isAllowed(serverOf(serverNode));
    }

    /**
     * Checked: the node is permitted, or (for a folder) all its visible servers are permitted
     */
    public boolean isChecked(ServerTreeNode viewNode) {
        McpPermissions permissions = getPermissions();
        if (!viewNode.isFolder()) return isServerAllowed(permissions, viewNode);
        if (permissions.isAllowed(viewNode)) return true;
        List<ServerTreeNode> servers = visibleServers(viewNode);
        if (servers.isEmpty()) return false;
        for (ServerTreeNode server: servers) {
            if (!isServerAllowed(permissions, server)) return false;
        }
        return true;
    }

    /**
     * The node is permitted by a checked ancestor folder, so it can't be changed by itself
     */
    public boolean isImplied(ServerTreeNode viewNode) {
        McpPermissions permissions = getPermissions();
        if (permissions.contains(viewNode)) return false;
        ServerTreeNode parent = (ServerTreeNode) viewNode.getParent();
        return parent != null && permissions.isAllowed(parent);
    }

    public void toggle(ServerTreeNode viewNode) {
        if (isImplied(viewNode)) return;
        if (isChecked(viewNode)) uncheck(viewNode);
        else check(viewNode);
    }

    private void removeSubtreeEntries(ServerTreeNode fullNode) {
        for (Enumeration<?> e = fullNode.preorderEnumeration(); e.hasMoreElements();) {
            ServerTreeNode node = (ServerTreeNode) e.nextElement();
            entries.remove(node.isFolder() ? McpPermissions.entry(node) : McpPermissions.serverEntry(serverOf(node)));
        }
    }

    private void check(ServerTreeNode viewNode) {
        if (!viewNode.isFolder()) {
            entries.add(McpPermissions.serverEntry(serverOf(viewNode)));
            return;
        }
        if (isComplete(viewNode)) {
            ServerTreeNode full = fullNode(viewNode);
            removeSubtreeEntries(full);
            entries.add(McpPermissions.entry(full));
            return;
        }
        for (ServerTreeNode child: viewNode.childNodes()) {
            if (!isChecked(child)) check(child);
        }
    }

    private void uncheck(ServerTreeNode viewNode) {
        if (!viewNode.isFolder()) {
            entries.remove(McpPermissions.serverEntry(serverOf(viewNode)));
            return;
        }
        ServerTreeNode full = fullNode(viewNode);
        if (full == null) return;
        boolean folderEntry = entries.contains(McpPermissions.entry(full));
        removeSubtreeEntries(full);
        if (folderEntry) {
            // keep permissions for the hidden part of the folder
            keepHidden(full, viewNode);
        }
    }

    // Permits the parts of the full node which are not visible in the view node (view node can be null)
    private void keepHidden(ServerTreeNode fullNode, ServerTreeNode viewNode) {
        if (viewNode == null) {
            entries.add(fullNode.isFolder() ? McpPermissions.entry(fullNode) : McpPermissions.serverEntry(serverOf(fullNode)));
            return;
        }
        if (!fullNode.isFolder()) return; // visible server - unchecked
        for (ServerTreeNode child: fullNode.childNodes()) {
            keepHidden(child, findViewChild(viewNode, child));
        }
    }

    private static ServerTreeNode findViewChild(ServerTreeNode viewNode, ServerTreeNode fullChild) {
        for (ServerTreeNode child: viewNode.childNodes()) {
            if (child.isFolder() != fullChild.isFolder()) continue;
            if (child.isFolder() ? child.getFolder().equals(fullChild.getFolder())
                                 : child.getServer().getName().equals(fullChild.getServer().getName())) return child;
        }
        return null;
    }
}
