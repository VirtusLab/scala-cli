#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

// Finds the most recent GitHub Actions run of the "CI" workflow for a given
// head SHA, with a few retries to tolerate the race between this workflow
// (triggered by pull_request_target) and the main CI workflow (triggered by
// pull_request) both starting at roughly the same time.
//
// Inputs (env vars):
//   REPO             - "<owner>/<repo>" (required).
//   HEAD_SHA         - Head SHA of the PR to search runs for (required).
//   SERVER_URL       - GitHub server URL (e.g. https://github.com), used only
//                      for the fallback link (required).
//   PR_NUMBER        - Pull-request number, used only for the fallback link
//                      (required).
//   GH_TOKEN         - GitHub token; consumed transparently by `gh`.
//   WORKFLOW_NAME    - Workflow to look for (default: "CI").
//   MAX_ATTEMPTS     - Retry budget (default: 4).
//   RETRY_DELAY_MS   - Delay between retries in milliseconds (default: 10000).
//   RUN_ID_OUTPUT    - Output key for the resolved run id (default: "run_id").
//   RUN_URL_OUTPUT   - Output key for the resolved run URL (default: "run_url").
//
// Behavior:
//   - Polls `gh api repos/<REPO>/actions/runs?event=pull_request&head_sha=<SHA>`
//     up to MAX_ATTEMPTS times, picking the run with the highest run_number
//     whose `name` matches WORKFLOW_NAME.
//   - If no run is found after all attempts, falls back to the PR checks page
//     URL (`<SERVER_URL>/<REPO>/pull/<PR_NUMBER>/checks`) with an empty run id.
//   - Writes `<RUN_ID_OUTPUT>=<value>` and `<RUN_URL_OUTPUT>=<value>` to
//     $GITHUB_OUTPUT when that env var is set.

def envRequired(name: String): String =
  sys.env.get(name).filter(_.nonEmpty).getOrElse:
    System.err.println(s"::error::$name is required")
    sys.exit(1)

def envWithDefault(name: String, default: String): String =
  sys.env.get(name).filter(_.nonEmpty).getOrElse(default)

val repo         = envRequired("REPO")
val headSha      = envRequired("HEAD_SHA")
val serverUrl    = envRequired("SERVER_URL")
val prNumber     = envRequired("PR_NUMBER")
val workflowName = envWithDefault("WORKFLOW_NAME", "CI")
val maxAttempts  = envWithDefault("MAX_ATTEMPTS", "4").toInt
val retryDelayMs = envWithDefault("RETRY_DELAY_MS", "10000").toLong
val runIdKey     = envWithDefault("RUN_ID_OUTPUT", "run_id")
val runUrlKey    = envWithDefault("RUN_URL_OUTPUT", "run_url")

case class ResolvedRun(id: String, url: String)

def queryLatestRun(): Option[ResolvedRun] =
  val result = os
    .proc("gh", "api", "-H", "Accept: application/vnd.github+json",
          s"repos/$repo/actions/runs?event=pull_request&head_sha=$headSha&per_page=30")
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
        .flatMap { run =>
          for
            id  <- run.obj.get("id").map(_.num.toLong.toString)
            url <- run.obj.get("html_url").map(_.str)
            if id.nonEmpty && url.nonEmpty
          yield ResolvedRun(id, url)
        }

val resolved: ResolvedRun = {
  var found: Option[ResolvedRun] = None
  var attempt = 1
  while found.isEmpty && attempt <= maxAttempts do
    found = queryLatestRun()
    if found.isEmpty then
      println(s"CI run not yet discoverable for $headSha (attempt $attempt); retrying...")
      if attempt < maxAttempts then Thread.sleep(retryDelayMs)
    attempt += 1
  found.getOrElse {
    println("Falling back to PR checks page")
    ResolvedRun(id = "", url = s"$serverUrl/$repo/pull/$prNumber/checks")
  }
}

println(s"Resolved run id: ${resolved.id}")
println(s"Resolved run url: ${resolved.url}")

sys.env.get("GITHUB_OUTPUT").foreach { ghOutputPath =>
  val payload = s"$runIdKey=${resolved.id}\n$runUrlKey=${resolved.url}\n"
  os.write.append(os.Path(ghOutputPath), payload)
}
