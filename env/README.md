# Reproducible environment

One command brings up one copy of the app in a known state: the seed contacts, Vaadin production mode, on a port of your choice. Needs Docker; no JDK, Maven or licence key on the host.

```sh
env/up.sh                 # build the image for this commit, start a copy on http://localhost:8080/, wait until it answers
env/reset.sh              # back to the seed contacts (restarts the copy), wait until it answers
env/down.sh               # stop and remove the copy (the image is kept)
docker logs addressbook-old-app   # the app's log
```

Two copies side by side, each with its own data:

```sh
APP_ENV_PORT=18111 APP_ENV_PROJECT=ab-1 APP_ENV_CONTAINER=ab-1-app env/up.sh
APP_ENV_PORT=18112 APP_ENV_PROJECT=ab-2 APP_ENV_CONTAINER=ab-2-app env/up.sh --no-build
```

| Variable | Default | Meaning |
|---|---|---|
| `APP_ENV_PORT` | `8080` | Host port, bound to 127.0.0.1; the scripts check readiness at `http://127.0.0.1:<port>/` |
| `APP_ENV_PROJECT` | `addressbook-old` | Name of the copy; stored as a container label |
| `APP_ENV_CONTAINER` | `<project>-app` | Container name; `docker logs <name>` shows the app log |
| `APP_ENV_PRODUCTION_MODE` | `true` | `false` runs Vaadin in dev (debug) mode |

- `up.sh --no-build` uses the existing image for this tree. Images are `addressbook-old:<commit>` for a clean git checkout, and `addressbook-old:src-<hash>` otherwise (uncommitted changes, or an export without git), where the hash covers the build inputs (`Dockerfile`, `.dockerignore`, `pom.xml`, `src/`). A tag never names two different trees.
- The image runs the app the way the top-level `README.md` documents it, `mvn jetty:run` (Jetty 9.2 from `pom.xml`) on JDK 21, with all dependencies resolved when the image is built: the container starts offline in about 2 s.
- Production mode comes from the system property `-Dvaadin.productionMode`, which Vaadin reads before the servlet's own setting.
- The contacts are generated in memory from a fixed seed at start, so restarting a copy resets it. Copies share nothing.
- The scripts only touch a container that carries their labels for the same project: `down.sh` leaves a foreign container of the same name alone, and `up.sh` refuses to start over it. `down.sh` is safe to run when the copy is half-started or gone.
- These are the env scripts a test runner drives: one copy per script call, configured only through the `APP_ENV_*` variables.
