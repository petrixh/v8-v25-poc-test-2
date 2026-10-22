# Reproducible environment

One command brings up one copy of the app in a known state: the seed contacts, Vaadin production mode, on a port of your choice. Needs Docker; no JDK, Maven or licence key on the host.

```sh
env/up.sh                 # build the image for this commit, start a copy on http://localhost:8080/, wait until it answers
env/reset.sh              # back to the seed contacts (restarts the copy), wait until it answers
env/down.sh               # stop and remove the copy (the image is kept)
docker logs addressbook-new-app   # the app's log
```

Two copies side by side, each with its own data:

```sh
APP_ENV_PORT=18111 APP_ENV_PROJECT=ab-1 APP_ENV_CONTAINER=ab-1-app env/up.sh
APP_ENV_PORT=18112 APP_ENV_PROJECT=ab-2 APP_ENV_CONTAINER=ab-2-app env/up.sh --no-build
```

| Variable | Default | Meaning |
|---|---|---|
| `APP_ENV_PORT` | `8080` | Host port, bound to 127.0.0.1; the scripts check readiness at `http://127.0.0.1:<port>/` |
| `APP_ENV_PROJECT` | `addressbook-new` | Name of the copy; stored as a container label |
| `APP_ENV_CONTAINER` | `<project>-app` | Container name; `docker logs <name>` shows the app log |
| `APP_ENV_PRODUCTION_MODE` | `true` | `true` runs the production war (`mvn jetty:run-war`); `false` runs Vaadin in dev (debug) mode (`mvn jetty:run`) |

- `up.sh --no-build` uses the existing image for this tree. Images are `addressbook-new:<commit>` for a clean git checkout, and `addressbook-new:src-<hash>` otherwise (uncommitted changes, or an export without git), where the hash covers the build inputs (`Dockerfile`, `.dockerignore`, `pom.xml`, `src/` without the generated `src/main/frontend/generated/`). A tag never names two different trees.
- The image runs the app the way the top-level `README.md` documents it, with the Jetty 12.1 Maven plugin from `pom.xml` on JDK 21, and all dependencies resolved when the image is built: the container starts offline in about 5 s.
- Production mode is decided at build time (Vaadin 25). The container runs `mvn jetty:run-war`, which builds and runs the production war; with `APP_ENV_PRODUCTION_MODE=false` it runs `mvn jetty:run` instead (dev mode, the optional `vaadin-dev` dependency on the classpath). In dev mode the container allows Vaadin's dev tools for any host (`vaadin.devmode.hostsAllowed=*`), because requests arrive from the Docker network; the port is published on 127.0.0.1 only.
- `docker logs` shows Vaadin's mode once at start: "Vaadin is running in production mode." or "Vaadin is running in DEVELOPMENT mode".
- The contacts are generated in memory from a fixed seed at start, so restarting a copy resets it. Copies share nothing.
- The scripts only touch a container that carries their labels for the same project: `down.sh` leaves a foreign container of the same name alone, and `up.sh` refuses to start over it. `down.sh` is safe to run when the copy is half-started or gone.
- These are the env scripts a test runner drives: one copy per script call, configured only through the `APP_ENV_*` variables.
