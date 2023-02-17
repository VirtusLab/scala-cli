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
import scala.build.options.{
  BuildOptions,
  JavaOptions,
  Platform,
  ScalaJsOptions,
  ScalaNativeOptions,
  Scope
}
import scala.build.testrunner.AsmTestRunner
import scala.build.{Logger, Positioned, Sources}
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

  private def scalaJsSettings(options: ScalaJsOptions): JsonProject = {

    val scalaJsVersion = Some(options.version.getOrElse(Constants.scalaJsVersion))

    JsonProject(
      platform = Some(Platform.JS.repr),
      scalaJsVersion = scalaJsVersion,
      jsEsVersion = options.esVersionStr
    )
  }

  private def scalaNativeSettings(options: ScalaNativeOptions): JsonProject = {
    val scalaNativeVersion = Some(options.version.getOrElse(Constants.scalaNativeVersion))

    JsonProject(
      platform = Some(Platform.Native.repr),
      scalaNativeVersion = scalaNativeVersion
    )
  }

  private def jvmSettings(options: JavaOptions): JsonProject =
    JsonProject(
      platform = Some(Platform.JVM.repr),
      jvmVersion = options.jvmIdOpt.map(_.value)
    )

  private def platformSettings(options: BuildOptions): JsonProject =
    options.scalaOptions.platform.map(_.value) match {
      case Some(Platform.JS) =>
        scalaJsSettings(options.scalaJsOptions)
      case Some(Platform.Native) =>
        scalaNativeSettings(options.scalaNativeOptions)
      case _ => jvmSettings(options.javaOptions)
    }

  private def scalacOptionsSettings(options: BuildOptions): ScopedJsonProject =
    ScopedJsonProject(scalacOptions = options.scalaOptions.scalacOptions.toSeq.map(_.value.value))

  private def sourcesSettings(sources: Sources): ScopedJsonProject =
    ScopedJsonProject(sources =
      ProjectDescriptor.sources(sources, charSet).map(_._1.toNIO.toString)
    )

  private def scalaCompilerPlugins(options: BuildOptions): ScopedJsonProject =
    val compilerPlugins = options.scalaOptions.compilerPlugins.map(_.value)
      .map(ExportDependencyFormat(_, options.scalaParams.getOrElse(None)))

    ScopedJsonProject(scalaCompilerPlugins = compilerPlugins)

  private def dependencySettings(options: BuildOptions): ScopedJsonProject = {
    val directDeps = options.classPathOptions.extraDependencies.toSeq.map(_.value)
      .map(ExportDependencyFormat(_, options.scalaParams.getOrElse(None)))

    ScopedJsonProject(dependencies = directDeps)
  }

  private def repositorySettings(options: BuildOptions): ScopedJsonProject = {
    val resolvers = options.finalRepositories
      .getOrElse(Nil)
      .appended(Repositories.central)
      .appended(LocalRepositories.ivy2Local)
      .collect {
        case repo: MavenRepository => repo.root
        case repo: IvyRepository   => s"ivy:${repo.pattern.string}"
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
      customJarsDecls = customCompileOnlyJarsDecls ++ customJarsDecls
    )
  }

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): JsonProject = {
    val baseJsonProject = Seq(
      JsonProject(
        projectName = projectName,
        mainClass = optionsMain.mainClass
      ),
      scalaVersionSettings(optionsMain),
      platformSettings(optionsMain)
    )
      .foldLeft(JsonProject())(_ + _)

    val mainJsonProject = exportScope(optionsMain, sourcesMain)
    val testJsonProject = exportScope(optionsTest, sourcesTest)

    baseJsonProject
      .withScope(Scope.Main.name, mainJsonProject)
      .withScope(Scope.Test.name, testJsonProject)
  }

  def exportScope(
    options: BuildOptions,
    sources: Sources
  ): ScopedJsonProject =
    Seq(
      scalaCompilerPlugins(options),
      scalacOptionsSettings(options),
      sourcesSettings(sources),
      dependencySettings(options),
      repositorySettings(options),
      customResourcesSettings(options),
      customJarsSettings(options)
    )
      .foldLeft(ScopedJsonProject())(_ + _)
      .sorted
}
