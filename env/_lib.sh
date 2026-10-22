# Shared by up.sh, reset.sh and down.sh. Sourced, not run.
# Configuration (env vars, all optional):
#   APP_ENV_PORT              host port of this copy            (default 8080)
#   APP_ENV_PROJECT           name of this copy                 (default addressbook-new)
#   APP_ENV_CONTAINER         container name                    (default <project>-app)
#   APP_ENV_PRODUCTION_MODE   true: Vaadin production mode; false: dev (debug) mode (default true)

APP_ENV_PORT="${APP_ENV_PORT:-8080}"
APP_ENV_PROJECT="${APP_ENV_PROJECT:-addressbook-new}"
APP_ENV_CONTAINER="${APP_ENV_CONTAINER:-${APP_ENV_PROJECT}-app}"
APP_ENV_PRODUCTION_MODE="${APP_ENV_PRODUCTION_MODE:-true}"

ENV_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_DIR="$(dirname "$ENV_DIR")"
IMAGE_NAME="addressbook-new"
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

# Image tag: the commit when the app is a clean git checkout; otherwise (uncommitted changes,
# or an export without git) "src-" + a hash of the build inputs, so a tag never names two trees.
in_git() { [ "$(git -C "$REPO_DIR" rev-parse --show-toplevel 2>/dev/null)" = "$REPO_DIR" ]; }
clean_git() { in_git && [ -z "$(git -C "$REPO_DIR" status --porcelain)" ]; }
inputs_hash() {
  # The build inputs: the Dockerfile, .dockerignore and what it lets into the context (pom.xml, src/
  # without the files the Vaadin build generates).
  (cd "$REPO_DIR" && find Dockerfile .dockerignore pom.xml src -path src/main/frontend/generated -prune -o -type f -print0 | LC_ALL=C sort -z | xargs -0 sha256sum) \
    | sha256sum | cut -c1-12
}
image_tag() {
  if clean_git; then git -C "$REPO_DIR" rev-parse --short=12 HEAD; else echo "src-$(inputs_hash)"; fi
}
image_commit() { if clean_git; then git -C "$REPO_DIR" rev-parse HEAD; else echo "uncommitted"; fi; }

# Prints "absent", "ours" or "foreign" for the container named APP_ENV_CONTAINER.
container_state() {
  local labels
  # Only "No such container" means absent; any other failure (daemon unreachable, say) is an error.
  if ! labels="$(docker container inspect --format "{{index .Config.Labels \"$LABEL_APP\"}}|{{index .Config.Labels \"$LABEL_PROJECT\"}}" "$APP_ENV_CONTAINER" 2>&1)"; then
    if [[ "$labels" == *"No such container"* ]]; then echo absent; return; fi
    die "cannot check container $APP_ENV_CONTAINER: $labels"
  fi
  if [ "$labels" = "${IMAGE_NAME}|${APP_ENV_PROJECT}" ]; then echo ours; else echo foreign; fi
}

# Waits until the app answers 200 on its port, or fails when the container stops or time runs out.
wait_up() {
  local t0=$SECONDS code running
  while :; do
    code="$(curl -s -o /dev/null -m 5 -w '%{http_code}' "http://127.0.0.1:${APP_ENV_PORT}/" || true)"
    if [ "$code" = 200 ]; then log "up on http://127.0.0.1:${APP_ENV_PORT}/ after $((SECONDS - t0)) s"; return 0; fi
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
