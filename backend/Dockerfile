FROM maven:3.9.16-amazoncorretto-21-alpine AS build
WORKDIR /workspace/app

COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

# 21.0.12 or later: earlier JDKs carry time-zone rules from before B.C. stayed on UTC−7
# (2026-11-01), so America/Vancouver would fall back to UTC−8 and Pacific timestamps would be an
# hour behind. TZ only names the zone; the rules come from this JDK. Stay below 22: Java 24+ fails
# the Oracle TCPS handshake (renovate.json pins it).
FROM eclipse-temurin:21.0.13.1_2-jdk-alpine-3.23

RUN adduser -D -s /bin/sh spring
USER spring
VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app

# OpenShift uses the Deployment's probes instead, but Trivy (DS-0026) and `docker run` expect one.
# BusyBox wget: there is no curl in the eclipse-temurin alpine image.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

# The timezone comes from the TZ env var on the Deployment (openshift.deploy.yml), not from
# -Duser.timezone, which would override TZ and make the parameter silently ineffective.
#
# Heap is a percentage of the container's memory limit rather than a fixed -Xmx, so it follows the
# Deployment's limit: 60% of 768Mi is ~460MB, the rest for Metaspace (capped at 200m), code cache,
# thread stacks and Undertow's direct buffers.
ENTRYPOINT ["java","-Duser.name=LRMOPS-API","-XX:InitialRAMPercentage=15","-XX:MaxRAMPercentage=60","-XX:TieredStopAtLevel=1","-XX:+UseParallelGC","-XX:MinHeapFreeRatio=20","-XX:MaxHeapFreeRatio=40","-XX:GCTimeRatio=4","-XX:AdaptiveSizePolicyWeight=90","-XX:MaxMetaspaceSize=200m","-XX:ParallelGCThreads=2","-XX:CICompilerCount=2","-XX:+ExitOnOutOfMemoryError","-Djava.security.egd=file:/dev/./urandom","-cp","app:app/lib/*","ca.bc.gov.nrs.lrmops.LrmopsApiApplication"]
