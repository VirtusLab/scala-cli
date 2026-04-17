#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

//> using file ./pr-classify-lib/Env.scala
//> using file ./pr-classify-lib/EnvNames.scala
//> using file ./pr-classify-lib/Category.scala
//> using file ./pr-classify-lib/KeyValueFile.scala
//> using file ./pr-classify-lib/GitHubOutput.scala

// Classifies changed files into categories for CI job filtering.
// Inputs (env vars):
//   EVENT_NAME              - GitHub event name (pull_request, push, ...).
//   BASE_REF                - Base ref of the PR (used to compute the diff).
//   CHANGED_FILES_OVERRIDE  - Optional newline-separated list of changed files.
//                             When set, overrides the git-diff-based detection.
//   CLASSIFY_OUTPUT_FILE    - Optional path of a KEY=VALUE file to also write
//                             category results into (in addition to $GITHUB_OUTPUT).
// Outputs:
//   - `${category}=true|false` to $GITHUB_OUTPUT for every category,
//   - a Markdown summary table to $GITHUB_STEP_SUMMARY,
//   - and the same KEY=VALUE pairs to $CLASSIFY_OUTPUT_FILE when set.

import prclassify.*

enum ChangeSet:
  case All
  case Specific(files: Seq[String])

def splitLines(s: String): Seq[String] =
  s.linesIterator.map(_.trim).filter(_.nonEmpty).toIndexedSeq

val eventName = Env.opt(EnvNames.EventName).getOrElse("")

val changeSet: ChangeSet =
  if eventName != "pull_request" then
    println(s"Non-PR event ($eventName), setting all categories to true")
    ChangeSet.All
  else
    Env.opt(EnvNames.ChangedFilesOverride) match
      case Some(list) =>
        ChangeSet.Specific(splitLines(list))
      case None =>
        val baseRef = Env.required(EnvNames.BaseRef)
        val result  = os.proc("git", "diff", "--name-only", s"origin/$baseRef...HEAD")
          .call(check = false, mergeErrIntoOut = false, stderr = os.Inherit)
        if result.exitCode != 0 then
          System.err.println("::warning::Failed to compute diff, running all jobs")
          println("Diff computation failed, setting all categories to true")
          ChangeSet.All
        else ChangeSet.Specific(splitLines(result.out.text()))

val activeCategories: Set[Category] = changeSet match
  case ChangeSet.All             => Category.values.toSet
  case ChangeSet.Specific(files) => files.iterator.flatMap(Category.forPath).toSet

println("Change categories:")
Category.ordered.foreach(c => println(s"  ${c.key}=${activeCategories.contains(c)}"))

val entries: Seq[(String, String)] =
  Category.ordered.map(c => c.key -> activeCategories.contains(c).toString)

entries.foreach((k, v) => GitHubOutput.writeScalar(k, v))

Env.opt(EnvNames.ClassifyOutputFile).foreach: path =>
  KeyValueFile.appendAll(Env.toAbsolutePath(path), entries)

val categoryRows = Category.ordered
  .map(c => s"| ${c.key} | ${activeCategories.contains(c)} |")
  .mkString("\n")

// categoryRows lines start with `|` (stripMargin's margin marker), so
// concatenate them after the stripped template instead of interpolating.
GitHubOutput.writeSummary(
  s"""## Change categories
     || Category | Changed |
     ||----------|---------|
     |""".stripMargin + categoryRows
)
