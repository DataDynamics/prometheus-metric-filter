package io.datadynamics.prometheus.metricfilter.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/impala")
public class ImpalaController {

    private Logger log = LoggerFactory.getLogger(ImpalaController.class);

    @Autowired
    RestTemplate restTemplate;

}