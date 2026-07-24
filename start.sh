#!/usr/bin/env bash
# Starts the Artemis broker and dlq-rest-service together as the container's main process.
# If either one dies, the container exits (so `docker ps` / restart policies reflect real health).
set -euo pipefail

echo "[start.sh] Starting Artemis broker..."
"${ARTEMIS_INSTANCE}/bin/artemis" run &
ARTEMIS_PID=$!

echo "[start.sh] Waiting for broker to accept connections on 61616..."
until (exec 3<>/dev/tcp/localhost/61616) 2>/dev/null; do
    sleep 1
done
exec 3>&- || true
echo "[start.sh] Broker is up."

echo "[start.sh] Starting dlq-rest-service on :8080..."
(cd /workspace/dlq-rest-service && exec mvn -q spring-boot:run) &
REST_PID=$!

trap 'echo "[start.sh] Shutting down..."; kill "$ARTEMIS_PID" "$REST_PID" 2>/dev/null || true' TERM INT

# Exit (and take the container down) as soon as either process dies.
wait -n "$ARTEMIS_PID" "$REST_PID"
EXIT_CODE=$?
echo "[start.sh] A process exited (code $EXIT_CODE), stopping the other..."
kill "$ARTEMIS_PID" "$REST_PID" 2>/dev/null || true
exit "$EXIT_CODE"
