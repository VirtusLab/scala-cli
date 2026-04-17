#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

// Fetches the list of files changed in a pull request via the GitHub REST API
// (using the `gh` CLI, which is pre-installed on GitHub Actions runners).
//
// Inputs (env vars):
//   REPO          - "<owner>/<repo>" (required).
//   PR_NUMBER     - Pull-request number (required).
//   GH_TOKEN      - GitHub token; consumed transparently by `gh`.
//   OUTPUT_NAME   - Optional name of the step output to write the list under
//                   (default: "files"). The list is written as a multi-line
//                   output in $GITHUB_OUTPUT when that env var is set.
//
// Behavior:
//   - Calls `gh api --paginate repos/<REPO>/pulls/<PR_NUMBER>/files` and
//     collects each `.filename`.
//   - Echoes the list to stdout for log visibility.
//   - When $GITHUB_OUTPUT is defined, appends a heredoc-style multi-line
//     output (e.g. `files<<EOF ... EOF`) for later steps to consume.

import java.util.UUID

def envRequired(name: String): String =
  sys.env.get(name).filter(_.nonEmpty).getOrElse:
    System.err.println(s"::error::$name is required")
    sys.exit(1)

val repo      = envRequired("REPO")
val prNumber  = envRequired("PR_NUMBER")
val outputKey = sys.env.get("OUTPUT_NAME").filter(_.nonEmpty).getOrElse("files")

val rawJson = os
  .proc("gh", "api", "--paginate", "-H", "Accept: application/vnd.github+json",
        s"repos/$repo/pulls/$prNumber/files")
  .call(check = true)
  .out
  .text()

// `gh api --paginate` concatenates each page's JSON array. Split at the array
// boundary `][` so we can parse each page and flatten the results.
val files: Seq[String] = rawJson
  .split("\\]\\[")
  .toIndexedSeq
  .map(_.trim)
  .filter(_.nonEmpty)
  .flatMap { page =>
    val normalized =
      if page.startsWith("[") && page.endsWith("]") then page
      else if page.startsWith("[") then page + "]"
      else if page.endsWith("]") then "[" + page
      else "[" + page + "]"
    ujson.read(normalized).arr.flatMap(entry => entry.obj.get("filename").map(_.str))
  }

println("Changed files:")
files.foreach(println)

sys.env.get("GITHUB_OUTPUT").foreach { ghOutputPath =>
  val delimiter = s"EOF_${UUID.randomUUID().toString.replace("-", "")}"
  val payload   = (Seq(s"$outputKey<<$delimiter") ++ files :+ delimiter).mkString("\n") + "\n"
  os.write.append(os.Path(ghOutputPath), payload)
}
