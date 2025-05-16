package studio.ui;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import studio.kdb.K;
import studio.kdb.KType;
import studio.kdb.ListModel;
import studio.kdb.Parser;

import java.time.*;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExcelExporterTest {

    @BeforeAll
    public static void setTimestamp() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Kolkata")));
    }

    private void check(String expected, K.KBaseVector<? extends K.KBase> list) {
        ListModel model = new ListModel(list);
        Workbook w = ExcelExporter.buildWorkbook(model, null);
        assertEquals(expected, w.getSheetAt(0).getRow(1).getCell(0).toString());
    }

    @Test
    public void timestampExportTest() {
        K.KTimestamp timestamp = K.KTimestamp.of(LocalDateTime.of(
                LocalDate.of(2003,11,29),
                LocalTime.of(21,33,9,12_123_456)
        ));

        check("2003-11-29T21:33:09.012123456", new K.KTimestampVector(timestamp.toLong()));

        timestamp = K.KTimestamp.of(LocalDateTime.of(
                LocalDate.of(1950,11,29),
                LocalTime.of(21,33,9,12_000_000)
        ));

        check("1950-11-29T21:33:09.012000000", new K.KTimestampVector(timestamp.toLong()));


        check("", new K.KTimestampVector(Long.MIN_VALUE));
    }

    @Test
    public void timespanExportTest() {
        Duration duration = Duration.ofHours(10).plusMinutes(7)
                .plusSeconds(23).plusNanos(123456789);
        K.KTimespan timespan = K.KTimespan.of(duration);

        check("0D10:07:23.123456789", new K.KTimespanVector(timespan.toLong()));
        check("-0D10:07:23.123456789", new K.KTimespanVector(-timespan.toLong()));

        duration = duration.plusDays(2);
        timespan = K.KTimespan.of(duration);
        check("2D10:07:23.123456789", new K.KTimespanVector(timespan.toLong()));
        check("-2D10:07:23.123456789", new K.KTimespanVector(-timespan.toLong()));
    }

    @Test
    public void timesExportTest() {
        K.KTime time = K.KTime.of(LocalTime.of(11, 12, 34, 567_000_000));
        check("11:12:34.567", new K.KTimeVector(time.toInt()));
        check("-11:12:34.567", new K.KTimeVector(-time.toInt()));
    }

    @Test
    public void secondsExportTest() {
        K.KSecond time = K.KSecond.of(LocalTime.of(11, 12, 34));
        check("11:12:34", new K.KSecondVector(time.toInt()));
        check("-11:12:34", new K.KSecondVector(-time.toInt()));

    }

    @Test
    public void dateExportTest() {
        LocalDate localDate = LocalDate.of(2025, 5,16);
        K.KDate date = K.KDate.of(localDate);
        check("2025-05-16", new K.KDateVector(date.toInt()));

        localDate = LocalDate.of(2005, 5,16);
        date = K.KDate.of(localDate);
        check("2005-05-16", new K.KDateVector(date.toInt()));

        localDate = LocalDate.of(1950, 5,16);
        date = K.KDate.of(localDate);
        check("1950-05-16", new K.KDateVector(date.toInt()));

    }

    @Test
    public void monthExportTest() {
        LocalDate localDate = LocalDate.of(2025, 5,16);
        K.KMonth month = K.KMonth.of(localDate);
        check("2025-05", new K.KMonthVector(month.toInt()));

        localDate = LocalDate.of(2005, 5,16);
        month = K.KMonth.of(localDate);
        check("2005-05", new K.KMonthVector(month.toInt()));

        localDate = LocalDate.of(1950, 5,16);
        month = K.KMonth.of(localDate);
        check("1950-05", new K.KMonthVector(month.toInt()));
    }


    @Test
    public void minuteExportTest() {
        K.KMinute minute = K.KMinute.of(LocalTime.of(11, 12, 34));
        check("11:12", new K.KMinuteVector(minute.toInt()));
        check("-11:12", new K.KMinuteVector(-minute.toInt()));
    }

    @Test
    public void dateTimeExportTest() {
        double value = Parser.parse(KType.Datetime, "2025.05.16T16:26:31.123");
        check("2025-05-16T16:26:31.123", new K.KDatetimeVector(value));

        value = Parser.parse(KType.Datetime, "2000.05.16T16:26:31.123");
        check("2000-05-16T16:26:31.123", new K.KDatetimeVector(value));

        value = Parser.parse(KType.Datetime, "1950.05.16T16:26:31.123");
        check("1950-05-16T16:26:31.123", new K.KDatetimeVector(value));
    }

}
