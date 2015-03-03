package com.cloudant.se.util;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;

public class UJson {
    protected static final Logger log = Logger.getLogger(UJson.class);

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static int comparePrimitives(Object o, Object n) {
        if (o == null && n == null) {
            return 0;
        } else if (o == null) {
            return -1;
        } else if (n == null) {
            return 1;
        }

        //
        // Makes sure we aren't trying to compare lists or maps
        if (o instanceof Map || o instanceof Collection || n instanceof Map || n instanceof Collection) {
            log.warn("Unable to compare MAP or COLLECTION instances - assuming equal");
            return 0;
        }

        if (o instanceof Comparable && n instanceof Comparable) {
            return ObjectUtils.compare((Comparable) o, (Comparable) n);
        } else {
            return ObjectUtils.compare(o.toString(), n.toString());
        }
    }
}
