package scala.cli.exportCmd
import dependency.{AnyDependency, NoAttributes, ScalaNameAttributes}

import scala.annotation.unused
import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.ScalaOptions.*
import scala.build.options.{BuildOptions, ScalaOptions, Scope, ShadowingSeq}
import scala.build.{Artifacts, Logger, Positioned, Sources}
import scala.cli.ScalaCli
import scala.cli.exportCmd.POMBuilderHelper.*
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

  private def sources(sourcesMain: Sources, sourcesTest: Sources): MavenProject = {
    val mainSources = ProjectDescriptor.sources(sourcesMain)
    val testSources = ProjectDescriptor.sources(sourcesTest)
    MavenProject(
      mainSources = mainSources,
      testSources = testSources
    )
  }

  // todo: fill this - to be done in separate issue to reduce scope for maven export
  private def javaOptionsSettings(@unused options: BuildOptions): MavenProject =
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
    sources: Sources,
    toolchain: Artifacts.ScalaToolchain
  ): MavenProject = {

    val scalaV         = getScalaVersion(options)
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
          case _: ScalaNameAttributes => s"${name}_$getScalaPrefix"
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
        val mainDependenciesMaven = buildMavenDepModels(mainDeps, isCompileOnly)
        val testDependenciesMaven = buildMavenDepModels(testDeps, isCompileOnly)
        val resolvedDeps          = (mainDependenciesMaven ++ testDependenciesMaven).groupBy(k =>
          k.groupId + k.artifactId + k.version
        ).map { (_, list) =>
          val highestScope = MavenScopes.getHighestPriorityScope(list.map(_.scope))
          list.head.copy(scope = highestScope)
        }.toList

        val forkedModules = toolchain.providedModules.keySet.toSeq.sorted
        def isUpstreamToolchain(dep: MavenLibraryDependency) =
          dep.groupId.isDefaultOrg && forkedModules.contains(dep.artifactId)
        val upstreamToolchainExclusions =
          forkedModules.map(ScalaOptions.defaultOrganization -> _)
        val resolvedDepsWithExclusions = resolvedDeps
          .map: dep =>
            if isUpstreamToolchain(dep) then
              dep.copy(
                groupId = options.scalaOrganization,
                version = Artifacts.forkedVersionOf(toolchain, dep.artifactId)
                  .map(_.asString)
                  .getOrElse(scalaV)
              )
            else dep.copy(exclusions = upstreamToolchainExclusions)
          .distinct

        val scalaDep = if (!ProjectDescriptor.isPureJavaProject(options, sources)) {
          val scalaDep = if scalaV.startsWith("3") then "scala3-library_3" else "scala-library"
          val scalaCompilerDep =
            if scalaV.startsWith("3") then "scala3-compiler_3" else "scala-compiler"
          val scalaOrg = options.scalaOrganization
          List(
            MavenLibraryDependency(scalaOrg, scalaDep, scalaV, MavenScopes.Main),
            MavenLibraryDependency(scalaOrg, scalaCompilerDep, scalaV, MavenScopes.Main)
          )
        }
        else Nil

        resolvedDepsWithExclusions ++ scalaDep
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
    jdkVersion: String,
    sourcesMain: Sources
  ): MavenProject = {

    val pureJava = ProjectDescriptor.isPureJavaProject(options, sourcesMain)

    val javacOptions = javacOptionsSettings(options)

    val mavenJavaPlugin = buildJavaCompilerPlugin(javacOptions, jdkVersion)
    val mavenExecPlugin = buildJavaExecPlugin(jdkVersion)
    val scalaPlugin     = buildScalaPlugin(jdkVersion, options.customScalaOrganization)

    val reqdPlugins =
      if (pureJava) Seq(mavenJavaPlugin, mavenExecPlugin) else Seq(mavenJavaPlugin, scalaPlugin)

    MavenProject(
      plugins = reqdPlugins
    )
  }

  private def buildScalaPlugin(jdkVersion: String, scalaOrganization: Option[String]): MavenPlugin =
    val execElements =
      <executions>
        <execution>
          <goals>
            <goal>compile</goal>
            <goal>testCompile</goal>
          </goals>
        </execution>
      </executions>

    val configElements = scalaOrganization.toSeq.map: org =>
      <configuration>
        <scalaOrganization>{org}</scalaOrganization>
        <recompileMode>all</recompileMode>
      </configuration>

    MavenPlugin(
      "net.alchim31.maven",
      "scala-maven-plugin",
      mavenScalaPluginVersion,
      jdkVersion,
      execElements +: configElements
    )

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

    val sourceArg  = buildNode("source", jdkVersion)
    val targetArg  = buildNode("target", jdkVersion)
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
      Seq(configNode)
    )
  }

  private def buildJavaExecPlugin(jdkVersion: String): MavenPlugin =
    MavenPlugin(
      "org.codehaus.mojo",
      "exec-maven-plugin",
      mavenExecPluginVersion,
      jdkVersion,
      Seq(<configuration></configuration>)
    )

  private def customResourcesSettings(options: BuildOptions): MavenProject = {
    val resources = options.classPathOptions.resourcePaths.map { path =>
      if os.isFile(path) then
        MavenResource(
          directory = (path / os.up).toNIO.toAbsolutePath.toString,
          includes = Seq(path.last)
        )
      else
        MavenResource(directory = path.toNIO.toAbsolutePath.toString)
    }
    MavenProject(resources = resources)
  }

  def `export`(
    optionsMain: BuildOptions,
    optionsTest: BuildOptions,
    sourcesMain: Sources,
    sourcesTest: Sources
  ): Either[BuildException, MavenProject] = either {
    val jdk =
      optionsMain.javaOptions.jvmIdOpt.map(_.value)
        .getOrElse(Constants.defaultJavaVersion.toString)
    val toolchain = value {
      Artifacts.discoverToolchain(
        optionsMain.scalaOrganization,
        getScalaVersion(optionsMain),
        value(optionsMain.finalRepositories),
        logger,
        optionsMain.finalCache
      )
    }
    val projectChunks = Seq(
      sources(sourcesMain, sourcesTest),
      javaOptionsSettings(optionsMain),
      dependencySettings(optionsMain, optionsTest, Scope.Main, sourcesMain, toolchain),
      customResourcesSettings(optionsMain),
      plugins(optionsMain, jdk, sourcesMain),
      projectArtifactSettings(mavenAppGroupId, mavenAppArtifactId, mavenAppVersion)
    )
    projectChunks.foldLeft(MavenProject())(_ + _)
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
