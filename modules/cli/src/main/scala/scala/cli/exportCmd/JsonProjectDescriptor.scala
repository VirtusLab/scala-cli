package scala.cli.exportCmd

import com.github.plokhotnyuk.jsoniter_scala.core.WriterConfig
import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.parse.RepositoryParser
import coursier.{LocalRepositories, Repositories}
import dependency.NoAttributes

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import scala.build.errors.BuildException
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

  private def scalaVersionSettings(options: BuildOptions): JsonProject = {
    val sv = options.scalaOptions.scalaVersion
      .flatMap(_.versionOpt)
      .getOrElse(Constants.defaultScalaVersion)

    JsonProject(scalaVersion = Some(sv))
  }

  private def scalacOptionsSettings(options: BuildOptions): JsonProject =
    JsonProject(scalacOptions = options.scalaOptions.scalacOptions.toSeq.map(_.value.value))

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

  private def platformSettings(options: BuildOptions): JsonProject = {
    val platform = options.scalaOptions.platform.map(_.value.repr)
      .orElse(Some(Platform.JVM.repr))

    JsonProject(platform = platform)
  }

  private def sourcesSettings(sources: Sources): ScopedJsonProject =
    ScopedJsonProject(sources =
      ProjectDescriptor.sources(sources, charSet).map(_._1.toNIO.toString)
    )

  private def scalaCompilerPlugins(options: BuildOptions): JsonProject =
    JsonProject(scalaCompilerPlugins = options.scalaOptions.compilerPlugins.map(_.value.render))

  private def dependencySettings(options: BuildOptions): ScopedJsonProject = {
    val directDeps = options.classPathOptions.extraDependencies.toSeq
      .map(_.value.render)

    ScopedJsonProject(dependencies = directDeps)
  }

  private def repositorySettings(options: BuildOptions): ScopedJsonProject = {
    val resolvers = options.finalRepositories
      .getOrElse(Nil)
      .appended(Repositories.central)
      .appended(LocalRepositories.ivy2Local)
      .collect {
        case repo: MavenRepository => repo.root
        case repo: IvyRepository   => s"ivy:${repo.pattern}"
      }
      .distinct

    ScopedJsonProject(resolvers = resolvers)
  }

  private def customResourcesSettings(options: BuildOptions): ScopedJsonProject =
    ScopedJsonProject(resourcesDirs = options.classPathOptions.resourcesDir.map(_.toNIO.toString))

  private def customJarsSettings(options: BuildOptions): ScopedJsonProject = {

    val customCompileOnlyJarsDecls =
      options.classPathOptions.extraCompileOnlyJars.map(_.toNIO.toString)

    val customJarsDecls = options.classPathOptions.extraClassPath.map(_.toNIO.toString)

    ScopedJsonProject(
      extraDecls = customCompileOnlyJarsDecls ++ customJarsDecls
    )
  }

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): JsonProject = {
    val mainJsonProject = exportScope(Scope.Main, optionsMain, sourcesMain)
    val testJsonProject = exportScope(Scope.Test, optionsTest, sourcesTest)

    mainJsonProject + testJsonProject
  }

  def exportScope(
    scope: Scope,
    options: BuildOptions,
    sources: Sources
  ): JsonProject = {
    val baseSettings = JsonProject(
      projectName = projectName,
      mainClass = options.mainClass
    )

    val scopeSpecifics = Seq(
      ScopedJsonProject(scopeName = Some(scope.name)),
      sourcesSettings(sources),
      dependencySettings(options),
      repositorySettings(options),
      customResourcesSettings(options),
      customJarsSettings(options)
    )

    val scopedJsonProject = scopeSpecifics.foldLeft(ScopedJsonProject())(_ + _)
      .sorted

    val settings = Seq(
      baseSettings,
      JsonProject(scopes = Seq(scopedJsonProject)),
      scalaVersionSettings(options)
    ) ++
      (if (scope == Scope.Main)
         Seq(
           scalaCompilerPlugins(options),
           platformSettings(options),
           scalacOptionsSettings(options),
           if (options.platform.value == Platform.JS)
             scalaJsSettings(options.scalaJsOptions)
           else if (options.platform.value == Platform.Native)
             scalaNativeSettings(options.scalaNativeOptions)
           else JsonProject()
         )
       else Nil)

    settings.foldLeft(JsonProject())(_ + _)
  }
}
