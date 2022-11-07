package scala.cli.doc

import caseapp.*
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
import scala.cli.commands.{RestrictedCommandsParser, ScalaCommand}
import scala.cli.{CurrentParams, ScalaCli, ScalaCliCommands}

object GenerateReferenceDoc extends CaseApp[InternalDocOptions] {

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
      .replace("Scala js", "Scala.js")
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

  private def actualHelp(command: Command[_]): Help[_] =
    command match {
      case ext: scala.cli.commands.pgp.ExternalCommand =>
        ext.actualHelp
      case _ =>
        command.finalHelp
    }

  private def cliOptionsContent(
    commands: Seq[Command[_]],
    allArgs: Seq[Arg],
    nameFormatter: Formatter[Name],
    onlyRestricted: Boolean = false
  ): String = {
    val argsToShow = if (!onlyRestricted) allArgs
    else
      allArgs.filterNot(RestrictedCommandsParser.isExperimentalOrRestricted)

    val argsByOrigin = argsToShow.groupBy(arg => cleanUpOrigin(arg.origin.getOrElse("")))

    val commandOrigins = for {
      command <- commands
      origin  <- actualHelp(command).args.map(_.origin.getOrElse("")).map(cleanUpOrigin)
    } yield origin -> command

    val commandOriginsMap = commandOrigins.groupBy(_._1)
      .map {
        case (k, v) =>
          (k, v.map(_._2).distinct.sortBy(_.name))
      }

    val mainOptionsContent   = new StringBuilder
    val hiddenOptionsContent = new StringBuilder

    mainOptionsContent.append(
      s"""---
         |title: Command-line options
         |sidebar_position: 1
         |---
         |
         |${
          if (onlyRestricted)
            "**This document describes as scala-cli behaves if run as `scala` command. See more information in [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**"
          else ""
        }
         |
         |This is a summary of options that are available for each subcommand of the `${ScalaCli.baseRunnerName}` command.
         |
         |""".stripMargin
    )

    for ((origin, originArgs) <- argsByOrigin.toVector.sortBy(_._1)) {
      val distinctArgs          = originArgs.map(_.withOrigin(None)).distinct
      val originCommands        = commandOriginsMap.getOrElse(origin, Nil)
      val onlyForHiddenCommands = originCommands.nonEmpty && originCommands.forall(_.hidden)
      val allArgsHidden         = distinctArgs.forall(_.noHelp)
      val isInternal            = onlyForHiddenCommands || allArgsHidden
      val b                     = if (isInternal) hiddenOptionsContent else mainOptionsContent
      if (originCommands.nonEmpty) {
        val formattedOrigin = formatOrigin(origin)
        val formattedCommands = originCommands.map { c =>
          // https://scala-cli.virtuslab.org/docs/reference/commands#install-completions
          val names = c.names.map(_.mkString(" "))
          val text  = names.map("`" + _ + "`").mkString(" , ")
          s"[$text](./commands.md#${names.head.replace(" ", "-")})"
        }
        val availableIn = "Available in commands:\n\n" + formattedCommands.mkString(", ")
        val header      = if (isInternal) "###" else "##"
        b.append(
          s"""$header $formattedOrigin options
             |
             |$availableIn
             |
             |<!-- Automatically generated, DO NOT EDIT MANUALLY -->
             |
             |""".stripMargin
        )

        for (arg <- distinctArgs) {
          import caseapp.core.util.NameOps._
          arg.name.option(nameFormatter)
          val names = (arg.name +: arg.extraNames).map(_.option(nameFormatter))
          b.append(s"### `${names.head}`\n\n")
          if (names.tail.nonEmpty)
            b.append(names.tail.map(n => s"`$n`").mkString("Aliases: ", ", ", "\n\n"))

          if (isInternal || arg.noHelp) b.append("[Internal]\n")

          for (desc <- arg.helpMessage.map(_.message))
            b.append(
              s"""$desc
                 |
                 |""".stripMargin
            )
        }
      }
    }

    mainOptionsContent.append("## Internal options \n")
    mainOptionsContent.append(hiddenOptionsContent.toString)
    mainOptionsContent.toString
  }

