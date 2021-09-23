package scala.cli.export

import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.build.Sources

import java.nio.charset.StandardCharsets

import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.parse.RepositoryParser
import dependency.{NoAttributes, ScalaNameAttributes}

final case class Sbt(sbtVersion: String) {
  def export(options: BuildOptions, sources: Sources): SbtProject = {
    val q       = "\""
    val nl      = System.lineSeparator()
    val charset = StandardCharsets.UTF_8

    val mainSources = sources.paths.map {
      case (path, relPath) =>
        val language =
          if (path.last.endsWith(".java")) "java"
          else "scala" // FIXME Others
        // FIXME asSubPath might throw… Make it a SubPath earlier in the API?
        (relPath.asSubPath, language, os.read.bytes(path))
    }

    val extraMainSources = sources.inMemory.map {
      case (_, relPath, content, _) =>
        val language =
          if (relPath.last.endsWith(".java")) "java"
          else "scala"
        (relPath.asSubPath, language, content.getBytes(charset))
    }

    // TODO Handle Scala CLI cross-builds

    // TODO Detect pure Java projects?

    val (plugins, pluginSettings) =
      if (options.scalaJsOptions.enable)
        Seq(
          """"org.scala-js" % "sbt-scalajs" % "1.7.0""""
        ) -> Seq(
          "enablePlugins(ScalaJSPlugin)",
          "scalaJSUseMainModuleInitializer := true"
        )
      else if (options.scalaNativeOptions.enable)
        Seq(
          """"org.scala-native" % "sbt-scala-native" % "0.4.0""""
        ) -> Seq(
          "enablePlugins(ScalaNativePlugin)"
        )
      else
        Nil -> Nil

    val scalaVerSetting = {
      val sv = options.scalaOptions.scalaVersion.getOrElse(Constants.defaultScalaVersion)
      s"""scalaVersion := "$sv""""
    }

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

    val customJarsSettings =
      if (options.classPathOptions.extraCompileOnlyJars.isEmpty) Nil
      else {
        val jars = options.classPathOptions.extraCompileOnlyJars.map(p => s"""file("$p")""")
        Seq(s"""Compile / unmanagedClasspath ++= Seq(${jars.mkString(", ")})""")
      }

    // TODO options.classPathOptions.extraJars

    // TODO options.javaOptions.javaOpts
    // TODO options.scalaJsOptions.*
    // TODO options.scalaNativeOptions.*

    // TODO options.scalaOptions.addScalaLibrary

    val mainClassOptions = options.mainClass match {
      case None => Nil
      case Some(mainClass) =>
        Seq(s"""Compile / mainClass := Some("$mainClass")""")
    }

    val scalacOptionsSettings =
      if (options.scalaOptions.scalacOptions.isEmpty) Nil
      else {
        val options0 = options
          .scalaOptions
          .scalacOptions
          .map(o => "\"" + o.replace("\"", "\\\"") + "\"")
        Seq(s"""scalacOptions ++= Seq(${options0.mkString(", ")})""")
      }

    // TODO options.testOptions.frameworkOpt

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

    val settings =
      Seq(
        pluginSettings,
        Seq(scalaVerSetting),
        mainClassOptions,
        scalacOptionsSettings,
        repoSettings,
        depSettings,
        customJarsSettings
      )

    SbtProject(
      plugins,
      settings,
      "1.5.5",
      mainSources ++ extraMainSources,
      Nil
    )
  }
}
