package scala.cli.exportCmd

import coursier.ivy.IvyRepository
import coursier.maven.MavenRepository
import coursier.parse.RepositoryParser
import dependency.{AnyDependency, NoAttributes, ScalaNameAttributes}

import java.nio.charset.StandardCharsets
import java.nio.file.Path

import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.internal.Runner.frameworkName
import scala.build.options.{BuildOptions, Platform, Scope, ShadowingSeq}
import scala.build.testrunner.AsmTestRunner
import scala.build.{Logger, Positioned, Sources}
import scala.cli.ScalaCli

final case class MavenProjectDescription(extraSettings: Seq[String], logger: Logger)
    extends ProjectDescriptor {
  private val q  = "\""
  private val nl = System.lineSeparator()

  private def sources(sourcesMain: Sources, sourcesTest: Sources): MavenProject = {
    val mainSources = ProjectDescriptor.sources(sourcesMain)
    val testSources = ProjectDescriptor.sources(sourcesTest)
    MavenProject(
      mainSources = mainSources,
      testSources = testSources
    )
  }

  private def javaOptionsSettings(options: BuildOptions): MavenProject =
    MavenProject(
      settings = Nil
    )

  private def javacOptionsSettings(options: BuildOptions): List[String] = {

    val javacOptionsSettings =
      if (options.javaOptions.javacOptions.toSeq.isEmpty) Nil
      else {
        val options0 = options
          .javaOptions
          .javacOptions
          .toSeq
          .map(_.value)
          .map(o => "\"" + o.replace("\"", "\\\"") + "\"")
        options0
      }

    // MavenProject(
    //   settings = Seq(javacOptionsSettings)
    // )
    javacOptionsSettings.toList
  }

  private def dependencySettings(options: BuildOptions, scope: Scope): MavenProject = {

    val depSettings = {
      def toDependencies(deps: ShadowingSeq[Positioned[AnyDependency]], isCompileOnly: Boolean) =
        deps.toSeq.toList.map(_.value).map { dep =>
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
              val sep = "%%"
              (sep, suffixOpt0)
          }
          val scope0 =
            // FIXME This ignores the isCompileOnly when scope == Scope.Test
            if (scope == Scope.Test) "test"
            else if (isCompileOnly) "% provided"
            else ""

          MavenLibraryDependency(org, name, ver, scope.name)
        }
      toDependencies(options.classPathOptions.extraCompileOnlyDependencies, true)
    }

    MavenProject(
      dependencies = depSettings
    )
  }

  private def plugins(options: BuildOptions, scope: Scope, jdkVersion: String): MavenProject = {
    val mavenPlugins = MavenPlugin(
      "org.apache.maven.plugins",
      "maven-compiler-plugin",
      "3.8.1",
      javacOptionsSettings(options),
      jdkVersion
    )
    MavenProject(
      plugins = Seq(mavenPlugins)
    )
  }

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): Either[BuildException, MavenProject] = {
    val projectChunks = Seq(
      sources(sourcesMain, sourcesTest),
      javaOptionsSettings(optionsMain),
      dependencySettings(optionsMain, Scope.Main),
      plugins(optionsMain, Scope.Main, "17") //todo How to get the jdk version from directive
    )
    Right(projectChunks.foldLeft(MavenProject())(_ + _))
  }

}
