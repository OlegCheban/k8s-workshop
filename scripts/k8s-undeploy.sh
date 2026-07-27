#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

kubectl delete -k "$ROOT_DIR/k8s/overlays/local" --ignore-not-found
kubectl delete -k "$ROOT_DIR/observability/logging/k8s" --ignore-not-found
