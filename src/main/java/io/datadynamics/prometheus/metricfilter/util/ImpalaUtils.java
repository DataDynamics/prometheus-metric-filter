package io.datadynamics.prometheus.metricfilter.util;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.impala.thrift.TRuntimeProfileTree;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TCompactProtocol;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;

public class ImpalaUtils {

    private static Logger log = LoggerFactory.getLogger(ImpalaUtils.class);

    public static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static Map getRunning(String url) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        Elements rows = doc.select("h3");
        return MapUtils.queryStatus("running", Integer.parseInt(StringUtils.replace(rows.get(0).text(), " queries in flight", "")),
                "waitToClose", Integer.parseInt(StringUtils.replace(rows.get(1).text(), " waiting to be closed [?]", "")));
    }

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
        for (int i = 1; i < rows.size(); i++) {
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

    public static Map decodeQueryProfile(String encoded) {
        log.debug("Query Profile : {}", encoded);
        String[] tokens = encoded.split(" ");
        if (tokens.length < 3) {
            throw new IllegalArgumentException("인코딩되어 있는 Query Profile이 유효하지 않은 포맷입니다.");
        }
        try {
            return MapUtils.map(
                    "timestamp", tokens[0],
                    "queryId", tokens[1],
                    "queryProfile", decode(Base64Utils.decode(tokens[2].getBytes()))
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Query Profile을 디코딩할 수 없습니다.", e);
        }
    }

    public static TRuntimeProfileTree decode(byte[] input) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {
            int decompressedSize = inflater.inflate(buffer);
            outputStream.write(buffer, 0, decompressedSize);
        }

        byte[] bytes = outputStream.toByteArray();
        TRuntimeProfileTree tree = new TRuntimeProfileTree();
        TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());
        deserializer.deserialize(tree, bytes);
        tree.validate();
        return tree;
    }

    public static List<String> queryIds(String coordinatorUrl, Integer index) throws IOException {
        List<String> queryIds = new ArrayList<>();
        org.jsoup.nodes.Document doc = Jsoup.connect(coordinatorUrl + "/queries").get();
        Elements elements = doc.selectXpath(String.format("/html/body/div/table[%s]/tbody/tr", index)); // 1 = inflight, 2 = waited, 3 = completed
        if (elements.size() > 1) {
            for (int i = 1; i < elements.size(); i++) {
                Elements tds = elements.get(i).children();
                queryIds.add(tds.get(0).select("a").text());
            }
        }
        return queryIds;

    }

    public static Map<String, String> inflightQueryProfiles(String coordinatorUrl, List<String> queryIds) throws IOException {
        Map<String, String> profiles = new HashMap<>();
        for (String queryId : queryIds) {
            profiles.put(queryId, inflightQueryProfile(coordinatorUrl, queryId));
        }
        return profiles;
    }

    public static String inflightQueryProfile(String coordinatorUrl, String queryId) throws IOException {
        RestTemplate template = new RestTemplate();
        try {
            String url = coordinatorUrl + "/query_profile_plain_text?query_id=" + queryId;
            String profile = template.getForObject(url, String.class);
            log.debug("Query Profile : {}", profile);
            return profile;
        } catch (Exception e) {
            return ExceptionUtils.getStackTrace(e);
        }
    }

    public static void saveQueryProfiles(String coordinatorUrl, Integer index) throws IOException {
        List<String> output = new ArrayList<>();
        List<String> queryIds = queryIds(coordinatorUrl, index);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis());
        String filename = String.format("profiles_%s.txt", timestamp);
        Map<String, String> profiles = inflightQueryProfiles(coordinatorUrl, queryIds);
        profiles.keySet().forEach(queryId -> {
            output.add("=========================================================================");
            output.add(String.format("Query ID : %s", queryId));
            output.add("=========================================================================");
            try {
                output.addAll(getSummary(coordinatorUrl, queryId));
            } catch (IOException e) {
                // Ignored
            }
            output.add(profiles.get(queryId));
        });
        org.springframework.util.FileCopyUtils.copy(Joiner.on("\n").join(output).getBytes(), new File(filename));
    }

    public static List<String> getSummary(String coordinatorUrl, String queryId) throws IOException {
        List<String> output = new ArrayList<>();
        String url = String.format("%s/query_summary?query_id=%s", coordinatorUrl, queryId);
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        Element timeline = doc.getElementById("timeline");
        Element summary = doc.getElementById("summary");

        output.add("\n\n=[Timeline]==============================================================\n\n");
        if (StringUtils.isEmpty(timeline.text().trim())) {
            output.add("N/A");
        } else {
            output.add(timeline.text());
        }

        output.add("\n\n=[Summary]===============================================================\n\n");
        if (StringUtils.isEmpty(summary.text().trim())) {
            output.add("N/A");
        } else {
            output.add(summary.text());
        }

        output.add("\n\n=[Profile]===============================================================\n\n");
        return output;
    }

}
