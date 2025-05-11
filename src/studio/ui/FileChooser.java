package studio.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import studio.kdb.Config;
import studio.kdb.FileChooserConfig;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileChooser {

    private final static Logger log = LogManager.getLogger();

    public final static FileFilter Q_FF = new FileNameExtensionFilter("q script", "q");
    public final static FileFilter CSV_FF = new FileNameExtensionFilter("csv (Comma delimited)", "csv");
    public final static FileFilter TXT_FF = new FileNameExtensionFilter("txt (Tab delimited)", "txt");
    public final static FileFilter XML_FF = new FileNameExtensionFilter("xml", "xml");
    public final static FileFilter XLS_FF = new FileNameExtensionFilter("xls (Microsoft Excel)", "xls");
    public final static FileFilter JSON_FF = new FileNameExtensionFilter("json", "json");

    private final static Map<String, JFileChooser> fileChooserMap = new HashMap<>();

    private static boolean mocked = false;
    private static File mockResult;

    public static void setMocked(boolean isMocked) {
        if (mocked == isMocked) return;

        log.info("setMocked: {}", isMocked);
        mocked = isMocked;
    }

    public static void mock(File shouldReturn) {
        setMocked(true);
        mockResult = shouldReturn;
    }

    public static File openFile(Component parent, FileFilter... filters) {
        return openFile(parent, null, filters);
    }

    public static File openFile(Component parent, String title, FileFilter... filters) {
        return chooseFile(parent, Config.OPEN_FILE_CHOOSER, JFileChooser.OPEN_DIALOG, title, null, filters);
    }

    public static File saveFile(Component parent, FileFilter... filters) {
        return saveFile(parent, null, null, filters);
    }

    public static File saveFile(Component parent, String title, File defaultFile, FileFilter... filters) {
        return chooseFile(parent, Config.SAVE_FILE_CHOOSER, JFileChooser.SAVE_DIALOG, title, defaultFile, filters);
    }

    public static File chooseFile(Component parent, String fileChooserType, int dialogType, String title, File defaultFile, FileFilter... filters) {
        if (mocked) return mockResult;

        JFileChooser fileChooser = fileChooserMap.get(fileChooserType);
        FileChooserConfig config = Config.getInstance().getFileChooserConfig(fileChooserType);
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooserMap.put(fileChooserType, fileChooser);

            if (title != null) fileChooser.setDialogTitle(title);
            fileChooser.setDialogType(dialogType);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            for (FileFilter ff: filters) {
                fileChooser.addChoosableFileFilter(ff);
            }
            if (filters.length == 1) fileChooser.setFileFilter(filters[0]);

            if (defaultFile == null && ! config.getFilename().equals("")) {
                defaultFile = new File(config.getFilename());
            }

        }

        if (defaultFile != null) {
            fileChooser.setCurrentDirectory(defaultFile.getParentFile());
            fileChooser.setSelectedFile(defaultFile);
            fileChooser.ensureFileIsVisible(defaultFile);
        }

        Dimension preferredSize = config.getPreferredSize();
        if (preferredSize.width > 0 && preferredSize.height > 0) {
            fileChooser.setPreferredSize(preferredSize);
        }

        int option;
        if (dialogType == JFileChooser.OPEN_DIALOG) option = fileChooser.showOpenDialog(parent);
        else option = fileChooser.showSaveDialog(parent);

        File selectedFile = fileChooser.getSelectedFile();
        String filename = "";
        if (selectedFile != null) {
            filename = selectedFile.getAbsolutePath();
        }

        if (dialogType == JFileChooser.SAVE_DIALOG && option == JFileChooser.APPROVE_OPTION) {
            FileFilter ff = fileChooser.getFileFilter();
            if (ff instanceof FileNameExtensionFilter) {
                String ext = "." + ((FileNameExtensionFilter) ff).getExtensions()[0];
                if (!filename.endsWith(ext)) {
                    filename = filename + ext;
                    selectedFile = new File(filename);
                }
            }
        }

        config = new FileChooserConfig(filename, fileChooser.getSize());
        Config.getInstance().setFileChooserConfig(fileChooserType, config);

        return option == JFileChooser.APPROVE_OPTION ? selectedFile : null;
    }

}
