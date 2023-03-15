package scala.cli.commands

import caseapp.core.app.Command
import caseapp.core.complete.{Completer, CompletionItem}
import caseapp.core.help.{Help, HelpFormat}
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error, RemainingArgs}
import caseapp.{HelpMessage, Name}
import coursier.core.{Repository, Version}
import dependency.*

import java.util.concurrent.atomic.AtomicBoolean

import scala.annotation.tailrec
import scala.build.EitherCps.{either, value}
import scala.build.compiler.SimpleScalaCompiler
import scala.build.errors.BuildException
import scala.build.input.{ScalaCliInvokeData, SubCommand}
import scala.build.internal.util.WarningMessages
import scala.build.internal.{Constants, Runner}
import scala.build.options.{BuildOptions, ScalacOpt, Scope}
import scala.build.{Artifacts, Directories, Logger, Positioned, ReplArtifacts}
import scala.cli.commands.default.LegacyScalaOptions
import scala.cli.commands.shared.{
  GlobalSuppressWarningOptions,
  HasGlobalOptions,
  HelpMessages,
  ScalaCliHelp,
  ScalacOptions,
  SharedOptions
}
import scala.cli.commands.util.CommandHelpers
import scala.cli.commands.util.ScalacOptionsUtil.*
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.internal.ProcUtil
import scala.cli.util.ConfigDbUtils.*
import scala.cli.{CurrentParams, ScalaCli}
import scala.util.{Properties, Try}

