#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"

java_major() {
  local java_bin="$1"
  local version
  version="$("${java_bin}" -version 2>&1 | awk -F '"' '/version/ {print $2}')"
  if [[ "${version}" == 1.* ]]; then
    printf '%s\n' "${version#1.}" | cut -d. -f1
  else
    printf '%s\n' "${version}" | cut -d. -f1
  fi
}

java_home_is_usable() {
  [[ -n "${JAVA_HOME:-}" ]] &&
    [[ -x "${JAVA_HOME}/bin/java" ]] &&
    [[ "$(java_major "${JAVA_HOME}/bin/java")" -ge 17 ]]
}

path_java_is_usable() {
  command -v java >/dev/null 2>&1 &&
    [[ "$(java_major "$(command -v java)")" -ge 17 ]]
}

if ! java_home_is_usable && path_java_is_usable; then
  unset JAVA_HOME
fi

if ! java_home_is_usable && ! path_java_is_usable; then
  if [[ -x "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home/bin/java" ]]; then
    export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
  elif command -v /usr/libexec/java_home >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
  fi
fi

if ! java_home_is_usable && ! path_java_is_usable; then
  echo "Java 17+ is required. Set JAVA_HOME to a JDK 17 or newer installation." >&2
  exit 1
fi

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/tomljump-gradle-home}"

cd "${repo_root}"

./gradlew :core:tomljump-core:test
./gradlew :plugins:jetbrains:test
./gradlew :plugins:jetbrains:buildPlugin
