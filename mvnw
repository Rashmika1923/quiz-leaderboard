#!/bin/sh
# Maven Wrapper script
# Automatically downloads and uses the correct Maven version

set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Determine Maven home
if [ -n "$M2_HOME" ]; then
  MAVEN_HOME="$M2_HOME"
elif [ -n "$MAVEN_HOME" ]; then
  MAVEN_HOME="$MAVEN_HOME"
else
  MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.6"
fi

MAVEN_CMD="$MAVEN_HOME/bin/mvn"

# Download Maven if needed
if [ ! -f "$MAVEN_CMD" ]; then
  echo "Downloading Maven 3.9.6..."
  mkdir -p "$MAVEN_HOME"
  DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz"
  
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL "$DIST_URL" | tar xz -C "$MAVEN_HOME" --strip-components=1
  elif command -v wget > /dev/null 2>&1; then
    wget -qO- "$DIST_URL" | tar xz -C "$MAVEN_HOME" --strip-components=1
  else
    echo "Error: curl or wget required to download Maven" >&2
    exit 1
  fi
fi

exec "$MAVEN_CMD" "$@"
