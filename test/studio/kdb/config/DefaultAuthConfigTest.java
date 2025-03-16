package studio.kdb.config;

import org.junit.jupiter.api.Test;
import studio.core.Credentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DefaultAuthConfigTest {

    @Test
    public void test() {
        DefaultAuthConfig c1 = new DefaultAuthConfig();
        c1.setDefaultAuth("a");
        DefaultAuthConfig c2 = new DefaultAuthConfig();
        c2.setDefaultAuth("a");

        assertEquals(c1, c2);

        c2.setCredentials("empty", new Credentials("", ""));

        assertEquals(c1, c2);

        c1.setCredentials("a", new Credentials("au", "ap1"));
        c1.setCredentials("x", Credentials.DEFAULT);
        c1.setCredentials("b", new Credentials("bu", "bp"));

        c2.setCredentials("b", new Credentials("bu", "bp"));
        c2.setCredentials("a", new Credentials("au", "ap"));

        assertNotEquals(c1, c2);

        c1.setCredentials("a", new Credentials("au","ap"));
        assertEquals(c1, c2);

        c2.setDefaultAuth("b");
        assertNotEquals(c1, c2);
    }
}
