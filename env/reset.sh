#!/usr/bin/env bash
# Returns this copy to the seed state and returns when it answers again. See env/README.md.
# The contacts live in memory and are generated from a fixed seed at start, so a restart is a reset.
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/_lib.sh"

[ "$(container_state)" = ours ] || die "no copy of this env named $APP_ENV_CONTAINER to reset; run up.sh"
log "restarting"
docker restart -t 10 "$APP_ENV_CONTAINER" >/dev/null
wait_up
