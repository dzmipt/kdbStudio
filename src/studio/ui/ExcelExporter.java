package studio.ui;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import studio.kdb.K;
import studio.kdb.KFormatContext;
import studio.ui.action.QueryResult;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

class ExcelExporter {
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat();
    static {
        FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private static synchronized String sd(String s, java.util.Date x) {
        FORMATTER.applyPattern(s);
        return FORMATTER.format(x);
    }

    public static void exportTableX(final JFrame frame, final ResultTab tab, final File file,
                                    final boolean openIt) {
        if (tab == null) return;

        ResultGrid grid = tab.getGrid();
        if (grid == null) return;


        final TableModel model = grid.getTable().getModel();
        final String message = "Exporting data to " + file.getAbsolutePath();
        final String note = "0% complete";
        String title = "Studio for kdb+";
        UIManager.put("ProgressMonitor.progressText", title);

        final int min = 0;
        final int max = 100;
        final ProgressMonitor pm = new ProgressMonitor(frame, message, note, min, max);
        pm.setMillisToDecideToPopup(100);
        pm.setMillisToPopup(100);
        pm.setProgress(0);

        Runnable runner = () -> {
            try {
                Workbook workbook = buildWorkbook(model, pm);
                addDetails(workbook, tab);

                FileOutputStream fileOut = new FileOutputStream(file);
                workbook.write(fileOut);
                fileOut.close();
                workbook.close();
                if ((!pm.isCanceled()) && openIt) {
                    openTable(file);
                }
            } catch (Exception e) {
                StudioOptionPane.showError("\nThere was an error encoding the K types into Excel types.\n\n" +
                                e.getMessage() + "\n\n",
                        "Studio for kdb+");
            } finally {
                pm.close();
            }
        };

        Thread t = new Thread(runner);
        t.setName("Excel Exporter");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    public static Workbook buildWorkbook(TableModel model, ProgressMonitor pm) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("KDB Studio Query");
        Row headerRow = sheet.createRow(0);
        CellStyle headerCellStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerCellStyle.setFont(headerFont);
        for (int i = 0; i < model.getColumnCount(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(model.getColumnName(i));
            cell.setCellStyle(headerCellStyle);
        }
        int maxRow = model.getRowCount();
        int lastProgress = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            Row row = sheet.createRow(i + 1);
            for (int j = 0; j < model.getColumnCount(); j++) {
                Cell cell = row.createCell(j);
                K.KBase b = (K.KBase) model.getValueAt(i, j);
                if (b.isNull()) {
                    cell.setCellValue("");
                } else {
                    cell.setCellValue(b.toString(KFormatContext.EXCEL));
                }
            }

            if (pm != null) {
                if (pm.isCanceled()) {
                    return null;
                } else {
                    final int progress = (100 * i) / maxRow;
                    if (progress > lastProgress) {
                        lastProgress = progress;
                        final String note1 = "" + progress + "% complete";
                        SwingUtilities.invokeLater(() -> {
                            pm.setProgress(progress);
                            pm.setNote(note1);
                        });

                        Thread.yield();
                    }
                }
            }
        }
        return workbook;
    }

    private static void addDetails(Workbook workbook, ResultTab tab) {
        QueryResult queryResult = tab.getQueryResult();
        String server = null;
        String query = null;
        if (queryResult != null) {
            if (queryResult.getServer() != null) server = queryResult.getServer().getConnectionString();
            query = queryResult.getQuery();
        }
        if (query != null && server != null) {
            Sheet sheet = workbook.createSheet("Query Details");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Server:");
            row.createCell(1).setCellValue(server);
            row = sheet.createRow(1);
            row.createCell(0).setCellValue("Query:");
            row.createCell(1).setCellValue(query);
        }

    }

    public static void openTable(File file) {
        try {
            Runtime run = Runtime.getRuntime();
            String lcOSName = System.getProperty("os.name").toLowerCase();
            boolean MAC_OS_X = lcOSName.startsWith("mac os x");
            Process p = null;
            if (MAC_OS_X) {
                p = run.exec("open " + file);
            } else {
                run.exec("cmd.exe /c start " + file);
            }
        } catch (IOException e) {
            StudioOptionPane.showError("\nThere was an error opening excel.\n\n" + e.getMessage() +
                            "\n\nPerhaps you do not have Excel installed,\nor .xls files are not associated with Excel",
                    "Studio for kdb+");
        }
    }
}
