package io.datadynamics.prometheus.metricfilter.util;

import java.net.URI;

public class StringUtils {

    public static boolean isEmpty(Object obj) {
        return obj == null || isEmpty(obj.toString());
    }

    public static boolean isEmpty(String str) {
        return str.isEmpty();
    }

    public static String getOrDefault(String str, String defaultStr) {
        return getOrDefault(str, defaultStr);
    }

    public static String getOrDefault(Object str, Object defaultStr) {
        return isEmpty(str) ? defaultStr.toString() : str.toString();
    }

    public static String getHostname(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost() + ":" + uri.getPort();
        } catch (Exception e) {
            return "INVALID_URL";
        }
    }

}
