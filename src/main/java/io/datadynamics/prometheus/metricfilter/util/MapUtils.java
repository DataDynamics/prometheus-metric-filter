package io.datadynamics.prometheus.metricfilter.util;

import java.util.HashMap;
import java.util.Map;

public class MapUtils {

    public static Map queryStatus(String running, Integer runningValue,
                                  String waitToClose, Integer waitToCloseValue) {
        Map map = new HashMap();
        map.put(running, runningValue);
        map.put(waitToClose, waitToCloseValue);
        return map;
    }

    public static Map session(String sessionType, String sessionTypeValue,
                              String openQueries, String openQueriesValue,
                              String totalQueries, String totalQueriesValue,
                              String user, String userValue,
                              String delegatedUser, String delegatedUserValue,
                              String sessionId, String sessionIdValue,
                              String networkAddress, String networkAddressValue,
                              String defaultDatabase, String defaultDatabaseValue,
                              String startTime, String startTimeValue,
                              String lastAccessed, String lastAccessedValue,
                              String idleTimeoutSecs, String idleTimeoutSecsValue,
                              String expired, String expiredValue,
                              String closed, String closedValue,
                              String refCount, String refCountValue,
                              String action, String actionValue) {
        Map map = new HashMap();
        map.put(sessionType, sessionTypeValue);
        map.put(openQueries, Integer.parseInt(openQueriesValue));
        map.put(totalQueries, Integer.parseInt(totalQueriesValue));
        map.put(user, userValue);
        map.put(delegatedUser, delegatedUserValue);
        map.put(sessionId, sessionIdValue);
        map.put(networkAddress, networkAddressValue);
        map.put(defaultDatabase, defaultDatabaseValue);
        map.put(startTime, startTimeValue);
        map.put(lastAccessed, lastAccessedValue);
        map.put(idleTimeoutSecs, Integer.parseInt(idleTimeoutSecsValue));
        map.put(expired, Boolean.parseBoolean(expiredValue));
        map.put(closed, Boolean.parseBoolean(closedValue));
        map.put(refCount, Integer.parseInt(refCountValue));
        map.put(action, actionValue);
        return map;
    }

    public static Map map(String k1, Object v1) {
        Map map = new HashMap();
        map.put(k1, v1);
        return map;
    }

    public static Map map(String k1, Object v1, String k2, Object v2) {
        Map map = new HashMap();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    public static Map map(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map map = new HashMap();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    public static Map map(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4) {
        Map map = new HashMap();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return map;
    }
}
