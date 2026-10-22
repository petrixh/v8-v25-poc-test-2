#!/usr/bin/env bash
# Check the app log from ci/start-app.sh: the server started and Vaadin did not log its debug-mode warning.
# Run it after the first page load (ci/smoke.cjs): Vaadin logs the warning when the servlet initialises.
# Usage: ci/check-production-mode.sh [log]   (default: target/jetty.log)
set -euo pipefail
cd "$(dirname "$0")/.."
LOG="${1:-target/jetty.log}"
grep -q "Started Jetty Server" "$LOG" || { echo "No 'Started Jetty Server' in $LOG: wrong or empty log" >&2; exit 1; }
if grep -q "Vaadin is running in DEBUG MODE" "$LOG"; then
  echo "Vaadin logged its debug-mode warning: the app is not in production mode" >&2
  exit 1
fi
echo "Production mode: no debug-mode warning in $LOG"
