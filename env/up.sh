#!/usr/bin/env bash
# Starts one copy of the app on APP_ENV_PORT and returns when it answers. See env/README.md.
# Usage: env/up.sh [--no-build]   (--no-build: use the existing image for this commit)
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/_lib.sh"

build=true
for a in "$@"; do
  case "$a" in
    --no-build) build=false ;;
    *) echo "usage: $0 [--no-build]" >&2; exit 2 ;;
  esac
done

image="${IMAGE_NAME}:$(image_tag)"
if $build; then
  log "building $image"
  docker build -t "$image" --build-arg "GIT_COMMIT=$(image_commit)" --build-arg "IMAGE_TAG=${image#*:}" "$REPO_DIR"
elif ! docker image inspect "$image" >/dev/null 2>&1; then
  die "image $image not found; run without --no-build first"
fi

state="$(container_state)"
case "$state" in
  ours) log "replacing the existing copy"; docker rm -f "$APP_ENV_CONTAINER" >/dev/null ;;
  foreign) die "a container named $APP_ENV_CONTAINER exists and was not started by this env; not touching it" ;;
esac

# Dev mode: Vaadin's dev tools answer only the hosts in vaadin.devmode.hostsAllowed. Requests through
# the published port reach the container from the bridge network's gateway, so allow exactly that address.
dev_hosts=""
if [ "$APP_ENV_PRODUCTION_MODE" = false ]; then
  dev_hosts="$(docker network inspect bridge --format '{{(index .IPAM.Config 0).Gateway}}' 2>/dev/null || true)"
  [[ "$dev_hosts" =~ ^[0-9.]+$ ]] || die "cannot read the Docker bridge gateway address (got '$dev_hosts'); needed for dev mode"
fi

log "starting $image on port $APP_ENV_PORT (production mode: $APP_ENV_PRODUCTION_MODE)"
docker run -d --name "$APP_ENV_CONTAINER" \
  --label "$LABEL_APP=$IMAGE_NAME" --label "$LABEL_PROJECT=$APP_ENV_PROJECT" \
  -p "127.0.0.1:${APP_ENV_PORT}:8080" \
  -e "APP_PRODUCTION_MODE=$APP_ENV_PRODUCTION_MODE" -e "APP_DEV_HOSTS_ALLOWED=$dev_hosts" \
  "$image" >/dev/null
wait_up
