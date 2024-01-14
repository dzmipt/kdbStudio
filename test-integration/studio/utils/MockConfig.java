package studio.utils;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import studio.kdb.Config;
import studio.kdb.Workspace;

import java.io.File;
import java.io.IOException;

public class MockConfig extends Config {

    private static boolean initialized = false;

    public static synchronized void init() throws IOException {
        if (initialized) return;

        FilesBackup.setEnabled(false);
        File configFileName = File.createTempFile("kdbStudio", ".properties");
        Config.instance = new MockConfig(configFileName.getAbsolutePath());
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

    private Workspace workspace;

    private MockConfig(String filename) {
        super(filename);
    }

    @Override
    public void save() {
        // nothing
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
