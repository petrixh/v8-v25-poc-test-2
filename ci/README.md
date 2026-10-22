# CI

`.github/workflows/ci.yml` builds the app, starts it and smoke-checks it in a browser on every push to `main`, `mig-dev`, `stage/**`, `stabilise/**` and `fix/**`, and on every pull request. It needs no secrets and no Vaadin licence key.

## What the job does

One job on `ubuntu-24.04` (pinned, so the runner OS does not change underneath the migration):

1. JDK 21 (Temurin), with the Maven repository cached.
2. `mvn -B package`: compiles and builds `target/addressbook-2.0.war`. This is the build command; any commit can produce the deployable war this way. There are no tests yet, so surefire has nothing to run.
3. Node 22 and the smoke check's one dependency (`npm ci` in `ci/`: Playwright, pinned in `ci/package-lock.json`), then Chromium. The browser download is cached, keyed on the lock file.
4. `ci/start-app.sh 8080`: runs `mvn jetty:run` in the background in Vaadin production mode (`-Dvaadin.productionMode=true`) and waits until `http://localhost:8080/` answers HTTP 200. The app source hard-codes `productionMode = false`; Vaadin reads the system property first, so the property wins.
5. `node ci/smoke.cjs http://localhost:8080 target/smoke`: opens `/` in Chromium, waits for the contact grid to show rows, and fails on any console error, page error or HTTP response >= 400. It writes `target/smoke/smoke.png` and `target/smoke/smoke.json`.
6. `ci/check-production-mode.sh`: fails if the log has Vaadin's "running in DEBUG MODE" warning.
7. `ci/stop-app.sh`: stops the server by its PID (always runs).
8. On failure only: uploads `target/jetty.log` and `target/smoke/` as the `diagnostics` artifact, kept for 3 days. A failed upload does not fail the job.

It is one job on purpose. The war is never handed to another job through artifact storage, and nothing is uploaded on success, so CI stays within the account's storage quota. The browser and Maven caches don't count against that quota.

The smoke check launches Chromium with `HeadlessChrome` in the user agent replaced by `Chrome`. Vaadin 7/8 choose client code from the user agent and don't recognise headless Chrome, so without this the check would test something users never get.

## Run the same steps locally

You need JDK 21, Maven 3.9 and Node 22 (with npm). From the repository root:

```sh
mvn -B package
(cd ci && npm ci && npx playwright install chromium)   # once; skip the install if your browsers are already there
ci/start-app.sh 8080                                    # any free port
node ci/smoke.cjs http://localhost:8080 target/smoke
ci/check-production-mode.sh
ci/stop-app.sh
```

Then open `target/smoke/smoke.png`. It should show the filter field, the "New contact" button and the contact grid with rows.

- `ci/start-app.sh` reads `MVN` (Maven command, default `mvn`), `APP_LOG` (default `target/jetty.log`) and `START_TIMEOUT` (seconds, default 180). It writes the server's PID to `target/jetty.pid`, and `ci/stop-app.sh` stops that PID.
- If you set `PLAYWRIGHT_BROWSERS_PATH`, Playwright installs browsers there and looks for them there.
- The smoke check fails, with exit code 1, if nothing answers at the URL.
- The app keeps its contacts in memory. Restart it to reset the data.
