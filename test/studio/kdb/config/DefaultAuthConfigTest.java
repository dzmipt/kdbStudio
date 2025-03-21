package studio.kdb.config;

import org.junit.jupiter.api.Test;
import studio.core.Credentials;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class DefaultAuthConfigTest {

    @Test
    public void test() {
        DefaultAuthConfig c1 = new DefaultAuthConfig("a", new HashMap<>());
        DefaultAuthConfig c2 = new DefaultAuthConfig("a", new HashMap<>());

        assertEquals(c1, c2);

        c2 = new DefaultAuthConfig(c2, "empty", new Credentials("", ""));

        assertEquals(c1, c2);

        Map<String, Credentials> m1 = new HashMap<>();
        m1.put("a", new Credentials("au", "ap1"));
        m1.put("x", Credentials.DEFAULT);
        m1.put("b", new Credentials("bu", "bp"));

        Map<String, Credentials> m2 = new HashMap<>();
        m2.put("b", new Credentials("bu", "bp"));
        m2.put("a", new Credentials("au", "ap"));

        c1 = new DefaultAuthConfig("a", m1);
        c2 = new DefaultAuthConfig("a", m2);
        assertNotEquals(c1, c2);

        c1 = new DefaultAuthConfig(c1, "a", new Credentials("au","ap"));
        assertEquals(c1, c2);

        c2 = new DefaultAuthConfig(c2, "b");
        assertNotEquals(c1, c2);
    }
}
