package studio.kdb;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ParserTest {

    @Test
    public void testLocalDateTime() {
        LocalDateTime dt = LocalDateTime.of(2024,12,1,0,0);
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.01D00:00:00.000000000"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.01D00:00:00.000"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.01D00:00:00"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.01D00:00:00"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.01"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12m"));

        dt = LocalDateTime.of(2024,12,9,0,0);
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.09"));

        dt = LocalDateTime.of(2024,12,9,16,47,13,123_000_000);
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.09T16:47:13.123000000"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.09T16:47:13.123"));
        assertEquals(dt, Parser.parseLocalDateTime("2024.12.09T16:47:13.12300"));

        assertNull(Parser.parseLocalDateTime("2024.02.30T16:47:13.12300"));
        assertNull(Parser.parseLocalDateTime("2023.02.29T16:47:13.12300"));
        assertNull(Parser.parseLocalDateTime("2023.02.25T24:00:00.000000000"));
        assertNull(Parser.parseLocalDateTime("2023.00.01T01:00:00.000000000"));
    }

    @Test
    public void testDuration() {
        Duration duration = Duration.ofDays(1);
        assertEquals(duration, Parser.parseDuration("1D"));
        assertEquals(duration, Parser.parseDuration("1D00:00:00.000000000"));
        assertEquals(duration, Parser.parseDuration("24:00:00.000"));
        assertEquals(duration, Parser.parseDuration("24:00:00"));
        assertEquals(duration, Parser.parseDuration("24:00"));
        assertEquals(duration, Parser.parseDuration("+24:00"));

        duration = Duration.ZERO.minusMinutes(15).minusNanos(123_000_000);
        assertEquals(duration, Parser.parseDuration("-0D00:15:00.123"));
        assertEquals(duration, Parser.parseDuration("-0D00:15:00.123000000"));
        assertEquals(duration, Parser.parseDuration("-00:15:00.123000000"));
    }


    private void checkParsing(KType type, String expected, String text) {
        double value = Parser.parse(type, text);

        String formatted = new KFormat(type).format(value, new StringBuffer(), null).toString();

        assertEquals(expected, formatted, "assert for type: " + type);
    }

    @Test
    public void testParseTimestamp() {
        KType type = KType.Timestamp;
        checkParsing(type,"2024.12.09D18:37:53.123456000", "2024.12.09D18:37:53.123456000"); // doesn't work till ns precision
        checkParsing(type, "2024.12.09D18:37:53.123000064", "2024.12.09D18:37:53.123"); // inaccuracy in doubles
        checkParsing(type, "2024.12.09D18:37:53.000000000", "2024.12.09D18:37:53");
        checkParsing(type, "2024.12.09D18:37:00.000000000", "2024.12.09D18:37");
        checkParsing(type, "2024.12.09D00:00:00.000000000", "2024.12.09");
        checkParsing(type, "2024.12.01D00:00:00.000000000", "2024.12");

        checkParsing(type, "1950.12.09D18:37:53.123456000", "1950.12.09D18:37:53.123456000"); // doesn't work till ns precision
        checkParsing(type, "1950.12.09D18:37:53.123000064", "1950.12.09D18:37:53.123"); // inaccuracy in doubles
        checkParsing(type, "1950.12.09D18:37:53.000000000", "1950.12.09D18:37:53");
        checkParsing(type, "1950.12.09D18:37:00.000000000", "1950.12.09D18:37");
        checkParsing(type, "1950.12.09D00:00:00.000000000", "1950.12.09");
        checkParsing(type, "1950.12.01D00:00:00.000000000", "1950.12");
    }

    @Test
    public void testParseDatetime() {
        KType type = KType.Datetime;
        checkParsing(type,"2024.12.09T18:37:53.123", "2024.12.09D18:37:53.123456000");
        checkParsing(type, "2024.12.09T18:37:53.123", "2024.12.09D18:37:53.123");
        checkParsing(type, "2024.12.09T18:37:53.000", "2024.12.09D18:37:53");
        checkParsing(type, "2024.12.09T18:37:00.000", "2024.12.09D18:37");
        checkParsing(type, "2024.12.09T00:00:00.000", "2024.12.09");
        checkParsing(type, "2024.12.01T00:00:00.000", "2024.12");

        checkParsing(type, "1950.12.09T18:37:53.123", "1950.12.09D18:37:53.123456000");
        checkParsing(type, "1950.12.09T18:37:53.123", "1950.12.09D18:37:53.123");
        checkParsing(type, "1950.12.09T18:37:53.000", "1950.12.09D18:37:53");
        checkParsing(type, "1950.12.09T18:37:00.000", "1950.12.09D18:37");
        checkParsing(type, "1950.12.09T00:00:00.000", "1950.12.09");
        checkParsing(type, "1950.12.01T00:00:00.000", "1950.12");
    }

    @Test
    public void testParseDate() {
        KType type = KType.Date;
        checkParsing(type,"2024.12.09D18:37:53.123456000", "2024.12.09D18:37:53.123456000");
        checkParsing(type, "2024.12.09D18:37:53.123000064", "2024.12.09D18:37:53.123");
        checkParsing(type, "2024.12.09D18:37:53.000000000", "2024.12.09D18:37:53");
        checkParsing(type, "2024.12.09D18:37:00.000000000", "2024.12.09D18:37");
        checkParsing(type, "2024.12.09", "2024.12.09");
        checkParsing(type, "2024.12.01", "2024.12");

        checkParsing(type, "1950.12.09D18:37:53.123456256", "1950.12.09D18:37:53.123456000");
        checkParsing(type, "1950.12.09D18:37:53.123000064", "1950.12.09D18:37:53.123");
        checkParsing(type, "1950.12.09D18:37:53.000000256", "1950.12.09D18:37:53");
        checkParsing(type, "1950.12.09D18:37:00.000000000", "1950.12.09D18:37");
        checkParsing(type, "1950.12.09", "1950.12.09");
        checkParsing(type, "1950.12.01", "1950.12");
    }

    @Test
    public void testParseMonth() {
        KType type = KType.Month;
        // demonstrate big inaccuracy
        checkParsing(type,"2024.12.09D18:37:53.123455984", "2024.12.09D18:37:53.123456000");
        checkParsing(type, "2024.12.09D18:37:53.122999997", "2024.12.09D18:37:53.123");
        checkParsing(type, "2024.12.09D18:37:52.999999980", "2024.12.09D18:37:53");
        checkParsing(type, "2024.12.09D18:37:00.000000046", "2024.12.09D18:37");
        checkParsing(type, "2024.12.08D23:59:59.999999980", "2024.12.09");
        checkParsing(type, "2024.12", "2024.12");

        checkParsing(type, "1950.12.09D18:37:53.123455984", "1950.12.09D18:37:53.123456000");
        checkParsing(type, "1950.12.09D18:37:53.123000150", "1950.12.09D18:37:53.123");
        checkParsing(type, "1950.12.09D18:37:52.999999980", "1950.12.09D18:37:53");
        checkParsing(type, "1950.12.09D18:37:00.000000046", "1950.12.09D18:37");
        checkParsing(type, "1950.12.08D23:59:59.999999980", "1950.12.09");
        checkParsing(type, "1950.12", "1950.12");
    }

    @Test
    public void testParseTimespan() {
        KType type = KType.Timespan;

        checkParsing(type,"1D00:00:00.000000000", "1D");
        checkParsing(type,"1D10:15:00.000000000", "1D10:15");
        checkParsing(type,"0D15:38:07.123456000", "15:38:07.123456000");
        checkParsing(type,"0D15:38:07.017000000", "15:38:07.017000000");
        checkParsing(type,"0D15:38:00.000000000", "15:38:00");
        checkParsing(type,"0D15:38:00.000000000", "15:38");

        checkParsing(type,"-0D00:15:00.123000000", "-00:15:00.123");
        checkParsing(type,"-1D10:15:00.000000000", "-1D10:15");
    }

    @Test
    public void testParseTime() {
        KType type = KType.Time;

        checkParsing(type,"24:00:00.000", "1D");
        checkParsing(type,"34:15:00.000", "1D10:15");
        checkParsing(type,"15:38:07.123456000", "15:38:07.123456000");
        checkParsing(type,"15:38:07.017", "15:38:07.017000000");
        checkParsing(type,"15:38:00.000", "15:38:00");
        checkParsing(type,"15:38:00.000", "15:38");

        checkParsing(type,"-00:15:00.123", "-00:15:00.123");
        checkParsing(type,"-34:15:00.000", "-1D10:15");
    }


    @Test
    public void testParseMinute() {
        KType type = KType.Minute;

        checkParsing(type,"24:00", "1D");
        checkParsing(type,"34:15", "1D10:15");
        checkParsing(type,"15:38:07.123456000", "15:38:07.123456000");
        checkParsing(type,"15:38:07.017000000", "15:38:07.017000000");
        checkParsing(type,"15:38", "15:38:00");
        checkParsing(type,"15:38", "15:38");

        checkParsing(type,"-00:15:00.123000000", "-00:15:00.123");
        checkParsing(type,"-34:15", "-1D10:15");
    }


    @Test
    public void testParseSecond() {
        KType type = KType.Second;

        checkParsing(type,"24:00:00", "1D");
        checkParsing(type,"34:15:00", "1D10:15");
        checkParsing(type,"15:38:07.123456000", "15:38:07.123456000");
        checkParsing(type,"15:38:07.017000000", "15:38:07.017000000");
        checkParsing(type,"15:38:00", "15:38:00");
        checkParsing(type,"15:38:00", "15:38");

        checkParsing(type,"-00:15:00.123000000", "-00:15:00.123");
        checkParsing(type,"-34:15:00", "-1D10:15");
    }

}
