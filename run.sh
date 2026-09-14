#!/usr/bin/env bash
# Launch the plugin in a local Jenkins for development: ./run.sh
#
# Pins the JDK to Java 21 (the parent POM / maven-hpi-plugin support Java 17/21;
# Java 26 is not supported) and uses the project's Maven wrapper (Maven 3.9.x).
set -euo pipefail

# Find a Java 17 or 21 JDK. Override by exporting JAVA_HOME before running.
if [ -z "${JAVA_HOME:-}" ] || ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -qE '"(17|21)\.'; then
  for candidate in \
    /usr/lib/jvm/java-21-openjdk-amd64 \
    /usr/lib/jvm/java-1.21.0-openjdk-amd64 \
    /usr/lib/jvm/java-17-openjdk-amd64; do
    if [ -x "$candidate/bin/java" ]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [ -z "${JAVA_HOME:-}" ]; then
  echo "ERROR: No Java 17/21 JDK found. Set JAVA_HOME to a Java 17 or 21 install." >&2
  exit 1
fi

echo "Using JAVA_HOME=$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# Port for the dev Jenkins (override with: PORT=9000 ./run.sh). 8080 is often taken.
PORT="${PORT:-8090}"
# Listen on all interfaces so it's reachable from the Windows browser under WSL2.
HOST="${HOST:-0.0.0.0}"
echo "Jenkins will be available at: http://localhost:${PORT}/jenkins/"

cd "$(dirname "$0")"
exec ./mvnw hpi:run "-Dport=${PORT}" "-Dhost=${HOST}" "$@"
