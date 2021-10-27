// using scala 3.0.2
// using lib com.lihaoyi::os-lib:0.7.8
// using lib "com.lihaoyi::fansi:0.2.14"

import scala.util.matching.Regex
import scala.io.StdIn.readLine
import fansi.Color.{Red, Blue, Green}

val SnippetBlock  = """ *```[^ ]+ title=([\w\.\-\/_]+) *""".r
val CodeBlockEnds = """ *``` *""".r
val BashCommand   = """ *```bash *(fail)? *""".r
val CheckBlock    = """ *\<\!\-\- Expected(-regex)?: *""".r
val CheckBlockEnd = """ *\-\-\> *""".r
val Clear         = """ *<!--+ *clear *-+-> *""".r

case class Options(
  files: Seq[String] = Nil,
  dest: Option[os.Path] = None,
  stopAtFailure: Boolean = false,
  step: Boolean = false
)

enum Commands:
  def context: Context
  def name = toString.takeWhile(_ != '(')
  def log = this match {
    case _: Clear => ""
    case Check(patterns, regex, _) =>
      val kind = if regex then "regexes" else "patterns"
      s"last output matches $kind: ${patterns.map(p => s"'$p'").mkString(", ")}"
    case Run(cmd, shouldFail, _) =>
      val prefix = if shouldFail then "[failure expected] " else ""
      cmd.mkString(prefix, " ", "")
    case Snippet(name, _, _) =>
      name
  }

  case Snippet(fileName: String, lines: Seq[String], context: Context)
  case Run(scriptLines: Seq[String], shouldFail: Boolean, context: Context)
  case Check(patterns: Seq[String], regex: Boolean, context: Context)
  case Clear(context: Context)

case class Context(file: os.RelPath, line: Int):
  def proceed(linesToSkip: Int = 1) = copy(line = line + linesToSkip)
  override def toString             = s"$file:$line"

case class FailedCheck(line: Int, file: os.RelPath, txt: String)
    extends RuntimeException(s"[$file:$line] $txt")

def check(cond: Boolean, msg: => String)(using c: Context) =
  if !cond then throw FailedCheck(c.line, c.file, msg)

@annotation.tailrec
def parse(content: Seq[String], currentCommands: Seq[Commands], context: Context): Seq[Commands] =
  given Context = context

  inline def parseMultiline(
    lines: Seq[String],
    newCommand: Seq[String] => Commands,
    endMarker: Regex = CodeBlockEnds
  ) =
    val codeLines = lines.takeWhile(l => !endMarker.matches(l))
    check(codeLines.size > 0, "Block cannot be empty!")
    check(codeLines.size < lines.size, "Block should end!")
    parse(
      content = lines.drop(codeLines.size + 1),
      currentCommands = currentCommands :+ newCommand(codeLines),
      context = context.proceed(codeLines.size + 2)
    )

  content match
    case Nil => currentCommands

    case SnippetBlock(name) :: tail =>
      parseMultiline(tail, Commands.Snippet(name, _, context))

    case BashCommand(failGroup) :: tail =>
      parseMultiline(tail, Commands.Run(_, failGroup != null, context))

    case CheckBlock(regexOpt) :: tail =>
      val isRegex = regexOpt == "-regex"
      parseMultiline(tail, Commands.Check(_, isRegex, context), CheckBlockEnd)

    case Clear() :: rest =>
      parse(rest, currentCommands :+ Commands.Clear(context), context.proceed())

    case _ :: tail =>
      parse(tail, currentCommands, context.proceed())

case class TestCase(path: os.RelPath, failure: Option[String])

def checkPath(options: Options)(path: os.Path): Seq[TestCase] =
  try
    if !os.isDir(path) then
      if path.last.endsWith(".md") then
        checkFile(path, options)
        println(Green(s"[${path.relativeTo(os.pwd)}] Completed."))
        Seq(TestCase(path.relativeTo(os.pwd), None))
      else Nil
    else
      val toCheck =
        os.list(path).filterNot(_.last.startsWith("."))
      toCheck.toList.flatMap(checkPath(options))
  catch
    case e @ FailedCheck(line, file, text) =>
      println(Red(e.getMessage))
      Seq(TestCase(path.relativeTo(os.pwd), Some(e.getMessage)))
    case e: Throwable =>
      val short = s"Unexpected exception ${e.getClass.getName}"
      println(Red(short))
      e.printStackTrace()
      Seq(TestCase(path.relativeTo(os.pwd), Some(s"$short: ${e.getMessage}")))

val fakeLineMarker = "//fakeline"

def shoulAlignContent(file: String | os.Path) =
  file.toString.endsWith(".scala") || file.toString.endsWith(".java")

def mkBashScript(content: Seq[String]) =
  s"""#!/usr/bin/env bash
     |
     |set -e
     |
     |${content.mkString("\n")}
     |""".stripMargin

