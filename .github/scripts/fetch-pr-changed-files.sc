#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

//> using file ./pr-classify-lib/Env.scala
//> using file ./pr-classify-lib/EnvNames.scala
//> using file ./pr-classify-lib/GitHubOutput.scala

// Fetches the list of files changed in a pull request via `gh api`. The `gh`
// CLI is pre-installed on GitHub Actions runners and will pick up the token
// from $GH_TOKEN.
//
// Inputs (env vars):
//   REPO          - "<owner>/<repo>" (required).
//   PR_NUMBER     - Pull-request number (required).
//   GH_TOKEN      - GitHub token; consumed transparently by `gh`.
//   OUTPUT_NAME   - Optional $GITHUB_OUTPUT key to use (default: "files").
//
// Outputs:
//   - Echoes the file list to stdout for log visibility.
//   - Writes a multi-line output (`<OUTPUT_NAME><<EOF ... EOF`) to
//     $GITHUB_OUTPUT when that env var is set.

import prclassify.*

val repo      = Env.required(EnvNames.Repo)
val prNumber  = Env.required(EnvNames.PrNumber)
val outputKey = Env.withDefault(EnvNames.OutputName, "files")

val rawJson = os
  .proc(
    "gh",
    "api",
    "--paginate",
    "-H",
    "Accept: application/vnd.github+json",
    s"repos/$repo/pulls/$prNumber/files"
  )
  .call(check = true)
  .out
  .text()

// `gh api --paginate` concatenates each page's JSON array; split at the
// array boundary `][` so each page can be parsed individually.
val files: Seq[String] = rawJson
  .split("\\]\\[")
  .toIndexedSeq
  .map(_.trim)
  .filter(_.nonEmpty)
  .flatMap: page =>
    val normalized =
      if page.startsWith("[") && page.endsWith("]") then page
      else if page.startsWith("[") then page + "]"
      else if page.endsWith("]") then "[" + page
      else "[" + page + "]"
    ujson.read(normalized).arr.flatMap(entry => entry.obj.get("filename").map(_.str))

println("Changed files:")
files.foreach(println)

GitHubOutput.writeMultiline(outputKey, files)
