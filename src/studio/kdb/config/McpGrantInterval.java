package studio.kdb.config;

import java.time.Duration;

// Interval for "allow further queries" in the MCP query confirmation dialog
public enum McpGrantInterval {
    MIN_5("5 minutes", Duration.ofMinutes(5)),
    MIN_15("15 minutes", Duration.ofMinutes(15)),
    HOUR_1("1 hour", Duration.ofHours(1)),
    HOUR_4("4 hours", Duration.ofHours(4)),
    UNTIL_EXIT("until Studio exits", null);

    private final String description;
    private final Duration duration;

    McpGrantInterval(String description, Duration duration) {
        this.description = description;
        this.duration = duration;
    }

    // null means unlimited (until Studio exits)
    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return description;
    }
}