def checkFile(file: os.Path, options: Options): Unit =
  val content  = os.read.lines(file).toList
  val commands = parse(content, Vector(), Context(file.relativeTo(os.pwd), 1))
  val destName = file.last.stripSuffix(".md")
  val out      = os.temp.dir(prefix = destName)

  var lastOutput: String = null
  val allSources         = Set.newBuilder[os.Path]

  def runCommand(cmd: Commands, log: String => Unit) =
    given Context = cmd.context

    cmd match
      case Commands.Run(cmds, shouldFail, _) =>
        val script = out / ".scala" / "run.sh"
        os.write.over(script, mkBashScript(cmds), createFolders = true)
        os.perms.set(script, "rwxr-xr-x")
        val res = os.proc(script).call(cwd = out, mergeErrIntoOut = true, check = false)

        log(res.out.text())

        if shouldFail then
          check(res.exitCode != 0, s"Commands should fail.")
        else
          check(res.exitCode == 0, s"Commands failed.")

        lastOutput = res.out.text()

      case Commands.Snippet(name, code, c) =>
        val (prefixLines, codeLines) =
          code match
            case shbang :: tail if shbang.startsWith("#!") =>
              List(shbang + "\n") -> tail
            case other =>
              Nil -> other

        val file = out / os.RelPath(name)
        codeLines.foreach(log)

        val prefix =
          if !shoulAlignContent(file) then prefixLines.mkString("")
          else prefixLines.mkString("", "", s"$fakeLineMarker\n" * c.line)

        os.write.over(file, code.mkString(prefix, "\n", ""), createFolders = true)

      case Commands.Check(patterns, regex, line) =>
        check(lastOutput != null, "No output stored from previous commands")
        val lines = lastOutput.linesIterator.toList

        if regex then
          patterns.foreach { pattern =>
            val regex = pattern.r
            check(
              lines.exists(regex.matches),
              s"Regex: $pattern, does not matches any line in:\n$lastOutput"
            )
          }
        else
          patterns.foreach { pattern =>
            check(
              lines.exists(_.contains(pattern)),
              s"Pattern: $pattern does not exists in  any line in:\n$lastOutput"
            )
          }
      case Commands.Clear(_) =>
        os.list(out).foreach(os.remove.all)

  try
    println(Blue(s"\n[${file.relativeTo(os.pwd)}]  Running checks in $out"))

    commands.foreach { cmd =>
      val logs = List.newBuilder[String]

      def printResult(success: Boolean) =
        val commandName = "[" + cmd.name + "]"
        val cmdLog =
          if success then Green(commandName)
          else Red(commandName)
        println(s"$cmdLog ${cmd.log}")
        println(logs.result.mkString("\n"))

      def pause(): Unit =
        println(s"After [${cmd.context}] using $out. Press ENTER key to continue...")
        readLine()

      try
        runCommand(cmd, logs.addOne)
        printResult(success = true)
        if options.step then pause()
      catch
        case e: Throwable =>
          printResult(success = false)
          if options.stopAtFailure then pause()
          throw e
    }
  finally if options.dest.isEmpty then os.remove.all(out)

  // remove empty space at beginning of all files
  if options.dest.nonEmpty then
    val exampleDir = options.dest.get / destName
    os.remove.all(exampleDir)
    os.makeDir(exampleDir)

    val relFile = file.relativeTo(os.pwd)
    val header  = s"File was generated from based on $relFile, do not edit manually!"
    allSources.result().foreach { s =>
      val content = os.read.lines(s)
      val cleared =
        if !shoulAlignContent(s) || content.size < 2 then content
        else
          val head = content.take(1).dropWhile(_ == fakeLineMarker)
          val tail = content.drop(1).dropWhile(_ == fakeLineMarker)
          head ++ tail

      os.write.over(s, content.mkString(s"// $header\n\n", "\n", ""))
    }
    val withoutFrontMatter =
      if !content.head.startsWith("---") then content
      else
        content.tail.dropWhile(l => !l.startsWith("---")).tail

    val readmeLines = List("<!--", "  " + header, "-->", "") ++ withoutFrontMatter
    os.write(exampleDir / "README.md", readmeLines.mkString("\n"))

    os.list(out).filter(_.toString.endsWith(".scala")).foreach(p => os.copy.into(p, exampleDir))

@main def check(args: String*) =
  def processFiles(dest: Options) =
    val paths = dest.files.map { str =>
      val path = os.pwd / os.RelPath(str)
      assert(os.exists(path), s"Provided path $str does not exists in ${os.pwd}")
      path
    }
    val testCases    = paths.flatMap(checkPath(dest))
    val (failed, ok) = testCases.partition(_.failure.nonEmpty)
    if testCases.size > 1 then
      if ok.nonEmpty then
        println(Green("Completed:"))
        val lines = ok.map(tc => s"\t${Green(tc.path.toString)}")
        println(lines.mkString("\n"))
        println("")
      if failed.nonEmpty then
        println(Red("Failed:"))
        val lines = failed.map(tc => s"\t${Red(tc.path.toString)}: ${tc.failure.get}")
        println(lines.mkString("\n"))
        println("")
        sys.exit(1)

  def parseArgs(args: Seq[String], options: Options): Options = args match
    case Nil => options
    case "--step" :: rest =>
      parseArgs(rest, options.copy(step = true))
    case "--stopAtFailure" :: rest =>
      parseArgs(rest, options.copy(stopAtFailure = true))
    case "--dest" :: dest :: rest =>
      parseArgs(rest, options.copy(dest = Some(os.pwd / dest)))
    case "--dest" :: Nil =>
      println(Red("Exptected a destanation after `--dest` parameter"))
      sys.exit(1)
    case path :: rest => parseArgs(rest, options.copy(files = options.files :+ path))

  processFiles(parseArgs(args, Options()))
