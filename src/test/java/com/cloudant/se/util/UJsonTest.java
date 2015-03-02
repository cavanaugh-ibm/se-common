package com.cloudant.se.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class UJsonTest {
    @Test
    public void testToCamelCase() {
        assertEquals("Id", UJson.toCamelCase("ID"));
        assertEquals("PhoneNumber", UJson.toCamelCase("PHONE_NUMBER"));
        assertEquals("PhoneTypeCode", UJson.toCamelCase("PHONE_TYPE_CODE"));
    }

}
