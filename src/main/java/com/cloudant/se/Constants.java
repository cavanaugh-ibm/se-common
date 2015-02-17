package com.cloudant.se;

public class Constants {
	public static final String	GENERATED	= "CLOUDANT_GENERATED_ID";

	public enum WriteCode {
		CONFLICT, EXCEPTION, INSERT, MAX_ATTEMPTS, SECURITY, TIMEOUT, UPDATE, MISSING_REV;
	}
}
