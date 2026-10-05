# Kubernetes Native Microservices Workshop

This repository contains a workshop-ready Kubernetes-native microservices demo
built with Spring Boot 3.x and Java 21. It demonstrates service discovery,
ConfigMaps, Actuator health probes, distributed tracing, structured logging,
and an OpenTelemetry Collector agent/gateway topology.

## Observability architecture

```text
card-service ──HTTP/W3C trace context──> account-service

Java services ──OTLP/gRPC traces──────────────────────────────────┐
                                                                  │ 
Java services ──JSON stdout──> /var/log/pods/k8s-workshop_*       │
                                      │ filelog                   │
                                      v                           │
                         OTel Collector Agents                    │
                              (DaemonSet)                         │
                                      │                           v
                                      └──OTLP/gRPC logs──> OTel Collector Gateway
                                                              (Deployment + Service)
                                                                 │
                                              ┌──────────────────┴──────────────────┐
                                              │                                     │
                                           traces                                  logs
                                              v                                     v
                                            Jaeger                         Elasticsearch ──> Kibana
```

The Java services continue to use SLF4J structured key-value logging. Spring
Boot writes one Logstash-compatible JSON object per line to stdout; no
OpenTelemetry Logback appender is used.

The Collector agents run on every Kubernetes node and:

1. read only application logs from `/var/log/pods/k8s-workshop_*/*/*.log`;
2. parse the CRI/Docker container envelope;
3. parse the nested Spring Boot JSON object;
4. enrich it with Kubernetes metadata;
5. forward logs over OTLP/gRPC to the gateway.

The gateway is the only component that knows the backend endpoints:

- logs are written to Elasticsearch index `otel-logs`;
- traces are forwarded to Jaeger over OTLP/gRPC.

The monitoring foundation is installed separately in the `monitoring`
namespace:

```text
kube-state-metrics ─┐
kubelet/cAdvisor ───┤
node-exporter ──────┼──HTTP scrape──> Prometheus ──> PVC (10d) ──> Grafana
Java services ──────┘                       │
  :8081/actuator/prometheus                 └──rules──> Alertmanager
```

Prometheus uses the pull model and scrapes metric endpoints directly. Metrics
do not pass through the OpenTelemetry Collector. Java service metrics are
scraped from the management port through a PodMonitor; OTLP metric export from
the applications stays disabled. Collector self-monitoring is added in the next
monitoring stage.

## Prerequisites

- Java 21
- Docker Desktop with Kubernetes enabled
- `kubectl`
- Helm 3
- Docker Engine and Docker Compose

This local setup keeps Jaeger in Docker Compose outside Kubernetes. Therefore,
the Collector gateway reaches it through `host.docker.internal:4317`, which is
a Docker Desktop-specific hostname. In a non-Docker-Desktop cluster, deploy
Jaeger in the cluster or configure a reachable external OTLP endpoint.

## Build and test

From the repository root:

```bash
./mvnw clean test
./mvnw clean package
```

## Start Jaeger

```bash
docker compose -f observability/tracing/docker-compose-all.yaml up -d
```

Jaeger UI:

- http://localhost:16686

## Build local application images

```bash
./scripts/build-images.sh
```

## Deploy the complete workshop

```bash
./scripts/k8s-deploy.sh
```

The script installs, in order:

- the `observability` namespace;
- Elasticsearch and Kibana;
- OTel Collector RBAC and configuration;
- OTel Collector gateway and node agents;
- `kube-prometheus-stack` in the `monitoring` namespace;
- both Java services in the `k8s-workshop` namespace.

To apply the resources manually:

```bash
kubectl apply -k observability/logging/k8s
bash scripts/monitoring-deploy.sh
kubectl apply -k k8s/overlays/local
```

The monitoring installation is reproducible: chart version `87.18.1` is pinned
in `scripts/monitoring-deploy.sh`. Workshop overrides are stored in
`observability/monitoring/kube-prometheus-stack-values.yaml`.

The applications send traces to the cross-namespace endpoint:

```text
http://otel-collector-gateway.observability.svc.cluster.local:4317
```

## Access the services

Card Service API:

```bash
kubectl -n k8s-workshop port-forward svc/card-service 8080:8080
```

The local overlay also exposes NodePort `30080`, available through:

```text
http://localhost:30080/api/cards/...
```

Kibana:

```bash
kubectl -n observability port-forward svc/kibana 5601:5601
```

Open http://localhost:5601 and create a Data View:

- name/index pattern: `otel-logs*`
- time field: `@timestamp`

Collector internal metrics:

```bash
kubectl -n observability port-forward svc/otel-collector-gateway 8888:8888
curl http://localhost:8888/metrics
```

## Monitoring foundation

The Helm release installs Prometheus Operator, Prometheus, Grafana,
Alertmanager, kube-state-metrics, and node-exporter. Prometheus scrapes every
30 seconds, keeps data for 10 days (up to 8 GB), and stores it on a 10 GiB PVC.
The PVC uses the cluster's default StorageClass.

