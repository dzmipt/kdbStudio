package kx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IPCTest {

    @Test
    public void errorDeserilisation() {
        byte[] data = new byte[] {-128, 116, 121, 112, 101, 0};
        KMessage message = IPC.deserialise(data, false, true);

        assertTrue(message.getError() instanceof K4Exception);
        assertEquals("'type", message.getError().getMessage());
    }
}
