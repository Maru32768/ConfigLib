#!/bin/bash
# Locate and extract ConfigLib sources JARs to /tmp/configlib-sources/.
# Prints the JAR path (spigot or paper) on success, or "LOCAL_BUILD" if no JAR is found.

PLATFORM_JAR=$(find ~/.gradle/caches/modules-2/files-2.1/com.github.Maru32768.ConfigLib \
  \( -name "spigot-*-sources.jar" -o -name "paper-*-sources.jar" \) 2>/dev/null | sort | tail -1)

if [ -z "$PLATFORM_JAR" ]; then
  echo "LOCAL_BUILD"
  exit 0
fi

COMMON_JAR=$(find ~/.gradle/caches/modules-2/files-2.1/com.github.Maru32768.ConfigLib \
  -name "common-*-sources.jar" 2>/dev/null | sort | tail -1)

mkdir -p /tmp/configlib-sources
unzip -qo "$PLATFORM_JAR" -d /tmp/configlib-sources
[ -n "$COMMON_JAR" ] && unzip -qo "$COMMON_JAR" -d /tmp/configlib-sources

echo "$PLATFORM_JAR"
