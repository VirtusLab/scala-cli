package scala.cli.export

import coursier.maven.MavenRepository
import coursier.parse.RepositoryParser
import dependency.NoAttributes

import java.nio.charset.StandardCharsets

import scala.build.Sources
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, ScalaJsOptions, ScalaNativeOptions}

final case class Mill(
  millVersion: String,
  launchers: Seq[(os.RelPath, Array[Byte])]
) extends BuildTool {

  private val charSet = StandardCharsets.UTF_8

  private def sourcesSettings(sources: Sources): MillProject = {
    val allSources = BuildTool.sources(sources, charSet)
    MillProject(mainSources = allSources)
  }

  private def scalaVersionSettings(options: BuildOptions, sources: Sources): MillProject = {

    val pureJava = !options.scalaOptions.addScalaLibrary.contains(true) &&
      sources.paths.forall(_._1.last.endsWith(".java")) &&
      sources.inMemory.forall(_._2.last.endsWith(".java")) &&
      options.classPathOptions.extraDependencies.forall(_.nameAttributes == NoAttributes)

    val sv = options.scalaOptions.scalaVersion.getOrElse(Constants.defaultScalaVersion)

    if (pureJava)
      MillProject()
    else
      MillProject(scalaVersion = Some(sv))
  }

  private def scalaJsSettings(options: ScalaJsOptions): MillProject = {

    val scalaJsVersion = Some(options.version.getOrElse(Constants.scalaJsVersion))

    val moduleKindDecls =
      if (options.moduleKindStr.isEmpty) Nil
      else
        Seq(s"""def moduleKind = ModuleKind.${options.moduleKind}""")

    MillProject(
      scalaJsVersion = scalaJsVersion,
      extraDecls = moduleKindDecls
    )
  }

  private def scalaNativeSettings(options: ScalaNativeOptions): MillProject = {
    val scalaNativeVersion = Some(options.version.getOrElse(Constants.scalaNativeVersion))
    MillProject(scalaNativeVersion = scalaNativeVersion)
  }

  private def dependencySettings(options: BuildOptions): MillProject = {
    val deps = options.classPathOptions.extraDependencies.map(_.render)
    MillProject(deps = deps)
  }

  private def repositorySettings(options: BuildOptions): MillProject = {

    val repoDecls =
      if (options.classPathOptions.extraRepositories.isEmpty) Nil
      else {
        val repos = options.classPathOptions
          .extraRepositories
          .map(repo => (repo, RepositoryParser.repository(repo)))
          .zipWithIndex
          .map {
            case ((repoStr, Right(repo: MavenRepository)), idx) =>
              // TODO repo.authentication?
              s"""coursier.maven.MavenRepository("${repo.root}")"""
            case _ =>
              ???
          }
        Seq(s"""def repositories = super.repositories ++ Seq(${repos.mkString(", ")})""")
      }

    MillProject(
      extraDecls = repoDecls
    )
  }

  private def customJarsSettings(options: BuildOptions): MillProject = {

    val customCompileOnlyJarsDecls =
      if (options.classPathOptions.extraCompileOnlyJars.isEmpty) Nil
      else {
        val jars =
          options.classPathOptions.extraCompileOnlyJars.map(p => s"""PathRef(os.Path("$p"))""")
        Seq(s"""def compileClasspath = super.compileClasspath() ++ Seq(${jars.mkString(", ")})""")
      }

    val customJarsDecls =
      if (options.classPathOptions.extraClassPath.isEmpty) Nil
      else {
        val jars = options.classPathOptions.extraClassPath.map(p => s"""PathRef(os.Path("$p"))""")
        Seq(
          s"""def unmanagedClasspath = super.unmanagedClasspath() ++ Seq(${jars.mkString(", ")})"""
        )
      }

    MillProject(
      extraDecls = customCompileOnlyJarsDecls ++ customJarsDecls
    )
  }

  private def testFrameworkSettings(options: BuildOptions): MillProject = {

    val testFrameworkDecls = options.testOptions.frameworkOpt match {
      case None => Nil
      case Some(fw) =>
        Seq(s"""def testFramework = "$fw"""")
    }

    MillProject(
      extraTestDecls = testFrameworkDecls
    )
  }

  def export(options: BuildOptions, sources: Sources): MillProject = {

    // FIXME Put a sensible value in MillProject.nameOpt

    val baseSettings = MillProject(
      millVersion = Some(millVersion),
      launchers = launchers,
      mainClass = options.mainClass
    )

    val settings = Seq(
      baseSettings,
      sourcesSettings(sources),
      scalaVersionSettings(options, sources),
      dependencySettings(options),
      repositorySettings(options),
      if (options.scalaJsOptions.enable) scalaJsSettings(options.scalaJsOptions) else MillProject(),
      if (options.scalaNativeOptions.enable) scalaNativeSettings(options.scalaNativeOptions)
      else MillProject(),
      customJarsSettings(options),
      testFrameworkSettings(options)
    )

    settings.foldLeft(MillProject())(_ + _)
  }
}
