#!/usr/bin/env bash
# Start the app in Vaadin production mode with `mvn jetty:run-war`, in the background, and wait until / answers HTTP 200.
# Usage: ci/start-app.sh [port]        (default port 8080)
# Env:   MVN            Maven command (default: mvn)
#        APP_PRODUCTION_MODE  true (default): `mvn jetty:run-war`, the production war;
#                       false: `mvn jetty:run`, development (debug) mode
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

# Vaadin 25 decides production mode at build time. jetty:run-war builds and runs the war
# (production frontend bundle, without the optional vaadin-dev dependency): production mode.
# jetty:run runs the class directory with vaadin-dev on the classpath: development mode.
case "${APP_PRODUCTION_MODE:-true}" in
  true) GOAL=jetty:run-war ;;
  false) GOAL=jetty:run ;;
  *) echo "APP_PRODUCTION_MODE must be true or false" >&2; exit 2 ;;
esac
"$MVN" -B -Djetty.port="$PORT" "$GOAL" > "$LOG" 2>&1 &
PID=$!
echo "$PID" > "$PIDFILE"
echo "Started mvn $GOAL on port $PORT (pid $PID, log $LOG)"

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
