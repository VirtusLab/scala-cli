package scala.build

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.info.{BuildInfo, ScopedBuildInfo}
import scala.build.internal.AppCodeWrapper
import scala.build.internal.util.WarningMessages
import scala.build.options.{BuildOptions, HasScope, Scope}
import scala.build.preprocessing.ScriptPreprocessor

/** Information gathered from preprocessing command inputs - sources (including unwrapped scripts)
  * and build options from using directives. Only scope requirements remain in this object after
  * resolving them in [[CrossSources.scopedSources]]
  *
  * @param paths
  *   paths and relative paths to sources on disk with the scope they belong to
  * @param inMemory
  *   in memory sources (e.g. snippets) with the scope they belong to
  * @param defaultMainClass
  * @param resourceDirs
  * @param buildOptions
  *   build options sources with the scope they belong to
  * @param unwrappedScripts
  *   in memory script sources with the scope they belong to, their code must be wrapped before
  *   compiling
  */

final case class ScopedSources(
  paths: Seq[HasScope[(os.Path, os.RelPath)]],
  inMemory: Seq[HasScope[Sources.InMemory]],
  defaultMainElemPath: Option[os.Path],
  resourceDirs: Seq[HasScope[os.Path]],
  buildOptions: Seq[HasScope[BuildOptions]],
  unwrappedScripts: Seq[HasScope[Sources.UnwrappedScript]]
) {
  def buildOptionsFor(scope: Scope): Seq[BuildOptions] =
    scope match {
      case Scope.Test => buildOptions.flatMap(_.valueFor(Scope.Test).toSeq) ++
          buildOptions.flatMap(_.valueFor(Scope.Main).toSeq)
      case _ => buildOptions.flatMap(_.valueFor(scope).toSeq)
    }

  /** Resolve scope requirements and create a Sources instance
    * @param scope
    *   scope to be resolved
    * @param baseOptions
    *   options that have already been collected for this build, they should consist of:
    *   - options from the console
    *   - options from using directives from the sources
    *   - options from resolved using directives that had Scala version and platform requirements
    *     that fit the current build
    * @return
    *   [[Sources]] instance that belong to specified scope
    */
  def sources(
    scope: Scope,
    baseOptions: BuildOptions,
    workspace: os.Path,
    logger: Logger
  ): Either[BuildException, Sources] = either {
    val combinedOptions = combinedBuildOptions(scope, baseOptions)

    val codeWrapper = ScriptPreprocessor.getScriptWrapper(combinedOptions, logger)

    val wrappedScripts = unwrappedScripts
      .flatMap(_.valueFor(scope).toSeq)
      .map(_.wrap(codeWrapper))

    codeWrapper match {
      case _: AppCodeWrapper if wrappedScripts.size > 1 =>
        wrappedScripts.find(_.originalPath.exists(_._1.toString == "main.sc"))
          .foreach(_ => logger.diagnostic(WarningMessages.mainScriptNameClashesWithAppWrapper))
      case _ => ()
    }

    val defaultMainClass = defaultMainElemPath.flatMap { mainElemPath =>
      wrappedScripts.collectFirst {
        case Sources.InMemory(Right((_, path)), _, _, Some(wrapperParams))
            if mainElemPath == path =>
          wrapperParams.mainClass
      }
    }

    val needsBuildInfo = combinedOptions.sourceGeneratorOptions.useBuildInfo.getOrElse(false)

    val maybeBuildInfoSource = if (needsBuildInfo && scope == Scope.Main)
      Seq(
        Sources.InMemory(
          Left("build-info"),
          os.rel / "BuildInfo.scala",
          value(buildInfo(combinedOptions, workspace)).generateContents().getBytes(
            StandardCharsets.UTF_8
          ),
          None
        )
      )
    else Nil

    Sources(
      paths.flatMap(_.valueFor(scope).toSeq),
      inMemory.flatMap(_.valueFor(scope).toSeq) ++ wrappedScripts ++ maybeBuildInfoSource,
      defaultMainClass,
      resourceDirs.flatMap(_.valueFor(scope).toSeq),
      combinedOptions
    )
  }

  /** Combine build options that had no requirements (console and using directives) or their
    * requirements have been resolved (e.g. target using directives) with build options that require
    * the specified scope
    *
    * @param scope
    *   scope to be resolved
    * @param baseOptions
    *   options that have already been collected for this build (had no requirements or they have
    *   been resolved)
    * @return
    *   Combined BuildOptions, baseOptions' values take precedence
    */
  def combinedBuildOptions(scope: Scope, baseOptions: BuildOptions): BuildOptions =
    buildOptionsFor(scope)
      .foldRight(baseOptions)(_.orElse(_))

  def buildInfo(baseOptions: BuildOptions, workspace: os.Path): Either[BuildException, BuildInfo] =
    either {
      def getScopedBuildInfo(scope: Scope): ScopedBuildInfo =
        val combinedOptions = combinedBuildOptions(scope, baseOptions)
        val sourcePaths     = paths.flatMap(_.valueFor(scope).toSeq).map(_._1.toString)
        val inMemoryPaths   =
          (inMemory.flatMap(_.valueFor(scope).toSeq).flatMap(_.originalPath.toOption) ++
            unwrappedScripts.flatMap(_.valueFor(scope).toSeq).flatMap(_.originalPath.toOption))
            .map(_._2.toString)

        ScopedBuildInfo(combinedOptions, sourcePaths ++ inMemoryPaths)

      val baseBuildInfo = value(BuildInfo(combinedBuildOptions(Scope.Main, baseOptions), workspace))

      val mainBuildInfo = getScopedBuildInfo(Scope.Main)
      val testBuildInfo = getScopedBuildInfo(Scope.Test)

      baseBuildInfo
        .withScope(Scope.Main.name, mainBuildInfo)
        .withScope(Scope.Test.name, testBuildInfo)
    }
}
