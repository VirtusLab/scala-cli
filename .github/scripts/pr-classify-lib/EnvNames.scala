package prclassify

/** Canonical names of the environment variables consumed or produced by the PR classification /
  * commenting workflows. Kept in one place so renaming a variable is a single-file change.
  */
object EnvNames:

  // ---- Inputs to classify-changes.sc / check-override-keywords.sc ----
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

  // ---- KEY=VALUE / comment artifact paths ----
  /** Path where `classify-changes` writes its KEY=VALUE output. */
  val ClassifyOutputFile = "CLASSIFY_OUTPUT_FILE"

  /** Path where `check-override-keywords` writes its KEY=VALUE output. */
  val OverrideOutputFile = "OVERRIDE_OUTPUT_FILE"

  /** Path where `build-pr-classification-comment` writes the rendered Markdown comment.
    */
  val CommentOutputFile = "COMMENT_OUTPUT_FILE"

  // ---- CI run context (embedded in the rendered comment) ----
  val CiRunId  = "CI_RUN_ID"
  val CiRunUrl = "CI_RUN_URL"

  // ---- GitHub Actions built-ins ----
  val GitHubOutput      = "GITHUB_OUTPUT"
  val GitHubStepSummary = "GITHUB_STEP_SUMMARY"
