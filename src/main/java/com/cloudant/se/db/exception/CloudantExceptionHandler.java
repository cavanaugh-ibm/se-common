package com.cloudant.se.db.exception;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.lightcouch.DocumentConflictException;

import com.cloudant.se.Constants.WriteCode;

public class CloudantExceptionHandler {
    public static WriteCode getWriteCode(Throwable thrown) {
        if (CloudantExceptionHandler.isConflictException(thrown)) {
            return WriteCode.CONFLICT;
        } else if (CloudantExceptionHandler.isMissingRevException(thrown)) {
            return WriteCode.MISSING_REV;
        } else if (CloudantExceptionHandler.isPermissionException(thrown)) {
            return WriteCode.SECURITY;
        } else if (CloudantExceptionHandler.isTimeoutException(thrown)) {
            return WriteCode.TIMEOUT;
        } else {
            return WriteCode.EXCEPTION;
        }

    }

    public static boolean isConflictException(Throwable thrown) {
        return (thrown instanceof DocumentConflictException);
    }

    public static boolean isMissingRevException(Throwable thrown) {
        if (thrown instanceof IllegalArgumentException) {
            return containsIgnoreCase(thrown.getMessage(), "rev may not be null");
        }

        return false;
    }

    public static boolean isPermissionException(Throwable thrown) {
        return thrown.getMessage().contains("\"error\":\"forbidden\",\"reason\":\"_writer access is required for this request\"");
    }

    public static boolean isTimeoutException(Throwable thrown) {
        // Get a handle to the cause of this throwable. If no cause, then the throwable is the cause itself
        Throwable cause = ExceptionUtils.getRootCause(thrown);
        cause = cause != null ? cause : thrown;

        String message = cause.getMessage();

        if (containsIgnoreCase(message, "Request Time-out<html><body><h1>408 Request Time-out")) {
            return true;
        } else if (containsIgnoreCase(message, "Connection timed out: connect")) {
            return true;
        } else if (containsIgnoreCase(message, "connect timed out")) {
            return true;
        } else if (containsIgnoreCase(message, "Operation timed out")) {
            return true;
        } else if (containsIgnoreCase(message, "Connection reset")) {
            return true;
        } else if (containsIgnoreCase(message, "Read timed out")) {
            return true;
        } else if (containsIgnoreCase(message, "failed to respond")) {
            return true;
        } else if (containsIgnoreCase(message, "Remote host closed connection during handshake")) {
            return true;
        }

        return false;
    }
}
