package studio.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionSelectionTest {

    private ServerTreeNode root;

    private static Server server(String name, String host) {
        return new Server(name, host, 5000, "", "", null, "auth", false);
    }

    @BeforeEach
    public void init() {
        root = new ServerTreeNode();
        ServerTreeNode prod = root.add("prod");
        prod.add(server("hdb-eu", "eu1"));
        prod.add(server("rdb-eu", "eu2"));
        prod.add(server("hdb-us", "us1"));
        ServerTreeNode dev = root.add("dev");
        dev.add(server("hdb-dev", "d1"));
        dev.add("empty");
    }

    private static ServerTreeNode find(ServerTreeNode node, String label) {
        for (java.util.Enumeration<?> e = node.preorderEnumeration(); e.hasMoreElements();) {
            ServerTreeNode n = (ServerTreeNode) e.nextElement();
            String text = n.isFolder() ? n.getFolder() : n.getServer().getName();
            if (text.equals(label)) return n;
        }
        throw new IllegalArgumentException(label);
    }

    private static List<String> allowed(PermissionSelection selection, ServerTreeNode root) {
        return selection.getPermissions().allowedServers(root).stream().map(Server::getFullName).toList();
    }

    @Test
    public void filteredAllServersPermitsOnlyVisible() {
        PermissionSelection selection = new PermissionSelection(root, new McpPermissions(List.of()));
        ServerTreeNode view = root.filter("hdb");
        selection.toggle(view); // "All servers" in the filtered view
        assertEquals(List.of("prod/hdb-eu", "prod/hdb-us", "dev/hdb-dev"), allowed(selection, root));
        assertFalse(selection.getPermissions().getEntries().contains("folder:"));
        assertTrue(selection.isChecked(view));
        assertFalse(selection.isChecked(root));
        // dev folder is complete in the view (empty folder is hidden though) -> entries per server
        assertTrue(selection.getPermissions().getEntries().contains("server:dev/hdb-dev"));
    }

    @Test
    public void unfilteredFolderStoresFolderEntry() {
        PermissionSelection selection = new PermissionSelection(root, new McpPermissions(List.of("server:prod/hdb-eu")));
        selection.toggle(find(root, "prod"));
        assertEquals(Set.of("folder:prod"), selection.getPermissions().getEntries());
        assertTrue(selection.isImplied(find(root, "hdb-us")));

        selection.toggle(find(root, "hdb-us")); // implied - no change
        assertEquals(Set.of("folder:prod"), selection.getPermissions().getEntries());

        selection.toggle(find(root, "prod"));
        assertTrue(selection.getPermissions().getEntries().isEmpty());
    }

    @Test
    public void filteredFolderCompleteByFolderName() {
        PermissionSelection selection = new PermissionSelection(root, new McpPermissions(List.of()));
        ServerTreeNode view = root.filter("prod"); // the folder name matches -> the whole folder is visible
        selection.toggle(find(view, "prod"));
        assertEquals(Set.of("folder:prod"), selection.getPermissions().getEntries());
    }

    @Test
    public void filteredUncheckKeepsHiddenServers() {
        PermissionSelection selection = new PermissionSelection(root, new McpPermissions(List.of("folder:prod")));
        ServerTreeNode view = root.filter("eu");
        ServerTreeNode viewProd = find(view, "prod");
        assertTrue(selection.isChecked(viewProd));
        selection.toggle(viewProd);
        assertEquals(List.of("prod/hdb-us"), allowed(selection, root));
        assertFalse(selection.isChecked(viewProd));
    }

    @Test
    public void filteredServerToggle() {
        PermissionSelection selection = new PermissionSelection(root, new McpPermissions(List.of()));
        ServerTreeNode view = root.filter("us1");
        selection.toggle(find(view, "hdb-us"));
        assertEquals(Set.of("server:prod/hdb-us"), selection.getPermissions().getEntries());
        selection.toggle(find(view, "hdb-us"));
        assertTrue(selection.getPermissions().getEntries().isEmpty());
    }
}
