package studio.ui;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import studio.kdb.K;
import studio.kdb.KFormatContext;

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

    public static void exportTableX(final JFrame frame, final JTable table, final File file,
                             final boolean openIt) {

        final TableModel model = table.getModel();
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
                if (!b.isNull()) {
                    Class<?> columnClass = model.getColumnClass(j);
                    if (columnClass == K.KSymbolVector.class) {
                        cell.setCellValue(((K.KSymbol)b).s);
                    } else if (columnClass == K.KDateVector.class) {
                        cell.setCellValue(sd("yyyy-MM-dd", ((K.KDate) b).toDate()));
                    } else if (columnClass == K.KTimeVector.class) {
                        cell.setCellValue(sd("HH:mm:ss.SSS", ((K.KTime) b).toTime()));
                    } else if (columnClass == K.KTimestampVector.class) {
                        char[] cs = sd("yyyy-MM-dd HH:mm:ss.SSS",
                                ((K.KTimestamp) b).toTimestamp()).toCharArray();
                        cs[10] = 'T';
                        cell.setCellValue(new String(cs));
                    } else if (columnClass == K.KMonthVector.class) {
                        cell.setCellValue(sd("yyyy-MM", ((K.Month) b).toDate()));
                    } else if (columnClass == K.KMinuteVector.class) {
                        cell.setCellValue(sd("HH:mm", ((K.Minute) b).toDate()));
                    } else if (columnClass == K.KSecondVector.class) {
                        cell.setCellValue(sd("HH:mm:ss", ((K.Second) b).toDate()));
                    } else if (columnClass == K.KBooleanVector.class) {
                        cell.setCellValue(((K.KBoolean) b).b ? 1 : 0);
                    } else if (columnClass == K.KDoubleVector.class) {
                        cell.setCellValue(((K.KDouble) b).toDouble());
                    } else if (columnClass == K.KFloatVector.class) {
                        cell.setCellValue(((K.KFloat) b).f);
                    } else if (columnClass == K.KLongVector.class) {
                        cell.setCellValue(((K.KLong) b).toLong());
                    } else if (columnClass == K.KIntVector.class) {
                        cell.setCellValue(((K.KInteger) b).toInt());
                    } else if (columnClass == K.KShortVector.class) {
                        cell.setCellValue(((K.KShort) b).s);
                    } else if (columnClass == K.KCharacterVector.class) {
                        cell.setCellValue(((K.KCharacter) b).c);
                    } else {
                        cell.setCellValue(b.toString(KFormatContext.NO_TYPE));
                    }
                } else {
                    cell.setCellValue("");
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
