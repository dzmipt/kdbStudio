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

    public static File propertiesFile;
    public static File workspaceFile;
    public static File serversFile;

    public static synchronized void mock() throws IOException {
        if (initialized) return;

        FilesBackup.setEnabled(false);
        propertiesFile = File.createTempFile("kdbStudio", ".properties");
        propertiesFile.deleteOnExit();
        workspaceFile = File.createTempFile("kdbStudioWorkspace", ".properties");
        workspaceFile.deleteOnExit();
        serversFile = File.createTempFile("kdbStudioServers", ".json");
        serversFile.deleteOnExit();
        Config.instance = new MockConfig(propertiesFile.getAbsolutePath());
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

    public void reload() {
        this.config = new PropertiesConfig(getFilename());
        super.init();
    }

    @Override
    protected String getWorkspaceFilename() {
        return workspaceFile.getAbsolutePath();
    }

    @Override
    protected String getServerConfigFilename() {
        return serversFile.getAbsolutePath();
    }

    private Workspace workspace;

    private MockConfig(String filename) {
        super(filename);
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
