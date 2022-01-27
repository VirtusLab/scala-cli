package scala.cli.doc

import caseapp._
import caseapp.core.Arg
import caseapp.core.util.Formatter
import munit.internal.difflib.Diff

import java.nio.charset.StandardCharsets
import java.util.{Arrays, Locale}

import scala.build.preprocessing.ScalaPreprocessor
import scala.build.preprocessing.directives.{
  DirectiveHandler,
  RequireDirectiveHandler,
  UsingDirectiveHandler
}
import scala.cli.ScalaCliCommands

object GenerateReferenceDoc extends CaseApp[Options] {

  private def cleanUpOrigin(origin: String): String = {
    val origin0      = origin.takeWhile(_ != '[').stripSuffix("Options")
    val actualOrigin = if (origin0 == "WithFullHelp" || origin0 == "WithHelp") "Help" else origin0
    if (actualOrigin == "Shared") actualOrigin
    else actualOrigin.stripPrefix("Shared")
  }

  private def formatOrigin(origin: String, keepCapitalization: Boolean = true): String = {
    val l = origin.head :: origin.toList.tail.flatMap { c =>
      if (c.isUpper) " " :: c.toLower :: Nil
      else c :: Nil
    }
    val value = l.mkString
      .replace("Scala native", "Scala Native")
      .replace("Scala js", "Scala.JS")
      .split("\\s+")
      .map(w => if (w == "ide") "IDE" else w)
      .mkString(" ")
    val valueNeedsLowerCasing = keepCapitalization ||
      (value.startsWith("Scala") && !value.startsWith("Scalac")) ||
      !value.head.isUpper
    if (valueNeedsLowerCasing) value
    else value.head.toLower +: value.tail
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
    }
    else
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

