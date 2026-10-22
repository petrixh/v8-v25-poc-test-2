#!/usr/bin/env bash
# Stop the app started by ci/start-app.sh, by the PID in target/jetty.pid. Safe to run when it is not running.
# Kills the PID only if it is still our server (a java process running jetty:run), so a stale PID file
# never stops an unrelated process that reused the PID.
set -uo pipefail
cd "$(dirname "$0")/.."
PIDFILE=target/jetty.pid
[ -f "$PIDFILE" ] || { echo "No $PIDFILE: nothing to stop"; exit 0; }
PID=$(cat "$PIDFILE")
ARGS=$(ps -o args= -p "$PID" 2>/dev/null || true)
if [ -z "$ARGS" ]; then
  echo "pid $PID is not running"
elif [[ "$ARGS" != *java* || "$ARGS" != *jetty:run* ]]; then
  echo "pid $PID is not our server (it runs: ${ARGS:0:120}); not killing it" >&2
else
  kill "$PID"
  for _ in $(seq 1 30); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
  if kill -0 "$PID" 2>/dev/null; then
    echo "pid $PID did not stop in 30s; killing it"
    kill -9 "$PID"
  fi
  echo "Stopped pid $PID"
fi
rm -f "$PIDFILE"
