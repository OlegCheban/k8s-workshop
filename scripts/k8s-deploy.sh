#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

kubectl apply -f "$ROOT_DIR/observability/logging/k8s/00-namespace.yaml"
kubectl apply -f "$ROOT_DIR/observability/logging/k8s/elasticsearch.yaml"
kubectl apply -f "$ROOT_DIR/observability/logging/k8s/kibana.yaml"
kubectl apply -f "$ROOT_DIR/observability/logging/k8s/fluentd/rbac.yaml"
kubectl apply -f "$ROOT_DIR/observability/logging/k8s/fluentd/configmap.yaml"
kubectl apply -f "$ROOT_DIR/observability/logging/k8s/fluentd/daemonset.yaml"

kubectl apply -k "$ROOT_DIR/k8s/overlays/local"
