#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

kubectl apply -k "$ROOT_DIR/observability/logging/k8s"

# `kubectl apply` does not prune resources removed from the manifests. Clean up
# the exact legacy Fluentd resources when upgrading an existing installation.
kubectl -n observability delete \
  daemonset/fluentd \
  configmap/fluentd-config \
  serviceaccount/fluentd \
  --ignore-not-found
kubectl delete \
  clusterrole/fluentd \
  clusterrolebinding/fluentd \
  --ignore-not-found

bash "$SCRIPT_DIR/monitoring-deploy.sh"

kubectl apply -k "$ROOT_DIR/k8s/overlays/local"
