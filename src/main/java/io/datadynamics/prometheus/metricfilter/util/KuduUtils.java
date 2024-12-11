package io.datadynamics.prometheus.metricfilter.util;

import org.springframework.web.client.RestTemplate;

import java.util.List;

public class KuduUtils {

    public static List getMetrics(String url) {
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.getForObject(url, List.class);
    }

}
