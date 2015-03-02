package com.cloudant.se.util;

import java.io.File;

public class UFile {
    public static File ensureReadWriteDirectory(File directory) {
        if (directory.exists()) {
            if (directory.isDirectory()) {
                if (directory.canRead() && directory.canWrite()) {
                    return directory;
                } else {
                    throw new IllegalArgumentException(directory.getName() + " must be a directory that we can read/write to");
                }
            } else {
                throw new IllegalArgumentException(directory.getName() + " must point at a directory not a file");
            }
        } else {
            if (directory.mkdirs()) {
                return directory;
            } else {
                throw new RuntimeException("Unable to create directory for \"" + directory.getName() + "\"");
            }
        }
    }
}
