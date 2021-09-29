package scala.cli.export

import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.parse.RepositoryParser
import dependency.{NoAttributes, ScalaNameAttributes}

import java.nio.charset.StandardCharsets

import scala.build.Sources
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, ScalaJsOptions, ScalaNativeOptions}

final case class Sbt(
  sbtVersion: String,
  extraSettings: Seq[String]
) extends BuildTool {
  private val charSet = StandardCharsets.UTF_8
  private val q       = "\""
  private val nl      = System.lineSeparator()

  private def mainSources(sources: Sources): SbtProject = {
    val allSources = BuildTool.sources(sources, charSet)
    SbtProject(
      mainSources = allSources,
      settings = Seq(
        // Using main sources as test sources too, so that their test suites
        // are run too.
        Seq(
          "// Scala CLI doesn't distinguish main and test sources for now.",
          "Test / sources ++= (Compile / sources).value"
        )
      )
    )
  }

  private def sbtVersionProject: SbtProject =
    SbtProject(sbtVersion = Some(sbtVersion))

  private def pureJavaSettings(options: BuildOptions, sources: Sources): SbtProject = {

    val pureJava = !options.scalaOptions.addScalaLibrary.contains(true) &&
      sources.paths.forall(_._1.last.endsWith(".java")) &&
      sources.inMemory.forall(_._2.last.endsWith(".java")) &&
      options.classPathOptions.extraDependencies.forall(_.nameAttributes == NoAttributes)

    val settings =
      if (pureJava)
        Seq(
          "crossPaths := false",
          "autoScalaLibrary := false"
        )
      else if (options.scalaOptions.addScalaLibrary.getOrElse(true))
        Nil
      else
        Seq(
          "autoScalaLibrary := false"
        )

    SbtProject(settings = Seq(settings))
  }

  private def scalaJsSettings(options: ScalaJsOptions): SbtProject = {

    val plugins = Seq(
      s""""org.scala-js" % "sbt-scalajs" % "${options.finalVersion}""""
    )
    val pluginSettings = Seq(
      "enablePlugins(ScalaJSPlugin)",
      "scalaJSUseMainModuleInitializer := true"
    )

    val linkerConfigCalls = BuildTool.scalaJsLinkerCalls(options)
    val linkerConfigSettings =
      if (linkerConfigCalls.isEmpty) Nil
      else
        Seq(s"""scalaJSLinkerConfig ~= { _${linkerConfigCalls.mkString} }""")

    // TODO options.dom

    SbtProject(
      plugins = plugins,
      settings = Seq(pluginSettings, linkerConfigSettings)
    )
  }

  private def scalaNativeSettings(options: ScalaNativeOptions): SbtProject = {

    val plugins = Seq(
      s""""org.scala-native" % "sbt-scala-native" % "${options.finalVersion}""""
    )
    val pluginSettings = Seq(
      "enablePlugins(ScalaNativePlugin)"
    )

    val configCalls = {
      var calls = Seq.empty[String]
      calls
    }

    val (configImports, configSettings) =
      if (configCalls.isEmpty) ("", Nil)
      else
        (
          "import scala.scalanative.build._",
          Seq(s"""nativeConfig ~= { _${configCalls.mkString} }""")
        )

    SbtProject(
      plugins = plugins,
      settings = Seq(pluginSettings, configSettings),
      imports = Seq(configImports)
    )
  }

  private def scalaVersionSettings(options: BuildOptions): SbtProject = {

    val scalaVerSetting = {
      val sv = options.scalaOptions.scalaVersion.getOrElse(Constants.defaultScalaVersion)
      s"""scalaVersion := "$sv""""
    }

    SbtProject(
      settings = Seq(Seq(scalaVerSetting))
    )
  }

  private def repositorySettings(options: BuildOptions): SbtProject = {

    val repoSettings =
      if (options.classPathOptions.extraRepositories.isEmpty) Nil
      else {
        val repos = options.classPathOptions
          .extraRepositories
          .map(repo => (repo, RepositoryParser.repository(repo)))
          .zipWithIndex
          .map {
            case ((_, Right(repo: IvyRepository)), idx) =>
              // TODO repo.authentication?
              // TODO repo.metadataPatternOpt
              s"""Resolver.url("repo-$idx") artifacts "${repo.pattern.string}""""
            case ((_, Right(repo: MavenRepository)), idx) =>
              // TODO repo.authentication?
              s""""repo-$idx" at "${repo.root}""""
            case _ =>
              ???
          }
        Seq(s"""resolvers ++= Seq(${repos.mkString(", ")})""")
      }

    SbtProject(
      settings = Seq(repoSettings)
    )
  }

  private def customJarsSettings(options: BuildOptions): SbtProject = {

    val customCompileOnlyJarsSettings =
      if (options.classPathOptions.extraCompileOnlyJars.isEmpty) Nil
      else {
        val jars = options.classPathOptions.extraCompileOnlyJars.map(p => s"""file("$p")""")
        Seq(s"""Compile / unmanagedClasspath ++= Seq(${jars.mkString(", ")})""")
      }

    val customJarsSettings =
      if (options.classPathOptions.extraJars.isEmpty) Nil
      else {
        val jars = options.classPathOptions.extraJars.map(p => s"""file("$p")""")
        Seq(
          s"""Compile / unmanagedClasspath ++= Seq(${jars.mkString(", ")})""",
          s"""Runtime / unmanagedClasspath ++= Seq(${jars.mkString(", ")})"""
        )
      }

    SbtProject(
      settings = Seq(customCompileOnlyJarsSettings, customJarsSettings)
    )
  }

  private def javaOptionsSettings(options: BuildOptions): SbtProject = {

    val javaOptionsSettings =
      if (options.javaOptions.javaOpts.isEmpty) Nil
      else
        Seq(
          "run / javaOptions ++= Seq(" + nl +
            options.javaOptions.javaOpts.map(opt => "  \"" + opt + "\"," + nl).mkString +
            ")"
        )

    SbtProject(
      settings = Seq(javaOptionsSettings)
    )
  }

  private def mainClassSettings(options: BuildOptions): SbtProject = {

    val mainClassOptions = options.mainClass match {
      case None => Nil
      case Some(mainClass) =>
        Seq(s"""Compile / mainClass := Some("$mainClass")""")
    }

    SbtProject(
      settings = Seq(mainClassOptions)
    )
  }

  private def scalacOptionsSettings(options: BuildOptions): SbtProject = {

    val scalacOptionsSettings =
      if (options.scalaOptions.scalacOptions.isEmpty) Nil
      else {
        val options0 = options
          .scalaOptions
          .scalacOptions
          .map(o => "\"" + o.replace("\"", "\\\"") + "\"")
        Seq(s"""scalacOptions ++= Seq(${options0.mkString(", ")})""")
      }

    SbtProject(
      settings = Seq(scalacOptionsSettings)
    )
  }

  private def testFrameworkSettings(options: BuildOptions): SbtProject = {

    val testFrameworkSettings = options.testOptions.frameworkOpt match {
      case None => Nil
      case Some(fw) =>
        Seq(s"""testFrameworks += new TestFramework("$fw")""")
    }

    SbtProject(
      settings = Seq(testFrameworkSettings)
    )
  }

  private def dependencySettings(options: BuildOptions): SbtProject = {

    val depSettings = {
      val depStrings = options.classPathOptions
        .extraDependencies
        .map { dep =>
          val org  = dep.organization
          val name = dep.name
          val ver  = dep.version
          // TODO dep.userParams
          // TODO dep.exclude
          // TODO dep.attributes
          val (sep, suffixOpt) = dep.nameAttributes match {
            case NoAttributes => ("%", None)
            case s: ScalaNameAttributes =>
              val suffixOpt0 =
                if (s.fullCrossVersion.getOrElse(false)) Some(".cross(CrossVersion.full)")
                else None
              val sep =
                if (s.platform.getOrElse(false)) "%%%"
                else "%%"
              (sep, suffixOpt0)
          }

          val baseDep = s"""$q$org$q $sep $q$name$q % $q$ver$q"""
          suffixOpt.fold(baseDep)(suffix => s"($baseDep)$suffix")
        }

      if (depStrings.isEmpty) Nil
      else if (depStrings.lengthCompare(1) == 0)
        Seq(s"""libraryDependencies += ${depStrings.head}""")
      else {
        val count = depStrings.length
        val allDeps = depStrings
          .iterator
          .zipWithIndex
          .map {
            case (dep, idx) =>
              val maybeComma = if (idx == count - 1) "" else ","
              "  " + dep + maybeComma + nl
          }
          .mkString
        Seq(s"""libraryDependencies ++= Seq($nl$allDeps)""")
      }
    }

    SbtProject(
      settings = Seq(depSettings)
    )
  }

  def export(options: BuildOptions, sources: Sources): SbtProject = {

    // TODO Handle Scala CLI cross-builds

    val projectChunks = Seq(
      SbtProject(settings = Seq(extraSettings)),
      mainSources(sources),
      sbtVersionProject,
      scalaVersionSettings(options),
      scalacOptionsSettings(options),
      mainClassSettings(options),
      pureJavaSettings(options, sources),
      javaOptionsSettings(options),
      if (options.scalaJsOptions.enable)
        scalaJsSettings(options.scalaJsOptions)
      else
        SbtProject(),
      if (options.scalaNativeOptions.enable)
        scalaNativeSettings(options.scalaNativeOptions)
      else
        SbtProject(),
      customJarsSettings(options),
      testFrameworkSettings(options),
      repositorySettings(options),
      dependencySettings(options)
    )

    projectChunks.foldLeft(SbtProject())(_ + _)
  }
}
