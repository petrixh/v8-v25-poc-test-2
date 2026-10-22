#!/usr/bin/env bash
# Stop the app started by ci/start-app.sh, by the PID in target/jetty.pid. Safe to run when it is not running.
set -uo pipefail
cd "$(dirname "$0")/.."
PIDFILE=target/jetty.pid
[ -f "$PIDFILE" ] || { echo "No $PIDFILE: nothing to stop"; exit 0; }
PID=$(cat "$PIDFILE")
if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  for _ in $(seq 1 30); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
  if kill -0 "$PID" 2>/dev/null; then
    echo "pid $PID did not stop in 30s; killing it"
    kill -9 "$PID"
  fi
  echo "Stopped pid $PID"
else
  echo "pid $PID is not running"
fi
rm -f "$PIDFILE"
