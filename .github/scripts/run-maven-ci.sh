#!/usr/bin/env bash

# Run Maven in CI with bounded recovery for transient repository failures.
# A failed/limited Maven Central request can leave *.lastUpdated markers in
# ~/.m2. When that directory is cached, later jobs may incorrectly report that
# an existing artifact cannot be found. -U plus marker cleanup repairs that
# state; retries are intentionally limited to repository/network failures so
# genuine compilation and test failures still fail immediately.
set -euo pipefail

readonly max_attempts="${MAVEN_CI_MAX_ATTEMPTS:-3}"
readonly base_delay_seconds="${MAVEN_CI_RETRY_DELAY_SECONDS:-15}"
readonly local_repository="${HOME}/.m2/repository"

if [[ $# -eq 0 ]]; then
  echo "Usage: run-maven-ci.sh <Maven arguments>" >&2
  exit 2
fi

clear_failed_download_markers() {
  if [[ -d "${local_repository}" ]]; then
    find "${local_repository}" -type f -name '*.lastUpdated' -delete
  fi
}

is_repository_failure() {
  local log_file="$1"
  grep -Eqi \
    'Could not (find artifact|resolve|transfer)|PluginResolutionException|DependencyResolutionException|status code: (429|500|502|503|504)|Too Many Requests|Connection (reset|timed out)|Read timed out|Unknown host|Name or service not known|Temporary failure in name resolution' \
    "${log_file}"
}

# Repair a negative lookup restored from an earlier GitHub Actions cache before
# the first Maven invocation. -U then forces Maven to re-check remote metadata.
clear_failed_download_markers

for ((attempt = 1; attempt <= max_attempts; attempt++)); do
  attempt_log="$(mktemp)"
  echo "Maven CI attempt ${attempt}/${max_attempts}"

  set +e
  ./mvnw \
    -B \
    -U \
    -Dmaven.wagon.http.retryHandler.count=3 \
    "$@" 2>&1 | tee "${attempt_log}"
  maven_status=${PIPESTATUS[0]}
  set -e

  if [[ ${maven_status} -eq 0 ]]; then
    rm -f "${attempt_log}"
    exit 0
  fi

  if ! is_repository_failure "${attempt_log}" || [[ ${attempt} -eq ${max_attempts} ]]; then
    rm -f "${attempt_log}"
    exit "${maven_status}"
  fi

  rm -f "${attempt_log}"
  clear_failed_download_markers
  delay_seconds=$((base_delay_seconds * attempt))
  echo "Transient Maven repository failure detected; retrying in ${delay_seconds}s..." >&2
  sleep "${delay_seconds}"
done

