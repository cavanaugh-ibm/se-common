package com.cloudant.se.log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorCode;

/**
 * This is a customized log4j appender, which will create a new file for every run of the application.
 *
 * @author Veera Sundar
 * @see http://veerasundar.com/blog/2009/08/how-to-create-a-new-log-file-for-each-time-the-application-runs/
 */
public class NewLogFileForEachRunAppender extends FileAppender {
    boolean renamed = false;

    public NewLogFileForEachRunAppender() {
    }

    public NewLogFileForEachRunAppender(Layout layout, String filename) throws IOException {
        super(layout, filename);
    }

    public NewLogFileForEachRunAppender(Layout layout, String filename, boolean append) throws IOException {
        super(layout, filename, append);
    }

    public NewLogFileForEachRunAppender(Layout layout, String filename, boolean append, boolean bufferedIO, int bufferSize) throws IOException {
        super(layout, filename, append, bufferedIO, bufferSize);
    }

    @Override
    public void activateOptions() {
        if (fileName != null) {
            try {
                fileName = getNewLogFileName();
                setFile(fileName, fileAppend, bufferedIO, bufferSize);
            } catch (Exception e) {
                errorHandler.error("Error while activating log options", e, ErrorCode.FILE_OPEN_FAILURE);
            }
        }
    }

    private String getNewLogFileName() {
        if (fileName != null) {
            if (renamed) {
                return fileName;
            } else {
                final String DOT = ".";
                final String HIPHEN = "-";
                final File logFile = new File(fileName);
                final String fileName = logFile.getName();
                final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                String newFileName = "";

                final int dotIndex = fileName.lastIndexOf(DOT);
                if (dotIndex != -1) {
                    // the file name has an extension. so, insert the time stamp between the file name and the extension
                    newFileName = fileName.substring(0, dotIndex) + HIPHEN + dateFormat.format(new Date()) + DOT + fileName.substring(dotIndex + 1);
                } else {
                    // the file name has no extension. So, just append the timestamp at the end.
                    newFileName = fileName + HIPHEN + dateFormat.format(new Date());
                }

                renamed = true;
                if (logFile.getParent() != null) {
                    return logFile.getParent() + File.separator + newFileName;
                } else {
                    return newFileName;
                }
            }
        }

        return null;
    }
}