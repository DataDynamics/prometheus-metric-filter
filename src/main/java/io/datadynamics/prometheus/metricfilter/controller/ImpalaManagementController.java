package io.datadynamics.prometheus.metricfilter.controller;

import io.datadynamics.prometheus.metricfilter.util.ImpalaUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(
            summary = "세션 목록",
            description = "Impala Coordinator에 등록되어 있는 모든 세션목록을 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "정상적으로 세션 정보를 수집한 경우")
            }
    )
    @GetMapping("/sessions")
    public List<Map> sessions(@RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.getSessions(url);
    }

    @Operation(
            summary = "세션 강제 종료",
            description = "지정한 Session ID로 세션을 강제 종료합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "정상적으로 세션을 종료한 경우"),
                    @ApiResponse(responseCode = "404", description = "존재하지 않는 세션을 강제 종료한 경우")
            }
    )
    @GetMapping("/sessions/killBySessionId")
    public ResponseEntity killBySessionId(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url,
            @Parameter(name = "sessionId", description = "Impala Session ID", required = true, example = "9f409c414838f91d:768089000d2676b4")
            @RequestParam(name = "sessionId", required = true) String sessionId) throws IOException {
        return ImpalaUtils.killSession(url, sessionId) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "세션 강제 종료",
            description = "지정한 User가 생성한 모든 세션을 강제 종료합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "정상적으로 세션을 강제 종료한 경우")
            }
    )
    @GetMapping("/sessions/killByUsername")
    public ResponseEntity killByUsername(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url,
            @Parameter(name = "username", description = "Impala Username", required = true, example = "john")
            @RequestParam(name = "username", required = true) String username) throws IOException {
        List<Map> sessions = ImpalaUtils.getSessions(url);
        Map<String, Boolean> result = new java.util.HashMap<>();
        sessions.stream().filter(session -> username.equals(session.get("user"))).forEach(session -> {
            result.put((String) session.get("sessionId"), ImpalaUtils.killSession(url, (String) session.get("sessionId")));
        });
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "User 기반 세션수",
            description = "세션 목록에서 Username 기준으로 세션수를 합산합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "정상적으로 세션 처리 현황을 처리한 경우")
            }
    )
    @GetMapping("/sessions/stats/byUsername")
    public ResponseEntity sessionStatsByUsername(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.getSessionStatsByUsername(url));
    }

    @Operation(
            summary = "Delegated User 기반 세션수",
            description = "세션 목록에서 Delegated Username 기준으로 세션수를 합산합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Delegated User 기반 세션수")
            }
    )
    @GetMapping("/sessions/stats/byDelegatedUsername")
    public ResponseEntity sessionStatsByDelegatedUsername(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
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

    @Operation(
            summary = "현재 실행중인 Impala Query의 Query ID 목록",
            description = "현재 실행중인 Impala Query ID 목록을 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "정상적으로 Query ID 목록을 생성한 경우")
            }
    )
    @GetMapping("/profiles/inflightQueryIds")
    public List<String> inflightQueryIds(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.queryIds(url, 1);
    }

    @Operation(
            summary = "현재 종료 대기중인 Impala Query의 Query ID 목록",
            description = "현재 종료 대기중인 Impala Query ID 목록을 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "정상적으로 Query ID 목록을 생성한 경우")
            }
    )
    @GetMapping("/profiles/waitToCloseQueryIds")
    public List<String> waitToCloseQueryIds(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.queryIds(url, 2);
    }

    @Operation(
            summary = "실행을 완료한 Impala Query의 Query ID 목록",
            description = "실행을 완료한 Impala Query ID 목록을 반환합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "정상적으로 Query ID 목록을 생성한 경우")
            }
    )
    @GetMapping("/profiles/completedQueryIds")
    public List<String> completedQueryIds(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
        return ImpalaUtils.queryIds(url, 3);
    }

    @GetMapping("/profiles")
    public ResponseEntity<String> queryProfile(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url,
            @Parameter(name = "queryId", description = "Impala Query ID", required = true, example = "8b46af661ebd0bb4:19ef3ee600000000")
            @RequestParam(name = "queryId", required = true) String queryId) throws IOException {
        String profile = ImpalaUtils.queryProfile(url, queryId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profiles/inflight")
    public ResponseEntity<String> inflightQueryProfiles(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.queryProfiles(url, 1));
    }

    @GetMapping("/profiles/waitToClose")
    public ResponseEntity<String> waitToCloseQueryProfiles(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.queryProfiles(url, 2));
    }

    @GetMapping("/profiles/completed")
    public ResponseEntity<String> completedQueryProfiles(
            @Parameter(name = "url", description = "Impala Coordinator URL", required = true, example = "http://impala:25000")
            @RequestParam(name = "url", required = true) String url) throws IOException {
        return ResponseEntity.ok(ImpalaUtils.queryProfiles(url, 3));
    }

}