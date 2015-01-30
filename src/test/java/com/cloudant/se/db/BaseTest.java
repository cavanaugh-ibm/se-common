package com.cloudant.se.db;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.cloudant.client.api.CloudantClient;
import com.cloudant.client.api.Database;
import com.cloudant.client.api.model.ConnectOptions;
import com.cloudant.se.util.Utils;

public class BaseTest {
	protected static CloudantClient	client;
	protected static String			databaseName;
	protected static Database		database;
	protected static Properties		props;

	@BeforeClass
	public static void setUpClass() {
		props = Utils.getProperties();

		ConnectOptions options = new ConnectOptions();
		// options.setConnectionTimeout(props.get("couchdb.http.connection.timeout"));
		// options.setMaxConnections(props.get("couchdb.max.connections"));
		// options.setProxyHost(props.get("couchdb.proxy.host"));
		// options.setProxyPort(props.get("couchdb.proxy.port"));
		// options.setSocketTimeout(props.get("http.socket.timeout"));

		client = new CloudantClient(props.getProperty("cloudant.account"), props.getProperty("cloudant.username"), props.getProperty("cloudant.password"), options);

		databaseName = props.getProperty("cloudant.database") + "-" + System.currentTimeMillis();
		database = client.database(databaseName, true);
	}

	@AfterClass
	public static void tearDownClass() {
		client.deleteDB(databaseName, "delete database");
		client.shutdown();
	}
}
