package com.cloudant.se.app;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;

public class BaseAppOptions {
    public static final Logger log              = Logger.getLogger(BaseAppOptions.class);

    @Parameter(names = "-claccount", description = "Cloudant destination account")
    public String              cloudantAccount  = System.getProperty("cloudant_account");

    @Parameter(names = "-cldatabase", description = "Cloudant destination database")
    public String              cloudantDatabase = System.getProperty("cloudant_database");

    @Parameter(names = "-clpassword", description = "Cloudant destination password", password = true)
    public String              cloudantPassword = System.getProperty("cloudant_password");

    @Parameter(names = "-cluser", description = "Cloudant destination user")
    public String              cloudantUser     = System.getProperty("cloudant_user");

    @Parameter(names = { "-c", "-config" }, description = "The configuration file to load from")
    public String              configFileName;

    @Parameter(names = { "-?", "--help" }, help = true, description = "Display this help")
    public boolean             help;

    @Parameter(names = { "-verbose" }, description = "Enable verbose messages to the screen")
    public boolean             verbose          = false;

    @Parameter(names = { "-verboseLog" }, description = "Enable verbose messages to the log")
    public boolean             verboseLog       = false;

    @Parameter(names = { "-debug" }, description = "Enable debug messages to the screen")
    public boolean             debug            = false;

    @Parameter(names = { "-debugLog" }, description = "Enable debug messages to the  log")
    public boolean             debugLog         = false;

    @Parameter(names = { "-trace" }, description = "Enable trace messages to the screen", hidden = true)
    public boolean             trace            = false;

    @Parameter(names = { "-traceLog" }, description = "Enable trace messages to the  log", hidden = true)
    public boolean             traceLog         = false;

    @Parameter(names = { "-log" }, description = "The log file to use")
    public String              logFileName;

    @Parameter(names = { "-local" }, description = "Do not connect to a Cloudant instance - run locally")
    public boolean             local            = false;

    @Parameter(names = { "-threaded" }, description = "Run with multiple threads")
    public boolean             threaded         = true;

    @DynamicParameter(names = "-D", description = "Dynamic parameters")
    public Map<String, String> extraParams      = new HashMap<String, String>();

    @Override
    public String toString() {
        return "BaseAppOptions [cloudantAccount=" + cloudantAccount + ", cloudantDatabase=" + cloudantDatabase + ", cloudantPassword=" + cloudantPassword + ", cloudantUser=" + cloudantUser
                + ", configFileName=" + configFileName + ", help=" + help + ", verbose=" + verbose + ", verboseLog=" + verboseLog + ", debug=" + debug + ", debugLog=" + debugLog + ", trace=" + trace
                + ", traceLog=" + traceLog + ", logFileName=" + logFileName + ", local=" + local + ", threaded=" + threaded + ", extraParams=" + extraParams + "]";
    }
}
