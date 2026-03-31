#!/usr/bin/env bash
set -euo pipefail

# Classifies changed files into categories for CI job filtering.
# Inputs (env vars): EVENT_NAME, BASE_REF
# Outputs: writes category=true/false pairs to $GITHUB_OUTPUT and a summary table to $GITHUB_STEP_SUMMARY

if [[ "$EVENT_NAME" != "pull_request" ]]; then
  echo "Non-PR event ($EVENT_NAME), setting all categories to true"
  for cat in code docs ci format_config benchmark gifs mill_wrapper; do
    echo "$cat=true" >> "$GITHUB_OUTPUT"
  done
  exit 0
fi

CHANGED_FILES=$(git diff --name-only "origin/$BASE_REF...HEAD" || echo "DIFF_FAILED")
if [[ "$CHANGED_FILES" == "DIFF_FAILED" ]]; then
  echo "::warning::Failed to compute diff, running all jobs"
  for cat in code docs ci format_config benchmark gifs mill_wrapper; do
    echo "$cat=true" >> "$GITHUB_OUTPUT"
  done
  exit 0
fi

CODE=false; DOCS=false; CI=false; FORMAT_CONFIG=false; BENCHMARK=false; GIFS=false; MILL_WRAPPER=false

while IFS= read -r file; do
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

echo "code=$CODE" >> "$GITHUB_OUTPUT"
echo "docs=$DOCS" >> "$GITHUB_OUTPUT"
echo "ci=$CI" >> "$GITHUB_OUTPUT"
echo "format_config=$FORMAT_CONFIG" >> "$GITHUB_OUTPUT"
echo "benchmark=$BENCHMARK" >> "$GITHUB_OUTPUT"
echo "gifs=$GIFS" >> "$GITHUB_OUTPUT"
echo "mill_wrapper=$MILL_WRAPPER" >> "$GITHUB_OUTPUT"

echo "## Change categories" >> "$GITHUB_STEP_SUMMARY"
echo "| Category | Changed |" >> "$GITHUB_STEP_SUMMARY"
echo "|----------|---------|" >> "$GITHUB_STEP_SUMMARY"
for cat in code docs ci format_config benchmark gifs mill_wrapper; do
  val=$(eval echo \$$( echo $cat | tr 'a-z' 'A-Z'))
  echo "| $cat | $val |" >> "$GITHUB_STEP_SUMMARY"
done
