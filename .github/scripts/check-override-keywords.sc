#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

//> using file ./pr-classify-lib/Env.scala
//> using file ./pr-classify-lib/EnvNames.scala
//> using file ./pr-classify-lib/OverrideKey.scala
//> using file ./pr-classify-lib/KeyValueFile.scala
//> using file ./pr-classify-lib/GitHubOutput.scala

// Checks the PR body for [test_*] override keywords.
// Inputs (env vars):
//   EVENT_NAME            - GitHub event name (pull_request, push, ...).
//   PR_BODY               - The PR body to scan for override keywords.
//   OVERRIDE_OUTPUT_FILE  - Optional path of a KEY=VALUE file to also write
//                           override results into (in addition to $GITHUB_OUTPUT).
// Outputs:
//   - `${keyword}=true|false` to $GITHUB_OUTPUT for every override,
//   - a Markdown summary table to $GITHUB_STEP_SUMMARY,
//   - and the same KEY=VALUE pairs to $OVERRIDE_OUTPUT_FILE when set.

import prclassify.*

val eventName = Env.opt(EnvNames.EventName).getOrElse("")
val prBody    = Env.opt(EnvNames.PrBody).getOrElse("")

val active: Set[OverrideKey] =
  if eventName != "pull_request" then
    println("Non-PR event, setting all overrides to true")
    OverrideKey.values.toSet
  else OverrideKey.values.iterator.filter(o => prBody.contains(o.marker)).toSet

OverrideKey.ordered.foreach: o =>
  if active.contains(o) then println(s"Override ${o.marker} found")

println("Override keywords:")
OverrideKey.ordered.foreach(o => println(s"  ${o.keyword}=${active.contains(o)}"))

val entries: Seq[(String, String)] =
  OverrideKey.ordered.map(o => o.keyword -> active.contains(o).toString)

entries.foreach((k, v) => GitHubOutput.writeScalar(k, v))

Env.opt(EnvNames.OverrideOutputFile).foreach: path =>
  KeyValueFile.appendAll(Env.toAbsolutePath(path), entries)

val overrideRows = OverrideKey.ordered
  .map(o => s"| ${o.marker} | ${active.contains(o)} |")
  .mkString("\n")

// overrideRows lines start with `|` (stripMargin's margin marker), so
// concatenate them after the stripped template instead of interpolating.
GitHubOutput.writeSummary(
  s"""## Override keywords
     || Keyword | Active |
     ||---------|--------|
     |""".stripMargin + overrideRows
)
