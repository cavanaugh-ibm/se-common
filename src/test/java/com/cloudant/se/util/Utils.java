package com.cloudant.se.util;

import java.util.Properties;

public class Utils {
	public static String getHostName(String account) {
		if (account.startsWith("http://")) {
			return account.substring(7);
		}
		else if (account.startsWith("https://")) {
			return account.substring(8);
		}
		else {
			return account + ".cloudant.com";
		}
	}

	public static Properties getProperties() {
		Properties properties = new Properties();
		try {
			properties.load(Utils.class.getClassLoader().getResourceAsStream("cloudant-base.properties"));
			properties.load(Utils.class.getClassLoader().getResourceAsStream("cloudant-account.properties"));
		} catch (Exception e) {
			String msg = "Could not read configuration files from the classpath";
			throw new IllegalStateException(msg, e);
		}

		return properties;
	}
}