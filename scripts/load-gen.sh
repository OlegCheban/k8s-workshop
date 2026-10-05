#!/usr/bin/env bash
# Generates steady traffic against card-service so that the RED metrics have
# something to show. The generator runs as a pod inside the cluster, because
# the pod network is not reachable from the host.
#
# Usage: load-gen.sh [iterations] [sleep-seconds]
set -euo pipefail

readonly NAMESPACE="k8s-workshop"
readonly POD_NAME="load-gen"
readonly IMAGE="curlimages/curl:8.11.1"

readonly ITERATIONS="${1:-1800}"
readonly SLEEP_SECONDS="${2:-1}"

readonly SCRIPT=$(cat <<'INNER'
set -u
C=http://card-service:8080
A=http://account-service:8080
H="Content-Type: application/json"

# Both services keep state in memory, so a restarted pod has neither the
# account nor the card. Repeating these two calls is safe: the second attempt
# returns 409 and is simply counted as a client error.
curl -s -o /dev/null -X POST "$A/api/accounts" -H "$H" -d '{"accountId":"A-1"}'
curl -s -o /dev/null -X POST "$C/api/cards" -H "$H" -d '{"cardId":"C-1","accountId":"A-1"}'

i=0
while [ "$i" -lt "$ITERATIONS" ]; do
  # add-money and pay both call account-service, so each iteration produces a
  # two-service distributed trace as well as metrics on both sides.
  curl -s -o /dev/null -X POST "$C/api/cards/C-1/add-money" -H "$H" -d '{"amount":10}'
  curl -s -o /dev/null -X POST "$C/api/cards/C-1/pay" -H "$H" -d '{"amount":5,"merchant":"SHOP"}'
  curl -s -o /dev/null "$C/api/cards/C-1"

  # One deliberate 404 every fifth iteration keeps the error ratio non-zero,
  # so the E of RED is not a flat line at zero.
  if [ $((i % 5)) -eq 0 ]; then
    curl -s -o /dev/null "$C/api/cards/NOPE"
  fi

  i=$((i + 1))
  sleep "$SLEEP_SECONDS"
done
INNER
)

kubectl -n "$NAMESPACE" delete pod "$POD_NAME" --ignore-not-found

kubectl -n "$NAMESPACE" run "$POD_NAME" \
  --restart=Never \
  --image="$IMAGE" \
  --env="ITERATIONS=${ITERATIONS}" \
  --env="SLEEP_SECONDS=${SLEEP_SECONDS}" \
  --command -- sh -c "$SCRIPT"

cat <<EOF

Generating ${ITERATIONS} iterations, ${SLEEP_SECONDS}s apart.

  kubectl -n ${NAMESPACE} logs -f ${POD_NAME}
  kubectl -n ${NAMESPACE} delete pod ${POD_NAME}
EOF
