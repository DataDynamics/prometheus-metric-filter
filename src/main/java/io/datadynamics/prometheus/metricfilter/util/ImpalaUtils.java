package io.datadynamics.prometheus.metricfilter.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.impala.thrift.TRuntimeProfileTree;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TCompactProtocol;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    public static Map decodeQueryProfile(String decoded) {
        log.debug("Query Profile : {}", decoded);
        String[] tokens = decoded.split(" ");
        if (tokens.length < 3) {
            throw new IllegalArgumentException("인코딩되어 있는 Query Profile이 유효하지 않은 포맷입니다.");
        }
        try {
            return MapUtils.map(
                    "timestamp", tokens[0],
                    "queryId", tokens[2],
                    "queryProfile", decode(Base64Utils.decode(tokens[2].getBytes()))
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Query Profile을 디코딩할 수 없습니다.", e);
        }
    }

    // https://github.com/apache/impala/blob/master/lib/python/impala_py_lib/profiles.py

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

}
