package scala.cli.commands.scalafix

import caseapp.*
import caseapp.core.help.HelpFormat
import dependency.*
import scalafix.interfaces.ScalafixError.*
import scalafix.interfaces.{
  ScalafixError,
  ScalafixException,
  ScalafixRule,
  Scalafix as ScalafixInterface
}

import java.util.Optional
import scala.build.input.{Inputs, Script, SourceScalaFile}
import scala.build.internal.{Constants, ExternalBinaryParams, FetchExternalBinary, Runner}
import scala.build.options.{BuildOptions, Scope}
import scala.build.{Build, BuildThreads, FixArtifacts, Logger, Sources}
import scala.cli.CurrentParams
import coursier.cache.FileCache
import scala.cli.commands.compile.Compile.buildOptionsOrExit
import scala.cli.commands.fmt.FmtUtil.*
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.commands.{compile, ScalaCommand, SpecificationLevel}
import scala.cli.config.Keys
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.collection.mutable
import scala.collection.mutable.Buffer
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import scala.build.EitherCps.{either, value}

object Scalafix extends ScalaCommand[ScalafixOptions] {
  override def group: String = HelpCommandGroup.Main.toString
  override def sharedOptions(options: ScalafixOptions): Option[SharedOptions] = Some(options.shared)
  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.EXPERIMENTAL

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
    List("scalafix")
  )

  override def runCommand(options: ScalafixOptions, args: RemainingArgs, logger: Logger): Unit = {
    val buildOptions = buildOptionsOrExit(options)
    val buildOptionsWithSemanticDb = buildOptions.copy(scalaOptions =
      buildOptions.scalaOptions.copy(semanticDbOptions =
        buildOptions.scalaOptions.semanticDbOptions.copy(generateSemanticDbs = Some(true))
      )
    )
    val inputs        = options.shared.inputs(args.all).orExit(logger)
    val threads       = BuildThreads.create()
    val compilerMaker = options.shared.compilerMaker(threads)
    val configDb      = ConfigDbUtils.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    val (sourcePaths, workspace, _) =
      if (args.all.isEmpty)
        (Seq(os.pwd), os.pwd, None)
      else {
        val s = inputs.sourceFiles().collect {
          case sc: Script          => sc.path
          case sc: SourceScalaFile => sc.path
        }
        (s, inputs.workspace, Some(inputs))
      }

    val scalaVersion =
      options.buildOptions.orExit(logger).scalaParams.orExit(logger).map(_.scalaVersion)
        .getOrElse(Constants.defaultScalaVersion)

    val scalaBinaryVersion = scalaVersion match
      case v if v.startsWith("2.12.") => "2.12"
      case v if v.startsWith("2.13.") => "2.13"
      case v if v.startsWith("3.")    => "2.13"
      case _ =>
        logger.error("Unsupported scala version " + scalaVersion)
        sys.exit(1)
    val configFilePathOpt = options.scalafixConf.map(os.Path(_, os.pwd))
    val relPaths          = sourcePaths.map(_.toNIO.getFileName)

    //  --config | -c  <.scalafix.conf>
    //        File path to a .scalafix.conf configuration file.
    //  --sourceroot  </foo/myproject>
    //        Absolute path passed to semanticdb with
    //	-P:semanticdb:sourceroot:<path>. Relative filenames persisted in the
    //	Semantic DB are absolutized by the sourceroot. Defaults to current
    //	working directory if not provided.
    //  --classpath  <entry1.jar:entry2.jar:target/scala-2.12/classes>
    //        java.io.File.pathSeparator separated list of directories or jars
    //	containing '.semanticdb' files. The 'semanticdb' files are emitted by
    //	the semanticdb-scalac compiler plugin and are necessary for semantic
    //	rules like ExplicitResultTypes to function.
    //  --classpath-auto-roots  <target:project/target>
    //        Automatically infer --classpath starting from these directories.
    //	Ignored if --classpath is provided.
    //  --no-strict-semanticdb
    //        Disable validation when loading semanticdb files.
    //  --rules | -r  <ProcedureSyntax OR file:LocalFile.scala OR scala:full.Name OR https://gist.com/.../Rule.scala>
    //        Scalafix rules to run.
    //  --test
    //        Exit non-zero code if files have not been fixed. Won't write to files.

    val res = Build.build(
      inputs,
      buildOptionsWithSemanticDb,
      compilerMaker,
      None,
      logger,
      crossBuilds = false,
      buildTests = false,
      partial = None,
      actionableDiagnostics = actionableDiagnostics
    )
    val builds = res.orExit(logger)

    val successfulBuildOpt = for {
      build <- builds.get(Scope.Main)
      sOpt  <- build.successfulOpt
    } yield sOpt

    val classPaths = successfulBuildOpt.map(_.fullClassPath).getOrElse(Seq.empty)
    val externalDeps =
      options.shared.dependencies.compileOnlyDependency ++ successfulBuildOpt.map(
        _.options.classPathOptions.extraCompileOnlyDependencies.values.flatten.map(_.value.render)
      ).getOrElse(Seq.empty)
    val scalacOptions = options.shared.scalac.scalacOption ++ successfulBuildOpt.map(
      _.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
    ).getOrElse(Seq.empty)

    val scalafixOptions =
      configFilePathOpt.map(file => Seq("-c", file.toString)).getOrElse(Nil) ++
        Seq("--sourceroot", workspace.toString) ++
        Seq("--classpath", classPaths.mkString(java.io.File.pathSeparator))
        //Seq("--scalac-options") ++ scalacOptions

    either {
      val artifacts = FixArtifacts.artifacts(
        value(buildOptions.finalRepositories),
        logger,
        buildOptions.internal.cache.getOrElse(FileCache())
      )

      val proc = Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
        value(artifacts.map(_.artifacts.map(_._2))),
        "scalafix.cli.Cli",
        scalafixOptions,
        logger,
        cwd = Some(workspace)
      )

      sys.exit(proc.waitFor())
    }

