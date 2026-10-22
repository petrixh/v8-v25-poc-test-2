# Reproducible environment for the Addressbook app: one container = one app copy.
# Driven by env/up.sh, env/reset.sh and env/down.sh (see env/README.md).
#
# The app runs the way it documents: the Jetty Maven plugin from pom.xml (Jetty 12.1, ee11)
# on JDK 21, `mvn jetty:run-war` in production mode and `mvn jetty:run` in development
# mode. Running the same plugin keeps the servlet container, its version and the classpath
# identical to a developer run, so the runtime image needs Maven + JDK and the build
# happens in the same image.
# All dependencies and plugins are resolved at image build; the container starts with
# `mvn -o` (offline).
#
# No licence key is used or needed: none is copied, passed or read here.

# Maven 3.9.11 on Temurin JDK 21, pinned by digest.
FROM maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237

RUN useradd --create-home --uid 10001 app \
 && mkdir -p /app /opt/m2 \
 && chown app:app /app /opt/m2
USER app
WORKDIR /app

ENV MAVEN_ARGS="-B -Dmaven.repo.local=/opt/m2"

# Dependencies and plugins first, so a source-only change reuses this layer.
COPY --chown=app:app pom.xml ./
RUN mvn dependency:go-offline

# Build the war (this also runs every plugin jetty:run and jetty:run-war use: resources,
# compiler, vaadin prepare-frontend and build-frontend) and load the jetty plugin with all
# its dependencies, so the start can be offline.
COPY --chown=app:app src ./src
RUN mvn package jetty:help -Ddetail=false

# Vaadin production mode by default. Vaadin 25 decides it at build time: the war that
# `mvn package` builds (production bundle, no vaadin-dev) runs in production mode, run
# with jetty:run-war. APP_PRODUCTION_MODE=false runs `mvn jetty:run` instead: the class
# directory with the optional vaadin-dev dependency, i.e. development (debug) mode.
# Any other value is rejected at start (as ci/start-app.sh does).
# Dev tools only answer the hosts in vaadin.devmode.hostsAllowed (default: localhost). Requests
# through the published port arrive from the Docker network's gateway address, so up.sh passes
# that address as APP_DEV_HOSTS_ALLOWED; other containers on the network are not allowed.
ENV APP_PRODUCTION_MODE=true APP_DEV_HOSTS_ALLOWED=
EXPOSE 8080
ENTRYPOINT []
# Last, so a new commit does not invalidate the dependency layers above.
# The base image's own OCI labels (Maven's description, source, url, version) are replaced.
ARG GIT_COMMIT=unknown
ARG IMAGE_TAG=unknown
LABEL org.opencontainers.image.title="addressbook-new" \
      org.opencontainers.image.description="Addressbook (new app, Vaadin 25) test environment: Jetty 12.1 Maven plugin on JDK 21, Vaadin production mode by default. See env/README.md" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.version="${IMAGE_TAG}" \
      org.opencontainers.image.base.name="maven:3.9.11-eclipse-temurin-21" \
      org.opencontainers.image.source="" \
      org.opencontainers.image.url="" \
      org.opencontainers.image.ref.name=""
CMD ["sh", "-c", "case \"$APP_PRODUCTION_MODE\" in true) set -- jetty:run-war ;; false) set -- jetty:run ${APP_DEV_HOSTS_ALLOWED:+\"-Dvaadin.devmode.hostsAllowed=$APP_DEV_HOSTS_ALLOWED\"} ;; *) echo \"APP_PRODUCTION_MODE must be true or false, got '$APP_PRODUCTION_MODE'\" >&2; exit 2 ;; esac; exec mvn -o -Djetty.port=8080 \"$@\""]
