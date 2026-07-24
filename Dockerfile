# Fully self-contained test environment: JDK 17 + Maven + a standalone Apache Artemis broker +
# the project source itself, pre-built. No volume mount needed.
#
# Default behavior (no args) starts the Artemis broker in the foreground. `docker exec -it
# <container> bash` gets you a shell in the same container with the project already built at
# /workspace and the broker reachable at localhost:61616 — from there `cd dlq-rest-service &&
# mvn spring-boot:run` starts the REST API on :8080, testable from the host via the published port.

FROM eclipse-temurin:17-jdk-jammy

ARG ARTEMIS_VERSION=2.33.0
ARG MAVEN_VERSION=3.9.9

ENV ARTEMIS_HOME=/opt/artemis
ENV ARTEMIS_INSTANCE=/var/lib/artemis-instance
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${ARTEMIS_INSTANCE}/bin:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Maven (pinned, pulled from the permanent archive mirror rather than the latest-only current mirror)
RUN curl -fsSL "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -o /tmp/maven.tar.gz \
    && mkdir -p "${MAVEN_HOME}" \
    && tar -xzf /tmp/maven.tar.gz -C "${MAVEN_HOME}" --strip-components=1 \
    && rm /tmp/maven.tar.gz

# Apache Artemis broker distribution (version matches artemis-core-client in the project's pom.xml)
RUN curl -fsSL "https://archive.apache.org/dist/activemq/activemq-artemis/${ARTEMIS_VERSION}/apache-artemis-${ARTEMIS_VERSION}-bin.tar.gz" -o /tmp/artemis.tar.gz \
    && mkdir -p "${ARTEMIS_HOME}" \
    && tar -xzf /tmp/artemis.tar.gz -C "${ARTEMIS_HOME}" --strip-components=1 \
    && rm /tmp/artemis.tar.gz

# Create a broker instance: admin/admin login, bound to all interfaces so published ports work,
# NIO journal (avoids relying on libaio, which isn't reliably available in containers).
RUN "${ARTEMIS_HOME}/bin/artemis" create "${ARTEMIS_INSTANCE}" \
    --user admin \
    --password admin \
    --require-login \
    --host 0.0.0.0 \
    --http-host 0.0.0.0 \
    --no-autotune \
    --nio \
    --silent \
    --force

WORKDIR /workspace

# Copy the POMs first so a plain source change doesn't force re-downloading every dependency.
COPY pom.xml ./
COPY dlq-core/pom.xml dlq-core/pom.xml
COPY dlq-rest-service/pom.xml dlq-rest-service/pom.xml
RUN mvn -q -B -N install

# Build dlq-core first (dlq-rest-service depends on it) and install it to the local repo.
COPY dlq-core/ dlq-core/
RUN cd dlq-core && mvn -q -B install

# Then dlq-rest-service, which can now resolve dlq-core from the local repo. This also acts as a
# build-time sanity check that the whole project compiles and its tests pass before the image is
# considered good.
COPY dlq-rest-service/ dlq-rest-service/
RUN cd dlq-rest-service && mvn -q -B install

# 61616 = core protocol (what dlq-core connects to), 8161 = web console, 8080 = dlq-rest-service if run in-container
EXPOSE 61616 8161 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s \
    CMD curl -sf http://localhost:8161/console/ >/dev/null || exit 1

CMD ["artemis", "run"]
