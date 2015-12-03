package com.cloudant.se.app;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
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
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public abstract class BaseApp {
    protected static final Logger log             = Logger.getLogger(BaseApp.class);
    public int                    DEFAULT_THREADS = 10;
    private CloudantClient        client          = null;
    private Configuration         configuration   = null;
    private Database              database        = null;
    private JCommander            jCommander;
    private String                name            = null;
    private BaseAppOptions        options         = null;
    private ExecutorService       writerExecutor  = null;

    public BaseApp(String name) {
        this(name, new BaseAppOptions());
    }

    public BaseApp(String name, BaseAppOptions options) {
        this.name = name;
        this.options = options;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Database getDatabase() {
        return database;
    }

    public BaseAppOptions getOptions() {
        return options;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BaseApp [");
        builder.append("client=");
        builder.append(client);
        builder.append(", configuration=");
        builder.append(configuration);
        builder.append(", database=");
        builder.append(database);
        builder.append(", name=");
        builder.append(name);
        builder.append(", options=");
        builder.append(options);
        builder.append(", writerExecutor=");
        builder.append(writerExecutor);
        builder.append("]");
        return builder.toString();
    }

    private void setupAndValidateProperty(String propertyKey, String optionValue, String name) {
        configuration.setProperty(propertyKey, StringUtils.defaultIfBlank(optionValue, getConfigStr(propertyKey)));
        Assert.hasText(getConfigStr(propertyKey), name + " must be provided");
    }

    private boolean stopWriters(boolean wait) {
        boolean stopped = true;

        //
        // If we have a write executor, kill it!!
        if (writerExecutor != null) {

            if (wait) {
                log.info("Stopping writers and waiting");
                writerExecutor.shutdown();
            } else {
                log.info("Stopping writers by killing");
                writerExecutor.shutdownNow();
            }

            try {
                stopped = writerExecutor.awaitTermination(1, wait ? TimeUnit.DAYS : TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                stopped = false;
            }
        }

        if (stopped) {
            log.info("Writers stopped successfully");
        } else {
            log.error("Writers were not stopped");
        }

        return stopped;
    }

    protected boolean connectToCloudant() {
        try {
            ConnectOptions options = new ConnectOptions();
            options.setMaxConnections(getConfigInt("write.threads", DEFAULT_THREADS));

            options.setProxyHost(getConfigStr("proxy.host", "localhost"));
            options.setProxyPort(getConfigInt("proxy.port", 0));
            options.setProxyUser(getConfigStr("proxy.user", null));
            options.setProxyPass(getConfigStr("proxy.pass", null));

            log.info(" --- Connecting to Cloudant --- ");
            client = new CloudantClient(getConfigStr("cloudant.account"), getConfigStr("cloudant.user"), getConfigStr("cloudant.pass"), options);
            database = client.database(getConfigStr("cloudant.database"), false);
            log.info(" --- Connected to Cloudant --- ");

            return true;
        } catch (Exception e) {
            log.fatal("Unable to connect to the database", e);
            return false;
        }
    }

    /**
     * The main work method of most application starter classes. For now, abstract until we learn more about what classes implement this.
     */
    protected abstract void doWork();

    protected boolean getConfigBool(String key, boolean defaultValue) {
        return configuration.getBoolean(key, defaultValue);
    }

    protected int getConfigInt(String key, int defaultValue) {
        return configuration.getInt(key, defaultValue);
    }

    protected String getConfigStr(String key) {
        return configuration.getString(key);
    }

    protected String getConfigStr(String key, String defaultValue) {
        return configuration.getString(key, defaultValue);
    }

    protected String[] getConfigStrArray(String key) {
        return configuration.getStringArray(key);
    }

    protected String getStringFrom(String fileName, String optionValue, boolean helpIfBothMissing) {
        if (StringUtils.isNotBlank(fileName)) {
            try {
                return Files.toString(new File(fileName), Charsets.UTF_8);
            } catch (Exception e) {
                //
                // Whatever the exception is - drop it
                log.error("Unable to read a value from - " + fileName, e);
            }
        } else if (StringUtils.isNotBlank(optionValue)) {
            return optionValue;
        }

        //
        // Show the help message if asked - and kick a runtime e
        if (helpIfBothMissing) {
            jCommander.usage();
            log.error("Unable to find a value in the file or the configuration key");
            throw new ParameterException("Unable to find a value in the file or the configuration key");
        }

        return null;
    }

    protected boolean isReadableFile(String fileName) {
        File f = new File(fileName);

        return f.exists() && f.canRead() && !f.isDirectory();
    }

    protected int loadConfiguration(String[] args) {
        jCommander = new JCommander();
        jCommander.setProgramName(name);
        jCommander.addObject(options);

        //
        // Try to parse the options we were given
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            e.printStackTrace();

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
            // Read the configuration from our file and let it validate itself (if given)
            if (StringUtils.isNotBlank(options.configFileName)) {
                configuration = new PropertiesConfiguration(options.configFileName);
            } else {
                configuration = new PropertiesConfiguration();
            }

            mergeOptions();

            ThreadFactory writeThreadFactory = new ThreadFactoryBuilder().setNameFormat("ldr-w-%d").build();
            if (options.threaded) {
                int threads = getConfigInt("write.threads", DEFAULT_THREADS);
                log.info(" --- Setting up for running with " + threads + " threads --- ");
                writerExecutor = new StatusingNotifyingBlockingThreadPoolExecutor(threads, threads * 2, 30, TimeUnit.SECONDS, writeThreadFactory);
            }
            else {
                log.info(" --- Setting up for running with main thread --- ");
            }

            validateConfig();
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error detected - " + e.getMessage());
            configuration = null;
            return -2;
        } catch (Exception e) {
            System.err.println("Unexpected exception - see log for details - " + e.getMessage());
            log.error(e.getMessage(), e);
            return -1;
        }

        return 0;
    }

    protected void mergeOptions() {
        if (!options.local) {
            setupAndValidateProperty("cloudant.account", options.cloudantAccount, "Cloudant account");
            setupAndValidateProperty("cloudant.database", options.cloudantDatabase, "Cloudant database");
            setupAndValidateProperty("cloudant.user", options.cloudantUser, "Cloudant user");
            setupAndValidateProperty("cloudant.pass", options.cloudantPassword, "Cloudant password");
        }
    }

    protected void setConfigValue(String key, Object value) {
        configuration.setProperty(key, value);
    }

    protected int start() {
        log.info("Configuration complete, starting up");

        //
        // Get our connection to Cloudant (if we are not running local)
        if (!options.local && !connectToCloudant()) {
            stopWriters(false);
            return -4;
        }

        try {
            doWork();
        } catch (Exception e) {
            log.fatal("Unable to complete processing - exiting", e);
            stopWriters(false);
            return -5;
        }

        //
        // All work should be queued - wait for writers
        stopWriters(true);

        log.info("App complete, shutting down");
        return 0;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Future submitCallable(Callable callable) {
        if (options.threaded) {
            return writerExecutor.submit(callable);
        }
        else {
            try {
                Object o = callable.call();
                return Futures.immediateFuture(o);
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
        }
    }

    // /**
    // * Return a {@link BufferedReader} for {@code file}. If {@code file} is "-" then stdin will be used instead.
    // */
    // @SuppressWarnings("resource")
    // private BufferedReader readerForFile(String fileName) throws IOException, FileNotFoundException {
    // if (fileName != null) {
    // InputStream inputStream = new FileInputStream(fileName);
    // return new BufferedReader(new InputStreamReader(new UnicodeBomInputStream(inputStream).skipBOM(), Charset.defaultCharset()));
    // }
    // else {
    // return null;
    // }
    // }

    /**
     * This method is abstract because we want the implementer to think about the validation even if there is nothing to do. Most cases its a noop
     */
    protected abstract void validateConfig();
}
