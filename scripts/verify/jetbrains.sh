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
    [[ "$(java_major "${JAVA_HOME}/bin/java")" -ge 21 ]]
}

path_java_is_usable() {
  command -v java >/dev/null 2>&1 &&
    [[ "$(java_major "$(command -v java)")" -ge 21 ]]
}

candidate_java_homes() {
  if [[ -n "${JAVA_HOME:-}" ]]; then
    printf '%s\n' "${JAVA_HOME}"
  fi

  printf '%s\n' \
    "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" \
    "/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" \
    "/usr/lib/jvm/temurin-21-jdk" \
    "/usr/lib/jvm/java-21-openjdk" \
    "/usr/lib/jvm/java-21-openjdk-amd64" \
    "/usr/lib/jvm/jdk-21"

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    /usr/libexec/java_home -v 21 2>/dev/null || true
  fi
}

find_java_21_home() {
  local candidate
  while IFS= read -r candidate; do
    [[ -n "${candidate}" ]] || continue
    [[ -x "${candidate}/bin/java" ]] || continue
    [[ "$(java_major "${candidate}/bin/java")" -ge 21 ]] || continue
    printf '%s\n' "${candidate}"
    return 0
  done < <(candidate_java_homes)

  return 1
}

if ! java_home_is_usable; then
  discovered_java_home="$(find_java_21_home || true)"
  if [[ -n "${discovered_java_home}" ]]; then
    export JAVA_HOME="${discovered_java_home}"
  elif path_java_is_usable; then
    unset JAVA_HOME
  fi
fi

if ! java_home_is_usable && ! path_java_is_usable; then
  echo "Java 21+ is required but was not found automatically." >&2
  echo "Install JDK 21, or set JAVA_HOME to a JDK 21+ installation and rerun this script." >&2
  exit 1
fi

if java_home_is_usable; then
  echo "Using Java $(java_major "${JAVA_HOME}/bin/java") from JAVA_HOME=${JAVA_HOME}"
else
  echo "Using Java $(java_major "$(command -v java)") from PATH: $(command -v java)"
fi

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/tomljump-gradle-home}"

cd "${repo_root}"

rm -rf "${GRADLE_USER_HOME}/caches/8.10.2/transforms"

./gradlew :core:tomljump-core:test
./gradlew :plugins:jetbrains:test
./gradlew :plugins:jetbrains:buildPlugin
