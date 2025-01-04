package io.datadynamics.prometheus.metricfilter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class UtilityConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}