package studio.utils;

import org.junit.jupiter.api.Test;
import studio.core.Credentials;
import studio.core.DefaultAuthenticationMechanism;
import studio.kdb.Server;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QPadConverterTest {

    private final Credentials cred = new Credentials("aUser", "aPassword");

    private Server convert(String line, String defaultAuth) {
        return QPadConverter.convert(line, defaultAuth, cred);
    }

    private Server convert(String line) {
        return convert(line, "auth");
    }

    @Test
    public void testDefaultAuthMethod() {
        Server server = convert("`:server.com:11223`name", "authMethod");
        assertEquals("server.com", server.getHost());
        assertEquals(11223, server.getPort());
        assertEquals("name", server.getName());
        assertEquals("authMethod", server.getAuthenticationMechanism());
        assertEquals(cred.getUsername(), server.getUsername());
        assertEquals(cred.getPassword(), server.getPassword());
    }
    @Test
    public void testTreeStructure() {
        Server server = convert("`:server.com:11223`Parent`folder`name");
        assertEquals("Parent/folder/name", server.getFullName());
        assertEquals("name", server.getName());
    }

    @Test
    public void testTreeStructure2() {
        Server server = convert("`:server.com:11223:user:password`Parent`folder`name");
        assertEquals("Parent/folder/name", server.getFullName());
        assertEquals("user", server.getUsername());
        assertEquals("password", server.getPassword());
        assertEquals(DefaultAuthenticationMechanism.NAME, server.getAuthenticationMechanism());
    }

    @Test
    public void testAuthMechanism() {
        Server server = convert("`:server.com:11223:user:testAuth?password`Parent`folder`name");
        assertEquals("Parent/folder/name", server.getFullName());
        assertEquals("user", server.getUsername());
        assertEquals("testAuth?password", server.getPassword());
        assertEquals("testAuth", server.getAuthenticationMechanism());
    }

    @Test
    public void testComment() {
        Server server = convert("#`:server.com:11223:user:auth?password`Parent`folder`name");
        assertEquals(Server.NO_SERVER, server);
    }

    @Test
    public void testEmpty() {
        Server server = convert("");
        assertEquals(Server.NO_SERVER, server);
    }

    @Test
    public void testWithoutName() {
        Server server = convert("`:server.com:11223");
        assertEquals(Server.NO_SERVER, server);
    }

    @Test
    public void testShouldStartWithBackTick() {
        Server server = convert("server.com:11223`root`folder`somename");
        assertEquals(Server.NO_SERVER, server);
    }

    @Test
    public void testColonInServerNameCouldBeOmitted() {
        Server server = convert("`server.com:11223`root`folder`somename");
        assertEquals("server.com", server.getHost());
        assertEquals(11223, server.getPort());
        assertEquals("root/folder/somename", server.getFullName());
    }

    @Test
    public void testInvalidPort() {
        Server server = convert("`:server.com:port`name");
        assertEquals(Server.NO_SERVER, server);
    }

    @Test
    public void testNoPort() {
        Server server = convert("`:server.com`name");
        assertEquals(Server.NO_SERVER, server);
    }

    @Test
    public void testNoPassword() {
        Server server = convert("`:server.com:11223:user`name");
        assertEquals("", server.getPassword());
        assertEquals("user", server.getUsername());
    }

    @Test
    public void testEmptyFolder() {
        Server server = convert("`:server.com:11223`folder``server");
        assertEquals("[empty]", server.getFolder().getFolder());
    }

    @Test
    public void testPasswordWithColon() {
        Server server = convert("`:server.com:11223:user:password:something`name");
        assertEquals("password:something", server.getPassword());
    }

}
