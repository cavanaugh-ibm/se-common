package com.cloudant.se.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.se.concurrent.StatusingNotifyingBlockingThreadPoolExecutor;
import com.cloudant.se.util.ULog;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public abstract class BaseApp {
    protected static final Logger log            = Logger.getLogger(BaseApp.class);
    protected CloudantClient      client         = null;
    protected Configuration       config         = null;
    protected Database            database       = null;
    protected String              name           = null;
    protected BaseAppOptions      options        = null;
    protected ExecutorService     writerExecutor = null;

    public BaseApp(String name) {
        this(name, new BaseAppOptions());
    }

    public BaseApp(String name, BaseAppOptions options) {
        this.name = name;
        this.options = options;
    }

    public void run(String[] args) {
        int configReturnCode = loadConfiguration(args);
        switch (configReturnCode) {
            case 0:
                // config worked, user accepted design
                System.exit(start());
                break;
            default:
                // config did NOT work, error out
                System.exit(configReturnCode);
                break;
        }
    }

    protected int loadConfiguration(String[] args) {
        JCommander jCommander = new JCommander();
        jCommander.setProgramName(name);
        jCommander.addObject(options);

        //
        // Try to parse the options we were given
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            jCommander.usage();
            return 1;
        }

        //
        // Show the help if they asked for it
        if (options.help) {
            jCommander.usage();
            return 2;
        }

        //
        // Setup our logging
        ULog.resetLogging(options);

        //
        // Read the config they gave us
        try {
            //
            // Read the configuration from our file and let it validate itself
            config = new PropertiesConfiguration(options.configFileName);
            mergeOptions();

            int threads = config.getInt("write.threads");
            ThreadFactory writeThreadFactory = new ThreadFactoryBuilder().setNameFormat("ldr-w-%d").build();
            writerExecutor = new StatusingNotifyingBlockingThreadPoolExecutor(threads, threads * 2, 30, TimeUnit.SECONDS, writeThreadFactory);

            validateConfig();
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error detected - " + e.getMessage());
            config = null;
            return -2;
        } catch (Exception e) {
            System.err.println("Unexpected exception - see log for details - " + e.getMessage());
            log.error(e.getMessage(), e);
            return -1;
        }

        return 0;
    }

    protected void mergeOptions() {
        setupAndValidateProperty("cloudant.account", options.cloudantAccount, "Cloudant account");
        setupAndValidateProperty("cloudant.database", options.cloudantDatabase, "Cloudant database");
        setupAndValidateProperty("cloudant.user", options.cloudantUser, "Cloudant user");
        setupAndValidateProperty("cloudant.pass", options.cloudantPassword, "Cloudant password");
    }

    private void setupAndValidateProperty(String propertyKey, String optionValue, String name) {
        config.setProperty(propertyKey, StringUtils.defaultIfBlank(optionValue, config.getString(propertyKey)));
        Assert.hasText(config.getString(propertyKey), name + " must be provided");
    }

    protected boolean connectToCloudant() {
        try {
            ConnectOptions options = new ConnectOptions();
            options.setMaxConnections(config.getInt("write.threads"));

            options.setProxyHost(config.getString("proxy.host"));
            options.setProxyPort(config.getInt("proxy.port", 0));
            options.setProxyUser(config.getString("proxy.user"));
            options.setProxyPass(config.getString("proxy.pass"));

            log.info(" --- Connecting to Cloudant --- ");
            client = new CloudantClient(config.getString("cloudant.account"), config.getString("cloudant.user"), config.getString("cloudant.pass"), options);
            database = client.database(config.getString("cloudant.database"), false);
            log.info(" --- Connected to Cloudant --- ");

            return true;
        } catch (Exception e) {
            log.fatal("Unable to connect to the database", e);
            return false;
        }
    }

    protected int start() {
        log.info("Configuration complete, starting up");

        //
        // Get our connection to cloudant cloudant
        if (!connectToCloudant()) {
            return -4;
        }

        doWork();

        log.info("Waiting for writers to complete");
        writerExecutor.shutdown();

        try {
            writerExecutor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
        }
        log.info("All writers have completed");

        log.info("App complete, shutting down");
        return 0;
    }

    protected abstract void doWork();

    protected abstract void validateConfig();
}
