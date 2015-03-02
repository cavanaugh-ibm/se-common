package com.cloudant.se.util;

import static java.lang.String.format;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.springframework.util.Assert;

public class UConfig {
    public static File ensureReadWriteDirectory(Configuration config, String storageKey, String defaultPath) {
        Assert.hasText(config.getString(storageKey, defaultPath), format("Configuration must provide a valid value for [%s]", storageKey));
        return UFile.ensureReadWriteDirectory(new File(config.getString(storageKey, defaultPath)));
    }
}
