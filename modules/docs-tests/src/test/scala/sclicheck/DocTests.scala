package sclicheck

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration
import scala.util.matching.Regex

class DocTests extends munit.FunSuite {
  override def munitTimeout = new FiniteDuration(600, TimeUnit.SECONDS)
  case class DocTestEntry(name: String, path: os.Path, depth: Int = Int.MaxValue)

  val docsRootPath: os.Path      = os.Path(sys.env("MILL_WORKSPACE_ROOT")) / "website" / "docs"
  val entries: Seq[DocTestEntry] = Seq(
    DocTestEntry("root", docsRootPath, depth = 1),
    DocTestEntry("cookbook", docsRootPath / "cookbooks"),
    DocTestEntry("command", docsRootPath / "commands"),
    DocTestEntry("guide", docsRootPath / "guides"),
    DocTestEntry("reference", docsRootPath / "reference")
  )

  val options: Options = Options(scalaCliCommand = Seq(TestUtil.scalaCliPath.toString))

  private val ReleaseNotesMd = os.rel / "release_notes.md"

  /** `## [v1.12.0](https://…)` style headings that delimit per-version release note sections. */
  private val ReleaseVersionHeading: Regex = """^##\s+\[(v[\w.\-]+)\]""".r

  private def isReleaseVersionHeadingLine(line: String): Boolean =
    ReleaseVersionHeading.findFirstMatchIn(line).nonEmpty

  private def lineContainsAnyChecks(l: String): Boolean =
    l.startsWith("```md") || l.startsWith("```bash") ||
    l.startsWith("```scala compile") || l.startsWith("```scala fail") ||
    l.startsWith("````markdown compile") || l.startsWith("````markdown fail") ||
    l.startsWith("```java compile") || l.startsWith("````java fail")
  private def fileContainsAnyChecks(f: os.Path): Boolean =
    os.read.lines(f).exists(lineContainsAnyChecks)

  /** One sclicheck run per `## [v…]` section so each gets its own timeout and workspace (Option C).
    */
  private def releaseNotesSections(file: os.Path): Seq[(String, IndexedSeq[String])] =
    val lines  = os.read.lines(file).toIndexedSeq
    val starts = lines.zipWithIndex.collect {
      case (line, i) if isReleaseVersionHeadingLine(line) => i
    }
    if starts.isEmpty && fileContainsAnyChecks(file) then Seq(("release_notes", lines))
    else if starts.isEmpty then Nil
    else
      starts.zipWithIndex.map { case (startIdx, chunkIdx) =>
        val endIdx =
          if chunkIdx + 1 < starts.size then starts(chunkIdx + 1)
          else lines.size
        val slice =
          if chunkIdx == 0 then lines.slice(0, endIdx)
          else lines.slice(startIdx, endIdx)
        val ver = ReleaseVersionHeading.findFirstMatchIn(lines(startIdx)).get.group(1)
        (ver, slice)
      }.filter { case (_, slice) => slice.exists(lineContainsAnyChecks) }

  for {
    DocTestEntry(tpe, dir, depth) <- entries
    inputs = os.walk(dir, maxDepth = depth)
      .filter(_.last.endsWith(".md"))
      .filter(os.isFile(_))
      .filter(fileContainsAnyChecks)
      .map(_.relativeTo(dir))
      .sortBy(_.toString)
    md <- inputs
    if !(tpe == "root" && md == ReleaseNotesMd)
  }
    test(s"$tpe ${md.toString.stripSuffix(".md")}") {
      TestUtil.retryOnCi()(checkFile(dir / md, options))
    }

  private val releaseNotesFile = docsRootPath / "release_notes.md"
  if os.isFile(releaseNotesFile) && fileContainsAnyChecks(releaseNotesFile) then
    for (ver, slice) <- releaseNotesSections(releaseNotesFile) do
      val safeStem = ver.replaceAll("[^a-zA-Z0-9._\\-]", "_")
      test(s"root release_notes $ver") {
        TestUtil.retryOnCi() {
          TestUtil.withTmpDir("sclicheck-release-notes") { tmp =>
            val chunkFile = tmp / s"release_notes-$safeStem.md"
            os.write.over(chunkFile, slice.mkString("", "\n", "\n"))
            checkFile(chunkFile, options)
          }
        }
      }

}
