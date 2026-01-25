#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

kubectl delete -k "$ROOT_DIR/k8s/overlays/local" --ignore-not-found

kubectl delete -f "$ROOT_DIR/observability/logging/k8s/fluentd/daemonset.yaml" --ignore-not-found
kubectl delete -f "$ROOT_DIR/observability/logging/k8s/fluentd/configmap.yaml" --ignore-not-found
kubectl delete -f "$ROOT_DIR/observability/logging/k8s/fluentd/rbac.yaml" --ignore-not-found
kubectl delete -f "$ROOT_DIR/observability/logging/k8s/kibana.yaml" --ignore-not-found
kubectl delete -f "$ROOT_DIR/observability/logging/k8s/elasticsearch.yaml" --ignore-not-found
kubectl delete -f "$ROOT_DIR/observability/logging/k8s/00-namespace.yaml" --ignore-not-found
