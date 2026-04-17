#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

//> using file ./pr-classify-lib/Env.scala
//> using file ./pr-classify-lib/EnvNames.scala
//> using file ./pr-classify-lib/Category.scala
//> using file ./pr-classify-lib/OverrideKey.scala
//> using file ./pr-classify-lib/SuiteGroup.scala
//> using file ./pr-classify-lib/Signals.scala
//> using file ./pr-classify-lib/KeyValueFile.scala

// Builds the Markdown body for the PR sticky comment that summarizes how the
// `changes` job classified the diff and which suite groups will run / be
// skipped. Intended to be invoked from `ci.yml`'s `changes` job so the
// resulting comment reflects the exact classification CI is using; the
// `pr-classify-comment.yml` workflow then downloads the rendered body as
// an artifact and posts it as a sticky PR comment.
//
// Inputs (env vars):
//   CLASSIFY_OUTPUT_FILE  - KEY=VALUE file produced by classify-changes.sc.
//   OVERRIDE_OUTPUT_FILE  - KEY=VALUE file produced by check-override-keywords.sc.
//   COMMENT_OUTPUT_FILE   - Path to write the rendered Markdown to (default: comment.md).
//   CI_RUN_ID             - Run ID of the CI workflow run (optional).
//   CI_RUN_URL            - URL to the CI workflow run (optional).

import prclassify.*

/** Which active overrides add suite groups that wouldn't run on their own. */
def overrideContributions(signals: Signals): Seq[(OverrideKey, Seq[SuiteGroup])] =
  val baseline     = signals.withoutOverrides
  val baselineRuns = SuiteGroup.values.iterator.filter(baseline.shouldRun).toSet
  OverrideKey.ordered.flatMap: key =>
    if !signals.has(key) then None
    else
      val probe = baseline.withOverride(key, enabled = true)
      val added = SuiteGroup.ordered.filter(g => probe.shouldRun(g) && !baselineRuns.contains(g))
      if added.isEmpty then None else Some(key -> added)

def renderComment(
  signals: Signals,
  ciRunId: Option[String],
  ciRunUrl: Option[String]
): String =
  val (runGroups, skipGroups) = SuiteGroup.ordered.partition(signals.shouldRun)

  val categoryRows = Category.ordered
    .map(c => s"| ${c.key} | ${signals.has(c)} |")
    .mkString("\n")

  def listLine(prefix: String, groups: Seq[SuiteGroup]): String =
    if groups.isEmpty then s"- $prefix: _(none)_"
    else s"- $prefix: ${groups.map(_.label).mkString(", ")}"

  // categoryRows lines start with `|`, which is stripMargin's margin marker,
  // so concatenate them after the stripped template instead of interpolating.
  val headerSection =
    s"""### CI change classification
       |
       |**Change categories**
       |
       || Category | Changed |
       || --- | --- |
       |""".stripMargin + categoryRows

  val suitesSection =
    s"""**Suite groups**
       |
       |${listLine("Will run", runGroups)}
       |${listLine("Will be skipped", skipGroups)}""".stripMargin

  val overridesSection: Option[String] = overrideContributions(signals) match
    case Nil           => None
    case contributions =>
      val rows = contributions
        .map((key, added) => s"- `${key.marker}` forces on: ${added.map(_.label).mkString(", ")}")
        .mkString("\n")
      Some(
        s"""**Override keywords affecting the run set**
           |
           |$rows""".stripMargin
      )

  val ciRunSection: Option[String] = ciRunId match
    case Some(id) => Some(s"Full CI run: [#$id](${ciRunUrl.getOrElse("")})")
    case None     => ciRunUrl.map(url => s"Full CI run: $url")

  val sections =
    Seq(Some(headerSection), Some(suitesSection), overridesSection, ciRunSection).flatten

  sections.mkString("\n\n") + "\n"

val categoryMap = KeyValueFile.read(Env.requiredFile(EnvNames.ClassifyOutputFile))
val overrideMap = KeyValueFile.read(Env.requiredFile(EnvNames.OverrideOutputFile))
val signals     = Signals.fromKeyValueMaps(categoryMap, overrideMap)

val commentPath = Env.toAbsolutePath(
  Env.withDefault(EnvNames.CommentOutputFile, "comment.md")
)

val body = renderComment(
  signals,
  ciRunId = Env.opt(EnvNames.CiRunId),
  ciRunUrl = Env.opt(EnvNames.CiRunUrl)
)
println(
  s"""Generated comment body:
     |-----------------------
     |$body
     |-----------------------""".stripMargin
)

os.write.over(commentPath, body, createFolders = true)
println(s"Wrote comment to $commentPath")
