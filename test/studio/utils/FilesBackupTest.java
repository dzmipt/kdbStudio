package studio.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

public class FilesBackupTest {

    private static final Logger log = LogManager.getLogger();

    private Path folder;
    private Path file;
    private String filename;
    private FilesBackup filesBackup;
    private long backupPeriod;

    @BeforeEach
    public void prepare() throws IOException {
        folder = Files.createTempDirectory("kdbStudioFilesBackupTest");
        log.info("Folder is " + folder);

        Path tmpFolder = Files.createTempDirectory("kdbStudioFilesBackupTestConfig");
        file = tmpFolder.resolve("test.tmp");
        filename = file.toString();

        log.info("Reference file " + filename);

        backupPeriod = FilesBackup.BACKUP_PERIOD_MILLIS;
    }

    @AfterEach
    public void cleanup() throws IOException {
        FilesBackup.BACKUP_PERIOD_MILLIS = backupPeriod;

        FileUtils.deleteDirectory(folder.toFile());
        FileUtils.deleteDirectory(file.getParent().toFile());
    }

    private void backup(String text) throws IOException {
        Files.write(file, text.getBytes());
        filesBackup.backup(filename);
    }

    private void assertFile(Path file, String content) throws IOException {
        assertTrue(Files.exists(file), "File " + file.getFileName().toString() + " should exist");
        assertArrayEquals(content.getBytes(), Files.readAllBytes(file), "File " + file.getFileName().toString() + " should contain " + content);
    }

    private void expire(String filename) throws IOException {
        Path path = folder.resolve(filename);
        Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis() - 15 * 24 * 3600 * 1000L));
    }

    @Test
    public void testInit() throws IOException {
        FileUtils.deleteDirectory(folder.toFile());
        assertTrue(Files.notExists(folder));
        filesBackup = new FilesBackup(folder.toString());
        assertTrue(Files.exists(folder));
        long count = Files.list(folder).count();
        assertEquals(0, count);

        backup("test");
        assertFile(file, "test");
        assertFile(folder.resolve("test-0.tmp"), "test");
    }

    @Test
    public void testBackupAgain() throws IOException {
        filesBackup = new FilesBackup(folder.toString());
        backup("test");

        backup("test2");
        assertFile(file, "test2");
        assertFile(folder.resolve("test-0.tmp"), "test");
        assertEquals(1, Files.list(folder).count());
    }

    @Test
    public void testBackupAgainAfterBackupPeriodTimeout() throws IOException {
        FilesBackup.BACKUP_PERIOD_MILLIS = -1;
        filesBackup = new FilesBackup(folder.toString());
        backup("test");

        backup("test2");
        assertFile(file, "test2");
        assertEquals(2, Files.list(folder).count());
        assertFile(folder.resolve("test-0.tmp"), "test");
        assertFile(folder.resolve("test-1.tmp"), "test2");
    }

    @Test
    public void testBackupAfterStartup() throws IOException {
        filesBackup = new FilesBackup(folder.toString());
        backup("test");

        filesBackup = new FilesBackup(folder.toString());
        backup("test2");
        assertFile(folder.resolve("test-1.tmp"), "test2");
        assertEquals(2, Files.list(folder).count());
    }

    @Test
    public void testBackupWithCleanup() throws IOException {
        filesBackup = new FilesBackup(folder.toString());
        backup("test");

        filesBackup = new FilesBackup(folder.toString());
        backup("test2");

        expire("test-0.tmp");
        filesBackup = new FilesBackup(folder.toString());
        backup("test3");

        assertFile(folder.resolve("test-2.tmp"), "test3");
        assertEquals(2, Files.list(folder).count());
    }


    @Test
    public void testBackupWithCleanupAndAllExpired() throws IOException {
        filesBackup = new FilesBackup(folder.toString());
        backup("test");
        expire("test-0.tmp");

        filesBackup = new FilesBackup(folder.toString());
        backup("test2");
        assertFile(folder.resolve("test-0.tmp"), "test2");
        assertEquals(1, Files.list(folder).count());
    }

}