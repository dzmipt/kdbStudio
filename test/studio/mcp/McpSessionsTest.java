package studio.mcp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class McpSessionsTest {

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }

    @Test
    public void idleSessionsAreRemoved() {
        MutableClock clock = new MutableClock();
        McpSessions sessions = new McpSessions(clock);
        McpSessions.Session old = sessions.create("a", "1", "127.0.0.1");
        clock.now = clock.now.plus(Duration.ofHours(12));
        McpSessions.Session recent = sessions.create("b", "1", "127.0.0.1");
        clock.now = clock.now.plus(Duration.ofHours(13));
        sessions.create("c", "1", "127.0.0.1");
        assertNull(sessions.get(old.getId()));
        assertNotNull(sessions.get(recent.getId()));
        assertEquals(2, sessions.size());
    }

    @Test
    public void numberOfSessionsIsLimited() {
        MutableClock clock = new MutableClock();
        McpSessions sessions = new McpSessions(clock);
        McpSessions.Session first = sessions.create("first", "1", "127.0.0.1");
        for (int i = 0; i < McpSessions.MAX_SESSIONS + 10; i++) {
            clock.now = clock.now.plusSeconds(1);
            sessions.create("s" + i, "1", "127.0.0.1");
        }
        assertEquals(McpSessions.MAX_SESSIONS, sessions.size());
        assertNull(sessions.get(first.getId()));
    }
}
