package com.cloudant.se;

public class Constants {
	public static final String	GENERATED	= "CLOUDANT_GENERATED_ID";

	public enum WriteCode {
		INSERT, UPDATE, CONFLICT, TIMEOUT, MAX_ATTEMPTS, SECURITY, EXCEPTION;
	}
}
