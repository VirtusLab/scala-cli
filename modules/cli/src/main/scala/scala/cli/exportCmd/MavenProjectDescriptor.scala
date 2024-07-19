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
import scala.cli.exportCmd.POMBuilderHelper.*
import scala.xml.{Elem, XML}

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

final case class MavenProjectDescriptor(
  mavenPluginVersion: String,
  mavenScalaPluginVersion: String,
  extraSettings: Seq[String],
  logger: Logger
) extends ProjectDescriptor {
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

  // todo: fill this
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

    val scalaV = getScalaVersion(options)
    def getScalaMajorPrefix =
      scalaV match {
        case s"2.12.${patch}" => "2.12"
        case s"2.13.${patch}" => "2.13"
        case s"3.$x.$y"       => "3"
      }

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
          val artNameWithPrefix = dep.nameAttributes match {
            case NoAttributes           => name
            case s: ScalaNameAttributes => s"${name}_$getScalaMajorPrefix"
          }
          val scope0 =
            if (scope == Scope.Test) Some("test")
            else if (isCompileOnly)
              Some("provided") // maven seems to support either test or provided, not both
            else None

          MavenLibraryDependency(org, artNameWithPrefix, ver, scope0)
        }

        val scalaDep = if (!ProjectDescriptor.isPureJavaProject(options, sources)) {
          val scalaDep = if scalaV.startsWith("3") then "scala3-library_3" else "scala-library"
          val scalaCompilerDep =
            if scalaV.startsWith("3") then "scala3-compiler_3" else "scala-compiler"
          List(
            MavenLibraryDependency("org.scala-lang", scalaDep, scalaV),
            MavenLibraryDependency("org.scala-lang", scalaCompilerDep, scalaV)
          )
        }
        else Nil

        providedDeps ++ scalaDep
      }

      toDependencies(options.classPathOptions.allExtraDependencies, true)
    }

    MavenProject(
      dependencies = depSettings
    )
  }

  private def getScalaVersion(options: BuildOptions): String =
    options.scalaOptions.scalaVersion
      .flatMap(_.versionOpt)
      .getOrElse(ScalaCli.getDefaultScalaVersion)

  private def plugins(
    options: BuildOptions,
    scope: Scope,
    jdkVersion: String,
    sourcesMain: Sources
  ): MavenProject = {

    // todo: use this method from mill and sbt projects as well
    val pureJava = ProjectDescriptor.isPureJavaProject(options, sourcesMain)

    val javacOptions = javacOptionsSettings(options)
    // todo: set this option correctly in pom
    val javaOptions = javaOptionsSettings(options)

    val mavenJavaPlugin = buildJavaCompilerPlugin(javacOptions, jdkVersion)
    val scalaPlugin     = buildScalaPlugin(javacOptions, jdkVersion, getScalaVersion(options))

    val reqdPlugins = if (pureJava) Seq(mavenJavaPlugin) else Seq(mavenJavaPlugin, scalaPlugin)

    MavenProject(
      plugins = reqdPlugins
    )
  }

  private def buildScalaPlugin(
    javacOptions: Seq[String],
    jdkVersion: String,
    scalaVersion: String
  ): MavenPlugin = {

    val scalaVersionNode = buildNode("scalaVersion", scalaVersion)
    val javacOptionsElem = {
      val opts = javacOptions.map { opt =>
        buildNode("javacArg", opt)
      }
      <javacArgs>
        {opts}
      </javacArgs>
    }

    val execElements =
      <executions>
        <execution>
          <goals>
            <goal>compile</goal>
          </goals>
        </execution>
      </executions>

    MavenPlugin(
      "net.alchim31.maven",
      "scala-maven-plugin",
      mavenScalaPluginVersion,
      jdkVersion,
      execElements
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
    val configNode =
      <configuration>
        {javacOptionsElem}
        {sourceArg}
        {targetArg}
      </configuration>

    MavenPlugin(
      "org.apache.maven.plugins",
      "maven-compiler-plugin",
      mavenPluginVersion,
      jdkVersion,
      configNode
    )
  }

  private def customResourcesSettings(options: BuildOptions): MavenProject = {
    val resourceDirs =
      if (options.classPathOptions.resourcesDir.isEmpty) Nil
      else
        options.classPathOptions.resourcesDir.map(_.toNIO.toAbsolutePath.toString)
    MavenProject(
      resourceDirectories = resourceDirs
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
      customResourcesSettings(optionsMain),
      plugins(optionsMain, Scope.Main, jdk, sourcesMain),
      projectArtifactSettings()
    )
    Right(projectChunks.foldLeft(MavenProject())(_ + _))
  }

}
