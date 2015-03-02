package com.cloudant.se.util;

public class UJson {
    // public static String standardizeName(String name) {
    // // WordUtils.capitalize(name, new char[] { '_' }).replaceAll("_", "");
    // // WordUtils.capitalizeFully(name, new char[] { '_' }).replaceAll("_", "");
    // }

    public static String toCamelCase(String s) {
        String[] parts = s.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return camelCaseString;
    }

    private static String toProperCase(String s) {
        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }
}
