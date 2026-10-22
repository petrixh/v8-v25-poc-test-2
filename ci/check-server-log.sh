#!/usr/bin/env bash
# Fail if the app's server log has errors: SEVERE or ERROR lines, or Java exception stack traces.
# Run it after ci/smoke.cjs. In production mode the browser console shows no Vaadin client
# diagnostics, and server-side errors (Vaadin's error handler logs them while the response is still
# HTTP 200) only show up here. Vaadin (Flow) logs through SLF4J to slf4j-simple on stderr:
# "[thread] ERROR logger - message", followed by the stack trace.
# Usage: ci/check-server-log.sh [log]   (default: $APP_LOG, else target/jetty.log)
set -euo pipefail
cd "$(dirname "$0")/.."
LOG="${1:-${APP_LOG:-target/jetty.log}}"
[ -s "$LOG" ] || { echo "Server log $LOG is missing or empty" >&2; exit 1; }
# SEVERE / ERROR as a word (slf4j-simple's ERROR from Vaadin, the app's own java.util.logging SEVERE,
# Maven's and Jetty's [ERROR]); the first line of an exception
# ("java.lang.IllegalStateException: ..."); stack trace frames ("<tab>at pkg.Class.method(...)");
# "Caused by:" and "Exception in thread".
PATTERN='\b(SEVERE|ERROR)\b|^[[:alnum:]_$.]+(Exception|Error)(:|$)|^[[:space:]]+at [^[:space:]]+\(.*\)[[:space:]]*$|^Caused by: |^Exception in thread '
if grep -nE "$PATTERN" "$LOG" > /tmp/check-server-log.$$; then
  echo "Errors in server log $LOG (first 40 matching lines):" >&2
  head -n 40 /tmp/check-server-log.$$ >&2
  rm -f /tmp/check-server-log.$$
  exit 1
fi
rm -f /tmp/check-server-log.$$
echo "Server log: no SEVERE/ERROR lines or stack traces in $LOG"
