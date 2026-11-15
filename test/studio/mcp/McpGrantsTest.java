package studio.mcp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class McpGrantsTest {

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    @Test
    public void grantExpires() {
        MutableClock clock = new MutableClock();
        McpGrants grants = new McpGrants(clock);
        AtomicInteger events = new AtomicInteger();
        grants.addListener(events::incrementAndGet);

        assertFalse(grants.useGrant("k"));
        grants.grant("k", "label", Duration.ofMinutes(5));
        assertTrue(grants.useGrant("k"));
        assertTrue(grants.useGrant("k"));
        assertEquals(2, grants.getActive().get(0).getQueryCount());

        clock.now = clock.now.plus(Duration.ofMinutes(5));
        assertFalse(grants.useGrant("k"));
        assertTrue(grants.getActive().isEmpty());
        assertTrue(events.get() > 0);
    }

    @Test
    public void untilExitAndRevoke() {
        MutableClock clock = new MutableClock();
        McpGrants grants = new McpGrants(clock);
        grants.grant("a", "A", null);
        grants.grant("b", "B", Duration.ofHours(1));
        clock.now = clock.now.plus(Duration.ofDays(10));
        assertEquals(1, grants.getActive().size());
        assertTrue(grants.useGrant("a"));

        assertTrue(grants.revoke("a"));
        assertFalse(grants.revoke("a"));
        assertFalse(grants.useGrant("a"));

        grants.grant("c", "C", null);
        grants.revokeAll();
        assertTrue(grants.getActive().isEmpty());
    }
}
