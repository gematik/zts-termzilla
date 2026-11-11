FROM gematik1/osadl-alpine-openjdk25-jre@sha256:b562d62fdbfc77fa00bfc98b5bde97cc6add0e0c32da351b3acb97ffef1adce3

# Build the application
WORKDIR /app

# The STOPSIGNAL instruction sets the system call signal that will be sent to the container to exit
# SIGTERM = 15 - https://de.wikipedia.org/wiki/Signal_(Unix)
STOPSIGNAL SIGTERM

# Defining Healthcheck
HEALTHCHECK --interval=15s \
            --timeout=10s \
            --start-period=30s \
            --retries=3 \
            CMD ["/usr/bin/wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9090/actuator/health/liveness"]

# Default USERID and GROUPID
ARG USERID=10000
ARG GROUPID=10000

# Run as User (not root)
USER $USERID:$GROUPID

# Copy the application's jar to the container
COPY --chown=$USERID:$GROUPID core/target/termzilla-core-exec.jar /app/termzilla.jar

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=85.0", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "termzilla.jar"]

# Git Args
ARG COMMIT_HASH
ARG VERSION

LABEL de.gematik.vendor="gematik GmbH" \
      maintainer="zts@gematik.de" \
      de.gematik.app="Termzilla" \
      de.gematik.git-repo-name="https://gitlab.prod.ccs.gematik.solutions/zts/services/termzilla" \
      de.gematik.commit-sha=$COMMIT_HASH \
      de.gematik.version=$VERSION
