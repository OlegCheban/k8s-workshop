#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)

cd "$ROOT_DIR"

docker build -t card-service:local -f card-service/Dockerfile .
docker build -t account-service:local -f account-service/Dockerfile .
