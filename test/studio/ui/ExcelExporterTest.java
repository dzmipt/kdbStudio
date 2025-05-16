package studio.ui;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import studio.kdb.K;
import studio.kdb.ListModel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
                LocalTime.of(21,33,9,12_000_000)
        ));

        check("2003-11-29T21:33:09.012", new K.KTimestampVector(timestamp.toLong()));
        check("", new K.KTimestampVector(Long.MIN_VALUE));
    }

    @Test
    public void timesExportTest() {
        K.KTime time = K.KTime.of(LocalTime.of(11, 12, 34, 567_000_000));
        check("11:12:34.567", new K.KTimeVector(time.toInt()));
    }

    @Test
    @Disabled("Should be fixed")
    public void secondsExportTest() {
        K.KSecond time = K.KSecond.of(LocalTime.of(11, 12, 34));
        check("11:12:34", new K.KSecondVector(time.toInt()));
    }

}
