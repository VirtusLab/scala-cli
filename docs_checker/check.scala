// using scala 3.0.2
// using "org.scalameta::munit:0.7.29"
// using com.lihaoyi:ammonite-ops_2.13:2.4.0

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import collection.JavaConverters.*
import scala.sys.process.*
import scala.util.matching.Regex

import munit.Assertions.assert
import java.io.File
import ammonite.ops

val ScalaCodeBlock = """ *```scala name\:([\w\.]+)+""".r
val CodeBlockEnds  = """ *```""".r
val ScalaCliBlock  = """ *```scala-cli""".r
val CheckBlock     = """ *\<\!\-\- Expected(-regex):""".r
val CheckBlockEnd  = """ *\-\-\>""".r

enum Commands:
  def context: Context

  case Snippet(name: String, lines: Seq[String], context: Context)
  case Run(cmd: Seq[String], context: Context)
  case Check(patterns: Seq[String], regex: Boolean, context: Context)

case class Context(file: String, line: Int)

def msg(txt: String)(using c: Context): String = s"From ${c.file}:${c.line}: $txt"

def untilEndOfSnippet[T](
  lines: Seq[String],
  regex: Regex = CodeBlockEnds
)(using c: Context): (Seq[String], Seq[String], Context) =
  val codeLines = lines.takeWhile(l => !regex.matches(l))
  assert(codeLines.size > 0, msg("Block cannot be empty!"))
  assert(codeLines.size < lines.size, msg("Block should end!"))
  (codeLines, lines.drop(codeLines.size + 1), c.copy(line = c.line + codeLines.size + 2))

def parse(content: Seq[String], currentCommands: Seq[Commands], context: Context): Seq[Commands] =
  given Context = context
  content match
    case Nil => currentCommands
    case ScalaCodeBlock(name) :: tail =>
      val (codeLines, rest, newContext) = untilEndOfSnippet(tail)(using context)

      parse(rest, currentCommands :+ Commands.Snippet(name, codeLines, context), newContext)
    case ScalaCliBlock() :: tail =>
      val (codeLines, rest, newContext) = untilEndOfSnippet(tail)
      assert(codeLines.size != 0)
      val runCmd = Commands.Run(codeLines.head.split(" ").toList, newContext)
      parse(rest, currentCommands :+ runCmd, newContext)
    case CheckBlock(regexOpt) :: tail =>
      val isRegex                      = regexOpt == "-regex"
      val (patterns, rest, newContext) = untilEndOfSnippet(tail, CheckBlockEnd)
      parse(rest, currentCommands :+ Commands.Check(patterns, isRegex, context), newContext)
    case _ :: tail => parse(tail, currentCommands, context.copy(line = context.line + 1))

case class TestCase(path: Path, failure: Option[Throwable])

def checkPath(dest: Option[Path])(path: Path): Seq[TestCase] =
  try
    if !Files.isDirectory(path) then
      if path.getFileName.toString.endsWith(".md") then
        checkFile(path, dest)
        Seq(TestCase(path, None))
      else Nil
    else
      val toCheck =
        Files.list(path).iterator.asScala.filterNot(_.getFileName.toString.startsWith("."))
      toCheck.toList.flatMap(checkPath(dest))
  catch
    case e: Throwable =>
      e.printStackTrace()
      Seq(TestCase(path, Some(e)))

def ammPath(p: Path) = os.Path(p.toAbsolutePath)

val fakeLineMarker = "//fakeline"

def checkFile(file: Path, dest: Option[Path]) =
  val content  = Files.lines(file).iterator.asScala.toList
  val commands = parse(content, Vector(), Context(file.toString, 1))
  val destName = file.getFileName.toString.stripSuffix(".md")
  val out =
    dest match
      case None => Files.createTempDirectory(destName)
      case Some(dir) =>
        val out = dir.resolve(destName)
        ops.rm(ammPath(out))
        ops.mkdir(ammPath(out))
        out
  var lastOutput = ""
  val allSources = Set.newBuilder[Path]

  try
    println(s"Using $out as output to process $file")

    commands.foreach { cmd =>
      given Context = cmd.context
      cmd match
        case Commands.Run(cmd, _) =>
          println(s"### Running: ${cmd.mkString(" ")}")
          try lastOutput = Process(cmd, Some(out.toFile)).!!
          catch
            case e: Throwable =>
              throw new RuntimeException(msg(s"Error running ${cmd.mkString(" ")}"), e)
        case Commands.Snippet(name, code, c) =>
          println(s"### Writting $name with:\n${code.mkString("\n")}\n---")
          val prefix = (fakeLineMarker + "\n") * c.line
          val file   = out.resolve(name)
          allSources += file
          Files.write(file, code.mkString(prefix, "\n", "").getBytes)
        case Commands.Check(patterns, regex, line) =>
          assert(lastOutput != "")
          val lines = lastOutput.linesIterator.toList

          if regex then
            patterns.foreach { pattern =>
              val regex = pattern.r
              assert(
                lines.exists(regex.matches),
                msg(s"Regex: $pattern, does not matches any line in:\n$lastOutput")
              )
            }
          else
            patterns.foreach { pattern =>
              assert(
                lines.exists(_.contains(pattern)),
                msg(s"Pattern: $pattern does not exisits in  any line in:\n$lastOutput")
              )
            }
    }
  finally if dest.isEmpty then ops.rm(ammPath(out))

  // remove empty space at begining of all files
  if dest.nonEmpty then
    val header = s"File was generated from based on ${file}, do not edit manually!"
    allSources.result().foreach { s =>
      val content = ops.read.lines(ammPath(s)).dropWhile(_ == fakeLineMarker)
        .mkString(s"// $header\n\n", "\n", "")
      ops.write.over(ammPath(s), content)
    }
    val readmeLines = List("<!--", "  " + header, "-->", "") ++ content
    ops.write(ammPath(out.resolve("README.md")), readmeLines.mkString("\n"))

@main def check(args: String*) =
  def processFiles(dest: Option[Path], files: Seq[String]) =
    val testCases    = files.flatMap(a => checkPath(dest)(Paths.get(a)))
    val (failed, ok) = testCases.partition(_.failure.nonEmpty)
    println(s"Completed:\n\t${ok.map(_.path).mkString("\n\t")}")
    if failed.nonEmpty then
      println(s"Failed:\n\t${failed.map(_.path).mkString("\n\t")}")
      sys.exit(1)
    println("---")

  args match
    case Nil =>
      println("No inputs!")
    case "--dest" :: dest :: files => processFiles(Some(Paths.get(dest)), files)
    case files                     => processFiles(None, files)
