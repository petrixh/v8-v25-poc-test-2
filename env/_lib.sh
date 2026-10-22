# Shared by up.sh, reset.sh and down.sh. Sourced, not run.
# Configuration (env vars, all optional):
#   APP_ENV_PORT              host port of this copy            (default 8080)
#   APP_ENV_PROJECT           name of this copy                 (default addressbook-old)
#   APP_ENV_CONTAINER         container name                    (default <project>-app)
#   APP_ENV_PRODUCTION_MODE   true: Vaadin production mode; false: dev (debug) mode (default true)

APP_ENV_PORT="${APP_ENV_PORT:-8080}"
APP_ENV_PROJECT="${APP_ENV_PROJECT:-addressbook-old}"
APP_ENV_CONTAINER="${APP_ENV_CONTAINER:-${APP_ENV_PROJECT}-app}"
APP_ENV_PRODUCTION_MODE="${APP_ENV_PRODUCTION_MODE:-true}"

ENV_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_DIR="$(dirname "$ENV_DIR")"
IMAGE_NAME="addressbook-old"
# Every container this env starts carries these labels; scripts only touch containers that do.
LABEL_APP="app-env.app"
LABEL_PROJECT="app-env.project"
UP_TIMEOUT_S=300

log() { echo "[env $(date -u +%H:%M:%S) ${APP_ENV_CONTAINER}] $*"; }
die() { log "ERROR: $*" >&2; exit 1; }

if ! [[ "$APP_ENV_PORT" =~ ^[0-9]+$ ]] || (( APP_ENV_PORT < 1 || APP_ENV_PORT > 65535 )); then
  die "APP_ENV_PORT must be a port number, got '$APP_ENV_PORT'"
fi
[[ "$APP_ENV_CONTAINER" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || die "APP_ENV_CONTAINER is not a valid container name: '$APP_ENV_CONTAINER'"
[[ "$APP_ENV_PROJECT" =~ ^[a-zA-Z0-9][a-zA-Z0-9_.-]*$ ]] || die "APP_ENV_PROJECT is not a valid name: '$APP_ENV_PROJECT'"
[[ "$APP_ENV_PRODUCTION_MODE" == true || "$APP_ENV_PRODUCTION_MODE" == false ]] || die "APP_ENV_PRODUCTION_MODE must be true or false"

# Image tag: the commit of the app repo, with -dirty when the tree has changes;
# "local" when the app is not the top of a git checkout (an export, say).
in_git() { [ "$(git -C "$REPO_DIR" rev-parse --show-toplevel 2>/dev/null)" = "$REPO_DIR" ]; }
image_tag() {
  if ! in_git; then echo local; return; fi
  local sha; sha="$(git -C "$REPO_DIR" rev-parse --short=12 HEAD)"
  if [ -n "$(git -C "$REPO_DIR" status --porcelain)" ]; then echo "${sha}-dirty"; else echo "$sha"; fi
}
image_commit() { if in_git; then git -C "$REPO_DIR" rev-parse HEAD; else echo unknown; fi; }

# Prints "absent", "ours" or "foreign" for the container named APP_ENV_CONTAINER.
container_state() {
  local labels
  if ! labels="$(docker container inspect --format "{{index .Config.Labels \"$LABEL_APP\"}}|{{index .Config.Labels \"$LABEL_PROJECT\"}}" "$APP_ENV_CONTAINER" 2>/dev/null)"; then
    echo absent; return
  fi
  if [ "$labels" = "${IMAGE_NAME}|${APP_ENV_PROJECT}" ]; then echo ours; else echo foreign; fi
}

# Waits until the app answers 200 on its port, or fails when the container stops or time runs out.
wait_up() {
  local t0=$SECONDS code running
  while :; do
    code="$(curl -s -o /dev/null -m 5 -w '%{http_code}' "http://localhost:${APP_ENV_PORT}/" || true)"
    if [ "$code" = 200 ]; then log "up on http://localhost:${APP_ENV_PORT}/ after $((SECONDS - t0)) s"; return 0; fi
    running="$(docker container inspect --format '{{.State.Running}}' "$APP_ENV_CONTAINER" 2>/dev/null || echo gone)"
    if [ "$running" != true ]; then
      docker logs --tail 50 "$APP_ENV_CONTAINER" >&2 2>&1 || true
      die "container is not running ($running); last log lines above"
    fi
    if (( SECONDS - t0 > UP_TIMEOUT_S )); then
      docker logs --tail 50 "$APP_ENV_CONTAINER" >&2 2>&1 || true
      die "no HTTP 200 on port ${APP_ENV_PORT} within ${UP_TIMEOUT_S} s (last code '$code'); last log lines above"
    fi
    sleep 1
  done
}
