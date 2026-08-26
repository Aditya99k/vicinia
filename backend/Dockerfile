# syntax=docker/dockerfile:1
#
# Builds any one backend service's image — shared across all 16 services
# (see docker-compose.prod.yml) rather than one near-duplicate Dockerfile
# per module, parameterized by MODULE (a module directory name under
# backend/, e.g. order-service). Build context must be backend/ itself
# (not the module subdirectory), since the Maven reactor needs the root
# pom.xml plus every module's pom.xml present to resolve -am correctly.
#
# Usage (docker-compose.prod.yml does this per service):
#   docker build -f backend/Dockerfile --build-arg MODULE=order-service backend/

FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# -pl <module>,common -am: builds only the target module plus whatever it
# actually depends on (common, always; nothing else in this reactor) —
# not all 17 modules, so one service's image build never waits on every
# other service's compile. The .m2 cache mount means a second build of a
# different MODULE against an unchanged pom doesn't re-download the
# world; BuildKit is Docker's default builder as of the daemon version
# this was written against, so no extra flag is needed to enable it.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -pl ${MODULE},common -am package -DskipTests

# Alpine JRE, not the jammy/glibc runtime — smaller image, and every
# domain service here is a small CRUD-shaped process where the extra few
# hundred MB Alpine saves actually matters against DEPLOYMENT.md §2's
# 12GB VM budget (19 containers on one box).
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
RUN addgroup -S vicinia && adduser -S vicinia -G vicinia
USER vicinia
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar

# -Xmx160m + SerialGC, not a container-memory-percentage guess
# (-XX:MaxRAMPercentage): DEPLOYMENT.md §2 already computed a fixed
# per-service budget for the whole 12GB VM (19 JVMs at ~220MB each,
# Redpanda/Postgres/Redis on top, ~6GB headroom) — a fixed heap keeps
# every service's footprint predictable against that budget rather than
# each container silently sizing itself off however much RAM Docker
# reports as available. SerialGC over the JDK 21 default (G1) because
# these are small, low-throughput services where G1's extra bookkeeping
# memory isn't worth it — the classic "G1 needs headroom small heaps
# don't have" tradeoff.
ENTRYPOINT ["java", "-Xmx160m", "-XX:+UseSerialGC", "-jar", "app.jar"]
