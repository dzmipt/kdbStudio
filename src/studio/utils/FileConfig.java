package studio.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileConfig {

    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
            runnable -> {
                Thread thread = new Thread(runnable, "FileConfig writer scheduler");
                thread.setDaemon(true);
                return thread;
            });

    private final static List<FileConfig> all = new ArrayList<>();
    private AtomicReference<String> cache = new AtomicReference<>(null);
    private final Path path;

    private final static long SAVE_DELAY_SEC = 2;
    private final static Logger log = LogManager.getLogger();

    public FileConfig(Path path) {
        all.add(this);
        this.path = path;
    }

    public boolean fileExists() {
        return Files.exists(path);
    }

    public Path getPath() {
        return path;
    }

    public synchronized String getContent() throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    public Writer getWriter() {
        return new StringWriter() {
            @Override
            public void close() throws IOException {
                String old = cache.getAndSet(getBuffer().toString());
                if (old == null) {
                    scheduler.schedule(FileConfig.this::saveOnDisk, SAVE_DELAY_SEC, TimeUnit.SECONDS);
                }
            }
        };
    }

    public synchronized void saveOnDisk() {
        try {
            String content = cache.getAndSet(null);
            if (content == null) return;

            FilesBackup.getInstance().backup(path);
            String name = path.getFileName().toString();

            Path tmpPath = path.resolveSibling(
                    String.format("%s.%d.tmp", name, System.currentTimeMillis()) );

            Files.write(tmpPath, content.getBytes(StandardCharsets.UTF_8));
            Files.move(tmpPath, path, REPLACE_EXISTING);
            log.debug("Moved {} -> {}", tmpPath, path);
        } catch (IOException e) {
            log.error("Error during writing {}", path, e);
        }
    }

    public static void saveAllOnDisk() {
        all.forEach(FileConfig::saveOnDisk);
    }

    @Override
    public String toString() {
        return path.toString();
    }
}
