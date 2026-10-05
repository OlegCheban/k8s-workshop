#!/usr/bin/env bash
set -euo pipefail

readonly RELEASE_NAME="kube-prometheus-stack"
readonly NAMESPACE="monitoring"

command -v helm >/dev/null 2>&1 || {
  echo "Helm 3 is required to uninstall the monitoring stack." >&2
  exit 1
}

if helm status "${RELEASE_NAME}" --namespace "${NAMESPACE}" >/dev/null 2>&1; then
  helm uninstall "${RELEASE_NAME}" --namespace "${NAMESPACE}" --wait
fi

