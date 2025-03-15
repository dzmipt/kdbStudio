package studio.kdb;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.ExecAllOption;
import studio.utils.MockConfig;
import studio.utils.TableConnExtractor;

import java.awt.*;
import java.io.*;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {

    private Config config;
    private Server server;

    @BeforeAll
    public static void mockConfig() throws IOException {
        MockConfig.mock();
    }

    @BeforeEach
    public void init() throws IOException {
        MockConfig.serversFile.delete();
        MockConfig.propertiesFile.delete();

        config = Config.getInstance();
        ((MockConfig)config).reload();

        System.out.println("temp file " + MockConfig.propertiesFile.getAbsolutePath());

        ServerTreeNode parent = config.getServerTree().add("testFolder");
        server = new Server("testServer", "localhost",1111,
                "user", "pwd", Color.WHITE, DefaultAuthenticationMechanism.NAME, false, parent);
    }

    @Test
    public void addServer() {
        ServerTreeNode parent = new ServerTreeNode().add("addServerTestFolder");
        Server server1 = new Server("testServer1", "localhost",1112,
                "user", "pwd", Color.WHITE, DefaultAuthenticationMechanism.NAME, false, parent);

        config.getServerConfig().addServers(false, server1);
        assertEquals(1, config.getServerConfig().getServerNames().size());
        assertEquals(1, config.getServerTree().getChild("addServerTestFolder").getChildCount() );
        assertEquals(server1, config.getServerTree().getChild("addServerTestFolder").getChild(0).getServer() );
    }


    @Test
    public void addServerDifferentTreeNode() {
        ServerTreeNode parent1 = new ServerTreeNode().add("addServerTestFolder");
        Server server1 = new Server("testServer1", "localhost",1112,
                "user", "pwd", Color.WHITE, DefaultAuthenticationMechanism.NAME, false, parent1);

        ServerTreeNode parent2 = new ServerTreeNode().add("addServerTestFolder");
        Server server2 = new Server("testServer2", "localhost",1113,
                "user", "pwd", Color.WHITE, DefaultAuthenticationMechanism.NAME, false, parent2);

        config.getServerConfig().addServers(false, server1, server2);
        assertEquals(2, config.getServerConfig().getServerNames().size());
        assertEquals(2, config.getServerTree().getChild("addServerTestFolder").getChildCount() );
    }

    @Test
    public void addServerSameName() {
        ServerTreeNode parent1 = config.getServerTree().add("sameNameTestFolder");
        Server server1 = new Server("testServer1", "localhost",1112,
                "user", "pwd", Color.WHITE, DefaultAuthenticationMechanism.NAME, false, parent1);

        //ServerTreeNode parent2 = config.getServerTree().add("sameNameTestFolder");
        Server server2 = new Server("testServer1", "localhost",1113,
                "user", "pwd", Color.WHITE, DefaultAuthenticationMechanism.NAME, false, parent1);

        config.getServerConfig().addServers(false, server1);
        assertThrows(IllegalArgumentException.class, ()->config.getServerConfig().addServer(server2) );

        assertEquals(1, config.getServerConfig().getServerNames().size());
        assertEquals(1, config.getServerTree().getChild("sameNameTestFolder").getChildCount() );
        assertEquals(server1.getPort(), config.getServerConfig().getServer("sameNameTestFolder/testServer1").getPort());
    }

    @Test
    public void testServerHistoryDepth() {
        int depth = config.getServerHistoryDepth();
        config.setServerHistoryDepth(depth+1);
        assertEquals(depth+1, config.getServerHistoryDepth());
    }

    @Test
    public void testServerHistory() {
        assertEquals(0, config.getServerHistory().size());

        config.getServerConfig().addServer(server);
        assertEquals(0, config.getServerHistory().size());
        config.addServerToHistory(server);
        assertEquals(1, config.getServerHistory().size());
        assertEquals(server, config.getServerHistory().get(0));
        config.addServerToHistory(server);
        assertEquals(1, config.getServerHistory().size());


        Server server1 = server.newName("testServer1");
        Server server2 = server.newName("testServer2");
        Server server3 = server.newName("testServer3");

        config.getServerConfig().addServers(false, server1, server2, server3);
        config.addServerToHistory(server1);
        config.addServerToHistory(server2);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server2, config.getServerHistory().get(0));
        assertEquals(server1, config.getServerHistory().get(1));
        assertEquals(server, config.getServerHistory().get(2));

        config.addServerToHistory(server1);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server1, config.getServerHistory().get(0));
        assertEquals(server2, config.getServerHistory().get(1));
        assertEquals(server, config.getServerHistory().get(2));

        config.addServerToHistory(server);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server, config.getServerHistory().get(0));
        assertEquals(server1, config.getServerHistory().get(1));
        assertEquals(server2, config.getServerHistory().get(2));

        config.addServerToHistory(server);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server, config.getServerHistory().get(0));
        assertEquals(server1, config.getServerHistory().get(1));
        assertEquals(server2, config.getServerHistory().get(2));

        config.setServerHistoryDepth(3);
        config.addServerToHistory(server3);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server3, config.getServerHistory().get(0));
        assertEquals(server, config.getServerHistory().get(1));
        assertEquals(server1, config.getServerHistory().get(2));
    }

    private Config getConfig(Properties properties) throws IOException {
        File newFile = File.createTempFile("studioforkdb", ".tmp");
        newFile.deleteOnExit();
        OutputStream out = new FileOutputStream(newFile);
        properties.store(out, null);
        out.close();

        return new Config(newFile.toPath());
    } 
    
    private Config copyConfig(Config config, Consumer<Properties> propsModification) throws IOException {
        config.saveToDisk();
        try (InputStream in = new FileInputStream(MockConfig.propertiesFile)) {
            Properties p = new Properties();
            p.load(in);
            propsModification.accept(p);
            return getConfig(p);
        }
    }

    @Test
    public void addServersTest() {
        Server server1 = server.newName("testServer1");
        Server server2 = server.newName("comma,name");
        Server server3 = server.newName("testServer1");

        assertThrows(IllegalArgumentException.class, ()->config.getServerConfig().addServers(false, server1, server2, server3));
        assertEquals(0, config.getServerConfig().getServers().length);

        String[] errors = config.getServerConfig().addServers(true, server1, server2, server3);
        assertNull(errors[0]);
        assertNotNull(errors[1]);
        assertNotNull(errors[2]);
        Collection<String> names = config.getServerConfig().getServerNames();
        assertEquals(1, names.size());
        assertTrue(names.contains(server1.getFullName()));
        ServerTreeNode serverTree = config.getServerTree();
        assertEquals(1, serverTree.getChildCount());
        assertEquals(1, serverTree.getChild(0).getChildCount());
    }

    @Test
    public void testExecAllOptions() throws IOException {
        assertEquals(ExecAllOption.Ask, config.getEnum(Config.EXEC_ALL));

        config.setEnum(Config.EXEC_ALL, ExecAllOption.Ignore);
        assertEquals(ExecAllOption.Ignore, config.getEnum(Config.EXEC_ALL));

        Config newConfig = copyConfig(config, p -> {});
        assertEquals(ExecAllOption.Ignore, newConfig.getEnum(Config.EXEC_ALL));

        newConfig = copyConfig(config, p -> {
            p.setProperty("execAllOption", "testValue");
        });

        assertEquals(ExecAllOption.Ask, newConfig.getEnum(Config.EXEC_ALL));

    }

    @Test
    public void testConnExtractor() throws IOException {
        TableConnExtractor extractor = config.getTableConnExtractor();
        assertNotNull(extractor);

        String conn = "someWords, words";
        String host = "hostWords, words";
        String port = "someWords, ports";

        config.setConnColWords(conn);
        config.setHostColWords(host);
        config.setPortColWords(port);
        config.setTableMaxConnectionPopup(100);
        assertNotEquals(extractor, config.getTableConnExtractor());

        Config newConfig = copyConfig(config, p -> {});
        assertEquals(conn, newConfig.getConnColWords());
        assertEquals(host, newConfig.getHostColWords());
        assertEquals(port, newConfig.getPortColWords());
        assertEquals(100, newConfig.getTableMaxConnectionPopup());
    }

    @Test
    public void testAutoSaveFlags() throws IOException {
        assertFalse(config.getBoolean(Config.AUTO_SAVE));
        assertEquals(ActionOnExit.SAVE, config.getEnum(Config.ACTION_ON_EXIT));

        config.setBoolean(Config.AUTO_SAVE, true);
        config.setEnum(Config.ACTION_ON_EXIT, ActionOnExit.CLOSE_ANONYMOUS_NOT_SAVED);
        Config newConfig = copyConfig(config, p -> {});
        assertTrue(newConfig.getBoolean(Config.AUTO_SAVE));
        assertEquals(ActionOnExit.CLOSE_ANONYMOUS_NOT_SAVED, newConfig.getEnum(Config.ACTION_ON_EXIT));
    }

    @Test
    public void testGetServerByConnectionString() {
        config.setDefaultAuthMechanism("testAuth");
        config.setDefaultCredentials("testAuth", new Credentials("testUser", "testPassword"));

        Server server = config.getServerByConnectionString("host:123");
        assertEquals("host", server.getHost());
        assertEquals(123, server.getPort());
        assertEquals("testAuth", server.getAuthenticationMechanism());
        assertEquals("testUser", server.getUsername());
        assertEquals("testPassword", server.getPassword());

        server = config.getServerByConnectionString("host:123:uu:pp");
        assertEquals(DefaultAuthenticationMechanism.NAME, server.getAuthenticationMechanism());
        assertEquals("uu", server.getUsername());
        assertEquals("pp", server.getPassword());

        server = config.getServerByConnectionString("host:123:uu");
        assertEquals(DefaultAuthenticationMechanism.NAME, server.getAuthenticationMechanism());
        assertEquals("uu", server.getUsername());
        assertEquals("", server.getPassword());

        server = config.getServerByConnectionString("host:123:uu:");
        assertEquals(DefaultAuthenticationMechanism.NAME, server.getAuthenticationMechanism());
        assertEquals("uu", server.getUsername());
        assertEquals("", server.getPassword());

        server = config.getServerByConnectionString("host:123:uu:pp:pp1:");
        assertEquals(DefaultAuthenticationMechanism.NAME, server.getAuthenticationMechanism());
        assertEquals("uu", server.getUsername());
        assertEquals("pp:pp1:", server.getPassword());

        server = config.getServerByConnectionString("host:123:uu::pp:pp1:");
        assertEquals(DefaultAuthenticationMechanism.NAME, server.getAuthenticationMechanism());
        assertEquals("uu", server.getUsername());
        assertEquals(":pp:pp1:", server.getPassword());
    }
}
