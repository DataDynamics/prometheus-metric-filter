package io.datadynamics.prometheus.metricfilter.controller;

import com.google.common.base.Joiner;
import io.datadynamics.prometheus.metricfilter.Patterns;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static io.micrometer.common.util.StringUtils.isEmpty;

@Slf4j
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    @Autowired
    RestTemplate restTemplate;

    @GetMapping
    ResponseEntity<String> metrics(@RequestParam(name = "URL", required = true) String url, @RequestParam(name = "Metric's Name", required = true) String name) {
        Assert.notNull(url, "URL은 필수값입니다.");
        if (isEmpty(name)) {
            return ResponseEntity.badRequest().body("name 옵션을 지정하십시오.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "text/plain");

        HttpEntity request = new HttpEntity(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                String.class
        );

        String body = response.getBody();
        if (StringUtils.isEmpty(name)) {
            return ResponseEntity.ok(body);
        } else {
            List<String> metrics = new ArrayList<>();
            Scanner scanner = new Scanner(body);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith(String.format(Patterns.HELP, name)) || line.startsWith(String.format(Patterns.TYPE, name)) || line.startsWith(name)) {
                    // ignored
                } else {
                    metrics.add(line);
                }
            }
            scanner.close();

            return ResponseEntity.ok(Joiner.on("\n").join(metrics));
        }
    }

}
