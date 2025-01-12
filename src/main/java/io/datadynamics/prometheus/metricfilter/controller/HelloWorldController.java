package io.datadynamics.prometheus.metricfilter.controller;

import io.datadynamics.prometheus.metricfilter.util.MapUtils;
import io.micrometer.core.annotation.Counted;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/helloworld")
public class HelloWorldController {

    @Counted("helloworld.sayhello.count")
    @GetMapping("/sayhello")
    public ResponseEntity sayhello() throws IOException {

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }

        return ResponseEntity.ok(MapUtils.map("message", "Hello World"));
    }
}
