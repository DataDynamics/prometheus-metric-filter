# Prometheus Metrics Filter

## Installation

### Prometheus

다음의 커맨드로 다운로드후 압축해제합니다.

```shell
# wget https://github.com/prometheus/prometheus/releases/download/v3.0.0/prometheus-3.0.0.linux-amd64.tar.gz
# tar xvfz prometheus-3.0.0.linux-amd64.tar.gz
# mv prometheus-3.0.0 /opt/prometheus
```

다음과 같이 서비스 파일을 생성합니다.

```
# sudo vi /etc/systemd/system/prometheus.service
[Unit]
Description=Prometheus Server
Documentation=https://prometheus.io/docs/introduction/overview/
After=network-online.target

[Service]
User=prometheus
Restart=on-failure
ExecStart=/opt/prometheus/prometheus --config.file=/opt/prometheus/prometheus.yml --storage.tsdb.path=/opt/prometheus/data --storage.tsdb.retention.size=5GB

[Install]
WantedBy=multi-user.target
```

다음과 같이 Prometheus 설정 파일을 작성합니다.

```shell
# vi /opt/prometheus/prometheus.yml
# my global config
global:
  scrape_interval: 15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
  # scrape_timeout is set to the global default (10s).

# Alertmanager configuration
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          # - alertmanager:9093

# Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"

# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.

scrape_configs:

  - job_name: "kudu-master-1"
    metrics_path: /metrics
    scrape_interval: 5s
    params:
      url: ["http://hdu4.datalake.net:8051/metrics_prometheus"]
      name: ["kudu_tserver_clock_ntp_status,kudu_master_clock_ntp_status"]
    static_configs:
      - targets: ["nifi1.datalake.net:9091"]

  - job_name: "kudu-master-2"
    metrics_path: /metrics
    scrape_interval: 5s
    params:
      url: ["http://hdu5.datalake.net:8051/metrics_prometheus"]
      name: ["kudu_tserver_clock_ntp_status,kudu_master_clock_ntp_status"]
    static_configs:
      - targets: ["nifi1.datalake.net:9091"]

  - job_name: "kudu-master-3"
    metrics_path: /metrics
    scrape_interval: 5s
    params:
      url: ["http://hdu6.datalake.net:8051/metrics_prometheus"]
      name: ["kudu_tserver_clock_ntp_status,kudu_master_clock_ntp_status"]
    static_configs:
      - targets: ["nifi1.datalake.net:9091"]

  - job_name: "kudu-worker-1"
    metrics_path: /metrics
    scrape_interval: 5s
    params:
      url: ["http://hdw1.datalake.net:8050/metrics_prometheus"]
      name: ["kudu_tserver_clock_ntp_status,kudu_master_clock_ntp_status"]
    static_configs:
      - targets: ["nifi1.datalake.net:9091"]

  - job_name: "kudu-worker-2"
    metrics_path: /metrics
    scrape_interval: 5s
    params:
      url: ["http://hdw2.datalake.net:8050/metrics_prometheus"]
      name: ["kudu_tserver_clock_ntp_status,kudu_master_clock_ntp_status"]
    static_configs:
      - targets: ["nifi1.datalake.net:9091"]

  - job_name: "kudu-worker-3"
    metrics_path: /metrics
    scrape_interval: 5s
    params:
      url: ["http://hdw3.datalake.net:8050/metrics_prometheus"]
      name: ["kudu_tserver_clock_ntp_status,kudu_master_clock_ntp_status"]
    static_configs:
      - targets: ["nifi1.datalake.net:9091"]
```

### Grafana

다음의 커맨드로 설치를 진행하고 설치 및 서비스 재시작후 http://<IP>:3000 포트로 접속하도록 합니다.

```shell
# wget https://dl.grafana.com/oss/release/grafana-11.3.0+security~01-1.x86_64.rpm
# rpm -Uvh grafana-11.3.0+security~01-1.x86_64.rpm
# systemctl restart grafana-server
```