  private def commandsContent(commands: Seq[Command[_]], onlyRestricted: Boolean): String = {

    val b = new StringBuilder

    b.append(
      s"""---
         |title: Commands
         |sidebar_position: 3
         |---
         |
         |${
          if (onlyRestricted)
            "**This document describes as scala-cli behaves if run as `scala` command. See more information in [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**"
          else ""
        }
         |
         |
         |""".stripMargin
    )

    val (hiddenCommands, mainCommands) = commands.partition(_.hidden)

    def addCommand(c: Command[_], additionalIndentation: Int = 0): Unit = {

      val origins = c.parser0.args.flatMap(_.origin.toSeq).map(cleanUpOrigin).distinct.sorted

      val headerPrefix = "#" * additionalIndentation
      val names        = c.names.map(_.mkString(" "))

      b.append(s"$headerPrefix## ${names.head}\n\n")
      if (names.tail.nonEmpty) b.append(names.tail.mkString("Aliases: `", "`, `", "`\n\n"))

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
          s"""Accepts option groups: ${links.mkString(", ")}
             |""".stripMargin
        )
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
    requireHandlers: Seq[RequireDirectiveHandler],
    onlyRestricted: Boolean
  ): String = {
    val b = new StringBuilder

    b.append(
      s"""---
         |title: Directives
         |sidebar_position: 2
         |---
         |
         |${
          if (onlyRestricted)
            "**This document describes as scala-cli behaves if run as `scala` command. See more information in [SIP-46](https://github.com/scala/improvement-proposals/pull/46)**"
          else ""
        }
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

  def run(options: InternalDocOptions, args: RemainingArgs): Unit = {

    val scalaCli = new ScalaCliCommands(
      "scala-cli",
      ScalaCli.baseRunnerName,
      ScalaCli.fullRunnerName,
      isSipScala = false
    )
    val commands = scalaCli.commands
    val restrictedCommands =
      commands.iterator.collect { case s: ScalaCommand[_] if !s.isRestricted => s }.toSeq
    val allArgs       = commands.flatMap(actualHelp(_).args)
    val nameFormatter = scalaCli.actualDefaultCommand.nameFormatter

    val allCliOptionsContent = cliOptionsContent(commands, allArgs, nameFormatter)
    val restrictedCliOptionsContent =
      cliOptionsContent(restrictedCommands, allArgs, nameFormatter, onlyRestricted = true)

    val allCommandsContent        = commandsContent(commands, onlyRestricted = false)
    val restrictedCommandsContent = commandsContent(restrictedCommands, onlyRestricted = true)

    val allDirectivesContent = usingContent(
      ScalaPreprocessor.usingDirectiveHandlers,
      ScalaPreprocessor.requireDirectiveHandlers,
      onlyRestricted = false
    )
    val restrictedDirectivesContent = usingContent(
      ScalaPreprocessor.usingDirectiveHandlers.filterNot(_.isRestricted),
      ScalaPreprocessor.requireDirectiveHandlers.filterNot(_.isRestricted),
      onlyRestricted = true
    )
    val restrictedDocsDir = os.rel / "scala-command"

    if (options.check) {
      val content = Seq(
        (os.rel / "cli-options.md")                     -> allCliOptionsContent,
        (os.rel / "commands.md")                        -> allCommandsContent,
        (os.rel / "directives.md")                      -> allDirectivesContent,
        (os.rel / restrictedDocsDir / "cli-options.md") -> restrictedCliOptionsContent,
        (os.rel / restrictedDocsDir / "commands.md")    -> restrictedCommandsContent,
        (os.rel / restrictedDocsDir / "directives.md")  -> restrictedDirectivesContent
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
      maybeWrite(options.outputPath / "cli-options.md", allCliOptionsContent)
      maybeWrite(options.outputPath / "commands.md", allCommandsContent)
      maybeWrite(options.outputPath / "directives.md", allDirectivesContent)

      maybeWrite(
        options.outputPath / restrictedDocsDir / "cli-options.md",
        restrictedCliOptionsContent
      )
      maybeWrite(options.outputPath / restrictedDocsDir / "commands.md", restrictedCommandsContent)
      maybeWrite(
        options.outputPath / restrictedDocsDir / "directives.md",
        restrictedDirectivesContent
      )
    }
  }
}
