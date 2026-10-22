#!/usr/bin/env bash
# Check the app log from ci/start-app.sh: the server started and Vaadin logged that it runs in production mode.
# Vaadin 25 logs its mode once, when the servlet is deployed at start ("Vaadin is running in production mode."
# or "Vaadin is running in DEVELOPMENT mode"). Run it after the smoke check, like the server-log check.
# Usage: ci/check-production-mode.sh [log]   (default: $APP_LOG, else target/jetty.log)
set -euo pipefail
cd "$(dirname "$0")/.."
LOG="${1:-${APP_LOG:-target/jetty.log}}"
grep -q "Started oejs.Server" "$LOG" || { echo "No 'Started oejs.Server' (Jetty 12) in $LOG: wrong or empty log" >&2; exit 1; }
if grep -q "Vaadin is running in DEVELOPMENT mode" "$LOG"; then
  echo "Vaadin logged that it runs in development mode: the app is not in production mode" >&2
  exit 1
fi
if ! grep -q "Vaadin is running in production mode" "$LOG"; then
  echo "Vaadin did not log 'Vaadin is running in production mode' in $LOG" >&2
  exit 1
fi
echo "Production mode: Vaadin logged 'running in production mode' in $LOG"
