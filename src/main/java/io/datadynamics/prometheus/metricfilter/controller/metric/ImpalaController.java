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

	@GetMapping(produces = "text/plain")
	ResponseEntity<String> getMetrics(@RequestParam(name = "url", required = true) String url) throws IOException {
		String v1 = getPrometheusMetrics(url + "/metrics_prometheus");

		Map status = ImpalaUtils.getRunning(url + "/queries");
		List<String> metrics = new ArrayList();
		metrics.add(String.format("# HELP impala_wait_to_close_query_count Number of Query to wait to close"));
		metrics.add(String.format("# TYPE impala_wait_to_close_query_count counter"));
		metrics.add(String.format("impala_wait_to_close_query_count{host=\"%s\"} %s", url, status.get("waitToClose")));

		metrics.add(String.format("# HELP impala_running_query_count Number of Running Query"));
		metrics.add(String.format("# TYPE impala_running_query_count counter"));
		metrics.add(String.format("impala_running_query_count{host=\"%s\"} %s", url, status.get("running")));

		String v2 = Joiner.on("\n").join(metrics);
		String finalMetric = v1 + "\n" + v2;

		log.debug("Impala Metric:\n{}", finalMetric);
		return ResponseEntity.ok(finalMetric);
	}

	private String getPrometheusMetrics(String url) {
		String metrics = restTemplate.getForObject(url, String.class);
		String v1 = StringUtils.replace(metrics, "_'", "_");
		return StringUtils.replace(v1, "'_", "_");
	}
}