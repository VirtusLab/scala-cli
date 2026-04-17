package prclassify

/** Canonical names of the environment variables consumed or produced by the PR classification /
  * commenting workflows. Kept in one place so renaming a variable is a single-file change.
  */
object EnvNames:

  // ---- Inputs ----
  /** "pull_request", "push", ... */
  val EventName = "EVENT_NAME"

  /** Base ref of the PR; used to compute the git diff. */
  val BaseRef = "BASE_REF"

  /** Newline-separated list of changed files. When set, overrides the git-diff-based detection
    * (used by workflows without a full checkout).
    */
  val ChangedFilesOverride = "CHANGED_FILES_OVERRIDE"

  /** Body of the pull request (scanned for override markers). */
  val PrBody = "PR_BODY"

  /** "owner/repo". */
  val Repo     = "REPO"
  val PrNumber = "PR_NUMBER"
  val HeadSha  = "HEAD_SHA"

  /** e.g. "https://github.com". */
  val ServerUrl = "SERVER_URL"

  // ---- Outputs (file-based) ----
  /** Path where `classify-changes` writes its KEY=VALUE output. */
  val ClassifyOutputFile = "CLASSIFY_OUTPUT_FILE"

  /** Path where `check-override-keywords` writes its KEY=VALUE output. */
  val OverrideOutputFile = "OVERRIDE_OUTPUT_FILE"

  /** Path where `build-pr-classification-comment` writes the rendered Markdown comment.
    */
  val CommentOutputFile = "COMMENT_OUTPUT_FILE"

  // ---- Run-link context ----
  val ClassifyRunId  = "CLASSIFY_RUN_ID"
  val ClassifyRunUrl = "CLASSIFY_RUN_URL"
  val CiRunId        = "CI_RUN_ID"
  val CiRunUrl       = "CI_RUN_URL"

  // ---- Script tunables ----
  /** Name of the workflow to search for when resolving the CI run link. */
  val WorkflowName = "WORKFLOW_NAME"
  val MaxAttempts  = "MAX_ATTEMPTS"
  val RetryDelayMs = "RETRY_DELAY_MS"
  val RunIdOutput  = "RUN_ID_OUTPUT"
  val RunUrlOutput = "RUN_URL_OUTPUT"

  /** Name of the GITHUB_OUTPUT key `fetch-pr-changed-files` writes to. */
  val OutputName = "OUTPUT_NAME"

  // ---- GitHub Actions built-ins ----
  val GitHubOutput      = "GITHUB_OUTPUT"
  val GitHubStepSummary = "GITHUB_STEP_SUMMARY"
