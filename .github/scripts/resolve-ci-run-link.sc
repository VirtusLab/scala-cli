#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

//> using file ./pr-classify-lib/Env.scala
//> using file ./pr-classify-lib/EnvNames.scala
//> using file ./pr-classify-lib/GitHubOutput.scala

// Finds the most recent GitHub Actions run of the "CI" workflow for a given
// head SHA, with a few retries to tolerate the race between this workflow
// (triggered by pull_request_target) and the main CI workflow (triggered by
// pull_request) both starting at roughly the same time.
//
// Inputs (env vars):
//   REPO             - "<owner>/<repo>" (required).
//   HEAD_SHA         - Head SHA of the PR to search runs for (required).
//   SERVER_URL       - GitHub server URL (required; only used for fallback link).
//   PR_NUMBER       - Pull-request number (required; only used for fallback link).
//   GH_TOKEN         - GitHub token; consumed transparently by `gh`.
//   WORKFLOW_NAME    - Workflow to look for (default: "CI").
//   MAX_ATTEMPTS     - Retry budget (default: 4).
//   RETRY_DELAY_MS   - Delay between retries in milliseconds (default: 10000).
//   RUN_ID_OUTPUT    - Output key for the resolved run id (default: "run_id").
//   RUN_URL_OUTPUT   - Output key for the resolved run URL (default: "run_url").
//
// Outputs:
//   - `<RUN_ID_OUTPUT>=<id>` and `<RUN_URL_OUTPUT>=<url>` to $GITHUB_OUTPUT,
//     with an empty id and the PR checks-page URL on fallback.

import prclassify.*

val repo         = Env.required(EnvNames.Repo)
val headSha      = Env.required(EnvNames.HeadSha)
val serverUrl    = Env.required(EnvNames.ServerUrl)
val prNumber     = Env.required(EnvNames.PrNumber)
val workflowName = Env.withDefault(EnvNames.WorkflowName, "CI")
val maxAttempts  = Env.withDefault(EnvNames.MaxAttempts, "4").toInt
val retryDelayMs = Env.withDefault(EnvNames.RetryDelayMs, "10000").toLong
val runIdKey     = Env.withDefault(EnvNames.RunIdOutput, "run_id")
val runUrlKey    = Env.withDefault(EnvNames.RunUrlOutput, "run_url")

case class ResolvedRun(id: String, url: String)

def queryLatestRun(): Option[ResolvedRun] =
  val result = os
    .proc(
      "gh",
      "api",
      "-H",
      "Accept: application/vnd.github+json",
      s"repos/$repo/actions/runs?event=pull_request&head_sha=$headSha&per_page=30"
    )
    .call(check = false)
  if result.exitCode != 0 then
    System.err.println(s"gh api failed (exit ${result.exitCode}): ${result.err.text()}")
    None
  else
    val body = result.out.text().trim
    if body.isEmpty then None
    else
      val runs = ujson.read(body).obj.get("workflow_runs").map(_.arr).getOrElse(Seq.empty)
      runs
        .filter(r => r.obj.get("name").map(_.str).contains(workflowName))
        .sortBy(r => r.obj.get("run_number").map(_.num.toLong).getOrElse(0L))
        .lastOption
        .flatMap: run =>
          for
            id  <- run.obj.get("id").map(_.num.toLong.toString)
            url <- run.obj.get("html_url").map(_.str)
            if id.nonEmpty && url.nonEmpty
          yield ResolvedRun(id, url)

val resolved: ResolvedRun =
  var found: Option[ResolvedRun] = None
  var attempt                    = 1
  while found.isEmpty && attempt <= maxAttempts do
    found = queryLatestRun()
    if found.isEmpty then
      println(s"CI run not yet discoverable for $headSha (attempt $attempt); retrying...")
      if attempt < maxAttempts then Thread.sleep(retryDelayMs)
    attempt += 1
  found.getOrElse:
    println("Falling back to PR checks page")
    ResolvedRun(id = "", url = s"$serverUrl/$repo/pull/$prNumber/checks")

println(s"Resolved run id: ${resolved.id}")
println(s"Resolved run url: ${resolved.url}")

GitHubOutput.writeScalar(runIdKey, resolved.id)
GitHubOutput.writeScalar(runUrlKey, resolved.url)
