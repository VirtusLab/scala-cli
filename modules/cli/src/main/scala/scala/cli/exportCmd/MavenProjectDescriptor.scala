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
import scala.cli.commands.export0.ExportOptions
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
  mavenExecPluginVersion: String,
  extraSettings: Seq[String],
  mavenAppGroupId: String,
  mavenAppArtifactId: String,
  mavenAppVersion: String,
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

  // todo: fill this - to be done in separate issue to reduce scope for maven export
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

    javacOptionsSettings.toList
  }

  private def projectArtifactSettings(
    mavenAppGroupId: String,
    mavenAppArtifactId: String,
    mavenAppVersion: String
  ): MavenProject =
    MavenProject(
      groupId = Some(mavenAppGroupId),
      artifactId = Some(mavenAppArtifactId),
      version = Some(mavenAppVersion)
    )

  private def dependencySettings(
    options: BuildOptions,
    testOptions: BuildOptions,
    scope: Scope,
    sources: Sources
  ): MavenProject = {

    val scalaV = getScalaVersion(options)
    def getScalaPrefix =
      if scalaV.startsWith("3") then "3"
      else if scalaV.startsWith("2.13") then "2.13"
      else "2.12"

    def buildMavenDepModels(
      mainDeps: ShadowingSeq[Positioned[AnyDependency]],
      isCompileOnly: Boolean
    ) =
      mainDeps.toSeq.toList.map(_.value).map { dep =>
        val org  = dep.organization
        val name = dep.name
        val ver  = dep.version
        // TODO dep.userParams
        // TODO dep.exclude
        // TODO dep.attributes
        val artNameWithPrefix = dep.nameAttributes match {
          case NoAttributes           => name
          case s: ScalaNameAttributes => s"${name}_$getScalaPrefix"
        }
        val scope0 =
          if (scope == Scope.Test) MavenScopes.Test
          else if (isCompileOnly) {
            System.err.println(
              s"Warning: Maven seems to support either test or provided, not both. So falling back to use Provided scope."
            )
            MavenScopes.Provided
          }
          else MavenScopes.Main

        MavenLibraryDependency(org, artNameWithPrefix, ver, scope0)
      }

    val depSettings = {
      def toDependencies(
        mainDeps: ShadowingSeq[Positioned[AnyDependency]],
        testDeps: ShadowingSeq[Positioned[AnyDependency]],
        isCompileOnly: Boolean
      ): Seq[MavenLibraryDependency] = {
        val scopePriorities       = List()
        val mainDependenciesMaven = buildMavenDepModels(mainDeps, isCompileOnly)
        val testDependenciesMaven = buildMavenDepModels(testDeps, isCompileOnly)
        val resolvedDeps = (mainDependenciesMaven ++ testDependenciesMaven).groupBy(k =>
          k.groupId + k.artifactId + k.version
        ).map { (_, list) =>
          val highestScope = MavenScopes.getHighestPriorityScope(list.map(_.scope))
          list.head.copy(scope = highestScope)
        }.toList

        val scalaDep = if (!ProjectDescriptor.isPureJavaProject(options, sources)) {
          val scalaDep = if scalaV.startsWith("3") then "scala3-library_3" else "scala-library"
          val scalaCompilerDep =
            if scalaV.startsWith("3") then "scala3-compiler_3" else "scala-compiler"
          List(
            MavenLibraryDependency("org.scala-lang", scalaDep, scalaV, MavenScopes.Main),
            MavenLibraryDependency("org.scala-lang", scalaCompilerDep, scalaV, MavenScopes.Main)
          )
        }
        else Nil

        resolvedDeps ++ scalaDep
      }

      toDependencies(
        options.classPathOptions.allExtraDependencies,
        testOptions.classPathOptions.allExtraDependencies,
        true
      )
    }

    MavenProject(
      dependencies = depSettings
    )
  }

  private def getScalaVersion(options: BuildOptions): String =
    options.scalaParams.toOption.flatten.map(_.scalaVersion).getOrElse(
      ScalaCli.getDefaultScalaVersion
    )

  private def plugins(
    options: BuildOptions,
    scope: Scope,
    jdkVersion: String,
    sourcesMain: Sources
  ): MavenProject = {

    val pureJava = ProjectDescriptor.isPureJavaProject(options, sourcesMain)

    val javacOptions = javacOptionsSettings(options)

    val javaOptions = javaOptionsSettings(options)

    val mavenJavaPlugin = buildJavaCompilerPlugin(javacOptions, jdkVersion)
    val mavenExecPlugin = buildJavaExecPlugin(javacOptions, jdkVersion)
    val scalaPlugin     = buildScalaPlugin(javacOptions, jdkVersion, getScalaVersion(options))

    val reqdPlugins =
      if (pureJava) Seq(mavenJavaPlugin, mavenExecPlugin) else Seq(mavenJavaPlugin, scalaPlugin)

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
            <goal>testCompile</goal>
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

  private def buildJavaExecPlugin(
    javacOptions: Seq[String],
    jdkVersion: String
  ): MavenPlugin =
    MavenPlugin(
      "org.codehaus.mojo",
      "exec-maven-plugin",
      mavenExecPluginVersion,
      jdkVersion,
      <configuration></configuration>
    )

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
    val jdk =
      optionsMain.javaOptions.jvmIdOpt.map(_.value)
        .getOrElse(Constants.defaultJavaVersion.toString)
    val projectChunks = Seq(
      sources(sourcesMain, sourcesTest),
      javaOptionsSettings(optionsMain),
      dependencySettings(optionsMain, optionsTest, Scope.Main, sourcesMain),
      customResourcesSettings(optionsMain),
      plugins(optionsMain, Scope.Main, jdk, sourcesMain),
      projectArtifactSettings(mavenAppGroupId, mavenAppArtifactId, mavenAppVersion)
    )
    Right(projectChunks.foldLeft(MavenProject())(_ + _))
  }

}

enum MavenScopes(val priority: Int, val name: String) {
  case Main     extends MavenScopes(1, "main")
  case Test     extends MavenScopes(2, "test")
  case Provided extends MavenScopes(3, "provided")
}

object MavenScopes {
  def getHighestPriorityScope(scopes: Seq[MavenScopes]): MavenScopes =
    // if scope is empty return Main Scope, depending on priority, with 1 being highest
    scopes.minByOption(_.priority).getOrElse(Main)
}
