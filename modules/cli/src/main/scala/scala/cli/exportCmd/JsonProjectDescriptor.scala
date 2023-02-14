package scala.cli.exportCmd

import com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.parse.RepositoryParser
import coursier.{LocalRepositories, Repositories}
import dependency.NoAttributes

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import scala.build.internal.Constants
import scala.build.internal.Runner.frameworkName
import scala.build.options.{BuildOptions, Platform, ScalaJsOptions, ScalaNativeOptions, Scope}
import scala.build.testrunner.AsmTestRunner
import scala.build.{Logger, Sources}
import scala.cli.util.SeqHelpers.*

final case class JsonProjectDescriptor(
  projectName: Option[String] = None,
  logger: Logger
) extends ProjectDescriptor {
  private val charSet = StandardCharsets.UTF_8

  private def sourcesSettings(mainSources: Sources, testSources: Sources): JsonProject = {
    val mainSubPaths = ProjectDescriptor.sources(mainSources, charSet).map(_._1.toNIO.toString)
    val testSubPaths = ProjectDescriptor.sources(testSources, charSet).map(_._1.toNIO.toString)
    JsonProject(mainSources = mainSubPaths, testSources = testSubPaths)
  }

  private def scalaVersionSettings(options: BuildOptions): JsonProject = {
    val sv = options.scalaOptions.scalaVersion
      .flatMap(_.versionOpt) // FIXME If versionOpt is empty, the project is pure Java
      .getOrElse(Constants.defaultScalaVersion)

    JsonProject(scalaVersion = Some(sv))
  }

  private def scalaCompilerPlugins(buildOptions: BuildOptions): JsonProject =
    JsonProject(scalaCompilerPlugins =
      buildOptions.scalaOptions.compilerPlugins.toSeq.map(_.value.render)
    )

  private def scalacOptionsSettings(buildOptions: BuildOptions): JsonProject =
    JsonProject(scalacOptions = buildOptions.scalaOptions.scalacOptions.toSeq.map(_.value.value))

  private def scalaJsSettings(options: ScalaJsOptions): JsonProject = {

    val scalaJsVersion = Some(options.version.getOrElse(Constants.scalaJsVersion))

    val moduleKindDecls =
      if (options.moduleKindStr.isEmpty) Nil
      else
        Seq(options.moduleKind(logger))

    JsonProject(
      scalaJsVersion = scalaJsVersion,
      extraDecls = moduleKindDecls
    )
  }

  private def scalaNativeSettings(options: ScalaNativeOptions): JsonProject = {
    val scalaNativeVersion = Some(options.version.getOrElse(Constants.scalaNativeVersion))
    JsonProject(scalaNativeVersion = scalaNativeVersion)
  }

  private def dependencySettings(
    mainOptions: BuildOptions,
    testOptions: BuildOptions
  ): JsonProject = {
    def getExportDependencyFormat(
      options: BuildOptions,
      scope: Scope
    ): Seq[ExportDependencyFormat] = {
      val artifacts = options.artifacts(Logger.nop, scope).toSeq

      val allDeps = for {
        artifact         <- artifacts
        detailedArtifact <- artifact.detailedArtifacts
        exportDep = ExportDependencyFormat(detailedArtifact._1)
      } yield exportDep

      val directDeps = options.classPathOptions.extraDependencies.toSeq.map(_.value)

      allDeps
        .filter(exportDep =>
          directDeps.exists(anyDep =>
            anyDep.module.organization == exportDep.groupId &&
            (anyDep.module.name == exportDep.artifactId.name
            || anyDep.module.name == exportDep.artifactId.name) &&
            anyDep.version == exportDep.version
          )
        )
        .distinct
        .sorted
    }

    val mainDeps = getExportDependencyFormat(mainOptions, Scope.Main)
    val testDeps = getExportDependencyFormat(testOptions, Scope.Test)

    JsonProject(mainDeps = mainDeps, testDeps = testDeps)
  }

  private def repositorySettings(options: BuildOptions): JsonProject = {
    val resolvers = options.finalRepositories
      .getOrElse(Nil)
      .appended(Repositories.central)
      .appended(LocalRepositories.ivy2Local)
      .collect {
        case repo: MavenRepository =>
          ExportResolverFormat(
            name = "MavenRepository",
            location = Some(repo.root)
          ) // TODO repo.authentication?
        case repo: IvyRepository =>
          ExportResolverFormat(
            name = "IvyRepository",
            location = repo.pattern.chunks.headOption.map(_.string)
          ) // TODO repo.authentication?
      }
      .distinct
      .sortBy(_.name)

    JsonProject(resolvers = resolvers)
  }

  private def customResourcesSettings(options: BuildOptions): JsonProject =
    JsonProject(
      resourcesDirs = options.classPathOptions.resourcesDir.map(_.toNIO.toString)
    )

  private def customJarsSettings(options: BuildOptions): JsonProject = {

    val customCompileOnlyJarsDecls =
      options.classPathOptions.extraCompileOnlyJars.map(_.toNIO.toString)

    val customJarsDecls = options.classPathOptions.extraClassPath.map(_.toNIO.toString)

    JsonProject(
      extraDecls = customCompileOnlyJarsDecls ++ customJarsDecls
    )
  }

  private def platformSettings(options: BuildOptions): JsonProject = {
    val platform = options.scalaOptions.platform.map(_.value.repr)
      .orElse(Some(Platform.JVM.repr))

    JsonProject(platform = platform)
  }

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): JsonProject = {

    // FIXME Put a sensible value in JsonProject.nameOpt

    val baseSettings = JsonProject(
      projectName = projectName,
      mainClass = optionsMain.mainClass
    )

    val settings = Seq(
      baseSettings,
      sourcesSettings(sourcesMain, sourcesTest),
      scalaVersionSettings(optionsMain),
      platformSettings(optionsMain),
      scalacOptionsSettings(optionsMain),
      scalaCompilerPlugins(optionsMain),
      dependencySettings(optionsMain, optionsTest),
      repositorySettings(optionsMain),
      if (optionsMain.platform.value == Platform.JS) scalaJsSettings(optionsMain.scalaJsOptions)
      else JsonProject(),
      if (optionsMain.platform.value == Platform.Native)
        scalaNativeSettings(optionsMain.scalaNativeOptions)
      else JsonProject(),
      customResourcesSettings(optionsMain),
      customJarsSettings(optionsMain)
    )

    settings.foldLeft(JsonProject())(_ + _)
  }
}
