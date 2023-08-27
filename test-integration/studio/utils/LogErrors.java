package studio.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogErrors extends AbstractAppender {

    private final static LogErrors thisAppender = new LogErrors();
    private final static List<String> logs = Collections.synchronizedList(new ArrayList<>());

    public static void reset() {
        logs.clear();
    }

    public static String[] get() {
        return logs.toArray(new String[0]);
    }

    public static synchronized void init() {
        LoggerContext context = LoggerContext.getContext(false);
        Logger rootLogger = context.getRootLogger();
        for (Appender appender: rootLogger.getAppenders().values()) {
            if (appender == thisAppender) return;
        }
        context.getRootLogger().addAppender(thisAppender);

    }

    private LogErrors() {
        super("MyAppender", null,
                PatternLayout.newBuilder()
                        .withPattern("%d{yyyy.MM.dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                        .build());
        start();
    }

    @Override
    public void append(LogEvent event) {
        if (event.getLevel().isLessSpecificThan(Level.WARN)) return;
        logs.add( String.format("[%s] %s %s - %s",
                                    event.getThreadName(), event.getLevel(),
                                    event.getLoggerName(), event.getMessage().getFormattedMessage()) );
    }
}
