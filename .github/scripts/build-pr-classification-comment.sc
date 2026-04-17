#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

// Builds the Markdown body for the PR sticky comment that summarizes how the
// `changes` job classified the diff and which suite groups will run / be skipped.
//
// Inputs (env vars):
//   CLASSIFY_OUTPUT_FILE  - KEY=VALUE file produced by classify-changes.sh.
//   OVERRIDE_OUTPUT_FILE  - KEY=VALUE file produced by check-override-keywords.sh.
//   COMMENT_OUTPUT_FILE   - Path to write the resulting Markdown comment to
//                           (default: ./comment.md).
//   CLASSIFY_RUN_ID       - Run ID of the classification workflow (optional).
//   CLASSIFY_RUN_URL      - URL to the classification workflow run (optional).
//   CI_RUN_ID             - Run ID of the matching CI workflow run (optional;
//                           empty means "not yet resolved", link falls back).
//   CI_RUN_URL            - URL to the matching CI workflow run, or a fallback
//                           link (e.g. the PR's /checks page).

import java.nio.file.Paths

def envOpt(name: String): Option[String] =
  sys.env.get(name).filter(_.nonEmpty)

def envRequiredFile(name: String): os.Path =
  val raw = envOpt(name).getOrElse:
    System.err.println(s"::error::$name is missing")
    sys.exit(1)
  val path = toAbsolutePath(raw)
  if !os.exists(path) then
    System.err.println(s"::error::$name points to non-existent file: $path")
    sys.exit(1)
  path

def toAbsolutePath(s: String): os.Path =
  if Paths.get(s).isAbsolute then os.Path(s) else os.Path(s, os.pwd)

def readKeyValueFile(path: os.Path): Map[String, String] =
  os.read.lines(path).iterator.flatMap { line =>
    val trimmed = line.trim
    if trimmed.isEmpty || trimmed.startsWith("#") then None
    else
      trimmed.split("=", 2) match
        case Array(k, v) => Some(k.trim -> v.trim)
        case _           => None
  }.toMap

enum SuiteGroup(val label: String):
  case UnitAndMill         extends SuiteGroup("Unit tests & fish shell")
  case JvmIntegration      extends SuiteGroup("JVM integration tests")
  case NativeIntegration   extends SuiteGroup("Native integration tests")
  case DocsTests           extends SuiteGroup("Docs tests")
  case Checks              extends SuiteGroup("Checks")
  case Format              extends SuiteGroup("Format / scalafix")
  case ReferenceDoc        extends SuiteGroup("Reference docs")
  case BloopMemoryFootprint extends SuiteGroup("Bloop memory footprint")
  case SbtExportVcRedist   extends SuiteGroup("Sbt export / vc-redist")

enum OverrideKey(val keyword: String):
  case TestAll         extends OverrideKey("test_all")
  case TestNative      extends OverrideKey("test_native")
  case TestIntegration extends OverrideKey("test_integration")
  case TestDocs        extends OverrideKey("test_docs")
  case TestFormat      extends OverrideKey("test_format")

// Signals provided to suite-group expressions — keep the boolean predicates in
// sync with the SHOULD_RUN expressions used in .github/workflows/ci.yml.
case class Signals(
    code: Boolean,
    docs: Boolean,
    ci: Boolean,
    formatConfig: Boolean,
    benchmark: Boolean,
    gifs: Boolean,
    millWrapper: Boolean,
    testAll: Boolean,
    testNative: Boolean,
    testIntegration: Boolean,
    testDocs: Boolean,
    testFormat: Boolean
):
  def withoutOverrides: Signals = copy(
    testAll = false,
    testNative = false,
    testIntegration = false,
    testDocs = false,
    testFormat = false
  )

  def withOverride(key: OverrideKey, enabled: Boolean): Signals = key match
    case OverrideKey.TestAll         => copy(testAll = enabled)
    case OverrideKey.TestNative      => copy(testNative = enabled)
    case OverrideKey.TestIntegration => copy(testIntegration = enabled)
    case OverrideKey.TestDocs        => copy(testDocs = enabled)
    case OverrideKey.TestFormat      => copy(testFormat = enabled)

  def shouldRun(group: SuiteGroup): Boolean = group match
    case SuiteGroup.UnitAndMill =>
      code || ci || millWrapper || testAll
    case SuiteGroup.JvmIntegration =>
      code || ci || testAll || testIntegration
    case SuiteGroup.NativeIntegration =>
      code || ci || testAll || testNative
    case SuiteGroup.DocsTests =>
      code || docs || ci || gifs || testAll || testDocs
    case SuiteGroup.Checks =>
      code || docs || ci || formatConfig || testAll
    case SuiteGroup.Format =>
      code || docs || ci || formatConfig || testAll || testFormat
    case SuiteGroup.ReferenceDoc =>
      code || docs || ci || testAll
    case SuiteGroup.BloopMemoryFootprint =>
      code || ci || benchmark || testAll
    case SuiteGroup.SbtExportVcRedist =>
      code || ci || testAll

