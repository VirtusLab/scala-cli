package scala.cli.commands.fmt

import caseapp.*
import caseapp.core.help.HelpFormat
import dependency.*

import scala.build.input.{Inputs, Script, SourceScalaFile}
import scala.build.internal.{Constants, ExternalBinaryParams, FetchExternalBinary, Runner}
import scala.build.options.BuildOptions
import scala.build.{Logger, Sources}
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand
import scala.cli.commands.fmt.FmtUtil.*
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.util.ArgHelpers.*

object Fmt extends ScalaCommand[FmtOptions] {
  override def group: String = HelpCommandGroup.Main.toString
  override def sharedOptions(options: FmtOptions): Option[SharedOptions] = Some(options.shared)
  override def scalaSpecificationLevel                                   = SpecificationLevel.SHOULD

  val hiddenHelpGroups: Seq[HelpGroup] =
    Seq(
      HelpGroup.Scala,
      HelpGroup.Java,
      HelpGroup.Dependency,
      HelpGroup.ScalaJs,
      HelpGroup.ScalaNative,
      HelpGroup.CompilationServer,
      HelpGroup.Debug
    )
  override def helpFormat: HelpFormat = super.helpFormat
    .withHiddenGroups(hiddenHelpGroups)
    .withHiddenGroupsWhenShowHidden(hiddenHelpGroups)
    .withPrimaryGroup(HelpGroup.Format)
  override def names: List[List[String]] = List(
    List("fmt"),
    List("format"),
    List("scalafmt")
  )

  override def runCommand(options: FmtOptions, args: RemainingArgs, logger: Logger): Unit = {
    val buildOptions = buildOptionsOrExit(options)

    // TODO If no input is given, just pass '.' to scalafmt?
    val (sourceFiles, workspace, _) =
      if (args.all.isEmpty)
        (Seq(os.pwd), os.pwd, None)
      else {
        val i = options.shared.inputs(args.all).orExit(logger)
        val s = i.sourceFiles().collect {
          case sc: Script          => sc.path
          case sc: SourceScalaFile => sc.path
        }
        (s, i.workspace, Some(i))
      }
    CurrentParams.workspaceOpt = Some(workspace)
    val (versionMaybe, dialectMaybe, pathMaybe) = readVersionAndDialect(workspace, options, logger)
    val cache                                   = buildOptions.archiveCache

    if (sourceFiles.isEmpty)
      logger.debug("No source files, not formatting anything")
    else {
      val version =
        options.scalafmtVersion.getOrElse(versionMaybe.getOrElse(Constants.defaultScalafmtVersion))
      val dialectString = options.scalafmtDialect.orElse(dialectMaybe).getOrElse {
        options.buildOptions.orExit(logger).scalaParams.orExit(logger).map(_.scalaVersion)
          .getOrElse(Constants.defaultScalaVersion) match
          case v if v.startsWith("2.11.") => "scala211"
          case v if v.startsWith("2.12.") => "scala212"
          case v if v.startsWith("2.13.") => "scala213"
          case v if v.startsWith("3.")    => "scala3"
          case _                          => "default"
      }

      val entry = {
        val dialect       = ScalafmtDialect.fromString(dialectString)
        val prevConfMaybe = pathMaybe.map(p => os.read(p))
        scalafmtConfigWithFields(prevConfMaybe.getOrElse(""), Some(version), dialect)
      }
      val scalaFmtConfPath = {
        val confFileName = ".scalafmt.conf"
        val path =
          if (options.saveScalafmtConf) pathMaybe.getOrElse(workspace / confFileName)
          else workspace / Constants.workspaceDirName / confFileName
        os.write.over(path, entry, createFolders = true)
        path
      }

      val fmtCommand = options.scalafmtLauncher.filter(_.nonEmpty) match {
        case Some(launcher) =>
          Seq(launcher)
        case None =>
          val (url, changing) = options.binaryUrl(version)
          val params = ExternalBinaryParams(
            url,
            changing,
            "scalafmt",
            Seq(dep"${Constants.scalafmtOrganization}:${Constants.scalafmtName}:$version"),
            "org.scalafmt.cli.Cli"
          )
          FetchExternalBinary.fetch(
            params,
            cache,
            logger,
            () => buildOptions.javaHome().value.javaCommand
          )
            .orExit(logger)
            .command
      }

      logger.debug(s"Launching scalafmt with command $fmtCommand")

      val command = fmtCommand ++
        sourceFiles.map(_.toString) ++
        options.scalafmtCliOptions ++
        Seq("--config", scalaFmtConfPath.toString)
      val process = Runner.maybeExec(
        "scalafmt",
        command,
        logger,
        cwd = Some(workspace)
      )
      sys.exit(process.waitFor())
    }
  }
}
