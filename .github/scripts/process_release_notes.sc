#!/usr/bin/env -S scala-cli shebang
//> using scala 3
//> using toolkit default
//> using options -Werror -Wunused:all

case class Replacement(find: String, replace: String)

// Hardcoded regex replacements for processing release notes
// These transform GitHub-idiomatic syntax to website-compatible Markdown
val replacements: Seq[Replacement] = Seq(
  // 1. Contributor link: Transform "by @user in" to "by [@user](https://github.com/user) in"
  // Excludes bots: dependabot, github-actions, scala-steward
  // Only matches if not already a link (negative lookahead to avoid matching already-formatted entries)
  // Must come BEFORE PR link pattern to avoid interference
  // The pattern stops at the first " in" and doesn't match if there's a formatted link before it
  Replacement(
    find =
      "by @(?!dependabot\\[bot\\]|github-actions\\[bot\\]|scala-steward)([^\\[\\]]+?) in(?!.*\\[@.*?\\]\\()",
    replace = "by [@$1](https://github.com/$1) in"
  ),
  // 2. New contributor link: Transform "@user made" to "[@user](https://github.com/user) made"
  // Excludes bots: dependabot, github-actions, scala-steward
  // Only matches if not already a link (negative lookbehind and lookahead to avoid matching already-formatted entries)
  Replacement(
    find =
      "(?<!\\[)@(?!dependabot\\[bot\\]|github-actions\\[bot\\]|scala-steward)(.*?) made(?!.*\\]\\()",
    replace = "[@$1](https://github.com/$1) made"
  ),
  // 3. PR link: Transform "in https://github.com/VirtusLab/scala-cli/pull/123" to "in [#123](https://github.com/VirtusLab/scala-cli/pull/123)"
  // Only matches if not already formatted as a markdown link
  Replacement(
    find = "in https://github\\.com/VirtusLab/scala-cli/pull/(.*?)$(?!.*\\]\\()",
    replace = "in [#$1](https://github.com/VirtusLab/scala-cli/pull/$1)"
  )
)

def applyReplacements(text: String, replacements: Seq[Replacement]): String = {
  // Process line by line to handle $ end-of-line anchors correctly
  val lines          = text.linesIterator.toSeq
  val processedLines = lines.map { line =>
    replacements.foldLeft(line) { (current, replacement) =>
      try
        val regex = replacement.find.r
        // Manually handle replacement to avoid issues with $ in replacement strings
        var result  = current
        val matches =
          regex.findAllMatchIn(current).toSeq.reverse // Process from end to avoid offset issues
        for matchData <- matches do
          // Build replacement string by substituting $1, $2, etc. with actual group values
          var replacementText = replacement.replace
          // Replace $1, $2, etc. with actual group values (in reverse order to avoid replacing $11 when we mean $1)
          for i <- matchData.groupCount to 1 by -1 do
            val groupValue = if matchData.group(i) != null then matchData.group(i) else ""
            replacementText = replacementText.replace(s"$$$i", groupValue)
          // Replace the matched portion
          result = result.substring(0, matchData.start) + replacementText +
            result.substring(matchData.end)
        result
      catch
        case e: Exception =>
          System.err.println(
            s"Warning: Failed to apply regex '${replacement.find}': ${e.getMessage}"
          )
          if System.getenv("DEBUG") == "true" then e.printStackTrace()
          current
    }
  }
  // Rejoin lines with newlines, preserving original line endings
  val lineEnding = if text.contains("\r\n") then "\r\n"
  else if text.contains("\n") then "\n" else System.lineSeparator()
  processedLines.mkString(lineEnding) +
    (if text.endsWith("\n") || text.endsWith("\r\n") then lineEnding else "")
}

def printUsageMessage(): Unit = {
  println("Usage: process_release_notes.sc <command> <file>")
  println("Commands:")
  println("  apply    - Apply regexes to the file (modifies in place)")
  println("  check    - Check if file needs regexes applied (exits with error if needed)")
  println("  verify   - Verify file has regexes applied (exits with error if not)")
}

if args.length < 2 then
  println(s"Error: too few arguments: ${args.length}")
  printUsageMessage()
  sys.exit(1)

val command  = args(0)
val filePath = os.Path(args(1), os.pwd)

if !os.exists(filePath) then
  println(s"Error: file does not exist: $filePath")
  sys.exit(1)

if System.getenv("DEBUG") == "true" then
  println(s"Loaded ${replacements.length} replacement patterns")
  replacements.zipWithIndex.foreach { case (r, i) =>
    println(s"  Pattern ${i + 1}: Find='${r.find}', Replace='${r.replace}'")
  }

val originalContent    = os.read(filePath)
val transformedContent = applyReplacements(originalContent, replacements)

command match
  case "apply" =>
    os.write.over(filePath, transformedContent)
    println(s"Applied regexes to: $filePath")

  case "check" =>
    if originalContent != transformedContent then
      println(s"Error: File $filePath needs regexes applied")
      println("Run: .github/scripts/process_release_notes.sc apply <file>")
      sys.exit(1)
    else
      println(s"File $filePath is already processed correctly")
      sys.exit(0)

  case "verify" =>
    // Check for patterns that should have been transformed
    // All patterns have negative lookaheads to avoid matching already-formatted entries
    val patternsToCheck     = replacements
    val needsTransformation =
      patternsToCheck.exists { replacement =>
        try
          val regex = replacement.find.r
          regex.findFirstIn(originalContent).isDefined
        catch
          case _: Exception => false
      }

    if needsTransformation then
      println(s"Error: File $filePath contains patterns that need transformation")
      println("The following patterns were found that should be transformed:")
      patternsToCheck.foreach { replacement =>
        try
          val regex = replacement.find.r
          if regex.findFirstIn(originalContent).isDefined then
            println(s"  - Pattern: ${replacement.find}")
        catch
          case _: Exception => ()
      }
      println("Run: .github/scripts/process_release_notes.sc apply <file>")
      sys.exit(1)
    else
      println(s"File $filePath is properly formatted")
      sys.exit(0)

  case _ =>
    println(s"Error: unknown command: $command")
    printUsageMessage()
    sys.exit(1)
