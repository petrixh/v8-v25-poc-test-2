Addressbook Tutorial
====================

This tutorial teaches you some of the basic concepts in [Vaadin](https://vaadin.com) (Vaadin 25, Flow). It is meant to be
a fast read for learning how to get started - not an example on how application should be
designed. Please note this example requires Java 21 and Maven 3.9.

![Addressbook Screenshot](addressbook_screenshot.png "Addressbook Screenshot")


Running the example from the command line
-------------------
```
$ mvn jetty:run
```

Open [http://localhost:8080/](http://localhost:8080/). This runs Vaadin in development (debug) mode. Vaadin usage statistics are turned off for every `mvn jetty:run` (the Jetty plugin in `pom.xml` sets `vaadin.devmode.usageStatistics.enabled=false`).

To build and run it in production mode, the way it is deployed:
```
$ mvn package          # target/addressbook-2.0.war, a production build
$ mvn jetty:run-war    # builds the war and runs it on Jetty
```

Vaadin 25 decides production mode at build time: the war is built with the production frontend bundle and without the optional `vaadin-dev` dependency. No Vaadin licence key is needed; the app uses only the free `vaadin-core` components.

Running the tests
-------------------
```
$ mvn test
```

JUnit 6 unit tests of the in-memory backend (`src/test/java`): `ContactService` stores and returns copies, and the 100 demo contacts match the original app's (`src/test/resources/.../demo-contacts.csv`). Maven runs them in UTC and en-US whatever the host's settings (surefire `argLine` in `pom.xml`). `mvn package` runs them too, and so does `mvn jetty:run-war`, which builds the war first: a failing test stops the app from starting. `mvn jetty:run` compiles the tests but does not run them.

### End-to-end tests

```
$ mvn jetty:run-war                                   # or env/up.sh, or ci/start-app.sh 8080; production mode
$ mvn verify -Pe2e -Dapp.url=http://localhost:8080    # in a second terminal
```

Playwright for Java (1.63.0) tests in `src/test/java/.../e2e`, run by Maven failsafe with the `e2e` profile: one class per workflow (`AB01BrowseContactsIT` ... `AB11ConcurrentEditIT`; AB07 is not covered yet). They drive the running app in headless Chromium (en-US, UTC, 1400 x 900) at the URL in `app.url`, and never start the app themselves. Without `-Pe2e` they are compiled but not run.

- **Fresh app:** the contacts are in memory and shared by every session, and the tests change them. Start (or restart, e.g. `env/reset.sh`) the app right before a run; a second run against the same app fails.
- **Fixed order:** the classes run in the order of their `@Order` (AB01, AB02, AB03, AB06, AB11, AB05, AB10, AB04, AB08, AB09). Tests that change data come after the tests whose checks they would break. Where an earlier test's change still shows, the check is relative and the class says so: AB10 compares with the list as it was when it started, and AB04, AB08 and AB09 check that the contact that was first before their save is now below the new one. A single class (`-Dit.test=AB04AddContactIT`) also passes against a fresh app.
- **One app per run:** the classes run one after the other against one app. Running them in parallel on several app copies is not set up.
- **Browsers:** Playwright downloads its Chromium on the first run. To use browsers you already have, set `PLAYWRIGHT_BROWSERS_PATH` to their folder and `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`.
- **Failures** name the workflow step; screenshots of the open pages go to `target/e2e-failures/`, the reports to `target/failsafe-reports/`.

CI runs them after the smoke check, against the app the job started: see [ci/README.md](ci/README.md).

Reproducible environment
-------------------
With Docker, one command builds and starts a copy of the app in a known state (seed contacts, Vaadin production mode):
```
$ env/up.sh
```

Open [http://localhost:8080/](http://localhost:8080/). `env/reset.sh` returns it to the seed contacts and `env/down.sh` stops it. Ports, names and several copies side by side: see [env/README.md](env/README.md).


Importing in IntelliJ IDEA 14
--------------------
These instructions were tested on IntelliJ IDEA 14 CE. You can get it from https://www.jetbrains.com/idea/

To get the project up and running in IDEA, do:
- File -> New -> Project from Version Control -> Git
- The URL to use is https://github.com/vaadin/addressbook.git
- If you get a message about "Non-managed pom.xml file found". Choose "Add as Maven Project"
- If you get a message about no JDK or SDK being selected. Choose "Configure" and select your installed JDK. You can also set the JDK using File -> Project Structure
- To start the project, find the "Maven Projects" tab on the right hand side of the screen and navigate to
  - Vaadin Web Application -> Plugins -> jetty -> jetty:run
  - Click the play button or right click and select Run (Select Debug instead to run in debug mode)

You should now have a Jetty server running on localhost:8080. Navigate to http://localhost:8080 to play with the application

Importing in NetBeans 8
--------------------
These instructions were tested on NetBeans 8.0.2. You can get it from https://www.netbeans.org

To checkout and run the project in NetBeans, do:
- Team -> Git -> Clone
- Set repository URL to https://github.com/vaadin/addressbook.git
- Finish
- Right click the imported project (Vaadin Addressbook Application) and select Run
- Select GlassFish Server 4.1 -> Remember in Current IDE Session -> OK

You should now have a GlassFish server running on localhost:8080 and a browser tab should also be automatically opened with this location

Importing in Eclipse
--------------------
These instructions were tested on Eclipse IDE for Java EE Developers Luna SR2. You can get it from http://eclipse.org/downloads/

To checkout and run the project in Eclipse, do:
- File -> Import...
- Check out Maven Projects from SCM
- Choose Git from SCM menu
  - If you do not see "Git" in the SCM menu, click "Find more SCM connectors in the m2e Marketplace" and install "m2e-egit". Restart Eclipse and start over.
- Set the repository URL to https://github.com/vaadin/addressbook.git
- Right click the imported "addressbook" and choose Run As -> Maven Build...
  - Set the goal to "jetty:run" and click "Run"

You should now have a Jetty server running on localhost:8080. Navigate to [http://localhost:8080/](http://localhost:8080/) to play with the application

To use the built in server adapters of Eclipse, instead of doing "Run As -> Maven Build..." you can do
- Run As -> Run on Server
- Select the server you want to run on: a Jakarta EE 11 (Servlet 6.1) container, such as Jetty 12.1 (ee11, as used by `mvn jetty:run`), and click ok
- *Do not use the suggested J2EE Preview server* as it is outdated, deprecated and does not support Servlet 6.1, which is required for this application
