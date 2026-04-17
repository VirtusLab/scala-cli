#!/usr/bin/env bash
set -euo pipefail

# Classifies changed files into categories for CI job filtering.
# Inputs (env vars):
#   EVENT_NAME              - GitHub event name (pull_request, push, ...).
#   BASE_REF                - Base ref of the PR (used to compute the diff).
#   CHANGED_FILES_OVERRIDE  - Optional newline-separated list of changed files.
#                             When set, overrides the git-diff-based detection
#                             (used by workflows that don't have a full checkout,
#                             e.g. pull_request_target commenting).
#   CLASSIFY_OUTPUT_FILE    - Optional path of a KEY=VALUE file to also write
#                             category results into (in addition to $GITHUB_OUTPUT).
# Outputs: writes category=true/false pairs to $GITHUB_OUTPUT (when set) and
# a summary table to $GITHUB_STEP_SUMMARY (when set). When CLASSIFY_OUTPUT_FILE
# is provided, also writes the same KEY=VALUE pairs there.

CATEGORIES=(code docs ci format_config benchmark gifs mill_wrapper)

write_output() {
  local key="$1"
  local val="$2"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "$key=$val" >> "$GITHUB_OUTPUT"
  fi
  if [[ -n "${CLASSIFY_OUTPUT_FILE:-}" ]]; then
    echo "$key=$val" >> "$CLASSIFY_OUTPUT_FILE"
  fi
}

write_summary() {
  [[ -n "${GITHUB_STEP_SUMMARY:-}" ]] || return 0
  echo "$1" >> "$GITHUB_STEP_SUMMARY"
}

set_all_true_and_exit() {
  local reason="$1"
  echo "$reason, setting all categories to true"
  for cat in "${CATEGORIES[@]}"; do
    write_output "$cat" "true"
  done
  exit 0
}

if [[ "${EVENT_NAME:-}" != "pull_request" ]]; then
  set_all_true_and_exit "Non-PR event (${EVENT_NAME:-unknown})"
fi

if [[ -n "${CHANGED_FILES_OVERRIDE:-}" ]]; then
  CHANGED_FILES="$CHANGED_FILES_OVERRIDE"
else
  CHANGED_FILES=$(git diff --name-only "origin/$BASE_REF...HEAD" || echo "DIFF_FAILED")
  if [[ "$CHANGED_FILES" == "DIFF_FAILED" ]]; then
    echo "::warning::Failed to compute diff, running all jobs"
    set_all_true_and_exit "Diff computation failed"
  fi
fi

CODE=false; DOCS=false; CI=false; FORMAT_CONFIG=false; BENCHMARK=false; GIFS=false; MILL_WRAPPER=false

while IFS= read -r file; do
  [[ -z "$file" ]] && continue
  case "$file" in
    modules/*|build.mill|project/*) CODE=true ;;
    website/*) DOCS=true ;;
    .github/*) CI=true ;;
    .scalafmt.conf|.scalafix.conf) FORMAT_CONFIG=true ;;
    gcbenchmark/*) BENCHMARK=true ;;
    gifs/*) GIFS=true ;;
    mill|mill.bat) MILL_WRAPPER=true ;;
  esac
done <<< "$CHANGED_FILES"

echo "Change categories:"
echo "  code=$CODE"
echo "  docs=$DOCS"
echo "  ci=$CI"
echo "  format_config=$FORMAT_CONFIG"
echo "  benchmark=$BENCHMARK"
echo "  gifs=$GIFS"
echo "  mill_wrapper=$MILL_WRAPPER"

write_output code "$CODE"
write_output docs "$DOCS"
write_output ci "$CI"
write_output format_config "$FORMAT_CONFIG"
write_output benchmark "$BENCHMARK"
write_output gifs "$GIFS"
write_output mill_wrapper "$MILL_WRAPPER"

write_summary "## Change categories"
write_summary "| Category | Changed |"
write_summary "|----------|---------|"
for cat in "${CATEGORIES[@]}"; do
  val=$(eval echo \$$( echo $cat | tr 'a-z' 'A-Z'))
  write_summary "| $cat | $val |"
done