//    val scalafix = ScalafixInterface
//      .fetchAndClassloadInstance(scalaBinaryVersion)
//      .newArguments()
//      .withWorkingDirectory(workspace.toNIO)
//      .withPaths(relPaths.asJava)
//      .withRules(options.rules.asJava)
//      .withConfig(configFilePathOpt.map(_.toNIO).toJava)
//      .withScalaVersion(scalaVersion)
//
//    logger.debug(
//      s"Processing ${sourcePaths.size} Scala sources"
//    )
//
//    val rulesThatWillRun: Either[ScalafixException, mutable.Buffer[ScalafixRule]] =
//      try
//        Right(scalafix.rulesThatWillRun().asScala)
//      catch
//        case e: ScalafixException => Left(e)
//    val needToBuild: Boolean = rulesThatWillRun match
//      case Right(rules) => rules.exists(_.kind().isSemantic)
//      case Left(_)      => true
//
//    val preparedScalafixInstance = if (needToBuild) {
//      val res = Build.build(
//        inputs,
//        buildOptionsWithSemanticDb,
//        compilerMaker,
//        None,
//        logger,
//        crossBuilds = false,
//        buildTests = false,
//        partial = None,
//        actionableDiagnostics = actionableDiagnostics
//      )
//      val builds = res.orExit(logger)
//
//      val successfulBuildOpt = for {
//        build <- builds.get(Scope.Main)
//        sOpt  <- build.successfulOpt
//      } yield sOpt
//
//      val classPaths = successfulBuildOpt.map(_.fullClassPath).getOrElse(Seq.empty)
//      val externalDeps =
//        options.shared.dependencies.compileOnlyDependency ++ successfulBuildOpt.map(
//          _.options.classPathOptions.extraCompileOnlyDependencies.values.flatten.map(_.value.render)
//        ).getOrElse(Seq.empty)
//      val scalacOptions = options.shared.scalac.scalacOption ++ successfulBuildOpt.map(
//        _.options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
//      ).getOrElse(Seq.empty)
//
//      scalafix
//        .withScalacOptions(scalacOptions.asJava)
//        .withClasspath(classPaths.map(_.toNIO).asJava)
//        .withToolClasspath(Seq.empty.asJava, externalDeps.asJava)
//    }
//    else
//      scalafix
//
//    val customScalafixInstance = preparedScalafixInstance
//      .withParsedArguments(options.scalafixArg.asJava)
//
//    val errors = if (options.check) {
//      val evaluation = customScalafixInstance.evaluate()
//      if (evaluation.isSuccessful)
//        evaluation.getFileEvaluations.foldLeft(List.empty[String]) {
//          case (errors, fileEvaluation) =>
//            val problemMessage = fileEvaluation.getErrorMessage.toScala.orElse(
//              fileEvaluation.previewPatchesAsUnifiedDiff.toScala
//            )
//            errors ++ problemMessage
//        }
//      else
//        evaluation.getErrorMessage.toScala.toList
//    }
//    else
//      customScalafixInstance.run().map(prepareErrorMessage).toList
//
//    if (errors.isEmpty) sys.exit(0)
//    else {
//      errors.tapEach(logger.error)
//      sys.exit(1)
//    }
  }

  private def prepareErrorMessage(error: ScalafixError): String = error match
    case ParseError => "A source file failed to be parsed"
    case CommandLineError =>
      "A command-line argument was parsed incorrectly"
    case MissingSemanticdbError =>
      "A semantic rewrite was run on a source file that has no associated META-INF/semanticdb/.../*.semanticdb"
    case StaleSemanticdbError =>
      """The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin.
        |To resolve this error re-compile the project and re-run Scalafix""".stripMargin
    case TestError =>
      "A Scalafix test error was reported. Run `fix` without `--check` or `--diff` to fix the error"
    case LinterError  => "A Scalafix linter error was reported"
    case NoFilesError => "No files were provided to Scalafix so nothing happened"
    case NoRulesError => "No rules were provided to Scalafix so nothing happened"
    case _            => "Something unexpected happened running Scalafix"

}
