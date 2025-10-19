package studio.kdb;

import kx.IPC;
import kx.K4Exception;
import kx.KConnection;
import kx.KMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class KSerialiseWrongStringTest {
    private static KConnection kConn = null;

    private static final Logger log = LogManager.getLogger();

    @BeforeAll
    public static void connect() throws K4Exception, IOException, InterruptedException {
        String qTestPort = System.getenv("qTestPort");
        if (qTestPort == null) {
            log.info("qTestPort is not defined.");
            return;
        }
        int qPort = Integer.parseInt(qTestPort);

        log.info("Connection to port {}", qPort);
        kConn = new KConnection("localhost", qPort, false);

    }

    @AfterAll
    public static void exit() throws K4Exception, IOException, InterruptedException {
        if (kConn == null) return;;
        kConn.close();
    }

    @Test
    @EnabledIfEnvironmentVariable(named="qTestPort", matches="[0-9]+")
    public void testWrongStringTest() throws K4Exception, IOException, InterruptedException {
        KMessage message = kConn.k(new K.KString("`char$ 252 252"));
        assertNull(message.getError());

        K.KBase kBase = message.getObject();
        assertEquals(KType.CharVector, kBase.getType());


        K.KList func = new K.KList(
                new K.KString("{x~`char$ 252 252}"),
                kBase
        );

        KMessage message2 = kConn.k(func);
        assertNull(message2.getError());

        K.KBase kBase2 = message2.getObject();
        assertEquals(KType.Boolean, kBase2.getType());
        assertTrue(((K.KBoolean)kBase2).toBoolean());
    }

    @Test
    public void testDeserialisation() throws IOException {
        byte[] data = new byte[] {
                10, //type: string
                0, // attr
                0, 0, 0, 2, // len (int): 2
                (byte)252, (byte)252 // data - 2 bytes
        };

        KMessage message = IPC.deserialise(data, false, false);
        assertNull(message.getError());
        K.KBase base = message.getObject();
        assertEquals(KType.CharVector, base.getType());;
        K.KString str = (K.KString) base;

        assertEquals(2, str.count());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        str.serialise(out);

        assertArrayEquals(data, out.toByteArray());

    }

}
