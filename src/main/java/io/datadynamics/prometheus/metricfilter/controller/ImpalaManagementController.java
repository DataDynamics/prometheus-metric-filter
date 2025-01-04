package io.datadynamics.prometheus.metricfilter.controller;

import io.datadynamics.prometheus.metricfilter.util.ImpalaUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/impala")
public class ImpalaManagementController {

    @GetMapping("/sessions")
    public List<Map> sessions(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.getSessions(url);
    }

    @GetMapping("/sessions/killBySessionId")
    public ResponseEntity killBySessionId(@RequestParam(name = "url", required = true) String url, @RequestParam(name = "sessionId", required = true) String sessionId) throws IOException {
        return ImpalaUtils.killSession(url, sessionId) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/sessions/killByUsername")
    public ResponseEntity killByUsername(@RequestParam(name = "url", required = true) String url, @RequestParam(name = "username", required = true) String username) throws IOException {
        List<Map> sessions = ImpalaUtils.getSessions(url);
        Map<String, Boolean> result = new java.util.HashMap<>();
        sessions.stream().filter(session -> username.equals(session.get("user"))).forEach(session -> {
            result.put((String) session.get("sessionId"), ImpalaUtils.killSession(url, (String) session.get("sessionId")));
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions/stats/byUsername")
    public ResponseEntity sessionStatsByUsername(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.getSessionStatsByUsername(url));
    }

    @GetMapping("/sessions/stats/byDelegatedUsername")
    public ResponseEntity sessionStatsByDelegatedUsername(@RequestParam(name = "url", required = true) String url) throws IOException {
        List<Map> sessions = ImpalaUtils.getSessions(url);
        Map<String, AtomicInteger> result = new java.util.HashMap<>();
        sessions.forEach(session -> {
            String username = (String) session.get("delegatedUser");
            if (result.get(username) == null) {
                result.put(username, new AtomicInteger(0));
            }
            result.get(username).incrementAndGet();
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profiles/inflightQueryIds")
    public List<String> inflightQueryIds(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.queryIds(url, 1);
    }

    @GetMapping("/profiles/waitToCloseQueryIds")
    public List<String> waitToCloseQueryIds(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.queryIds(url, 2);
    }

    @GetMapping("/profiles/completedQueryIds")
    public List<String> completedQueryIds(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.queryIds(url, 3);
    }

    @GetMapping("/profiles")
    public ResponseEntity<String> queryProfile(@RequestParam(name = "url", required = true) String url, @RequestParam(name = "queryId", required = true) String queryId) throws IOException {
        String profile = ImpalaUtils.queryProfile(url, queryId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profiles/inflight")
    public ResponseEntity<String> inflightQueryProfiles(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.queryProfiles(url, 1));
    }

    @GetMapping("/profiles/waitToClose")
    public ResponseEntity<String> waitToCloseQueryProfiles(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.queryProfiles(url, 2));
    }

    @GetMapping("/profiles/completed")
    public ResponseEntity<String> completedQueryProfiles(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.queryProfiles(url, 3));
    }

}