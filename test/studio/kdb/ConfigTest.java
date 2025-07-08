package studio.kdb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.config.ActionOnExit;
import studio.kdb.config.ExecAllOption;
import studio.utils.HistoricalList;
import studio.utils.MockConfig;
import studio.utils.QConnection;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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
        MockConfig.cleanupConfigs();

        config = Config.getInstance();
        ((MockConfig)config).reload();
        config.setDefaultAuthMechanism("testAuth");
        config.setDefaultCredentials("testAuth", new Credentials("testUser", "testPassword"));

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
    public void testServerHistory() {
        HistoricalList<Server> serverHistory = config.getServerHistory();

        assertEquals(0, serverHistory.size());

        config.getServerConfig().addServer(server);
        assertEquals(0, config.getServerHistory().size());
        serverHistory.add(server);

        assertEquals(1, config.getServerHistory().size());
        assertEquals(server, config.getServerHistory().get(0));

        serverHistory.add(server);
        assertEquals(1, config.getServerHistory().size());


        Server server1 = server.newName("testServer1");
        Server server2 = server.newName("testServer2");
        Server server3 = server.newName("testServer3");

        config.getServerConfig().addServers(false, server1, server2, server3);
        serverHistory.add(server1);
        serverHistory.add(server2);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server2, config.getServerHistory().get(0));
        assertEquals(server1, config.getServerHistory().get(1));
        assertEquals(server, config.getServerHistory().get(2));

        serverHistory.add(server1);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server1, config.getServerHistory().get(0));
        assertEquals(server2, config.getServerHistory().get(1));
        assertEquals(server, config.getServerHistory().get(2));

        serverHistory.add(server);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server, config.getServerHistory().get(0));
        assertEquals(server1, config.getServerHistory().get(1));
        assertEquals(server2, config.getServerHistory().get(2));

        serverHistory.add(server);
        assertEquals(3, config.getServerHistory().size());
        assertEquals(server, config.getServerHistory().get(0));
        assertEquals(server1, config.getServerHistory().get(1));
        assertEquals(server2, config.getServerHistory().get(2));

    }


    private Config getConfig(JsonObject json) throws IOException {
        Path newBase = MockConfig.createTempDir();
        try (Writer writer = Files.newBufferedWriter(newBase.resolve(Config.CONFIG_FILENAME))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(json, writer);
        }

        return new Config(newBase);
    }
    
    private Config copyConfig(Config config, Consumer<JsonObject> configModification) throws IOException {
        config.saveToDisk();
        try (Reader reader = Files.newBufferedReader(MockConfig.getBasePath().resolve(Config.CONFIG_FILENAME))) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            configModification.accept(json);
            return getConfig(json);
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

        newConfig = copyConfig(config, json -> {
            json.addProperty("execAllOption", "testValue");
        });

        assertEquals(ExecAllOption.Ask, newConfig.getEnum(Config.EXEC_ALL));
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
        config.getServerConfig().addServer(server);
        Server aServer = config.getServerByConnectionString("host:123");
        assertEquals("host", aServer.getHost());
        assertEquals(123, aServer.getPort());
        assertEquals("testAuth", aServer.getAuthenticationMechanism());
        assertEquals("testUser", aServer.getUsername());
        assertEquals("testPassword", aServer.getPassword());

        aServer = config.getServerByConnectionString("host:123:testUser:testPassword");
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());

        aServer = config.getServerByConnectionString("host:123:uu:pp");
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());
        assertEquals("uu", aServer.getUsername());
        assertEquals("pp", aServer.getPassword());

        aServer = config.getServerByConnectionString("host:123:uu");
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());
        assertEquals("uu", aServer.getUsername());
        assertEquals("", aServer.getPassword());

        aServer = config.getServerByConnectionString("host:123:uu:");
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());
        assertEquals("uu", aServer.getUsername());
        assertEquals("", aServer.getPassword());

        aServer = config.getServerByConnectionString("host:123:uu:pp:pp1:");
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());
        assertEquals("uu", aServer.getUsername());
        assertEquals("pp:pp1:", aServer.getPassword());

        aServer = config.getServerByConnectionString("host:123:uu::pp:pp1:");
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());
        assertEquals("uu", aServer.getUsername());
        assertEquals(":pp:pp1:", aServer.getPassword());
    }

    @Test
    public void testGetServerByNewAuthMethod() {
        config.getServerConfig().addServer(server);

        Server aServer = config.getServerByNewAuthMethod(new QConnection("localhost:1111:user:pwd"), "testAuth");
        assertEquals("testUser", aServer.getUsername());
        assertEquals("testPassword", aServer.getPassword());
        assertEquals("", aServer.getName());
        assertEquals("testAuth", aServer.getAuthenticationMechanism());

        aServer = config.getServerByNewAuthMethod(new QConnection("localhost:1111:user:p123"), DefaultAuthenticationMechanism.NAME);
        assertEquals("", aServer.getUsername());
        assertEquals("", aServer.getPassword());
        assertEquals("", aServer.getName());
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());
    }

    @Test
    public void testGetServer() {
        config.getServerConfig().addServer(server);

        Server aServer = config.getServer(new QConnection("localhost:1111:user:password"),  DefaultAuthenticationMechanism.NAME);
        assertEquals("user", aServer.getUsername());
        assertEquals("password", aServer.getPassword());
        assertEquals("", aServer.getName());
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());

        aServer = config.getServer(new QConnection("localhost:1111:user:pwd"),  DefaultAuthenticationMechanism.NAME);
        assertEquals("user", aServer.getUsername());
        assertEquals("pwd", aServer.getPassword());
        assertEquals("testServer", aServer.getName());
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());

        aServer = config.getServer(new QConnection("localhost:1111:user:p123"),  DefaultAuthenticationMechanism.NAME);
        assertEquals("user", aServer.getUsername());
        assertEquals("p123", aServer.getPassword());
        assertEquals("", aServer.getName());
        assertEquals(DefaultAuthenticationMechanism.NAME, aServer.getAuthenticationMechanism());


        aServer = config.getServer(new QConnection("localhost:1111:user:p123"), "testAuth");
        assertEquals("user", aServer.getUsername());
        assertEquals("p123", aServer.getPassword());
        assertEquals("", aServer.getName());
        assertEquals("testAuth", aServer.getAuthenticationMechanism());

        aServer = config.getServer(new QConnection("localhost:1111:testUser:p123"), "testAuth");
        assertEquals("testUser", aServer.getUsername());
        assertEquals("p123", aServer.getPassword());
        assertEquals("", aServer.getName());
        assertEquals("testAuth", aServer.getAuthenticationMechanism());

        aServer = config.getServer(new QConnection("localhost:1111:testUser:testPassword"), "testAuth");
        assertEquals("testUser", aServer.getUsername());
        assertEquals("testPassword", aServer.getPassword());
        assertEquals("", aServer.getName());
        assertEquals("testAuth", aServer.getAuthenticationMechanism());
    }

}
