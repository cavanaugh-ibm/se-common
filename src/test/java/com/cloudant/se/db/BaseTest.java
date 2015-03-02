package com.cloudant.se.db;

import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ConnectOptions;

public class BaseTest {
    protected static CloudantClient client;
    protected static Database       database;
    protected static String         databaseName;
    protected static Properties     props;

    @BeforeClass
    public static void setUpClass() {
        props = getProperties();

        ConnectOptions options = new ConnectOptions();
        // options.setConnectionTimeout(props.get("couchdb.http.connection.timeout"));
        // options.setMaxConnections(props.get("couchdb.max.connections"));
        // options.setProxyHost(props.get("couchdb.proxy.host"));
        // options.setProxyPort(props.get("couchdb.proxy.port"));
        // options.setSocketTimeout(props.get("http.socket.timeout"));

        client = new CloudantClient(getProp("cloudant.test.account", true), getProp("cloudant.test.username", true), getProp("cloudant.test.password", false), options);
    }

    @Before
    public void before() {
        databaseName = props.getProperty("cloudant.test.database.prefix", "JunitTesting-") + "-" + System.currentTimeMillis();
        database = client.database(databaseName, true);
    }

    @After
    public void after() {
        if (client != null) {
            client.deleteDB(databaseName, "delete database");
        }
        databaseName = null;
        database = null;
    }

    @AfterClass
    public static void tearDownClass() {
        if (client != null) {
            client.shutdown();
        }
    }

    protected static String getProp(String propName, boolean allowPropsFile) {
        String value = System.getProperty(propName);
        if (value == null && allowPropsFile) {
            value = props.getProperty(propName);
        }

        if (allowPropsFile) {
            Assert.assertNotNull(propName + " must be set as either a system property or in the properties file", value);
        } else {
            Assert.assertNotNull(propName + " must be set as a system property", value);
        }

        System.out.printf("Returning %s for %s\n", value, propName);
        return value;
    }

    protected static Properties getProperties() {
        Properties properties = new Properties();
        try {
            properties.load(BaseTest.class.getClassLoader().getResourceAsStream("cloudant.properties"));
        } catch (Exception e) {
            String msg = "Could not read configuration files from the classpath";
            throw new IllegalStateException(msg, e);
        }

        return properties;
    }
}
