package studio.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import studio.kdb.Config;
import studio.kdb.Workspace;
import studio.utils.log4j.EnvConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MockConfig extends Config {

    private static boolean initialized = false;

    private static Path basePath;

    public static synchronized void mock() throws IOException {
        if (initialized) return;

        FilesBackup.setEnabled(false);
        basePath = createTempDir();
        EnvConfig.setBaseFolder(basePath);
        Config.instance = new MockConfig(basePath);

        LoggerContext context = LoggerContext.getContext(false);
        for (Logger logger: context.getLoggers() ) {
            Appender[] appenders = logger.getAppenders().values().toArray(new Appender[0]);
            for(Appender appender: appenders) {
                if (appender instanceof RollingFileAppender ||
                    appender instanceof AsyncAppender) {
                    logger.removeAppender(appender);
                    logger.addAppender(NullAppender.createAppender("null"));
                }
            }
        }

        initialized = true;
    }

    private static List<Path> tmpDirs = new ArrayList<>();
    private final static Thread shutdownHook = new Thread() {
        @Override
        public void run() {
            for (Path dir: tmpDirs) {
                try {
                    FileUtils.deleteDirectory(dir.toFile());
                } catch (IOException e) {
                    System.err.printf("Error on folder %s removal\n", dir);
                    e.printStackTrace(System.err);
                }
            }
        }
    };

    public static synchronized Path createTempDir() throws IOException {
        if (tmpDirs.isEmpty()) Runtime.getRuntime().addShutdownHook(shutdownHook);
        Path path = Files.createTempDirectory("kdbStudio");
        tmpDirs.add(path);
        return path;
    }

    public static void cleanupConfigs() throws IOException {
        FileUtils.deleteDirectory(basePath.toFile());
        Files.createDirectory(basePath);
    }

    public static Path getBasePath() {
        return basePath;
    }

    public void reload() {
        super.init();
    }

    private Workspace workspace;

    private MockConfig(Path path) {
        super(path);
    }

    @Override
    public Workspace loadWorkspace() {
        if (workspace == null) workspace = new Workspace();
        return workspace;
    }

    @Override
    public void saveWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public void exit() {
    }

}
