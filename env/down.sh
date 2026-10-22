#!/usr/bin/env bash
# Stops and removes this copy, and only this copy. Safe after a half-finished up.sh and
# when the copy is already gone. The image is kept. See env/README.md.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/_lib.sh"

state="$(container_state)"
case "$state" in
  absent) log "no container, nothing to stop" ;;
  foreign) log "WARN: container $APP_ENV_CONTAINER was not started by this env; leaving it alone" ;;
  ours) docker rm -f "$APP_ENV_CONTAINER" >/dev/null; log "removed" ;;
esac
