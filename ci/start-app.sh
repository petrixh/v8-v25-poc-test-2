#!/usr/bin/env bash
# Start the app with `mvn jetty:run` in Vaadin production mode, in the background, and wait until / answers HTTP 200.
# Usage: ci/start-app.sh [port]        (default port 8080)
# Env:   MVN            Maven command (default: mvn)
#        APP_LOG        log file (default: target/jetty.log)
#        START_TIMEOUT  seconds to wait for HTTP 200 (default: 180)
# Writes the PID of the server JVM to target/jetty.pid. Stop it with ci/stop-app.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
PORT="${1:-8080}"
MVN="${MVN:-mvn}"
LOG="${APP_LOG:-target/jetty.log}"
TIMEOUT="${START_TIMEOUT:-180}"
PIDFILE=target/jetty.pid
mkdir -p target "$(dirname "$LOG")"

# The app hard-codes productionMode = false in @VaadinServletConfiguration. Vaadin reads the
# system property vaadin.productionMode before the servlet parameters, so this runs it in production mode.
"$MVN" -B -Djetty.port="$PORT" -Dvaadin.productionMode=true jetty:run > "$LOG" 2>&1 &
PID=$!
echo "$PID" > "$PIDFILE"
echo "Started mvn jetty:run on port $PORT (pid $PID, log $LOG)"

for ((i = 0; i < TIMEOUT; i += 2)); do
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "The app exited before it answered. Last log lines:" >&2
    tail -n 40 "$LOG" >&2
    exit 1
  fi
  code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORT/" || true)
  if [ "$code" = 200 ]; then
    echo "App is up: http://localhost:$PORT/ answered 200 after about ${i}s"
    exit 0
  fi
  sleep 2
done
echo "The app did not answer 200 within ${TIMEOUT}s (last code: $code). Last log lines:" >&2
tail -n 40 "$LOG" >&2
exit 1