Prometheus:

```bash
kubectl -n monitoring port-forward svc/kube-prometheus-stack-prometheus 9090:9090
```

Grafana:

```bash
kubectl -n monitoring get secret kube-prometheus-stack-grafana \
  -o jsonpath="{.data.admin-password}" | base64 --decode
kubectl -n monitoring port-forward svc/kube-prometheus-stack-grafana 3000:80
```

Open http://localhost:3000 and sign in as `admin` with the password returned by
the first command. The password is generated into a Kubernetes Secret and is
not committed to the repository.

Alertmanager:

```bash
kubectl -n monitoring port-forward svc/kube-prometheus-stack-alertmanager 9093:9093
```

External notification receivers are intentionally not configured yet. The
built-in Kubernetes alert rules and Alertmanager UI are available immediately.

Docker Desktop does not publish the controller-manager, scheduler, etcd, or
kube-proxy metrics on their usual node ports. Their ServiceMonitors are disabled
in the workshop values to avoid permanently failing targets and false alerts.
Enable them for a cluster that exposes those control-plane endpoints.

## Application metrics

Both services publish Micrometer metrics in the Prometheus text format on the
management port:

```bash
kubectl -n k8s-workshop port-forward deployment/card-service 8081:8081
curl http://localhost:8081/actuator/prometheus
```

The application Services publish port `8080` only, so the generated
EndpointSlice never contains the management port and a ServiceMonitor would
have no address to scrape. `observability/monitoring/k8s` therefore installs a
PodMonitor, which discovers pods directly through the API server and addresses
the container port by its name `management`. The object lives in the
`monitoring` namespace because the chart values restrict the operator's
`podMonitorNamespaceSelector` to it, and selects its targets in `k8s-workshop`
through `spec.namespaceSelector`.

Two labels identify a service, and they are not interchangeable:

- `service` is an application tag, added by `management.metrics.tags.service`.
  It matches the `service` field of the structured logs, so one identifier
  correlates metrics, logs, and traces. It exists only on meters the
  application itself publishes.
- `job` is a target label, taken from the pod label `app` through the
  PodMonitor's `jobLabel`. It is also present on the series Prometheus
  synthesises per target, such as `up` and `scrape_samples_scraped`, which
  carry no application tags at all. Without `jobLabel` the operator would name
  the job after the PodMonitor, giving both services the same value and making
  per-service target alerts impossible.

Use `service` to correlate across signals and `job` to alert on scrape health.

The RED signals come from the `http.server.requests` timer that Spring Boot
records for every request, with no instrumentation in the controllers:

```promql
sum by (service) (rate(http_server_requests_seconds_count[5m]))

sum by (service) (rate(http_server_requests_seconds_count{outcome="SERVER_ERROR"}[5m]))
  / sum by (service) (rate(http_server_requests_seconds_count[5m]))
```

Latency is available as a count/sum average and a maximum. Histogram buckets
are deliberately not enabled yet: they multiply the series count, and they are
introduced together with recording rules and SLO alerts in the next stage.

Verify the scrape targets:

```bash
kubectl -n monitoring get podmonitor
kubectl -n monitoring port-forward svc/kube-prometheus-stack-prometheus 9090:9090
```

Open http://localhost:9090/targets and look for
`podMonitor/monitoring/k8s-workshop-java-services/0`.

## Generating traffic

A freshly deployed workshop has no requests, so every RED query returns an
empty result and every graph is a flat line. The generator produces a steady
stream:

```bash
./scripts/load-gen.sh            # 1800 iterations, one second apart
./scripts/load-gen.sh 300 0.2    # shorter and faster
```

It runs as a pod inside the cluster, because the pod network is not reachable
from the host. Each iteration calls `add-money`, `pay`, and a card lookup, and
every fifth iteration adds a deliberate `404` so that the error ratio stays
around 6 percent instead of zero. Because `add-money` and `pay` both call
`account-service`, the same traffic also fills Jaeger with two-service traces.

```bash
kubectl -n k8s-workshop logs -f load-gen
kubectl -n k8s-workshop delete pod load-gen
```

Re-running the script replaces an existing `load-gen` pod.

## Structured logging

Application code uses the SLF4J fluent key-value API. Request-scoped records
include:

- `service`;
- `event` and `outcome`;
- `traceId` and `spanId`;
- `requestId`;
- `errorCode`, `downstreamService`, and `httpStatus` when applicable.

The Collector adds:

- `k8sNamespace`;
- `k8sPodName` and `k8sPodUid`;
- `k8sContainerName`;
- `k8sNodeName`;
- `k8sDeploymentName`.

Balances, transaction amounts, merchant names, account/card identifiers, and
raw downstream response bodies are deliberately excluded from logs.

An incoming `X-Request-Id` is accepted only when it contains 1-128 letters,
digits, dots, underscores, or hyphens. Otherwise, the service generates a UUID.
The effective ID is returned in the response and propagated from
`card-service` to `account-service`.

Example Kibana queries:

