package studio.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class McpPermissionsTest {

    private ServerTreeNode root, a, ab, abx, ac;
    private Server s1, s2, s3, s4, rootServer;

    private static Server server(String name) {
        return new Server(name, "host", 5000, "u", "p", null, "auth", false);
    }

    // ServerTreeNode.add(Server) returns the node before the server is re-parented; take the inserted child
    private static Server add(ServerTreeNode folder, Server server) {
        folder.add(server);
        return folder.getChild(folder.getChildCount() - 1).getServer();
    }

    @BeforeEach
    public void init() {
        root = new ServerTreeNode();
        a = root.add("a");
        ab = a.add("b");
        abx = ab.add("x");
        ac = a.add("c");
        s1 = add(ab, server("Server"));
        s2 = add(abx, server("Other"));
        s3 = add(ac, server("Server"));
        s4 = add(a, server("aServer"));
        rootServer = add(root, server("top"));
    }

    @Test
    public void entries() {
        assertEquals("folder:", McpPermissions.entry(root));
        assertEquals("folder:a/b", McpPermissions.entry(ab));
        assertEquals("server:a/b/Server", McpPermissions.serverEntry(s1));
        assertEquals("server:top", McpPermissions.serverEntry(rootServer));
    }

    @Test
    public void folderPermitsAllDescendants() {
        McpPermissions p = new McpPermissions(List.of("folder:a/b"));
        assertTrue(p.isAllowed(s1));
        assertTrue(p.isAllowed(s2));
        assertFalse(p.isAllowed(s3));
        assertFalse(p.isAllowed(s4));
        assertFalse(p.isAllowed(rootServer));
        assertTrue(p.isAllowed(abx));
        assertFalse(p.isAllowed(a));
    }

    @Test
    public void newServerInPermittedFolder() {
        McpPermissions p = new McpPermissions(List.of("folder:a/b"));
        Server added = add(abx.add("deep"), server("New"));
        assertTrue(p.isAllowed(added));
    }

    @Test
    public void serverPermission() {
        McpPermissions p = new McpPermissions(List.of("server:a/b/Server"));
        assertTrue(p.isAllowed(s1));
        assertFalse(p.isAllowed(s3));
        assertFalse(p.isAllowed(s2));
    }

    @Test
    public void rootPermitsEverything() {
        McpPermissions p = new McpPermissions(List.of("folder:"));
        for (Server s: List.of(s1, s2, s3, s4, rootServer)) {
            assertTrue(p.isAllowed(s), s.getFullName());
        }
    }

    @Test
    public void serverOutsideTreeIsNotPermitted() {
        McpPermissions p = new McpPermissions(List.of("folder:"));
        assertFalse(p.isAllowed(server("adhoc")));
        assertFalse(p.isAllowed((Server) null));
    }

    @Test
    public void movedServerLosesPermission() {
        McpPermissions p = new McpPermissions(List.of("folder:a/b"));
        ab.remove(s1);
        Server moved = add(ac, server("Moved"));
        assertFalse(p.isAllowed(moved));
        assertTrue(new McpPermissions(List.of("folder:a")).isAllowed(moved));
    }

    @Test
    public void allowedServers() {
        McpPermissions p = new McpPermissions(List.of("folder:a/b", "server:top"));
        List<String> names = p.allowedServers(root).stream().map(Server::getFullName).toList();
        assertEquals(List.of("a/b/x/Other", "a/b/Server", "top"), names); // tree order
    }

    @Test
    public void cleanup() {
        McpPermissions p = new McpPermissions(List.of("folder:a/b", "server:a/c/Server", "folder:gone", "server:a/missing"));
        McpPermissions cleaned = p.cleanup(root);
        assertEquals(Set.of("folder:a/b", "server:a/c/Server"), cleaned.getEntries());

        assertSame(cleaned, cleaned.cleanup(root));

        a.remove(ab);
        assertEquals(Set.of("server:a/c/Server"), cleaned.cleanup(root).getEntries());
    }
}
