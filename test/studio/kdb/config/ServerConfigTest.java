package studio.kdb.config;

import org.junit.jupiter.api.Test;
import studio.kdb.Server;
import studio.kdb.ServerTreeNode;
import studio.utils.FileConfig;
import studio.utils.QConnection;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerConfigTest {

    public void assertDeepEquals(ServerTreeNode node1, ServerTreeNode node2) {
        assertEquals(node1.isFolder(), node2.isFolder());

        if (node1.isFolder()) {
            assertEquals(node1.getFolder(), node2.getFolder());
            assertEquals(node1.getChildCount(), node2.getChildCount());

            for (int i=0; i< node1.getChildCount(); i++) {
                assertDeepEquals(node1.getChild(i), node2.getChild(i));
            }
        } else {
            assertEquals(node1.getServer(), node2.getServer());
        }
    }

    public void testWriteRead(ServerTreeNode node) throws IOException {
        File file = File.createTempFile("serverConfig", "json");
        file.delete();
        ServerConfig config = new ServerConfig(new FileConfig(file.toPath()));
        config.setRoot(node);
        FileConfig.saveAllOnDisk();

        ServerConfig config2 = new ServerConfig(new FileConfig(file.toPath()));
        assertDeepEquals(node, config2.getServerTree());
    }
    private Server server =
            new Server("name", "host", 1234,
                    "uuser", "pwd", new Color(1,2,3),"auth", true);
    @Test
    public void testEmpty() throws IOException {
        testWriteRead(new ServerTreeNode());
    }

    @Test
    public void testOneServer() throws IOException {
        ServerTreeNode root = new ServerTreeNode();
        root.add(server);
        testWriteRead(root);
    }

    @Test
    public void testComplexServer() throws IOException {
        Server s2 = server.newName("name2");
        Server s3 = s2.newAuthMethod("auth2");
        Server s4 = s3.newFolder(new ServerTreeNode("someFolder"));

        ServerTreeNode root = new ServerTreeNode();
        root.add(server);
        root.add(s2);
        root.add(s3);
        root.add(s4);
        testWriteRead(root);
    }

    @Test
    public void testGetByQConnection() throws IOException {
        File file = File.createTempFile("serverConfig", "json");
        file.delete();
        ServerConfig config = new ServerConfig(new FileConfig(file.toPath()));
        assertEquals(0, config.getServers().length);

        QConnection conn = new QConnection("host", 1234, "uuser", "pwd",true);
        Server aServer = config.getServer(conn, "auth");
        assertEquals("host", aServer.getHost());
        assertEquals(1234, aServer.getPort());
        assertEquals("uuser", aServer.getUsername());
        assertEquals("pwd", aServer.getPassword());
        assertTrue(aServer.getUseTLS());
        assertEquals("auth", aServer.getAuthenticationMechanism());
        assertEquals("", aServer.getName());
        assertEquals(Color.WHITE, aServer.getBackgroundColor());


        config.addServer(server);
        assertEquals(1, config.getServers().length);
        aServer = config.getServer(conn, "auth");
        assertEquals(new Color(1,2,3), aServer.getBackgroundColor());
        assertEquals("name", aServer.getName());

    }

}
