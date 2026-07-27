# Kubernetes Native Spring Boot Workshop

This repository contains a workshop-ready Kubernetes-native microservices demo
built with Spring Boot 3.x and Java 21. It demonstrates service discovery,
ConfigMaps, Actuator health probes, distributed tracing, structured logging,
and an OpenTelemetry Collector agent/gateway topology.

## Observability architecture

```text
card-service ──HTTP/W3C trace context──> account-service
     │                                      │
     └──────────── OTLP/gRPC traces ─────────┘
                           │
                           v
              OTel Collector Gateway
                 (Deployment + Service)
                    │              │
              traces│              │logs
                    v              v
                  Jaeger      Elasticsearch ──> Kibana
                                    ^
                                    │ OTLP/gRPC
                         OTel Collector Agents
                              (DaemonSet)
                                    ^
                                    │ filelog
                       /var/log/pods/k8s-workshop_*
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

Application metrics export is intentionally disabled for now. The agents and
gateway expose their own operational Prometheus metrics on port `8888`.

## Prerequisites

- Java 21
- Docker Desktop with Kubernetes enabled
- `kubectl`
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
- both Java services in the `k8s-workshop` namespace.

To apply the resources manually:

```bash
kubectl apply -k observability/logging/k8s
kubectl apply -k k8s/overlays/local
```

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

The undeploy script deletes the gateway PVC together with the gateway manifest.
The per-node `/var/lib/otelcol-agent` checkpoint directory is a hostPath and is
intentionally not deleted by Kubernetes.

## Spring Boot buildpacks (optional)

```bash
./mvnw -pl card-service spring-boot:build-image -Dspring-boot.build-image.imageName=card-service:local
./mvnw -pl account-service spring-boot:build-image -Dspring-boot.build-image.imageName=account-service:local
```
