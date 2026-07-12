package studio.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QConnectionTest {

    @Test
    public void testBasic() {
        QConnection connection = QConnection.get("`:server.some.domain:11");
        assertEquals("server.some.domain", connection.getHost());
        assertEquals(11, connection.getPort());
        assertEquals("", connection.getUser());
        assertEquals("", connection.getPassword());

        assertEquals(connection, QConnection.get(":server.some.domain:11"));
        assertEquals(connection, QConnection.get("server.some.domain:11"));
        assertEquals(connection, QConnection.get("`:server.some.domain:11:"));
        assertEquals(connection, QConnection.get("`:server.some.domain:11::"));
        assertEquals(connection, QConnection.get("  `:server.some.domain:11   "));

        assertEquals(connection.changeTLS(true), QConnection.get("`:tcps://server.some.domain:11::"));
    }

    @Test
    public void testNoHost() {
        QConnection connection = QConnection.get("::123");
        assertEquals("", connection.getHost());
        assertEquals(123, connection.getPort());
    }

    @Test
    public void testUserPassword() {
        QConnection connection = QConnection.get(":host:1:uu:p");
        assertEquals("host", connection.getHost());
        assertEquals("uu", connection.getUser());
        assertEquals("p", connection.getPassword());

        connection = QConnection.get(":host:1::p");
        assertEquals("", connection.getUser());
        assertEquals("p", connection.getPassword());

        connection = QConnection.get(":host:1:uu:");
        assertEquals("uu", connection.getUser());
        assertEquals("", connection.getPassword());

        connection = QConnection.get(":host:1:uu");
        assertEquals("uu", connection.getUser());
        assertEquals("", connection.getPassword());

        connection = QConnection.get(":host:1:uu:pp:bb");
        assertEquals("uu", connection.getUser());
        assertEquals("pp:bb", connection.getPassword());
    }

    @Test
    public void testProtocol() {
        QConnection connection = QConnection.get(":tcps://host:1:uu:p");
        assertTrue(connection.isUseTLS());

        connection = QConnection.get(":tcp://host:1:uu:p");
        assertFalse(connection.isUseTLS());
    }

    @Test
    public void testErrors() {
        assertThrows(IllegalArgumentException.class, ()-> {
            QConnection.get(":something://host:1:uu:p");
        });
        assertThrows(IllegalArgumentException.class, ()-> {
            QConnection.get(":host:xx:uu:p");
        });
        assertThrows(IllegalArgumentException.class, ()-> {
            QConnection.get(":host::uu:p");
        });

        assertThrows(IllegalArgumentException.class, ()-> {
            QConnection.get(":tcps://tcp://host:100");
        });

    }

    @Test
    public void testSpecifiedProtocol() {
        assertTrue(new QConnection.Parser("`:tcps://host:100").isSpecifiedProtocol());
        assertTrue(new QConnection.Parser("`:tcp://host:100").isSpecifiedProtocol());
        assertFalse(new QConnection.Parser("`:host:100").isSpecifiedProtocol());

    }

    @Test
    public void testSpecifiedUser() {
        assertFalse(new QConnection.Parser("`:tcps://host:100").isSpecifiedUser());
        assertTrue(new QConnection.Parser("`:tcp://host:100:user").isSpecifiedUser());
        assertTrue(new QConnection.Parser("`:host:100:user:").isSpecifiedUser());
        assertTrue(new QConnection.Parser("`:host:100:").isSpecifiedUser());
    }

    @Test
    public void testSpecifiedPassword() {
        assertFalse(new QConnection.Parser("`:tcps://host:100").isSpecifiedPassword());
        assertFalse(new QConnection.Parser("`:tcp://host:100:user").isSpecifiedPassword());
        assertTrue(new QConnection.Parser("`:host:100:user:").isSpecifiedPassword());
        assertFalse(new QConnection.Parser("`:host:100:").isSpecifiedPassword());
    }

}