  private def cliOptionsContent(
    commands: Seq[Command[_]],
    allArgs: Seq[Arg],
    nameFormatter: Formatter[Name]
  ): String = {

    val argsByOrigin = allArgs.groupBy(arg => cleanUpOrigin(arg.origin.getOrElse("")))

    val commandOrigins = for {
      command <- commands
      origin  <- command.finalHelp.args.map(_.origin.getOrElse("")).map(cleanUpOrigin)
    } yield origin -> command

    val commandOriginsMap = commandOrigins.groupBy(_._1)
      .map {
        case (k, v) =>
          (k, v.map(_._2).distinct.sortBy(_.name))
      }

    val b = new StringBuilder

    b.append(
      """---
        |title: Command-line options
        |sidebar_position: 1
        |---
        |
        |This is a summary of options that are available for each subcommand of the `scala-cli` command.
        |
        |""".stripMargin
    )

    for ((origin, originArgs) <- argsByOrigin.toVector.sortBy(_._1)) {
      val originArgs0     = originArgs.map(_.withOrigin(None)).distinct
      val originCommands  = commandOriginsMap.getOrElse(origin, Nil)
      val formattedOrigin = formatOrigin(origin)
      val formattedCommands = originCommands.map { c =>
        // https://scala-cli.virtuslab.org/docs/reference/commands#install-completions
        val names = c.names.map(_.mkString(" "))
        val text  = names.map("`" + _ + "`").mkString(" / ")
        s"[$text](./commands.md#${names.head.replace(" ", "-")})"
      }
      val availableIn = "Available in commands:\n" + formattedCommands.map("- " + _ + "\n").mkString
      b.append(
        s"""## $formattedOrigin options
           |
           |$availableIn
           |
           |<!-- Automatically generated, DO NOT EDIT MANUALLY -->
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
        for (desc <- arg.helpMessage.map(_.message))
          b.append(
            s"""$desc
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

    val (hiddenCommands, mainCommands) = commands.partition(_.hidden)

    def addCommand(c: Command[_], additionalIndentation: Int = 0): Unit = {

      val origins = c.parser0.args.flatMap(_.origin.toSeq).map(cleanUpOrigin).distinct.sorted

      val headerPrefix = "#" * additionalIndentation
      val names        = c.names.map(_.mkString(" "))
      b.append(
        s"""$headerPrefix## `${names.head}`
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

      for (desc <- c.messages.helpMessage.map(_.message))
        b.append(
          s"""$desc
             |
             |""".stripMargin
        )

      if (origins.nonEmpty) {
        val links = origins.map { origin =>
          val cleanedUp = formatOrigin(origin, keepCapitalization = false)
          val linkPart = cleanedUp
            .split("\\s+")
            .map(_.toLowerCase(Locale.ROOT).filter(_ != '.'))
            .mkString("-")
          s"[$cleanedUp](./cli-options.md#$linkPart-options)"
        }
        b.append(
          """Accepts options:
            |""".stripMargin
        )
        for (link <- links)
          b.append(s"- $link\n")
        b.append("\n")
      }
    }

    for (c <- mainCommands.iterator)
      addCommand(c)
    b.append(
      """## Hidden commands
        |
        |""".stripMargin
    )
    for (c <- hiddenCommands.iterator)
      addCommand(c, additionalIndentation = 1)

    b.toString
  }

  private def usingContent(
    usingHandlers: Seq[UsingDirectiveHandler],
    requireHandlers: Seq[RequireDirectiveHandler]
  ): String = {
    val b = new StringBuilder

    b.append(
      """---
        |title: Directives
        |sidebar_position: 2
        |---
        |
        |## using directives
        |
        |""".stripMargin
    )

    def addHandlers(handlers: Seq[DirectiveHandler[_]]): Unit =
      for (handler <- handlers.sortBy(_.name)) {
        b.append(
          s"""### ${handler.name}
             |
             |${handler.descriptionMd}
             |
             |${handler.usageMd}
             |
             |""".stripMargin
        )
        val examples = handler.examples
        if (examples.nonEmpty) {
          b.append(
            """#### Examples
              |""".stripMargin
          )
          for (ex <- examples)
            b.append(
              s"""`$ex`
                 |
                 |""".stripMargin
            )
        }
      }

    addHandlers(usingHandlers)

    b.append(
      """
        |## target directives
        |
        |""".stripMargin
    )

    addHandlers(requireHandlers)

    b.toString
  }

  def run(options: Options, args: RemainingArgs): Unit = {

    val scalaCli      = new ScalaCliCommands("scala-cli", isSipScala = false)
    val commands      = scalaCli.commands
    val allArgs       = commands.flatMap(_.finalHelp.args)
    val nameFormatter = scalaCli.actualDefaultCommand.nameFormatter

    val cliOptionsContent0 = cliOptionsContent(commands, allArgs, nameFormatter)
    val commandsContent0   = commandsContent(commands)
    val usingContent0 = usingContent(
      ScalaPreprocessor.usingDirectiveHandlers,
      ScalaPreprocessor.requireDirectiveHandlers
    )

    if (options.check) {
      val content = Seq(
        (os.rel / "cli-options.md") -> cliOptionsContent0,
        (os.rel / "commands.md")    -> commandsContent0,
        (os.rel / "directives.md")  -> usingContent0
      )
      var anyDiff = false
      for ((dest, content0) <- content) {
        val dest0   = options.outputPath / dest
        val diffOpt = maybeDiff(options.outputPath / dest, content0)
        diffOpt match {
          case Some(diff) =>
            anyDiff = true
            System.err.println(Console.RED + prettyPath(dest0) + Console.RESET + " differs:")
            System.err.println(diff.unifiedDiff)
          case None =>
            System.err.println(
              Console.GREEN + prettyPath(dest0) + Console.RESET + " is up-to-date."
            )
        }
      }
      if (anyDiff)
        sys.exit(1)
    }
    else {
      maybeWrite(options.outputPath / "cli-options.md", cliOptionsContent0)
      maybeWrite(options.outputPath / "commands.md", commandsContent0)
      maybeWrite(options.outputPath / "directives.md", usingContent0)
    }
  }
}
