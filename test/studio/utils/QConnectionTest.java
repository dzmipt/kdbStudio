package studio.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QConnectionTest {

    @Test
    public void testBasic() {
        QConnection connection = new QConnection("`:server.some.domain:11");
        assertEquals("server.some.domain", connection.getHost());
        assertEquals(11, connection.getPort());
        assertEquals("", connection.getUser());
        assertEquals("", connection.getPassword());

        assertEquals(connection, new QConnection(":server.some.domain:11"));
        assertEquals(connection, new QConnection("server.some.domain:11"));
        assertEquals(connection, new QConnection("`:server.some.domain:11:"));
        assertEquals(connection, new QConnection("`:server.some.domain:11::"));
        assertEquals(connection, new QConnection("  `:server.some.domain:11   "));

        assertEquals(connection.useTLS(true), new QConnection("`:tcps://server.some.domain:11::"));
    }

    @Test
    public void testNoHost() {
        QConnection connection = new QConnection("::123");
        assertEquals("", connection.getHost());
        assertEquals(123, connection.getPort());
    }

    @Test
    public void testUserPassword() {
        QConnection connection = new QConnection(":host:1:uu:p");
        assertEquals("host", connection.getHost());
        assertEquals("uu", connection.getUser());
        assertEquals("p", connection.getPassword());

        connection = new QConnection(":host:1::p");
        assertEquals("", connection.getUser());
        assertEquals("p", connection.getPassword());

        connection = new QConnection(":host:1:uu:");
        assertEquals("uu", connection.getUser());
        assertEquals("", connection.getPassword());

        connection = new QConnection(":host:1:uu");
        assertEquals("uu", connection.getUser());
        assertEquals("", connection.getPassword());

        connection = new QConnection(":host:1:uu:pp:bb");
        assertEquals("uu", connection.getUser());
        assertEquals("pp:bb", connection.getPassword());
    }

    @Test
    public void testProtocol() {
        QConnection connection = new QConnection(":tcps://host:1:uu:p");
        assertTrue(connection.isUseTLS());

        connection = new QConnection(":tcp://host:1:uu:p");
        assertFalse(connection.isUseTLS());
    }

    @Test
    public void testErrors() {
        assertThrows(IllegalArgumentException.class, ()-> {
            new QConnection(":something://host:1:uu:p");
        });
        assertThrows(IllegalArgumentException.class, ()-> {
            new QConnection(":host:xx:uu:p");
        });
        assertThrows(IllegalArgumentException.class, ()-> {
            new QConnection(":host::uu:p");
        });

    }
}