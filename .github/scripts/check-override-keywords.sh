#!/usr/bin/env bash
set -euo pipefail

# Checks the PR body for [test_*] override keywords.
# Inputs (env vars): EVENT_NAME, PR_BODY
# Outputs: writes override=true/false pairs to $GITHUB_OUTPUT and a summary table to $GITHUB_STEP_SUMMARY

if [[ "$EVENT_NAME" != "pull_request" ]]; then
  echo "Non-PR event, setting all overrides to true"
  for override in test_all test_native test_integration test_docs test_format; do
    echo "$override=true" >> "$GITHUB_OUTPUT"
  done
  exit 0
fi

TEST_ALL=false; TEST_NATIVE=false; TEST_INTEGRATION=false; TEST_DOCS=false; TEST_FORMAT=false

check_override() {
  local keyword="$1"
  local var_name="$2"
  if printf '%s' "$PR_BODY" | grep -qF "$keyword"; then
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

echo "test_all=$TEST_ALL" >> "$GITHUB_OUTPUT"
echo "test_native=$TEST_NATIVE" >> "$GITHUB_OUTPUT"
echo "test_integration=$TEST_INTEGRATION" >> "$GITHUB_OUTPUT"
echo "test_docs=$TEST_DOCS" >> "$GITHUB_OUTPUT"
echo "test_format=$TEST_FORMAT" >> "$GITHUB_OUTPUT"

echo "## Override keywords" >> "$GITHUB_STEP_SUMMARY"
echo "| Keyword | Active |" >> "$GITHUB_STEP_SUMMARY"
echo "|---------|--------|" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_all] | $TEST_ALL |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_native] | $TEST_NATIVE |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_integration] | $TEST_INTEGRATION |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_docs] | $TEST_DOCS |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_format] | $TEST_FORMAT |" >> "$GITHUB_STEP_SUMMARY"
