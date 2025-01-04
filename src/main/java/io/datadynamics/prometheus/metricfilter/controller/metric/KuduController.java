package io.datadynamics.prometheus.metricfilter.controller.metric;

import com.google.common.base.Joiner;
import io.datadynamics.prometheus.metricfilter.Patterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.apache.commons.lang3.StringUtils.isEmpty;

@RestController
@RequestMapping("/metrics/kudu")
public class KuduController {

    @Autowired
    RestTemplate restTemplate;
    private Logger log = LoggerFactory.getLogger(KuduController.class);

    @GetMapping(produces = "text/plain")
    ResponseEntity<String> getMetrics(@RequestParam(name = "url", required = true) String url,
                                      @RequestParam(name = "name", required = true) String name) {
        log.info("url: {}, name: {}", url, name);

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
        if (isEmpty(name)) {
            return ResponseEntity.ok(body);
        } else {
            String[] names = org.apache.commons.lang3.StringUtils.splitPreserveAllTokens(name, ",");
            List<String> metrics = new ArrayList<>();
            Scanner scanner = new Scanner(body);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                boolean isFilter = false;

                // metric name이 속해 있는지 확인한 후에 필터링 한다.
                for (String n : names) {
                    log.debug("검증할 Name : {}, LINE : {}", n, line);
                    String trimmedName = n.trim();
                    if ((!isEmpty(trimmedName)) && (line.startsWith(String.format(Patterns.METRIC_HELP, trimmedName)) ||
                            line.startsWith(String.format(Patterns.METRIC_TYPE, trimmedName)) ||
                            line.startsWith(String.format(Patterns.METRIC_NAME, trimmedName)))
                    ) {
                        log.debug("Filtered: {}", line);

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

            String metricsString = Joiner.on("\n").join(metrics);

            log.info("Kudu Metrics:\n{}", metricsString);
            return ResponseEntity.ok(metricsString);
        }
    }
}