package studio.ui;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import studio.kdb.K;
import studio.kdb.ListModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExcelExporterTest {

    private void check(String expected, K.KBaseVector<? extends K.KBase> list) {
        ListModel model = new ListModel(list);
        Workbook w = ExcelExporter.buildWorkbook(model, null);
        assertEquals(expected, w.getSheetAt(0).getRow(1).getCell(0).toString());
    }

    @Test
    public void timestampExportTest() {
        check("2003-11-29T21:33:09.012", new K.KTimestampVector(123456789012345678L));
        check("", new K.KTimestampVector(Long.MIN_VALUE));
    }
}