abstract class ScalaCommand[T <: HasGlobalOptions](implicit myParser: Parser[T], help: Help[T])
    extends Command()(myParser, help)
    with NeedsArgvCommand with CommandHelpers with RestrictableCommand[T] {

  def sharedOptions(t: T): Option[SharedOptions] = // hello borked unused warning
    None
  override def hasFullHelp = true
  override def hidden      = shouldExcludeInSip
  protected var argvOpt    = Option.empty[Array[String]]
  private val shouldExcludeInSip =
    (isRestricted || isExperimental) && !ScalaCli.allowRestrictedFeatures
  override def setArgv(argv: Array[String]): Unit = {
    argvOpt = Some(argv)
  }

  /** @return the actual Scala CLI program name which was run */
  protected def progName: String = ScalaCli.progName

  /** @return the actual Scala CLI runner name which was run */
  protected def fullRunnerName = ScalaCli.fullRunnerName

  /** @return the actual Scala CLI base runner name, for SIP it is scala otherwise scala-cli */
  protected def baseRunnerName = ScalaCli.baseRunnerName

  // TODO Manage to have case-app give use the exact command name that was used instead
  /** The actual sub-command name that was used. If the sub-command name is a list of strings, space
    * is used as the separator. If [[argvOpt]] hasn't been defined, it defaults to [[name]].
    */
  protected def actualCommandName: String =
    argvOpt.map { argv =>
      @tailrec
      def validCommand(potentialCommandName: List[String]): Option[List[String]] =
        if potentialCommandName.isEmpty then None
        else
          names.find(_ == potentialCommandName) match {
            case cmd @ Some(_) => cmd
            case _             => validCommand(potentialCommandName.dropRight(1))
          }

      val maxCommandLength: Int    = names.map(_.length).max max 1
      val maxPotentialCommandNames = argv.slice(1, maxCommandLength + 1).toList
      validCommand(maxPotentialCommandNames).getOrElse(List(""))
    }.getOrElse(List(name)).mkString(" ")

  protected def actualFullCommand: String =
    if actualCommandName.nonEmpty then s"$progName $actualCommandName" else progName

  protected def invokeData: ScalaCliInvokeData =
    ScalaCliInvokeData(
      progName,
      actualCommandName,
      SubCommand.Other,
      ProcUtil.isShebangCapableShell
    )

  given ScalaCliInvokeData = invokeData

  override def error(message: Error): Nothing = {
    System.err.println(
      s"""${message.message}
         |
         |To list all available options, run
         |  ${Console.BOLD}$actualFullCommand --help${Console.RESET}""".stripMargin
    )
    sys.exit(1)
  }

  // FIXME Report this in case-app default NameFormatter
  override lazy val nameFormatter: Formatter[Name] = {
    val parent = super.nameFormatter
    (t: Name) =>
      if (t.name.startsWith("-")) t.name
      else parent.format(t)
  }

  override def completer: Completer[T] = {
    val parent = super.completer
    new Completer[T] {
      def optionName(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem] =
        parent.optionName(prefix, state, args)
      def optionValue(
        arg: Arg,
        prefix: String,
        state: Option[T],
        args: RemainingArgs
      ): List[CompletionItem] = {
        val candidates = arg.name.name match {
          case "dependency" =>
            state.flatMap(sharedOptions).toList.flatMap { sharedOptions =>
              val logger = sharedOptions.logger
              val cache  = sharedOptions.coursierCache
              val sv = sharedOptions.buildOptions().orExit(logger)
                .scalaParams
                .toOption
                .flatten
                .map(_.scalaVersion)
                .getOrElse(Constants.defaultScalaVersion)
              val (fromIndex, completions) = cache.logger.use {
                coursier.complete.Complete(cache)
                  .withInput(prefix)
                  .withScalaVersion(sv)
                  .complete()
                  .unsafeRun()(cache.ec)
              }
              if (completions.isEmpty) Nil
              else {
                val prefix0 = prefix.take(fromIndex)
                val values  = completions.map(c => prefix0 + c)
                values.map { str =>
                  CompletionItem(str)
                }
              }
            }
          case "repository" => Nil // TODO
          case _            => Nil
        }
        candidates ++ parent.optionValue(arg, prefix, state, args)
      }
      def argument(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem] =
        parent.argument(prefix, state, args)
    }
  }

  def maybePrintGroupHelp(options: T): Unit =
    for (shared <- sharedOptions(options))
      shared.helpGroups.maybePrintGroupHelp(help, helpFormat)

  private def maybePrintWarnings(options: T): Unit = {
    import scala.cli.commands.shared.ScalacOptions.YScriptRunnerOption
    val logger = options.logging.logger
    sharedOptions(options).foreach { so =>
      val scalacOpts = so.scalac.scalacOption.toScalacOptShadowingSeq
      if scalacOpts.keys.contains(ScalacOpt(YScriptRunnerOption)) then
        logger.message(
          LegacyScalaOptions.yScriptRunnerWarning(scalacOpts.getOption(YScriptRunnerOption))
        )
    }
  }

  /** Print `scalac` output if passed options imply no inputs are necessary and raw `scalac` output
    * is required instead. (i.e. `--scalac-option -help`)
    * @param options
    *   command options
    */
  def maybePrintSimpleScalacOutput(options: T, buildOptions: BuildOptions): Unit =
    for {
      shared <- sharedOptions(options)
      scalacOptions        = shared.scalac.scalacOption
      updatedScalacOptions = scalacOptions.withScalacExtraOptions(shared.scalacExtra)
      if updatedScalacOptions.exists(ScalacOptions.ScalacPrintOptions)
      logger = shared.logger
      artifacts      <- buildOptions.artifacts(logger, Scope.Main).toOption
      scalaArtifacts <- artifacts.scalaOpt
      compilerClassPath   = scalaArtifacts.compilerClassPath
      scalaVersion        = scalaArtifacts.params.scalaVersion
      compileClassPath    = artifacts.compileClassPath
      simpleScalaCompiler = SimpleScalaCompiler("java", Nil, scaladoc = false)
      javacOptions        = buildOptions.javaOptions.javacOptions.map(_.value)
      javaHome            = buildOptions.javaHomeLocation().value
    } {
      val exitCode = simpleScalaCompiler.runSimpleScalacLike(
        scalaVersion,
        Option(javaHome),
        javacOptions,
        updatedScalacOptions,
        compileClassPath,
        compilerClassPath,
        logger
      )
      sys.exit(exitCode)
    }

  def maybePrintToolsHelp(options: T, buildOptions: BuildOptions): Unit =
    for {
      shared <- sharedOptions(options)
      logger = shared.logger
      artifacts      <- buildOptions.artifacts(logger, Scope.Main).toOption
      scalaArtifacts <- artifacts.scalaOpt
      scalaParams = scalaArtifacts.params
      if shared.helpGroups.helpScaladoc || shared.helpGroups.helpRepl || shared.helpGroups.helpScalafmt
    } {
      val exitCode: Either[BuildException, Int] = either {
        val (classPath: Seq[os.Path], mainClass: String) =
          if (shared.helpGroups.helpScaladoc) {
            val docArtifacts = value {
              Artifacts.fetch(
                Positioned.none(Seq(dep"org.scala-lang::scaladoc:${scalaParams.scalaVersion}")),
                value(buildOptions.finalRepositories),
                Some(scalaParams),
                logger,
                buildOptions.finalCache,
                None
              )
            }
            docArtifacts.files.map(os.Path(_, os.pwd)) -> "dotty.tools.scaladoc.Main"
          }
          else if (shared.helpGroups.helpRepl) {
            val initialBuildOptions = buildOptionsOrExit(options)
            val artifacts = initialBuildOptions.artifacts(logger, Scope.Main).orExit(logger)
            val replArtifacts = value {
              ReplArtifacts.default(
                scalaParams,
                artifacts.userDependencies,
                Nil,
                logger,
                buildOptions.finalCache,
                Nil,
                None
              )
            }
            replArtifacts.replClassPath -> replArtifacts.replMainClass
          }
          else {
            val fmtArtifacts = value {
              Artifacts.fetch(
                Positioned.none(Seq(
                  dep"${Constants.scalafmtOrganization}:${Constants.scalafmtName}:${Constants.defaultScalafmtVersion}"
                )),
                value(buildOptions.finalRepositories),
                Some(scalaParams),
                logger,
                buildOptions.finalCache,
                None
              )
            }
            fmtArtifacts.files.map(os.Path(_, os.pwd)) -> "org.scalafmt.cli.Cli"
          }
        val retCode = Runner.runJvm(
          buildOptions.javaHome().value.javaCommand,
          Nil,
          classPath,
          mainClass,
          Seq("-help"),
          logger
        ).waitFor()
        retCode
      }
      sys.exit(exitCode.orExit(logger))
    }

  override def helpFormat: HelpFormat = ScalaCliHelp.helpFormat

  override val messages: Help[T] =
    if shouldExcludeInSip then
      Help[T](helpMessage =
        Some(HelpMessage(HelpMessages.powerCommandUsedInSip(scalaSpecificationLevel)))
      )
    else if isExperimental then
      help.copy(helpMessage =
        help.helpMessage.map(hm =>
          hm.copy(
            message =
              s"""${hm.message}
                 |
                 |${WarningMessages.experimentalSubcommandUsed(name)}""".stripMargin,
            detailedMessage =
              if hm.detailedMessage.nonEmpty then
                s"""${hm.detailedMessage}
                   |
                   |${WarningMessages.experimentalSubcommandUsed(name)}""".stripMargin
              else hm.detailedMessage
          )
        )
      )
    else help

  /** @param options
    *   command-specific [[T]] options
    * @return
    *   Tries to create BuildOptions based on [[sharedOptions]] and exits on error. Override to
    *   change this behaviour.
    */
  def buildOptions(options: T): Option[BuildOptions] =
    sharedOptions(options).map(shared => shared.buildOptions().orExit(shared.logger))

  protected def buildOptionsOrExit(options: T): BuildOptions =
    buildOptions(options).getOrElse {
      sharedOptions(options).foreach(_.logger.debug("build options could not be initialized"))
      sys.exit(1)
    }

  private val shouldSuppressExperimentalFeatureWarningsAtomic: AtomicBoolean =
    new AtomicBoolean(false)
  override def shouldSuppressExperimentalFeatureWarnings: Boolean =
    shouldSuppressExperimentalFeatureWarningsAtomic.get()
  final override def main(progName: String, args: Array[String]): Unit = {
    shouldSuppressExperimentalFeatureWarningsAtomic
      .set {
        GlobalSuppressWarningOptions.shouldSuppressExperimentalFeatureWarning(args.toList)
          .orElse {
            configDb.toOption
              .flatMap(_.getOpt(Keys.suppressExperimentalFeatureWarning))
          }
          .getOrElse(false)
      }
    super.main(progName, args)
  }

  /** This should be overridden instead of [[run]] when extending [[ScalaCommand]].
    *
    * @param options
    *   the command's specific set of options
    * @param remainingArgs
    *   arguments remaining after parsing options
    */
  def runCommand(options: T, remainingArgs: RemainingArgs, logger: Logger): Unit

  /** This implementation is final. Override [[runCommand]] instead. This logic is invoked at the
    * start of running every [[ScalaCommand]].
    */
  final override def run(options: T, remainingArgs: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.logging.verbosity
    val logger = options.logging.logger
    if shouldExcludeInSip then
      logger.error(HelpMessages.powerCommandUsedInSip(scalaSpecificationLevel))
      sys.exit(1)
    else if isExperimental && !shouldSuppressExperimentalFeatureWarnings then
      logger.message(WarningMessages.experimentalSubcommandUsed(name))
    maybePrintWarnings(options)
    maybePrintGroupHelp(options)
    buildOptions(options).foreach { bo =>
      maybePrintSimpleScalacOutput(options, bo)
      maybePrintToolsHelp(options, bo)
    }
    runCommand(options, remainingArgs, options.logging.logger)
  }
}
