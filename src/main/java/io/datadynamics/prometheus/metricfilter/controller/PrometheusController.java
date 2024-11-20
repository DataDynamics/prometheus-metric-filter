package io.datadynamics.prometheus.metricfilter.controller;

import com.google.common.base.Joiner;
import io.datadynamics.prometheus.metricfilter.Patterns;
import io.datadynamics.prometheus.metricfilter.util.ImpalaUtils;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static io.micrometer.common.util.StringUtils.isEmpty;

@Slf4j
@RestController
@RequestMapping("/metrics")
public class PrometheusController {

    @Autowired
    RestTemplate restTemplate;

    @GetMapping
    ResponseEntity<String> getMetrics(@RequestParam(name = "URL", required = true) String url,
                                      @RequestParam(name = "Metric's Name", required = true) String name) {

        Assert.notNull(url, "URL은 필수값입니다.");

        if (isEmpty(name)) {
            return ResponseEntity.badRequest().body("name 옵션을 지정하십시오.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "text/plain"); // for Prometheus

        HttpEntity request = new HttpEntity(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
        );

        String body = response.getBody();
        String[] names = org.apache.commons.lang3.StringUtils.splitPreserveAllTokens(name);
        if (StringUtils.isEmpty(name)) {
            return ResponseEntity.ok(body);
        } else {
            List<String> metrics = new ArrayList<>();
            Scanner scanner = new Scanner(body);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                boolean isFilter = false;

                // metric name이 속해 있는지 확인한 후에 필터링 한다.
                for (String n : names) {
                    String trimmedName = n.trim();
                    if ((!StringUtils.isEmpty(trimmedName)) && line.startsWith(String.format(Patterns.METRIC_HELP, trimmedName)) ||
                            line.startsWith(String.format(Patterns.METRIC_TYPE, trimmedName)) ||
                            line.startsWith(String.format(Patterns.METRIC_NAME, trimmedName))
                    ) {
                        isFilter = true;
                        break;
                    }
                }

                // 핕터링 하지 않으면 metric을 사용한다.
                if (!isFilter) {
                    metrics.add(line);
                }
            }
            scanner.close();

            return ResponseEntity.ok(Joiner.on("\n").join(metrics));
        }
    }

    @GetMapping("/coordinator")
    ResponseEntity<String> getQueryMetrics(@RequestParam(name = "URL", required = true) String url) throws IOException {
        Map status = ImpalaUtils.getRunning(url);
        List<String> metrics = new ArrayList();
        metrics.add(String.format("# HELP impala_wait_to_close_query_count Number of Query to wait to close"));
        metrics.add(String.format("# TYPE impala_wait_to_close_query_count counter"));
        metrics.add(String.format("impala_wait_to_close_query_count{host=\"%s\"} %s", url, status.get("waitToClose")));

        metrics.add(String.format("# HELP impala_running_query_count Number of Running Query"));
        metrics.add(String.format("# TYPE impala_running_query_count counter"));
        metrics.add(String.format("impala_running_query_count{host=\"%s\"} %s", url, status.get("running")));

        return ResponseEntity.ok(Joiner.on("\n").join(metrics));
    }

}