```text
service : "card-service" and event : "card_payment"
traceId : "<trace-id-from-jaeger-or-a-log-record>"
requestId : "<value-from-X-Request-Id>"
k8sNamespace : "k8s-workshop"
```

## Reliability decisions

- Collector images are pinned to `otel/opentelemetry-collector-contrib:0.153.0`.
- `filelog` starts at the end of existing files during the Fluentd migration,
  avoiding historical re-ingestion.
- Per-node file offsets and the agent-to-gateway sending queue use
  `file_storage` under the node host path `/var/lib/otelcol-agent`.
- The agent mounts both `/var/log/pods` and `/var/lib/docker/containers`.
  Docker Desktop exposes pod log files as symlinks into the latter directory,
  so both read-only mounts are required there.
- Gateway sending queues use the
  `otel-collector-gateway-storage` PersistentVolumeClaim.
- Agents and gateway use `memory_limiter`, batching, retry, health probes, and
  resource limits.
- Collector logs are not collected: the `filelog` include scope is restricted
  to the application namespace and an explicit Collector exclusion is present.
- Elasticsearch request/response bodies and failed input documents are not
  enabled in Collector diagnostics.

Elasticsearch is currently version 7.17.24. The gateway therefore uses the
`bodymap` mapping mode: parsed application fields remain at the document root
and Kubernetes metadata is merged into that body before export. Collector
0.153 selects this mode through the `elastic.mapping.mode` scope attribute,
which the gateway sets with a transform processor. When Elasticsearch is
upgraded to a supported 8.x release, the OTel-native mapping mode can be
evaluated separately.

## Trace correlation

Both services use Micrometer Tracing with the OpenTelemetry bridge and export
traces over OTLP/gRPC. The endpoint is configured through
`OTEL_EXPORTER_OTLP_ENDPOINT`.

`card-service` constructs its client from Spring Boot's auto-configured
`RestClient.Builder`, enabling W3C trace-context propagation. The
`card-service -> account-service` call is therefore represented by one
distributed trace.

Sampling is configurable through `OTEL_TRACES_SAMPLER_PROBABILITY` and is set
to `1.0` in the workshop ConfigMaps so every test request can be found in
Jaeger. Production deployments should choose sampling according to traffic,
cost, and troubleshooting requirements.

## End-to-end verification

After creating a request that calls both services, verify:

1. Jaeger contains one distributed trace with spans from both services.
2. `otel-logs*` contains records from both services.
3. Searching Elasticsearch by the Jaeger `traceId` returns correlated logs.
4. Logs contain `traceId`, `spanId`, `requestId`, `service`, `event`, and
   Kubernetes metadata.
5. Logs do not contain balances or raw downstream response bodies.

Useful commands:

```bash
kubectl -n observability get pods
kubectl -n observability logs deployment/otel-collector-gateway
kubectl -n observability logs daemonset/otel-collector-agent
kubectl -n observability get pvc otel-collector-gateway-storage

kubectl -n monitoring get pods
kubectl -n monitoring get pvc
kubectl -n monitoring get prometheus,alertmanager

kubectl -n k8s-workshop get pods
kubectl -n k8s-workshop logs deployment/card-service
kubectl -n k8s-workshop logs deployment/account-service
```

## Apply configuration changes

ConfigMap changes do not restart pods automatically:

```bash
kubectl apply -f observability/logging/k8s/otel-collector/agent-configmap.yaml
kubectl apply -f observability/logging/k8s/otel-collector/gateway-configmap.yaml
kubectl -n observability rollout restart daemonset/otel-collector-agent
kubectl -n observability rollout restart deployment/otel-collector-gateway

kubectl apply -k k8s/overlays/local
kubectl -n k8s-workshop rollout restart deployment/account-service
kubectl -n k8s-workshop rollout restart deployment/card-service
```

## Application health probes

The application API listens on port `8080`. Actuator uses the separate
management port `8081`; it is available to Kubernetes probes but is not exposed
by the application Services.

```bash
kubectl -n k8s-workshop port-forward deployment/card-service 8081:8081
curl http://localhost:8081/actuator/health/readiness
curl http://localhost:8081/actuator/health/liveness
```

## Undeploy

```bash
./scripts/k8s-undeploy.sh
docker compose -f observability/tracing/docker-compose-all.yaml down
```

To remove only monitoring:

```bash
bash scripts/monitoring-undeploy.sh
```

The Prometheus StatefulSet PVC is retained by Kubernetes when the Helm release
is removed, protecting metric history from an accidental uninstall. Delete the
exact PVC manually only when that history is no longer needed.

The undeploy script deletes the gateway PVC together with the gateway manifest.
The per-node `/var/lib/otelcol-agent` checkpoint directory is a hostPath and is
intentionally not deleted by Kubernetes.

## Spring Boot buildpacks (optional)

```bash
./mvnw -pl card-service spring-boot:build-image -Dspring-boot.build-image.imageName=card-service:local
./mvnw -pl account-service spring-boot:build-image -Dspring-boot.build-image.imageName=account-service:local
```
