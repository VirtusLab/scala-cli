#!/usr/bin/env bash
set -euo pipefail

# Checks the PR body for [test_*] override keywords.
# Inputs (env vars):
#   EVENT_NAME            - GitHub event name (pull_request, push, ...).
#   PR_BODY               - The PR body to scan for override keywords.
#   OVERRIDE_OUTPUT_FILE  - Optional path of a KEY=VALUE file to also write
#                           override results into (in addition to $GITHUB_OUTPUT).
# Outputs: writes override=true/false pairs to $GITHUB_OUTPUT (when set) and
# a summary table to $GITHUB_STEP_SUMMARY (when set). When OVERRIDE_OUTPUT_FILE
# is provided, also writes the same KEY=VALUE pairs there.

OVERRIDES=(test_all test_native test_integration test_docs test_format)

write_output() {
  local key="$1"
  local val="$2"
  if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
    echo "$key=$val" >> "$GITHUB_OUTPUT"
  fi
  if [[ -n "${OVERRIDE_OUTPUT_FILE:-}" ]]; then
    echo "$key=$val" >> "$OVERRIDE_OUTPUT_FILE"
  fi
}

write_summary() {
  [[ -n "${GITHUB_STEP_SUMMARY:-}" ]] || return 0
  echo "$1" >> "$GITHUB_STEP_SUMMARY"
}

if [[ "${EVENT_NAME:-}" != "pull_request" ]]; then
  echo "Non-PR event, setting all overrides to true"
  for override in "${OVERRIDES[@]}"; do
    write_output "$override" "true"
  done
  exit 0
fi

TEST_ALL=false; TEST_NATIVE=false; TEST_INTEGRATION=false; TEST_DOCS=false; TEST_FORMAT=false

check_override() {
  local keyword="$1"
  local var_name="$2"
  if printf '%s' "${PR_BODY:-}" | grep -qF "$keyword"; then
    eval "$var_name=true"
    echo "Override $keyword found"
  fi
}

check_override "[test_all]" "TEST_ALL"
check_override "[test_native]" "TEST_NATIVE"
check_override "[test_integration]" "TEST_INTEGRATION"
check_override "[test_docs]" "TEST_DOCS"
check_override "[test_format]" "TEST_FORMAT"

echo "Override keywords:"
echo "  test_all=$TEST_ALL"
echo "  test_native=$TEST_NATIVE"
echo "  test_integration=$TEST_INTEGRATION"
echo "  test_docs=$TEST_DOCS"
echo "  test_format=$TEST_FORMAT"

write_output test_all "$TEST_ALL"
write_output test_native "$TEST_NATIVE"
write_output test_integration "$TEST_INTEGRATION"
write_output test_docs "$TEST_DOCS"
write_output test_format "$TEST_FORMAT"

write_summary "## Override keywords"
write_summary "| Keyword | Active |"
write_summary "|---------|--------|"
write_summary "| [test_all] | $TEST_ALL |"
write_summary "| [test_native] | $TEST_NATIVE |"
write_summary "| [test_integration] | $TEST_INTEGRATION |"
write_summary "| [test_docs] | $TEST_DOCS |"
write_summary "| [test_format] | $TEST_FORMAT |"
