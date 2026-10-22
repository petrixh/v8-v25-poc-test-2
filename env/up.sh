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
  docker build -t "$image" --build-arg "GIT_COMMIT=$(image_commit)" "$REPO_DIR"
elif ! docker image inspect "$image" >/dev/null 2>&1; then
  die "image $image not found; run without --no-build first"
fi

case "$(container_state)" in
  ours) log "replacing the existing copy"; docker rm -f "$APP_ENV_CONTAINER" >/dev/null ;;
  foreign) die "a container named $APP_ENV_CONTAINER exists and was not started by this env; not touching it" ;;
esac

log "starting $image on port $APP_ENV_PORT (production mode: $APP_ENV_PRODUCTION_MODE)"
docker run -d --name "$APP_ENV_CONTAINER" \
  --label "$LABEL_APP=$IMAGE_NAME" --label "$LABEL_PROJECT=$APP_ENV_PROJECT" \
  -p "127.0.0.1:${APP_ENV_PORT}:8080" \
  -e "APP_PRODUCTION_MODE=$APP_ENV_PRODUCTION_MODE" \
  "$image" >/dev/null
wait_up
