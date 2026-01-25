# Kubernetes Native Spring Boot Workshop

This repository contains a workshop-ready, Kubernetes-native microservices demo built with Spring Boot 3.x and Java 21. The goal is to showcase service discovery, configuration via ConfigMaps, health probes using Actuator health groups, and observability with tracing (Docker Compose) plus cluster logging (EFK on Kubernetes).

## Prerequisites

- Java 21
- Docker Desktop (with Kubernetes enabled)
- `kubectl`
- Docker Engine (for `docker compose` and image builds)

## Build the services

From the repo root:

```bash
./mvnw clean package
```

## Run the tracing stack (Docker Compose)

```bash
docker compose -f observability/tracing/docker-compose-all.yaml up -d
```

The Jaeger UI will be available at:

- http://localhost:16686

## Build local container images

```bash
./scripts/build-images.sh
```

## Deploy EFK logging on Kubernetes

```bash
kubectl apply -f observability/logging/k8s/00-namespace.yaml
kubectl apply -f observability/logging/k8s/elasticsearch.yaml
kubectl apply -f observability/logging/k8s/kibana.yaml
kubectl apply -f observability/logging/k8s/fluentd/rbac.yaml
kubectl apply -f observability/logging/k8s/fluentd/configmap.yaml
kubectl apply -f observability/logging/k8s/fluentd/daemonset.yaml
```

Access Kibana locally:

```bash
kubectl -n observability port-forward svc/kibana 5601:5601
```

Then open http://localhost:5601 and create an index pattern:

- Index pattern: `logstash-*`
- Time field: `@timestamp`

## Deploy the applications to Kubernetes

```bash
kubectl apply -k k8s/overlays/local
```

### Access the Card Service API

Baseline (recommended) port-forward:

```bash
kubectl -n k8s-workshop port-forward svc/card-service 8080:8080
```
Optional NodePort exposure (already in the local overlay):

- NodePort `30080` maps to `card-service:8080`
- Access via `http://localhost:30080/api/cards/...`

## Troubleshooting

```bash
kubectl -n k8s-workshop get pods
kubectl -n k8s-workshop describe pod <pod-name>
kubectl -n k8s-workshop logs deployment/card-service
kubectl -n k8s-workshop logs deployment/account-service

kubectl -n k8s-workshop port-forward svc/card-service 8080:8080
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/health/liveness
```

## Observability Notes

- **Tracing**: Both services export OTLP traces to the Docker Compose Jaeger instance (endpoint set via `OTEL_EXPORTER_OTLP_ENDPOINT`).
- **Logging**: Every API request logs JSON to stdout (Logback) and also emits a JSON line to stderr (for Fluentd). Fluentd ships logs from `/var/log/containers/*.log` into Elasticsearch and Kibana.

## Spring Boot buildpacks (optional)

If you prefer buildpacks instead of Dockerfiles:

```bash
./mvnw -pl card-service spring-boot:build-image -Dspring-boot.build-image.imageName=card-service:local
./mvnw -pl account-service spring-boot:build-image -Dspring-boot.build-image.imageName=account-service:local
```
