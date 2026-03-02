package sclicheck

class MarkdownLinkTests extends munit.FunSuite {

  private val docsRootPath: os.Path = os.Path(sys.env("MILL_WORKSPACE_ROOT")) / "website" / "docs"

  private val markdownLinkPattern = """\[([^\]]*)\]\(([^)]+)\)""".r

  private def stripFragment(url: String): String = url.split('#').head

  private def hasFileExtension(url: String): Boolean = {
    val lastSegment = stripFragment(url).split('/').last
    lastSegment.contains('.')
  }

  private def isRelativeDocLink(url: String): Boolean =
    url.startsWith(".") && !url.contains("://")

  private def hasDoubleSlash(url: String): Boolean =
    url.replace("://", "").contains("//")

  private def mdFileExistsFor(sourceFile: os.Path, relativeUrl: String): Boolean = {
    val dir        = sourceFile / os.up
    val targetPath = stripFragment(relativeUrl)
    try os.isFile(dir / os.RelPath(targetPath + ".md"))
    catch { case _: Exception => false }
  }

  case class LinkIssue(file: os.Path, line: Int, linkText: String, url: String, problem: String)

  private def findIssuesInFile(file: os.Path): Seq[LinkIssue] = {
    val lines  = os.read.lines(file)
    val issues = Seq.newBuilder[LinkIssue]
    for {
      (lineContent, lineIdx) <- lines.zipWithIndex
      m                      <- markdownLinkPattern.findAllMatchIn(lineContent)
    } {
      val linkText = m.group(1)
      val url      = m.group(2)
      if isRelativeDocLink(url) then {
        if hasDoubleSlash(url) then
          issues += LinkIssue(
            file,
            lineIdx + 1,
            linkText,
            url,
            "relative link contains double slash"
          )
        if !hasFileExtension(url) && mdFileExistsFor(file, url) then
          issues += LinkIssue(
            file,
            lineIdx + 1,
            linkText,
            url,
            "relative link to doc page without .md extension"
          )
      }
    }
    issues.result()
  }

  test("all relative doc links should use .md extension") {
    val allMdFiles = os.walk(docsRootPath)
      .filter(_.last.endsWith(".md"))
      .filter(os.isFile(_))
      .sorted

    val allIssues = allMdFiles.flatMap(findIssuesInFile)

    if allIssues.nonEmpty then {
      val report = allIssues
        .map { issue =>
          val relPath = issue.file.relativeTo(docsRootPath)
          s"  $relPath:${issue.line} — [${issue.linkText}](${issue.url})\n    ${issue.problem}"
        }
        .mkString("\n")
      fail(
        s"Found ${allIssues.size} relative doc link(s) with issues:\n$report\n\n" +
          "Relative links to other doc pages must include the .md extension, " +
          "otherwise they resolve incorrectly in production due to trailing slashes."
      )
    }
  }
}
