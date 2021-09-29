// using scala 3.0.2
// using "org.scalameta::munit:0.7.29"

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import collection.JavaConverters.*
import scala.sys.process.*
import scala.util.matching.Regex

import munit.Assertions.assert
import java.io.File


val ScalaCodeBlock = """ *```scala name\:([\w\.]+)+""".r
val CodeBlockEnds = """ *```""".r
val ScalaCliBlock = """ *```scala-cli""".r
val CheckBlock = """ *\<\!\-\- Expected(-regex):""".r
val CheckBlockEnd = """ *\-\-\>""".r

enum Commands:
    def context: Context

    case Snippet(name: String, lines: Seq[String], context: Context)
    case Run(cmd: Seq[String], context: Context)
    case Check(patterns: Seq[String], regex: Boolean, context: Context)

case class Context(file: String, line: Int)

def msg(txt: String)(using c: Context): String = s"From ${c.file}:${c.line}: $txt"

def untilEndOfSnippet[T](
    lines: Seq[String], 
    regex: Regex = CodeBlockEnds)(using c: Context): (Seq[String], Seq[String], Context) = 
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
            val isRegex = regexOpt == "-regex"
            val (patterns, rest, newContext) = untilEndOfSnippet(tail, CheckBlockEnd)
            parse(rest, currentCommands :+ Commands.Check(patterns, isRegex, context), newContext)
        case _ :: tail => parse(tail, currentCommands, context.copy(line = context.line + 1))

case class TestCase(path: Path, failure: Option[Throwable])

def checkPath(path: Path): Seq[TestCase] = 
    try 
        if !Files.isDirectory(path) then 
            if path.getFileName.toString.endsWith(".md") then
                checkFile(path)
                Seq(TestCase(path, None))
            else Nil
        else 
            val toCheck =  Files.list(path).iterator.asScala.filterNot(_.getFileName.toString.startsWith("."))
            toCheck.toList.flatMap(checkPath)
    catch
        case e: Throwable =>
            e.printStackTrace()
            Seq(TestCase(path, Some(e)))

def checkFile(file: Path) = 
    val content = Files.lines(file).iterator.asScala.toList
    val commands = parse(content, Vector(), Context(file.toString, 1))
    val out = Files.createTempDirectory("scala-cli-tests")
    println(s"Using $out as output to process $file")
    var lastOutput = ""
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
                val prefix = "\n" * c.line
                Files.write(out.resolve(name), code.mkString(prefix, "\n", "").getBytes)
            case Commands.Check(patterns, regex, line) =>
                assert(lastOutput != "")
                val lines = lastOutput.linesIterator.toList

                if regex then
                    patterns.foreach { pattern =>
                        val regex = pattern.r
                        assert(
                            lines.exists(regex.matches), 
                            msg(s"Regex: $pattern, does not matches any line in:\n$lastOutput"))
                    }
                else
                    patterns.foreach { pattern => assert(
                        lines.exists(_.contains(pattern)),
                        msg(s"Pattern: $pattern does not exisits in  any line in:\n$lastOutput")
                    )}

    }

@main def check(args: String*) =
    val testCases = args.flatMap(a => checkPath(Paths.get(a)))
    val (failed, ok) = testCases.partition(_.failure.nonEmpty)
    println(s"Completed:\n\t${ok.map(_.path).mkString("\n\t")}")
    if failed.nonEmpty then
        println(s"Failed:\n\t${failed.map(_.path).mkString("\n\t")}")
        sys.exit(1)
    println("---")
    
