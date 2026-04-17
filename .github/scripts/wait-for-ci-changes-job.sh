#!/usr/bin/env bash
# Polls the GitHub REST API until the `changes` job of the CI workflow run
# matching $HEAD_SHA has finished. Exits 0 on success (and writes the run id
# to $GITHUB_OUTPUT as `run_id`), non-zero on failure or timeout.
#
# Expected environment variables:
#   GH_TOKEN          - token used by `gh api`.
#   REPO              - "owner/name" of the repository.
#   HEAD_SHA          - PR head SHA to match CI runs against.
#   GITHUB_OUTPUT     - standard GitHub Actions output file.
#   MAX_ATTEMPTS      - (optional, default 40) number of polling attempts.
#   INTERVAL_SECONDS  - (optional, default 15) seconds between attempts.
#   CI_WORKFLOW_NAME  - (optional, default "CI") name of the workflow to wait for.
#   CI_JOB_NAME       - (optional, default "changes") name of the job to wait for.
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN is required}"
: "${REPO:?REPO is required}"
: "${HEAD_SHA:?HEAD_SHA is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

max_attempts="${MAX_ATTEMPTS:-40}"
interval="${INTERVAL_SECONDS:-15}"
workflow_name="${CI_WORKFLOW_NAME:-CI}"
job_name="${CI_JOB_NAME:-changes}"

attempt=0
while [ "$attempt" -lt "$max_attempts" ]; do
  attempt=$((attempt + 1))

  # Find the most recent matching workflow run for this head SHA. The run
  # may not be discoverable for a few seconds after the PR event dispatches.
  run_id=$(gh api \
    "repos/$REPO/actions/runs?event=pull_request&head_sha=$HEAD_SHA&per_page=30" \
    --jq "[.workflow_runs[] | select(.name==\"$workflow_name\")] | sort_by(.run_number) | last | .id // empty")

  if [ -z "$run_id" ]; then
    echo "$workflow_name run not yet discoverable for $HEAD_SHA (attempt $attempt/$max_attempts)"
    sleep "$interval"
    continue
  fi

  job=$(gh api "repos/$REPO/actions/runs/$run_id/jobs" \
    --jq ".jobs[] | select(.name==\"$job_name\") | {status: .status, conclusion: .conclusion}")
  status=$(echo "$job" | jq -r '.status // empty')
  conclusion=$(echo "$job" | jq -r '.conclusion // empty')

  if [ "$status" = "completed" ]; then
    if [ "$conclusion" = "success" ]; then
      echo "$job_name job completed in $workflow_name run $run_id"
      echo "run_id=$run_id" >> "$GITHUB_OUTPUT"
      exit 0
    fi
    echo "::error::$job_name job in $workflow_name run $run_id finished with conclusion=$conclusion"
    exit 1
  fi

  echo "$workflow_name run $run_id, $job_name job status=$status (attempt $attempt/$max_attempts)"
  sleep "$interval"
done

echo "::error::Timed out after $max_attempts attempts waiting for the $job_name job in $workflow_name"
exit 1