object Signals:
  def apply(categories: Map[String, String], overrides: Map[String, String]): Signals =
    def boolAt(map: Map[String, String], key: String): Boolean =
      map.get(key).exists(_.equalsIgnoreCase("true"))
    Signals(
      code = boolAt(categories, "code"),
      docs = boolAt(categories, "docs"),
      ci = boolAt(categories, "ci"),
      formatConfig = boolAt(categories, "format_config"),
      benchmark = boolAt(categories, "benchmark"),
      gifs = boolAt(categories, "gifs"),
      millWrapper = boolAt(categories, "mill_wrapper"),
      testAll = boolAt(overrides, "test_all"),
      testNative = boolAt(overrides, "test_native"),
      testIntegration = boolAt(overrides, "test_integration"),
      testDocs = boolAt(overrides, "test_docs"),
      testFormat = boolAt(overrides, "test_format")
    )

/** Which individual override keywords (among those currently active) actually
  * add suite groups that wouldn't otherwise run. Used to list only the
  * overrides that matter.
  */
extension (signals: Signals)
  def isOverrideActive(key: OverrideKey): Boolean = key match
    case OverrideKey.TestAll         => signals.testAll
    case OverrideKey.TestNative      => signals.testNative
    case OverrideKey.TestIntegration => signals.testIntegration
    case OverrideKey.TestDocs        => signals.testDocs
    case OverrideKey.TestFormat      => signals.testFormat

def overrideContributions(signals: Signals): Seq[(OverrideKey, Seq[SuiteGroup])] =
  val baseline     = signals.withoutOverrides
  val baselineRuns = SuiteGroup.values.filter(baseline.shouldRun).toSet
  OverrideKey.values.toIndexedSeq.flatMap { key =>
    if !signals.isOverrideActive(key) then None
    else
      val probe = baseline.withOverride(key, enabled = true)
      val added = SuiteGroup.values.toIndexedSeq
        .filter(g => probe.shouldRun(g) && !baselineRuns.contains(g))
      if added.isEmpty then None else Some(key -> added)
  }

def renderComment(
    signals: Signals,
    classifyRunId: Option[String],
    classifyRunUrl: Option[String],
    ciRunId: Option[String],
    ciRunUrl: Option[String]
): String =
  val groups = SuiteGroup.values.toIndexedSeq
  val (runGroups, skipGroups) = groups.partition(signals.shouldRun)
  val contributions = overrideContributions(signals)

  val sb = StringBuilder()
  sb ++= "### CI change classification\n\n"
  sb ++= "**Change categories**\n\n"
  sb ++= "| Category | Changed |\n"
  sb ++= "| --- | --- |\n"
  sb ++= s"| code | ${signals.code} |\n"
  sb ++= s"| docs | ${signals.docs} |\n"
  sb ++= s"| ci | ${signals.ci} |\n"
  sb ++= s"| format_config | ${signals.formatConfig} |\n"
  sb ++= s"| benchmark | ${signals.benchmark} |\n"
  sb ++= s"| gifs | ${signals.gifs} |\n"
  sb ++= s"| mill_wrapper | ${signals.millWrapper} |\n"
  sb ++= "\n**Suite groups**\n\n"
  sb ++= (
    if runGroups.isEmpty then "- Will run: _(none)_\n"
    else s"- Will run: ${runGroups.map(_.label).mkString(", ")}\n"
  )
  sb ++= (
    if skipGroups.isEmpty then "- Will be skipped: _(none)_\n"
    else s"- Will be skipped: ${skipGroups.map(_.label).mkString(", ")}\n"
  )

  if contributions.nonEmpty then
    sb ++= "\n**Override keywords affecting the run set**\n\n"
    for (key, added) <- contributions do
      sb ++= s"- `[${key.keyword}]` forces on: ${added.map(_.label).mkString(", ")}\n"

  ciRunId match
    case Some(id) =>
      val url = ciRunUrl.getOrElse("")
      sb ++= s"\nFull CI run: [#$id]($url)\n"
    case None =>
      ciRunUrl.foreach { url => sb ++= s"\nFull CI run: $url\n" }

  classifyRunId.foreach { id =>
    val url = classifyRunUrl.getOrElse("")
    sb ++= s"\n_Classified in run [#$id]($url)._\n"
  }

  sb.result()

val categories = readKeyValueFile(envRequiredFile("CLASSIFY_OUTPUT_FILE"))
val overrides  = readKeyValueFile(envRequiredFile("OVERRIDE_OUTPUT_FILE"))
val signals    = Signals(categories, overrides)

val commentPath = toAbsolutePath(envOpt("COMMENT_OUTPUT_FILE").getOrElse("comment.md"))

val body = renderComment(
  signals,
  classifyRunId = envOpt("CLASSIFY_RUN_ID"),
  classifyRunUrl = envOpt("CLASSIFY_RUN_URL"),
  ciRunId = envOpt("CI_RUN_ID"),
  ciRunUrl = envOpt("CI_RUN_URL")
)

os.write.over(commentPath, body, createFolders = true)
println(s"Wrote comment to $commentPath")
