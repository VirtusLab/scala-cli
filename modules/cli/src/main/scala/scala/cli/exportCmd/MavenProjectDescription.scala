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
import scala.cli.exportCmd.POMBuilderHelper._
import scala.xml.Elem

object POMBuilderHelper {
  def buildNode(name: String, value: String): Elem =
    new Elem(
      null,
      name,
      scala.xml.Null,
      scala.xml.TopScope,
      minimizeEmpty = false,
      scala.xml.Text(value)
    )
}

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

  private def projectArtifactSettings(): MavenProject =
    // todo: Is it really needed to configure the values, or always use the default?
    MavenProject(
      groupId = None,
      artifactId = None,
      version = None
    )

  private def dependencySettings(
    options: BuildOptions,
    scope: Scope,
    sources: Sources
  ): MavenProject = {

    val depSettings = {
      def toDependencies(
        deps: ShadowingSeq[Positioned[AnyDependency]],
        isCompileOnly: Boolean
      ): Seq[MavenLibraryDependency] = {
        val providedDeps = deps.toSeq.toList.map(_.value).map { dep =>
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

        val scalaDep = if (!ProjectDescriptor.isPureJavaProject(options, sources)) {
          // add scala dependency
          // todo: get scala version from directive
          val scalaDep = if true then "scala3-library_3" else "scala-library"
          List(MavenLibraryDependency("org.scala-lang", scalaDep, "${scala.version}"))
        }
        else Nil

        providedDeps ++ scalaDep
      }

      toDependencies(options.classPathOptions.extraCompileOnlyDependencies, true)
    }

    MavenProject(
      dependencies = depSettings
    )
  }

  private def plugins(
    options: BuildOptions,
    scope: Scope,
    jdkVersion: String,
    sourcesMain: Sources
  ): MavenProject = {

    // todo: use this method from mill and sbt projects as well
    val pureJava = ProjectDescriptor.isPureJavaProject(options, sourcesMain)

    val javacOptions = javacOptionsSettings(options)

    val mavenJavaPlugin = buildJavaCompilerPlugin(javacOptions, jdkVersion)
    val scalaPlugin     = buildScalaPlugin(javacOptions, jdkVersion)

    val reqdPlugins = if (pureJava) Seq(mavenJavaPlugin) else Seq(scalaPlugin)

    MavenProject(
      plugins = reqdPlugins
    )
  }

  private def buildScalaPlugin(javacOptions: Seq[String], jdkVersion: String): MavenPlugin = {

    val compileMode = buildNode("recompileMode", "incremental")
    val scalaVersion =
      buildNode("scalaVersion", "${scala.version}") // todo: set this value in properties
    val javacOptionsElem = {
      val opts = javacOptions.map { opt =>
        buildNode("javacArg", opt)
      }
      <javacArgs>
        {opts}
      </javacArgs>
    }

    val configurationElements = Seq(compileMode, /*scalaVersion,*/ javacOptionsElem)

    MavenPlugin(
      "net.alchim31.maven",
      "scala-maven-plugin",
      "4.9.1",
      jdkVersion,
      configurationElements
    )
  }

  private def buildJavaCompilerPlugin(
    javacOptions: Seq[String],
    jdkVersion: String
  ): MavenPlugin = {
    val javacOptionsElem = {
      val opts = javacOptions.map { opt =>
        buildNode("arg", opt)
      }
      <compilerArgs>
        {opts}
      </compilerArgs>
    }

    val sourceArg = buildNode("source", jdkVersion)
    val targetArg = buildNode("target", jdkVersion)

    MavenPlugin(
      "org.apache.maven.plugins",
      "maven-compiler-plugin",
      "3.8.1",
      jdkVersion,
      Seq(javacOptionsElem, sourceArg, targetArg)
    )
  }

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): Either[BuildException, MavenProject] = {
    val jdk = optionsMain.javaOptions.jvmIdOpt.map(_.value).getOrElse("17")
    val projectChunks = Seq(
      sources(sourcesMain, sourcesTest),
      javaOptionsSettings(optionsMain),
      dependencySettings(optionsMain, Scope.Main, sourcesMain),
      plugins(optionsMain, Scope.Main, jdk, sourcesMain),
      projectArtifactSettings()
    )
    Right(projectChunks.foldLeft(MavenProject())(_ + _))
  }

}
