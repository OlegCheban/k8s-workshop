#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

readonly RELEASE_NAME="kube-prometheus-stack"
readonly CHART_VERSION="87.18.1"
readonly NAMESPACE="monitoring"
readonly CHART="oci://ghcr.io/prometheus-community/charts/kube-prometheus-stack"
readonly VALUES_FILE="${ROOT_DIR}/observability/monitoring/kube-prometheus-stack-values.yaml"

command -v helm >/dev/null 2>&1 || {
  echo "Helm 3 is required to install the monitoring stack." >&2
  exit 1
}

helm upgrade --install "${RELEASE_NAME}" "${CHART}" \
  --version "${CHART_VERSION}" \
  --namespace "${NAMESPACE}" \
  --create-namespace \
  --values "${VALUES_FILE}" \
  --atomic \
  --wait \
  --timeout 15m \
  --history-max 5

# PodMonitor/ServiceMonitor/PrometheusRule objects are only discovered in the
# monitoring namespace, because the chart values restrict the operator's
# *NamespaceSelector to it. They are applied separately from the Helm release
# so that a monitor change does not require a chart upgrade.
kubectl apply -k "${ROOT_DIR}/observability/monitoring/k8s"
