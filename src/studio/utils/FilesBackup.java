package studio.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class FilesBackup {

    private final static long BACKUP_PERIOD_MILLIS = 24*60*60*1000L; // ond day
    private final static int RETAIN_BACKUP_HISTORY_DAYS = 14; // keep the last 2 weeks

    private final Map<String, Long> lastBackupTime = new HashMap<>();
    private final Map<String, Integer> backupIndex = new HashMap<>();
    private final Path backupDirPath;
    private static final Logger log = LogManager.getLogger();

    public FilesBackup(String backupFolder) {
        backupDirPath = Paths.get(backupFolder);
        if (Files.notExists(backupDirPath)) {
            try {
                log.info("Creating backup folder {}", backupDirPath);
                Files.createDirectories(backupDirPath);
            } catch (IOException e) {
                log.error("Error on backup folder creation", e);
            }
        }
    }

    public void backup(String filename) throws IOException {
        long current = System.currentTimeMillis();
        Long lastBackup = lastBackupTime.get(filename);

        if (lastBackup == null) {
            initBackupIndex(filename);
            lastBackupTime.put(filename, current);
            lastBackup = 0L;
        }

        if (current - lastBackup < BACKUP_PERIOD_MILLIS) return;

        doBackup(filename);
        lastBackupTime.put(filename, current);
    }

    private void initBackupIndex(String filename) throws IOException {
        BackupFile reference = new BackupFile(filename);

        cleanupBackupHistory(reference);

        int maxIndex = Files.list(backupDirPath)
                                .filter(Files::isRegularFile)
                                .map(p -> new BackupFile(p, reference) )
                                .filter(BackupFile::isValid)
                                .mapToInt(BackupFile::getIndex)
                                .max()
                                .orElse(-1);

        backupIndex.put(filename, maxIndex);
    }

    private void doBackup(String filename) throws IOException {
        BackupFile reference = new BackupFile(filename);
        cleanupBackupHistory(reference);

        int index = backupIndex.get(filename) + 1;
        Path dest = backupDirPath.resolve(reference.name + "-" + index + "." + reference.ext);
        log.info("Backing up {} to {}", filename, dest);
        Files.copy(reference.path, dest);
    }

    private void cleanupBackupHistory(BackupFile reference) throws IOException {
        Instant keepInstant = Instant.now().minus(RETAIN_BACKUP_HISTORY_DAYS, ChronoUnit.DAYS);

        Files.list(backupDirPath)
                .filter(Files::isRegularFile)
                .map(p -> new BackupFile(p, reference))
                .filter(BackupFile::isValid)
                .forEach(b -> {
                    try {
                        BasicFileAttributes attr = Files.readAttributes(b.path, BasicFileAttributes.class);
                        if (attr.lastModifiedTime().toInstant().isBefore(keepInstant)) {
                            log.info("Remove old backup {}", b.path);
                            Files.delete(b.path);
                        }
                    } catch (IOException e) {
                        log.error("Error during history clean up", e);
                    }
                });
    }

    private static class BackupFile {
        public boolean valid = false;
        public int index = -1;
        public String name;
        public String ext;
        public Path path;

        BackupFile(Path path) {
            this.path = path;
            ext = FilenameUtils.getExtension(path.toString());
            name = FilenameUtils.removeExtension(FilenameUtils.getName(path.toString()));
        }

        BackupFile(String filename) {
            this(Paths.get(filename));
        }

        BackupFile(Path backup, BackupFile reference) {
            this(backup);
            this.path = backup;

            if (! ext.equals(reference.ext)) return;
            if (! name.startsWith(reference.name + "-")) return;
            try {
                index = Integer.parseInt(name.substring(reference.name.length() + 1));
                valid = true;
            } catch (NumberFormatException e ) {}
        }

        boolean isValid() {
            return valid;
        }

        int getIndex() {
            return  index;
        }

    }
}
