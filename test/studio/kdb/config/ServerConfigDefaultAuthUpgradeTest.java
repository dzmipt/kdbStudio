package studio.kdb.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.kdb.Config;
import studio.kdb.Server;
import studio.utils.FileConfig;
import studio.utils.MockConfig;
import studio.utils.QConnection;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ServerConfigDefaultAuthUpgradeTest {

    private final DefaultAuthConfig defaults = new DefaultAuthConfig("authA",
            Map.of("authA", new Credentials("userA", "pwdA"),
                   "authB", new Credentials("userB", "pwdB")));

    private Server server(String auth, String user, String password) {
        return new Server("s", new QConnection("host", 1, user, password, false), auth, Color.WHITE);
    }

    private Server convert(Server server) {
        return ServerConfig.convertToDefaultAuth(server, defaults);
    }

    @Test
    public void defaultAuthWithDefaultCredentials() {
        Server s = convert(server("authA", "userA", "pwdA"));
        assertTrue(s.isDefaultAuthMethod());
        assertTrue(s.isDefaultCredentials());
        assertEquals(Color.WHITE, s.getBackgroundColor());
    }

    @Test
    public void otherAuthWithDefaultCredentials() {
        Server s = convert(server("authB", "userB", "pwdB"));
        assertEquals("authB", s.getAuthenticationMechanism());
        assertTrue(s.isDefaultCredentials());
    }

    @Test
    public void defaultAuthWithOwnCredentialsIsNotChanged() {
        Server s = server("authA", "own", "ownPwd");
        assertSame(s, convert(s));
    }

    @Test
    public void credentialsOfAnotherAuthAreNotChanged() {
        Server s = server("authB", "userA", "pwdA");
        assertSame(s, convert(s));
    }

    @Test
    public void emptyCredentialsWithoutConfiguredDefaults() {
        // no default credentials for authC: empty credentials are the default ones
        Server s = convert(server("authC", "", ""));
        assertEquals("authC", s.getAuthenticationMechanism());
        assertTrue(s.isDefaultCredentials());
    }

    @Test
    public void convertedServerIsResolvedToTheSameValues() {
        for (Server s: new Server[] {server("authA", "userA", "pwdA"), server("authB", "userB", "pwdB"),
                                     server("authA", "own", "ownPwd")}) {
            Server resolved = defaults.resolve(convert(s));
            assertEquals(s.getAuthenticationMechanism(), resolved.getAuthenticationMechanism());
            assertEquals(s.getConnection(), resolved.getConnection());
        }
    }

    // ---------------- upgrade on start up

    private final static String STUDIO_CONFIG = ("{'version': 'V2_4', 'defaultAuthConfig': {" +
            "'default': 'Plain', " +
            "'Plain': {'user': 'defUser', 'password': 'defPwd'}" +
            "}}").replace('\'', '"');

    private final static String SERVERS_V1 = ("{'version': 1, 'name': '', 'children': [" +
            "{'name': 'default', 'handle': '`:host:1:defUser:defPwd', 'authMethod': 'Plain'}," +
            "{'name': 'own', 'handle': '`:host:2:own:pwd', 'authMethod': 'Plain'}," +
            "{'name': 'other', 'handle': '`:host:3', 'authMethod': 'other'}," +
            "{'name': 'folder', 'children': [" +
                "{'name': 'inner', 'handle': '`:host:4:defUser:defPwd', 'bgColor': '123456', 'authMethod': 'Plain'}" +
            "]}" +
            "]}").replace('\'', '"');

    private Path basePath;

    @BeforeEach
    public void init() throws IOException {
        basePath = MockConfig.createTempDir();
        Files.writeString(basePath.resolve(Config.CONFIG_FILENAME), STUDIO_CONFIG);
    }

    private Config load() {
        Config config = new Config(basePath) {};
        FileConfig.saveAllOnDisk();
        return config;
    }

    @Test
    public void upgradeFromVersion1() throws IOException {
        Files.writeString(basePath.resolve(Config.SERVERCONFIG_FILENAME), SERVERS_V1);

        ServerConfig serverConfig = load().getServerConfig();
        assertTrue(serverConfig.getServer("default").isDefaultAuthMethod());
        assertTrue(serverConfig.getServer("folder/inner").isDefaultAuthMethod());
        assertEquals(new Color(0x123456), serverConfig.getServer("folder/inner").getBackgroundColor());

        Server own = serverConfig.getServer("own");
        assertFalse(own.isDefaultCredentials());
        assertEquals("own", own.getUsername());

        Server other = serverConfig.getServer("other");
        assertEquals("other", other.getAuthenticationMechanism());
        assertTrue(other.isDefaultCredentials());

        String servers = Files.readString(basePath.resolve(Config.SERVERCONFIG_FILENAME));
        assertEquals(ServerConfig.VERSION, ServerTreeNodeSerializer.getVersion(servers));
        assertFalse(servers.contains("defPwd"));

        // the next start up doesn't convert again
        ServerConfig serverConfig2 = load().getServerConfig();
        assertEquals(serverConfig.getServer("own"), serverConfig2.getServer("own"));
        assertTrue(serverConfig2.getServer("default").isDefaultAuthMethod());
    }

    @Test
    public void currentVersionIsNotConverted() throws IOException {
        Files.writeString(basePath.resolve(Config.SERVERCONFIG_FILENAME),
                SERVERS_V1.replace("\"version\": 1", "\"version\": " + ServerConfig.VERSION));

        ServerConfig serverConfig = load().getServerConfig();
        Server server = serverConfig.getServer("default");
        assertFalse(server.isDefaultAuthMethod());
        assertEquals("defUser", server.getUsername());
    }
}
