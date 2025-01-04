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

    /**
     * Impala Coordinator의 /queries에서 inflight, waiting to closed의 건수를 추출한다.
     *
     * @param url Coordinator URL
     * @return inflight, waiting to closed의 건수
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
    public static Map getRunning(String url) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        Elements rows = doc.select("h3");
        return MapUtils.queryStatus("running", Integer.parseInt(StringUtils.replace(rows.get(0).text(), " queries in flight", "")),
                "waitToClose", Integer.parseInt(StringUtils.replace(rows.get(1).text(), " waiting to be closed [?]", "")));
    }

    /**
     * Query Profile 로그 파일에 저장되어 있는 인코딩되어 있는 Query Profile을 디코딩한다.
     *
     * @param encoded 인코딩되어 있는 Query Profile
     * @return 디코딩한 Query Profile
     */
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

    /**
     * Thrift로 serialize되어 있는 Query Profile을 deserialize한다.
     *
     * @param serializedQueryProfile Serialize되어 있는 Query Profile
     * @return Deserialize한 Thrift Object
     * @throws Exception Serialize, Deserialize할 수 없는 경우
     */
    public static TRuntimeProfileTree decode(byte[] serializedQueryProfile) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(serializedQueryProfile);

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

    /**
     * Impala Coordinator에서 Query ID 목록을 추출한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @param index          1 = inflight, 2 = waited, 3 = completed
     * @return Query ID 목록
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
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

    /**
     * 실행중인 쿼리의 Query Profile을 모두 추출한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @param queryIds       Query ID 목록
     * @return Query Profile 목록
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
    public static Map<String, String> queryProfiles(String coordinatorUrl, List<String> queryIds) throws IOException {
        Map<String, String> profiles = new HashMap<>();
        for (String queryId : queryIds) {
            profiles.put(queryId, queryProfile(coordinatorUrl, queryId));
        }
        return profiles;
    }

    /**
     * Query ID에 해당하는 Impala Query의 Query Profile을 추출한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @param queryId        Query ID
     * @return Query Profile
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
    public static String queryProfile(String coordinatorUrl, String queryId) throws IOException {
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

    /**
     * Impala Coordinator에서 Query Profile을 추출하여 파일로 저장한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @param index          1 = inflight, 2 = waited to close, 3 = completed
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
    public static void saveQueryProfiles(String coordinatorUrl, Integer index) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis());
        String filename = String.format("profiles_%s.txt", timestamp);
        String output = queryProfiles(coordinatorUrl, index);
        org.springframework.util.FileCopyUtils.copy(output.getBytes(), new File(filename));
    }

    /**
     * Impala Coordinator에서 Query Profile을 모두 추출한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @param index          1 = inflight, 2 = waited to close, 3 = completed
     * @return 텍스트 형식의 Query Profile
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
    public static String queryProfiles(String coordinatorUrl, Integer index) throws IOException {
        List<String> output = new ArrayList<>();
        List<String> queryIds = queryIds(coordinatorUrl, index);
        Map<String, String> profiles = queryProfiles(coordinatorUrl, queryIds);
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
        return Joiner.on("\n").join(output);
    }

    /**
     * Coordinator의 /query_summary에서 쿼리의 Timeline, Summary를 추출한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @param queryId        Query ID
     * @return Timeline, Summary
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
    private static List<String> getSummary(String coordinatorUrl, String queryId) throws IOException {
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

    /**
     * Impaal Corodinator의 /sessions에서 세션 목록을 추출한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @return 세션 목록
     * @throws IOException Coordinator에 접속할 수 없는 경우
     */
    public static List<Map> getSessions(String coordinatorUrl) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(coordinatorUrl + "/sessions").get();
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

    /**
     * Session ID로 Impala Coordinator의 접속 세션을 강제 종료한다.
     *
     * @param coordinatorUrl Coordinator URL
     * @param sessionId      Session ID
     * @return 성공시 true
     */
    public static boolean killSession(String coordinatorUrl, String sessionId) {
        String url = String.format("%s/close_session?session_id=%s", coordinatorUrl, sessionId);
        RestTemplate template = new RestTemplate();
        String result = template.getForObject(url, String.class);
        String successPattern = String.format("Session %s closed successfully", sessionId);
        return org.springframework.util.StringUtils.countOccurrencesOf(result, successPattern) > 0;
    }

}