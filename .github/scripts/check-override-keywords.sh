#!/usr/bin/env bash
set -euo pipefail

# Checks the PR body for [test_*] override keywords.
# Inputs (env vars): EVENT_NAME, PR_BODY
# Outputs: writes override=true/false pairs to $GITHUB_OUTPUT and a summary table to $GITHUB_STEP_SUMMARY

if [[ "$EVENT_NAME" != "pull_request" ]]; then
  echo "Non-PR event, setting all overrides to true (except test_skip)"
  for override in test_all test_native test_integration test_docs test_format; do
    echo "$override=true" >> "$GITHUB_OUTPUT"
  done
  echo "test_skip=false" >> "$GITHUB_OUTPUT"
  # opt-in jobs only run on PRs, never on pushes to main or tags
  for override in test_legacy_lts test_lts_all_platforms; do
    echo "$override=false" >> "$GITHUB_OUTPUT"
  done
  exit 0
fi

TEST_ALL=false; TEST_NATIVE=false; TEST_INTEGRATION=false; TEST_DOCS=false; TEST_FORMAT=false; TEST_SKIP=false
TEST_LEGACY_LTS=false; TEST_LTS_ALL_PLATFORMS=false

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
check_override "[test_skip]" "TEST_SKIP"
check_override "[test_legacy_lts]" "TEST_LEGACY_LTS"
check_override "[test_lts_all_platforms]" "TEST_LTS_ALL_PLATFORMS"

# [test_all] also enables the opt-in jobs
if [[ "$TEST_ALL" == "true" ]]; then
  TEST_LEGACY_LTS=true
  TEST_LTS_ALL_PLATFORMS=true
fi

echo "Override keywords:"
echo "  test_all=$TEST_ALL"
echo "  test_native=$TEST_NATIVE"
echo "  test_integration=$TEST_INTEGRATION"
echo "  test_docs=$TEST_DOCS"
echo "  test_format=$TEST_FORMAT"
echo "  test_skip=$TEST_SKIP"
echo "  test_legacy_lts=$TEST_LEGACY_LTS"
echo "  test_lts_all_platforms=$TEST_LTS_ALL_PLATFORMS"

echo "test_all=$TEST_ALL" >> "$GITHUB_OUTPUT"
echo "test_native=$TEST_NATIVE" >> "$GITHUB_OUTPUT"
echo "test_integration=$TEST_INTEGRATION" >> "$GITHUB_OUTPUT"
echo "test_docs=$TEST_DOCS" >> "$GITHUB_OUTPUT"
echo "test_format=$TEST_FORMAT" >> "$GITHUB_OUTPUT"
echo "test_skip=$TEST_SKIP" >> "$GITHUB_OUTPUT"
echo "test_legacy_lts=$TEST_LEGACY_LTS" >> "$GITHUB_OUTPUT"
echo "test_lts_all_platforms=$TEST_LTS_ALL_PLATFORMS" >> "$GITHUB_OUTPUT"

echo "## Override keywords" >> "$GITHUB_STEP_SUMMARY"
echo "| Keyword | Active |" >> "$GITHUB_STEP_SUMMARY"
echo "|---------|--------|" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_all] | $TEST_ALL |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_native] | $TEST_NATIVE |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_integration] | $TEST_INTEGRATION |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_docs] | $TEST_DOCS |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_format] | $TEST_FORMAT |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_skip] | $TEST_SKIP |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_legacy_lts] | $TEST_LEGACY_LTS |" >> "$GITHUB_STEP_SUMMARY"
echo "| [test_lts_all_platforms] | $TEST_LTS_ALL_PLATFORMS |" >> "$GITHUB_STEP_SUMMARY"
