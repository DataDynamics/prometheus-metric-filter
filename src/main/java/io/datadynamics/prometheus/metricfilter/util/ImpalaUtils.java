package io.datadynamics.prometheus.metricfilter.util;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.*;
import java.util.Map;

public class ImpalaUtils {

    /**
     * @param url Session URL ("http://10.0.1.71:25000/sessions")
     * @return 세션 목록
     * @throws IOException
     */
    public List<Map> getSessions(String url) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        Element table = doc.getElementById("sessions-tbl");
        Elements rows = table.select("tr");
        List<Map> sessions = new ArrayList();
        for(int i = 1 ; i < rows.size(); i++) {
            Element row = rows.get(i);
            Elements cols = row.select("td");
            Map session = MapUtils.session(
                    "sessionType", cols.get(0).text(),
                    "openQueries", cols.get(1).text(),
                    "totalQueries", cols.get(2).text(),
                    "user", cols.get(3).text(),
                    "delegatedUser", cols.get(4).text(),
                    "sessionId", cols.get(5).text(),
                    "networkAddress", cols.get(6).text(),
                    "defaultDatabase", cols.get(7).text(),
                    "startTime", cols.get(8).text(),
                    "lastAccessed", cols.get(9).text(),
                    "idleTimeoutSecs", cols.get(10).text(),
                    "expired", cols.get(11).text(),
                    "closed", cols.get(12).text(),
                    "refCount", cols.get(13).text(),
                    "action", cols.get(14).text()
            );
            sessions.add(session);
        }
        return sessions;
    }

    public static Map getRunning(String url) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        Elements rows = doc.select("h3");
        System.out.println(StringUtils.replace(rows.get(0).text(), " queries in flight", ""));
        System.out.println(StringUtils.replace(rows.get(1).text(), " waiting to be closed [?]", ""));

        return MapUtils.queryStatus("running", Integer.parseInt(StringUtils.replace(rows.get(0).text(), " queries in flight", "")),
                "waitToClose", Integer.parseInt(StringUtils.replace(rows.get(1).text(), " waiting to be closed [?]", "")));
    }

}
