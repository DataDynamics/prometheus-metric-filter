package io.datadynamics.prometheus.metricfilter.controller.metric;

import com.google.common.base.Joiner;
import io.datadynamics.prometheus.metricfilter.util.ImpalaUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics/impala")
public class ImpalaController {

    private Logger log = LoggerFactory.getLogger(ImpalaController.class);

    @Autowired
    RestTemplate restTemplate;

    /**
     * Impala Prometheus Metric에서 Running Query, Wait To Closed 개수를 반환한다.
     *
     * @param url Coordinator URL
     * @return Running Query, Wait To Closed 개수를 포함한 Prometheus Metrics
     * @throws IOException Coordinator URL에 접속할 수 없는 경우
     */
    @GetMapping(produces = "text/plain")
    ResponseEntity<String> getMetrics(@RequestParam(name = "url", required = true) String url) throws IOException {
        String v1 = getPrometheusMetrics(url + "/metrics_prometheus");

        Map queryStatus = ImpalaUtils.getRunning(url);
        Map sessionStatus = ImpalaUtils.getActiveSession(url);

        List<String> metrics = new ArrayList();
        metrics.add(String.format("# HELP impala_wait_to_close_query_count Number of Query to wait to close"));
        metrics.add(String.format("# TYPE impala_wait_to_close_query_count gauge"));
        metrics.add(String.format("impala_wait_to_close_query_count{instance=\"%s\" job=\"impala-coordinator\"} %s", io.datadynamics.prometheus.metricfilter.util.StringUtils.getHostname(url), queryStatus.get("waitToClose")));

        metrics.add(String.format("# HELP impala_running_query_count Number of Running Query"));
        metrics.add(String.format("# TYPE impala_running_query_count gauge"));
        metrics.add(String.format("impala_running_query_count{instance=\"%s\" job=\"impala-coordinator\"} %s", io.datadynamics.prometheus.metricfilter.util.StringUtils.getHostname(url), queryStatus.get("running")));

        metrics.add(String.format("# HELP impala_session_count Number of Session"));
        metrics.add(String.format("# TYPE impala_session_count gauge"));
        metrics.add(String.format("impala_session_count{instance=\"%s\" job=\"impala-coordinator\"} %s", io.datadynamics.prometheus.metricfilter.util.StringUtils.getHostname(url), sessionStatus.get("sessions")));

        metrics.add(String.format("# HELP impala_active_session_count Number of Active Session"));
        metrics.add(String.format("# TYPE impala_active_session_count gauge"));
        metrics.add(String.format("impala_active_session_count{instance=\"%s\" job=\"impala-coordinator\"} %s", io.datadynamics.prometheus.metricfilter.util.StringUtils.getHostname(url), sessionStatus.get("active")));

        metrics.add(String.format("# HELP impala_inactive_session_count Number of Inactive Session"));
        metrics.add(String.format("# TYPE impala_inactive_session_count gauge"));
        metrics.add(String.format("impala_inactive_session_count{instance=\"%s\" job=\"impala-coordinator\"} %s", io.datadynamics.prometheus.metricfilter.util.StringUtils.getHostname(url), sessionStatus.get("inactive")));

        String v2 = Joiner.on("\n").join(metrics);
        String finalMetric = v1 + "\n" + v2;

        log.debug("Impala Metric:\n{}", finalMetric);
        return ResponseEntity.ok(finalMetric);
    }

    /**
     * Impala Prometheus Metric의 경우 JDK의 버전에 따라서 Metric에서 <pre>-'...'-</pre>이 포함된 Metric을 제거한다.
     *
     * @param url Impala Coordinator Prometheus Metric URL
     * @return 정리한 Metric
     */
    private String getPrometheusMetrics(String url) {
        String metrics = restTemplate.getForObject(url, String.class);
        String v1 = StringUtils.replace(metrics, "_'", "_");
        return StringUtils.replace(v1, "'_", "_");
    }
}