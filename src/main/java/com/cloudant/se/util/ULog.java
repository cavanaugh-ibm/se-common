package com.cloudant.se.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.cloudant.se.app.BaseAppOptions;

public class ULog {
    public static final String  PATTERN_CONSOLE = "%d{yyyy-MM-dd HH:mm:ss} %-5p - %m%n";
    public static final String  PATTERN_FILE    = "%d{yyyy-MM-dd HH:mm:ss} %-5p [%10.10t] %30.30c{1} - %m%n";
    private static final String NAME_FILE       = "file";
    private static final String NAME_STDOUT     = "stdout";

    public static void resetFileLogging(BaseAppOptions options) {
        Logger.getRootLogger().removeAppender(NAME_FILE);

        String fileName = null;
        Level newLevel = Level.WARN;
        boolean addLog = false;

        //
        // If they give us a log file, log INFO at a minimum
        if (StringUtils.isNotBlank(options.logFileName)) {
            fileName = options.logFileName;
            newLevel = Level.INFO;
            addLog = true;
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
            fileName = "application-" + dateFormat.format(new Date()) + ".log";
        }

        if (options.traceLog) {
            newLevel = Level.TRACE;
            addLog = true;
        } else if (options.debugLog) {
            newLevel = Level.DEBUG;
            addLog = true;
        } else if (options.verboseLog) {
            newLevel = Level.INFO;
            addLog = true;
        }

        if (addLog) {
            FileAppender fa = new FileAppender();
            fa.setFile(fileName);
            fa.setLayout(new PatternLayout(PATTERN_FILE));
            fa.setThreshold(newLevel);
            fa.activateOptions();
            Logger.getRootLogger().addAppender(fa);
        }
    }

    public static void resetLogging(BaseAppOptions options) {
        resetScreenLogging(options);
        resetFileLogging(options);
    }

    public static void resetScreenLogging(BaseAppOptions options) {
        Logger logger = Logger.getRootLogger();
        AppenderSkeleton appender = (AppenderSkeleton) logger.getAppender(NAME_STDOUT);

        if (appender != null) {
            if (options.trace) {
                appender.setThreshold(Level.TRACE);
            } else if (options.debug) {
                appender.setThreshold(Level.DEBUG);
            } else if (options.verbose) {
                appender.setThreshold(Level.INFO);
            }
        }
    }
}
