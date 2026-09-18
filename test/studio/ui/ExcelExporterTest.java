package studio.ui;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import studio.kdb.*;

import java.time.*;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class ExcelExporterTest {

    @BeforeAll
    public static void setTimestamp() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Kolkata")));
    }

    @BeforeEach
    public void exportTemporalValuesAsStringsByDefault() {
        Config.getInstance().setBoolean(Config.EXCEL_EXPORT_TEMPORAL_AS_DATE_TIME, false);
    }

    @AfterEach
    public void resetTemporalExportSetting() {
        Config.getInstance().setBoolean(Config.EXCEL_EXPORT_TEMPORAL_AS_DATE_TIME, true);
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

    @Test
    public void numericValuesExportAsExcelNumbers() {
        assertNumericCell(new K.KShortVector((short) 12), 12);
        assertNumericCell(new K.KIntVector(34), 34);
        assertNumericCell(new K.KLongVector(56), 56);
        assertNumericCell(new K.KFloatVector(7.5f), 7.5);
        assertNumericCell(new K.KDoubleVector(8.25), 8.25);
    }

    @Test
    public void booleanValuesExportAsExcelBooleans() {
        Workbook workbook = ExcelExporter.buildWorkbook(new ListModel(new K.KBooleanVector(true, false)), null);
        Cell trueCell = workbook.getSheetAt(0).getRow(1).getCell(0);
        Cell falseCell = workbook.getSheetAt(0).getRow(2).getCell(0);

        assertEquals(CellType.BOOLEAN, trueCell.getCellType());
        assertTrue(trueCell.getBooleanCellValue());
        assertEquals(CellType.BOOLEAN, falseCell.getCellType());
        assertFalse(falseCell.getBooleanCellValue());
    }

    @Test
    public void temporalValuesExportAsFormattedExcelDatesAndTimes() {
        Config.getInstance().setBoolean(Config.EXCEL_EXPORT_TEMPORAL_AS_DATE_TIME, true);
        double excelZeroDate = DateUtil.getExcelDate(K.ZERO_DATE);
        K.KDate date = K.KDate.of(LocalDate.of(2025, 5, 16));
        K.KMonth month = K.KMonth.of(LocalDate.of(2025, 5, 1));
        K.KTimestamp timestamp = K.KTimestamp.of(LocalDateTime.of(2003, 11, 29, 21, 33, 9, 12_000_000));
        K.KTime time = K.KTime.of(LocalTime.of(11, 12, 34, 567_000_000));
        K.KMinute minute = K.KMinute.of(LocalTime.of(11, 12));
        K.KSecond second = K.KSecond.of(LocalTime.of(11, 12, 34));
        K.KTimespan timespan = K.KTimespan.of(Duration.ofHours(10).plusMinutes(7).plusSeconds(23).plusMillis(123));

        assertTemporalCell(new K.KDateVector(date.toInt()), DateUtil.getExcelDate(date.toLocalDate()), "2025-05-16");
        assertTemporalCell(new K.KMonthVector(month.toInt()), DateUtil.getExcelDate(month.toLocalDateTime()), "2025-05");
        assertTemporalCell(new K.KDatetimeVector(1.5), excelZeroDate + 1.5, "2000-01-02 12:00:00.000");
        assertTemporalCell(new K.KTimestampVector(timestamp.toLong()), excelZeroDate + timestamp.toLong() / (double) K.NS_IN_DAY,
                "2003-11-29 21:33:09.012");
        assertTemporalCell(new K.KTimeVector(time.toInt()), time.toInt() / (double) K.MS_IN_DAY, "11:12:34.567");
        assertTemporalCell(new K.KMinuteVector(minute.toInt()), minute.toInt() / (24.0 * 60), "11:12");
        assertTemporalCell(new K.KSecondVector(second.toInt()), second.toInt() / (24.0 * 60 * 60), "11:12:34");
        assertTemporalCell(new K.KTimespanVector(timespan.toLong()), timespan.toLong() / (double) K.NS_IN_DAY, "10:07:23.123");
    }

    @Test
    public void finiteNumberCheckAcceptsOnlyFiniteNumericValues() {
        assertTrue(ExcelExporter.isFiniteNumber(new K.KByte((byte) 1)));
        assertTrue(ExcelExporter.isFiniteNumber(new K.KShort((short) 2)));
        assertTrue(ExcelExporter.isFiniteNumber(new K.KInteger(3)));
        assertTrue(ExcelExporter.isFiniteNumber(new K.KLong(4)));
        assertTrue(ExcelExporter.isFiniteNumber(new K.KFloat(5.5f)));
        assertTrue(ExcelExporter.isFiniteNumber(new K.KDouble(6.5)));

        assertFalse(ExcelExporter.isFiniteNumber(new K.KShort(Short.MIN_VALUE)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KInteger(Integer.MIN_VALUE)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KLong(Long.MIN_VALUE)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KFloat(Float.NaN)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KDouble(Double.NaN)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KDouble(Double.POSITIVE_INFINITY)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KInteger(Integer.MAX_VALUE)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KDate(0)));
        assertFalse(ExcelExporter.isFiniteNumber(new K.KSymbol("text")));
    }

    private void assertNumericCell(K.KBaseVector<? extends K.KBase> values, double expected) {
        Workbook workbook = ExcelExporter.buildWorkbook(new ListModel(values), null);
        Cell cell = workbook.getSheetAt(0).getRow(1).getCell(0);
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(expected, cell.getNumericCellValue());
    }

    private void assertTemporalCell(K.KBaseVector<? extends K.KBase> values, double expectedValue, String expectedText) {
        Workbook workbook = ExcelExporter.buildWorkbook(new ListModel(values), null);
        Cell cell = workbook.getSheetAt(0).getRow(1).getCell(0);
        assertEquals(CellType.NUMERIC, cell.getCellType());
        assertEquals(expectedValue, cell.getNumericCellValue(), 1e-10);
        assertEquals(expectedText, new DataFormatter(Locale.US).formatCellValue(cell));
    }

}
