package scala.cli.doc

import caseapp._
import caseapp.core.Arg
import caseapp.core.parser.ParserWithNameFormatter
import munit.internal.difflib.Diff

import java.nio.charset.StandardCharsets
import java.util.Arrays

import scala.cli.ScalaCli

object GenerateReferenceDoc extends CaseApp[Options] {

  private def cleanUpOrigin(origin: String): String = {
    val origin0 = origin.takeWhile(_ != '[').stripSuffix("Options")
    val actualOrigin = if (origin0 == "WithFullHelp") "WithHelp" else origin0
    if (actualOrigin == "Shared") actualOrigin
    else actualOrigin.stripPrefix("Shared")
  }

  private def formatOrigin(origin: String): String = {
    val l = origin.head :: origin.toList.tail.flatMap { c =>
      if (c.isUpper) " " :: c.toLower :: Nil
      else c :: Nil
    }
    l.mkString
      .replace("Scala native", "Scala Native")
      .replace("Scala js", "Scala.JS")
  }

  private def prettyPath(path: os.Path): String =
    if (path.startsWith(os.pwd)) path.relativeTo(os.pwd).toString
    else path.toString

  private def maybeWrite(dest: os.Path, content: String): Unit = {
    val content0 = content.getBytes(StandardCharsets.UTF_8)
    val needsUpdate = !os.exists(dest) || {
      val currentContent = os.read.bytes(dest)
      !Arrays.equals(content0, currentContent)
    }
    if (needsUpdate) {
      os.write.over(dest, content0, createFolders = true)
      System.err.println(s"Wrote ${prettyPath(dest)}")
    } else
      System.err.println(s"${prettyPath(dest)} doesn't need updating")
  }

  private def maybeDiff(dest: os.Path, content: String): Option[Diff] = {
    val currentContentOpt =
      if (os.exists(dest)) Some(new String(os.read.bytes(dest), StandardCharsets.UTF_8))
      else None
    currentContentOpt.filter(_ != content).map { currentContent =>
      new Diff(currentContent, content)
    }
  }

  private def cliOptionsContent(allArgs: Seq[Arg]): String = {

    val argsByOrigin = allArgs.groupBy(arg => cleanUpOrigin(arg.origin.getOrElse("")))

    val commandOrigins = for {
      command <- ScalaCli.commands
      origin <- command.finalHelp.args.map(_.origin.getOrElse("")).map(cleanUpOrigin)
    } yield origin -> command

    val commandOriginsMap = commandOrigins.groupBy(_._1).map { case (k, v) => (k, v.map(_._2).distinct.sortBy(_.name)) }

    val nameFormatter = ScalaCli.actualDefaultCommand.nameFormatter
    val b = new StringBuilder

    b.append(
      """---
        |title: Command-line options
        |sidebar_position: 1
        |---
        |
        |""".stripMargin
    )

    for ((origin, originArgs) <- argsByOrigin.toVector.sortBy(_._1)) {
      val originArgs0 = originArgs.map(_.withOrigin(None)).distinct
      val originCommands = commandOriginsMap.getOrElse(origin, Nil)
      val formattedOrigin = formatOrigin(origin)
      val formattedCommands = originCommands.map { c =>
        // http://localhost:3000/docs/reference/commands#install-completions
        val names = c.names.map(_.mkString(" "))
        val text = names.map("`" + _ + "`").mkString(" / ")
        s"[$text](./commands#${names.head.replace(" ", "-")})"
      }
      val availableIn = "Available in commands:\n" + formattedCommands.map("- " + _ + "\n").mkString
      b.append(
       s"""## $formattedOrigin options
          |
          |$availableIn
          |
          |""".stripMargin
      )

      for (arg <- originArgs0.distinct) {
        import caseapp.core.util.NameOps._
        arg.name.option(nameFormatter)
        val aliases = arg.extraNames.map(_.option(nameFormatter))
        b.append(
          s"""#### `${arg.name.option(nameFormatter)}`
             |
             |""".stripMargin
        )
        if (aliases.nonEmpty)
          b.append(
            s"""Aliases: ${aliases.map("`" + _ + "`").mkString(", ")}
               |
               |""".stripMargin
          )
      }
    }

    b.toString
  }

  private def commandsContent(commands: Seq[Command[_]]): String = {
    val b = new StringBuilder

    b.append(
      """---
        |title: Commands
        |sidebar_position: 3
        |---
        |
        |""".stripMargin
    )

    for (c <- commands) {
      val names = c.names.map(_.mkString(" "))
      b.append(
        s"""## `${names.head}`
           |
           |""".stripMargin
      )
      if (names.lengthCompare(1) > 0) {
        b.append("Aliases:\n")
        for (n <- names.tail) {
          b.append(s"- `$n`")
          b.append("\n")
        }
        b.append("\n")
      }
    }

    b.toString
  }

  def run(options: Options, args: RemainingArgs): Unit = {

    val commands = ScalaCli.commands
    val allArgs = commands.flatMap(_.finalHelp.args)

    val argsByOrigin = allArgs.groupBy(arg => cleanUpOrigin(arg.origin.getOrElse("")))

    val commandOrigins = for {
      command <- ScalaCli.commands
      origin <- command.finalHelp.args.map(_.origin.getOrElse("")).map(cleanUpOrigin)
    } yield origin -> command

    val commandOriginsMap = commandOrigins.groupBy(_._1).map { case (k, v) => (k, v.map(_._2).distinct.sortBy(_.name)) }

    val cliOptionsContent0 = cliOptionsContent(allArgs)
    val commandsContent0 = commandsContent(commands)

    if (options.check) {
      val content = Seq(
        (os.rel / "cli-options.md") -> cliOptionsContent0,
        (os.rel / "commands.md") -> commandsContent0
      )
      var anyDiff = false
      for ((dest, content0) <- content) {
        val dest0 = options.outputPath / dest
        val diffOpt = maybeDiff(options.outputPath / dest, content0)
        diffOpt match {
          case Some(diff) =>
            anyDiff = true
            System.err.println(Console.RED + prettyPath(dest0) + Console.RESET + " differs:")
            System.err.println(diff.unifiedDiff)
          case None =>
            System.err.println(Console.GREEN + prettyPath(dest0) + Console.RESET + " is up-to-date.")
        }
      }
      if (anyDiff)
        sys.exit(1)
    } else {
      maybeWrite(options.outputPath / "cli-options.md", cliOptionsContent0)
      maybeWrite(options.outputPath / "commands.md", commandsContent0)
    }
  }
}
