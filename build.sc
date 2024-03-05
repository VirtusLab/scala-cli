import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import $ivy.`io.get-coursier::coursier-launcher:2.1.9`
import $ivy.`io.github.alexarchambault.mill::mill-native-image-upload:0.1.25`
import $file.project.deps, deps.{Deps, Docker, InternalDeps, Scala, TestDeps}
import $file.project.publish, publish.{ghOrg, ghName, ScalaCliPublishModule, organization}
import $file.project.settings, settings.{
  CliLaunchers,
  FormatNativeImageConf,
  HasTests,
  LocalRepo,
  PublishLocalNoFluff,
  ScalaCliCrossSbtModule,
  ScalaCliSbtModule,
  ScalaCliScalafixModule,
  localRepoResourcePath,
  platformExecutableJarExtension,
  workspaceDirName,
  projectFileName,
  jvmPropertiesFileName
}
import $file.project.deps, deps.customRepositories
import $file.project.website

import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale

import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.github.alexarchambault.millnativeimage.upload.Upload
import mill._, scalalib.{publish => _, _}
import mill.contrib.bloop.Bloop

import _root_.scala.util.{Properties, Using}

// Tell mill modules are under modules/
implicit def millModuleBasePath: define.Ctx.BasePath =
  define.Ctx.BasePath(super.millModuleBasePath.value / "modules")

object cli extends Cross[Cli](Scala.scala3MainVersions) with CrossScalaDefaultToInternal

trait CrossScalaDefaultToInternal { _: mill.define.Cross[_] =>
  def defaultCrossSegments = Seq(Scala.defaultInternal)
}

// Publish a bootstrapped, executable jar for a restricted environments
object cliBootstrapped extends ScalaCliPublishModule {
  override def unmanagedClasspath = T(cli(Scala.defaultInternal).nativeImageClassPath())
  override def jar                = assembly()

  import mill.scalalib.Assembly

  override def mainClass = Some("scala.cli.ScalaCli")

  override def assemblyRules = Seq(
    Assembly.Rule.ExcludePattern(".*\\.tasty"),
    Assembly.Rule.ExcludePattern(".*\\.semanticdb")
  ) ++ super.assemblyRules

  override def resources = T.sources {
    super.resources() ++ Seq(propertiesFilesResources())
  }

  def propertiesFilesResources = T.persistent {
    val dir = T.dest / "resources"

    val dest    = dir / "java-properties" / "scala-cli-properties"
    val content = "scala-cli.kind=jvm.bootstrapped"
    os.write.over(dest, content, createFolders = true)
    PathRef(dir)
  }
}

object `specification-level` extends Cross[SpecificationLevel](Scala.all)
    with CrossScalaDefaultToInternal
object `build-macros` extends Cross[BuildMacros](Scala.scala3MainVersions)
    with CrossScalaDefaultToInternal
object config     extends Cross[Config](Scala.all) with CrossScalaDefaultToInternal
object options    extends Cross[Options](Scala.scala3MainVersions) with CrossScalaDefaultToInternal
object directives extends Cross[Directives](Scala.scala3MainVersions)
    with CrossScalaDefaultToInternal
object core           extends Cross[Core](Scala.scala3MainVersions) with CrossScalaDefaultToInternal
object `build-module` extends Cross[Build](Scala.scala3MainVersions)
    with CrossScalaDefaultToInternal
object runner extends Cross[Runner](Scala.runnerScalaVersions) with CrossScalaDefaultToInternal
object `test-runner` extends Cross[TestRunner](Scala.testRunnerScalaVersions)
    with CrossScalaDefaultToInternal
object `tasty-lib` extends Cross[TastyLib](Scala.all) with CrossScalaDefaultToInternal
// Runtime classes used within native image on Scala 3 replacing runtime from Scala
object `scala3-runtime` extends Cross[Scala3Runtime](Scala.scala3MainVersions)
    with CrossScalaDefaultToInternal
// Logic to process classes that is shared between build and the scala-cli itself
object `scala3-graal` extends Cross[Scala3Graal](Scala.mainVersions)
    with CrossScalaDefaultToInternal
// Main app used to process classpath within build itself
object `scala3-graal-processor` extends Cross[Scala3GraalProcessor](Scala.scala3MainVersions)
    with CrossScalaDefaultToInternal

object `scala-cli-bsp` extends JavaModule with ScalaCliPublishModule {
  def ivyDeps = super.ivyDeps() ++ Seq(
    Deps.bsp4j
  )
  def javacOptions = T {
    super.javacOptions() ++ Seq("-target", "8", "-source", "8")
  }
}
object integration extends CliIntegration {
  object test extends IntegrationScalaTests {
    def ivyDeps = super.ivyDeps() ++ Seq(
      Deps.jgit
    )
  }
  object docker extends CliIntegrationDocker {
    object test extends ScalaCliTests {
      def sources = T.sources {
        super.sources() ++ integration.sources()
      }
      def tmpDirBase = T.persistent {
        PathRef(T.dest / "working-dir")
      }
      def forkEnv = super.forkEnv() ++ Seq(
        "SCALA_CLI_TMP"                -> tmpDirBase().path.toString,
        "SCALA_CLI_IMAGE"              -> "scala-cli",
        "SCALA_CLI_PRINT_STACK_TRACES" -> "1"
      )
    }
  }
  object `docker-slim` extends CliIntegrationDocker {
    object test extends ScalaCliTests {
      def sources = T.sources {
        integration.docker.test.sources()
      }
      def tmpDirBase = T.persistent {
        PathRef(T.dest / "working-dir")
      }
      def forkEnv = super.forkEnv() ++ Seq(
        "SCALA_CLI_TMP"                -> tmpDirBase().path.toString,
        "SCALA_CLI_IMAGE"              -> "scala-cli-slim",
        "SCALA_CLI_PRINT_STACK_TRACES" -> "1"
      )
    }
  }
}

object `docs-tests` extends Cross[DocsTests](Scala.scala3MainVersions)
    with CrossScalaDefaultToInternal

trait DocsTests extends CrossSbtModule with ScalaCliScalafixModule with HasTests { main =>
  def ivyDeps = Agg(
    Deps.fansi,
    Deps.osLib,
    Deps.pprint
  )
  def tmpDirBase = T.persistent {
    PathRef(T.dest / "working-dir")
  }
  def extraEnv = T {
    Seq(
      "SCLICHECK_SCALA_CLI" -> cli(crossScalaVersion).standaloneLauncher().path.toString,
      "SCALA_CLI_CONFIG"    -> (tmpDirBase().path / "config" / "config.json").toString
    )
  }
  def forkEnv = super.forkEnv() ++ extraEnv()

  object test extends ScalaCliTests with ScalaCliScalafixModule {
    def forkEnv = super.forkEnv() ++ extraEnv() ++ Seq(
      "SCALA_CLI_EXAMPLES"                -> (os.pwd / "examples").toString,
      "SCALA_CLI_GIF_SCENARIOS"           -> (os.pwd / "gifs" / "scenarios").toString,
      "SCALA_CLI_WEBSITE_IMG"             -> (os.pwd / "website" / "static" / "img").toString,
      "SCALA_CLI_GIF_RENDERER_DOCKER_DIR" -> (os.pwd / "gifs").toString,
      "SCALA_CLI_SVG_RENDERER_DOCKER_DIR" -> (os.pwd / "gifs" / "svg_render").toString
    )
    def resources = T.sources {
      // Adding markdown directories here, so that they're watched for changes in watch mode
      Seq(
        PathRef(os.pwd / "website" / "docs" / "commands"),
        PathRef(os.pwd / "website" / "docs" / "cookbooks")
      ) ++ super.resources()
    }
  }
}

object packager extends ScalaModule with Bloop.Module {
  def skipBloop    = true
  def scalaVersion = Scala.scala213
  def ivyDeps = Agg(
    Deps.scalaPackagerCli
  )
  def mainClass = Some("packager.cli.PackagerCli")
}

object `generate-reference-doc` extends Cross[GenerateReferenceDoc](Scala.scala3MainVersions)
    with CrossScalaDefaultToInternal

trait GenerateReferenceDoc extends CrossSbtModule with ScalaCliScalafixModule {
  def moduleDeps = Seq(
    cli(crossScalaVersion)
  )
  def repositoriesTask = T.task(super.repositoriesTask() ++ customRepositories)
  def ivyDeps = Agg(
    Deps.caseApp,
    Deps.munit
  )
  def mainClass = Some("scala.cli.doc.GenerateReferenceDoc")

  def forkEnv = super.forkEnv() ++ Seq(
    "SCALA_CLI_POWER" -> "true"
  )
}

object dummy extends Module {
  // dummy projects to get scala steward updates for Ammonite and scalafmt, whose
  // versions are used in the fmt and repl commands, and ensure Ammonite is available
  // for all Scala versions we support.
  object amm extends Cross[Amm](Scala.listMaxAmmoniteScalaVersion)
  trait Amm extends Cross.Module[String] with CrossScalaModule with Bloop.Module {
    def crossScalaVersion = crossValue
    def skipBloop         = true
    def ivyDeps = Agg(
      Deps.ammonite
    )
  }
  object scalafmt extends ScalaModule with Bloop.Module {
    def skipBloop    = true
    def scalaVersion = Scala.defaultInternal
    def ivyDeps = Agg(
      Deps.scalafmtCli
    )
  }
  object pythonInterface extends JavaModule with Bloop.Module {
    def skipBloop = true
    def ivyDeps = Agg(
      Deps.pythonInterface
    )
  }
  object scalaPy extends ScalaModule with Bloop.Module {
    def skipBloop    = true
    def scalaVersion = Scala.defaultInternal
    def ivyDeps = Agg(
      Deps.scalaPy
    )
  }
}

trait BuildMacros extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule
    with HasTests {
  def crossScalaVersion = crossValue
  def compileIvyDeps = T {
    if (crossScalaVersion.startsWith("3")) super.compileIvyDeps()
    else super.compileIvyDeps() ++ Agg(Deps.scalaReflect(crossScalaVersion))
  }

  object test extends ScalaCliTests {

    // Is there a better way to add task dependency to test?
    def test(args: String*) = T.command {
      val res = super.test(args: _*)()
      testNegativeCompilation()()
      res
    }

    def scalacOptions = T {
      super.scalacOptions() ++ asyncScalacOptions(scalaVersion())
    }

    def testNegativeCompilation() = T.command {
      val base = os.pwd / "modules" / "build-macros" / "src"
      val negativeTests = Seq(
        "MismatchedLeft.scala" -> Seq(
          "Found\\: +EE1".r,
          "Found\\: +EE2".r,
          "Required\\: +E2".r
        )
      )

      val cpsSource = base / "main" / "scala" / "scala" / "build" / "EitherCps.scala"
      assert(os.exists(cpsSource))

      val sv = scalaVersion()
      def compile(extraSources: os.Path*) =
        os.proc("scala-cli", "compile", "-S", sv, cpsSource, extraSources).call(
          check =
            false,
          mergeErrIntoOut = true
        )
      assert(0 == compile().exitCode)

      val notPassed = negativeTests.filter { case (testName, expectedErrors) =>
        val testFile = base / "negative-tests" / testName
        val res      = compile(testFile)
        println(s"Compiling $testName:")
        println(res.out.text())
        val name = testFile.last
        if (res.exitCode != 0) {
          println(s"Test case $name failed to compile as expected")
          val lines = res.out.lines()
          println(lines)
          expectedErrors.forall { expected =>
            if (lines.exists(expected.findFirstIn(_).nonEmpty)) false
            else {
              println(s"ERROR: regex `$expected` not found in compilation output for $testName")
              true
            }
          }
        }
        else {
          println(s"[ERROR] $name compiled successfully but it should not!")
          true
        }

      }
      assert(notPassed.isEmpty)
    }
  }
}

def asyncScalacOptions(scalaVersion: String) =
  if (scalaVersion.startsWith("3")) Nil else Seq("-Xasync")

trait ProtoBuildModule extends ScalaCliPublishModule with HasTests
    with ScalaCliScalafixModule

trait Core extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with HasTests
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def moduleDeps = Seq(
    config(crossScalaVersion)
  )
  def compileModuleDeps = Seq(
    `build-macros`(crossScalaVersion)
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(crossScalaVersion)
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.bloopRifle.exclude(("org.scala-lang.modules", "scala-collection-compat_2.13")),
    Deps.collectionCompat,
    Deps.coursierJvm
      // scalaJsEnvNodeJs brings a guava version that conflicts with this
      .exclude(("com.google.collections", "google-collections"))
      // Coursier is not cross-compiled and pulls jsoniter-scala-macros in 2.13
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros"))
      // Let's favor our config module rather than the one coursier pulls
      .exclude((organization, "config_2.13"))
      .exclude(("org.scala-lang.modules", "scala-collection-compat_2.13")),
    Deps.dependency,
    Deps.guava, // for coursierJvm / scalaJsEnvNodeJs, see above
    Deps.jgit,
    Deps.nativeTools, // Used only for discovery methods. For linking, look for scala-native-cli
    Deps.osLib,
    Deps.pprint,
    Deps.scalaJsEnvJsdomNodejs,
    Deps.scalaJsLogging,
    Deps.swoval
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Seq(
    Deps.jsoniterMacros
  )

  private def vcsState = T.persistent {
    val isCI  = System.getenv("CI") != null
    val state = VcsVersion.vcsState().format()
    if (isCI) state
    else state + "-maybe-stale"
  }

  def constantsFile = T.persistent {
    val dir  = T.dest / "constants"
    val dest = dir / "Constants.scala"
    val testRunnerMainClass = `test-runner`(Scala.runnerScala3)
      .mainClass()
      .getOrElse(sys.error("No main class defined for test-runner"))
    val runnerMainClass = runner(Scala.runnerScala3)
      .mainClass()
      .getOrElse(sys.error("No main class defined for runner"))
    val detailedVersionValue =
      if (`local-repo`.developingOnStubModules) s"""Some("${vcsState()}")"""
      else "None"
    val testRunnerOrganization = `test-runner`(Scala.runnerScala3)
      .pomSettings()
      .organization
    val code =
      s"""package scala.build.internal
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def version = "${publishVersion()}"
         |  def detailedVersion: Option[String] = $detailedVersionValue
         |  def ghOrg = "$ghOrg"
         |  def ghName = "$ghName"
         |
         |  def scalaJsVersion = "${Scala.scalaJs}"
         |  def scalaJsCliVersion = "${Scala.scalaJsCli}"
         |  def scalajsEnvJsdomNodejsVersion = "${Deps.scalaJsEnvJsdomNodejs.dep.version}"
         |  def scalaNativeVersion = "${Deps.nativeTools.dep.version}"
         |
         |  def testRunnerOrganization = "$testRunnerOrganization"
         |  def testRunnerModuleName = "${`test-runner`(Scala.runnerScala3).artifactName()}"
         |  def testRunnerVersion = "${`test-runner`(Scala.runnerScala3).publishVersion()}"
         |  def testRunnerMainClass = "$testRunnerMainClass"
         |
         |  def runnerOrganization = "${runner(Scala.runnerScala3).pomSettings().organization}"
         |  def runnerModuleName = "${runner(Scala.runnerScala3).artifactName()}"
         |  def runnerVersion = "${runner(Scala.runnerScala3).publishVersion()}"
         |  def runnerMainClass = "$runnerMainClass"
         |
         |  def semanticDbPluginOrganization = "${Deps.scalametaTrees.dep.module.organization.value}"
         |  def semanticDbPluginModuleName = "semanticdb-scalac"
         |  def semanticDbPluginVersion = "${Deps.scalametaTrees.dep.version}"
         |
         |  def semanticDbJavacPluginOrganization = "${Deps.semanticDbJavac.dep.module.organization.value}"
         |  def semanticDbJavacPluginModuleName = "${Deps.semanticDbJavac.dep.module.name.value}"
         |  def semanticDbJavacPluginVersion = "${Deps.semanticDbJavac.dep.version}"
         |
         |  def localRepoResourcePath = "$localRepoResourcePath"
         |
         |  def jmhVersion = "1.29"
         |
         |  def ammoniteVersion = "${Deps.ammonite.dep.version}"
         |  def millVersion = "${InternalDeps.Versions.mill}"
         |  def lefouMillwRef = "${InternalDeps.Versions.lefouMillwRef}"
         |
         |  def scalafmtOrganization = "${Deps.scalafmtCli.dep.module.organization.value}"
         |  def scalafmtName = "${Deps.scalafmtCli.dep.module.name.value}"
         |  def defaultScalafmtVersion = "${Deps.scalafmtCli.dep.version}"
         |
         |  def toolkitOrganization = "${Deps.toolkit.dep.module.organization.value}"
         |  def toolkitName = "${Deps.toolkit.dep.module.name.value}"
         |  def toolkitTestName = "${Deps.toolkitTest.dep.module.name.value}"
         |  def toolkitDefaultVersion = "${Deps.toolkitVersion}"
         |
         |  def typelevelOrganization = "${Deps.typelevelToolkit.dep.module.organization.value}"
         |  def typelevelToolkitDefaultVersion = "${Deps.typelevelToolkitVersion}"
         |
         |  def defaultScalaVersion = "${Scala.defaultUser}"
         |  def defaultScala212Version = "${Scala.scala212}"
         |  def defaultScala213Version = "${Scala.scala213}"
         |  def scala3LtsPrefix = "${Scala.scala3LtsPrefix}"
         |
         |  def workspaceDirName = "$workspaceDirName"
         |  def projectFileName = "$projectFileName"
         |  def jvmPropertiesFileName = "$jvmPropertiesFileName"
         |  def scalacArgumentsFileName = "scalac.args.txt"
         |  def maxScalacArgumentsCount = 5000
         |
         |  def defaultGraalVMJavaVersion = ${deps.graalVmJavaVersion}
         |  def defaultGraalVMVersion = "${deps.graalVmVersion}"
         |
         |  def scalaCliSigningOrganization = "${Deps.signingCli.dep.module.organization.value}"
         |  def scalaCliSigningName = "${Deps.signingCli.dep.module.name.value}"
         |  def scalaCliSigningVersion = "${Deps.signingCli.dep.version}"
         |  def javaClassNameOrganization = "${Deps.javaClassName.dep.module.organization.value}"
         |  def javaClassNameName = "${Deps.javaClassName.dep.module.name.value}"
         |  def javaClassNameVersion = "${Deps.javaClassName.dep.version}"
         |
         |  def signingCliJvmVersion = ${Deps.Versions.signingCliJvmVersion}
         |
         |  def libsodiumVersion = "${deps.libsodiumVersion}"
         |  def libsodiumjniVersion = "${Deps.libsodiumjni.dep.version}"
         |
         |  def scalaPyVersion = "${Deps.scalaPy.dep.version}"
         |
         |  def giter8Organization = "${Deps.giter8.dep.module.organization.value}"
         |  def giter8Name = "${Deps.giter8.dep.module.name.value}"
         |  def giter8Version = "${Deps.giter8.dep.version}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code, createFolders = true)
    PathRef(dir)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())
}

trait Directives extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with HasTests
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def moduleDeps = Seq(
    options(crossScalaVersion),
    core(crossScalaVersion),
    `build-macros`(crossScalaVersion),
    `specification-level`(crossScalaVersion)
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(crossScalaVersion)
  }

  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.jsoniterMacros,
    Deps.svm
  )
  def ivyDeps = super.ivyDeps() ++ Agg(
    // Deps.asm,
    Deps.bloopConfig,
    Deps.jsoniterCore,
    Deps.pprint,
    Deps.usingDirectives
  )

  def repositoriesTask =
    T.task(super.repositoriesTask() ++ deps.customRepositories)

  object test extends ScalaCliTests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.pprint
    )
    def runClasspath = T {
      super.runClasspath() ++ Seq(`local-repo`.localRepoJar())
    }

    def generatedSources = super.generatedSources() ++ Seq(constantsFile())

    def constantsFile = T.persistent {
      val dir  = T.dest / "constants"
      val dest = dir / "Constants2.scala"
      val code =
        s"""package scala.build.tests
           |
           |/** Build-time constants. Generated by mill. */
           |object Constants {
           |  def cs = "${settings.cs().replace("\\", "\\\\")}"
           |}
           |""".stripMargin
      if (!os.isFile(dest) || os.read(dest) != code)
        os.write.over(dest, code, createFolders = true)
      PathRef(dir)
    }

    // uncomment below to debug tests in attach mode on 5005 port
    // def forkArgs = T {
    //   super.forkArgs() ++ Seq("-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y")
    // }
  }
}

trait Config extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def moduleDeps        = Seq(`specification-level`(crossScalaVersion))
  def ivyDeps = {
    val maybeCollectionCompat =
      if (crossScalaVersion.startsWith("2.12.")) Seq(Deps.collectionCompat)
      else Nil
    super.ivyDeps() ++ maybeCollectionCompat ++ Agg(
      Deps.jsoniterCoreJava8
    )
  }
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.jsoniterMacrosJava8
  )
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-release", "8")
  }

  // Disabling Scalafix in 2.13 and 3, so that it doesn't remove
  // some compatibility-related imports, that are actually only used
  // in Scala 2.12.
  def fix(args: String*) =
    if (crossScalaVersion.startsWith("2.12.")) super.fix(args: _*)
    else T.command(())
}

trait Options extends ScalaCliCrossSbtModule with ScalaCliPublishModule with HasTests
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def moduleDeps = Seq(
    core(crossScalaVersion)
  )
  def compileModuleDeps = Seq(
    `build-macros`(crossScalaVersion)
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(crossScalaVersion)
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.bloopConfig,
    Deps.signingCliShared
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Seq(
    Deps.jsoniterMacros
  )

  def repositoriesTask =
    T.task(super.repositoriesTask() ++ deps.customRepositories)

  object test extends ScalaCliTests {
    // uncomment below to debug tests in attach mode on 5005 port
    // def forkArgs = T {
    //   super.forkArgs() ++ Seq("-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y")
    // }
  }
}

trait Scala3Runtime extends CrossSbtModule with ScalaCliPublishModule {
  def crossScalaVersion = crossValue
  def ivyDeps           = super.ivyDeps()
}

trait Scala3Graal extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.osLib
  )

  def resources = T.sources {
    val extraResourceDir = T.dest / "extra"
    // scala3RuntimeFixes.jar is also used within
    // resource-config.json and BytecodeProcessor.scala
    os.copy.over(
      `scala3-runtime`(crossScalaVersion).jar().path,
      extraResourceDir / "scala3RuntimeFixes.jar",
      createFolders = true
    )
    super.resources() ++ Seq(mill.PathRef(extraResourceDir))
  }
}

trait Scala3GraalProcessor extends CrossScalaModule with ScalaCliPublishModule {
  def moduleDeps     = Seq(`scala3-graal`(crossScalaVersion))
  def finalMainClass = "scala.cli.graal.CoursierCacheProcessor"
}

trait Build extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with HasTests
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def millSourcePath    = super.millSourcePath / os.up / "build"
  def moduleDeps = Seq(
    options(crossScalaVersion),
    directives(crossScalaVersion),
    `scala-cli-bsp`,
    `test-runner`(crossScalaVersion),
    `tasty-lib`(crossScalaVersion)
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(crossScalaVersion)
  }

  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.jsoniterMacros,
    Deps.svm
  )
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.collectionCompat,
    Deps.javaClassName,
    Deps.jsoniterCore,
    Deps.scalametaTrees,
    Deps.nativeTestRunner,
    Deps.osLib,
    Deps.pprint,
    Deps.scalaJsEnvNodeJs,
    Deps.scalaJsTestAdapter,
    Deps.swoval,
    Deps.zipInputStream
  )

  def repositoriesTask =
    T.task(super.repositoriesTask() ++ deps.customRepositories)

  object test extends ScalaCliTests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.pprint,
      Deps.slf4jNop
    )
    def runClasspath = T {
      super.runClasspath() ++ Seq(`local-repo`.localRepoJar())
    }

    def generatedSources = super.generatedSources() ++ Seq(constantsFile())

    def constantsFile = T.persistent {
      val dir  = T.dest / "constants"
      val dest = dir / "Constants2.scala"
      val code =
        s"""package scala.build.tests
           |
           |/** Build-time constants. Generated by mill. */
           |object Constants {
           |  def cs = "${settings.cs().replace("\\", "\\\\")}"
           |  def toolkitOrganization = "${Deps.toolkit.dep.module.organization.value}"
           |  def toolkitName = "${Deps.toolkit.dep.module.name.value}"
           |  def toolkitTestName = "${Deps.toolkitTest.dep.module.name.value}"
           |  def toolkitVersion = "${Deps.toolkitTest.dep.version}"
           |  def typelevelToolkitOrganization = "${Deps.typelevelToolkit.dep.module.organization.value}"
           |  def typelevelToolkitVersion = "${Deps.typelevelToolkit.dep.version}"
           |
           |  def defaultScalaVersion = "${Scala.defaultUser}"
           |  def defaultScala212Version = "${Scala.scala212}"
           |  def defaultScala213Version = "${Scala.scala213}"
           |}
           |""".stripMargin
      if (!os.isFile(dest) || os.read(dest) != code)
        os.write.over(dest, code, createFolders = true)
      PathRef(dir)
    }

    // uncomment below to debug tests in attach mode on 5005 port
    // def forkArgs = T {
    //   super.forkArgs() ++ Seq("-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y")
    // }
  }
}

trait SpecificationLevel extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule {
  def crossScalaVersion = crossValue
  def scalacOptions = T {
    val isScala213 = crossScalaVersion.startsWith("2.13.")
    val extraOptions =
      if (isScala213) Seq("-Xsource:3")
      else Nil
    super.scalacOptions() ++ extraOptions ++ Seq("-release", "8")
  }
}

trait Cli extends CrossSbtModule with ProtoBuildModule with CliLaunchers
    with FormatNativeImageConf {
  // Copied from Mill: https://github.com/com-lihaoyi/mill/blob/ea367c09bd31a30464ca901cb29863edde5340be/scalalib/src/mill/scalalib/JavaModule.scala#L792
  def debug(port: Int, args: Task[Args] = T.task(Args())): Command[Unit] = T.command {
    try mill.api.Result.Success(
        mill.util.Jvm.runSubprocess(
          finalMainClass(),
          runClasspath().map(_.path),
          forkArgs() ++ Seq(
            s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port,quiet=y"
          ),
          forkEnv(),
          args().value,
          workingDir = forkWorkingDir(),
          useCpPassingJar = runUseArgsFile()
        )
      )
    catch {
      case e: Exception =>
        mill.api.Result.Failure("subprocess failed")
    }
  }

  def constantsFile = T.persistent {
    val dir  = T.dest / "constants"
    val dest = dir / "Constants.scala"
    val code =
      s"""package scala.cli.internal
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def pythonInterfaceOrg       = "${Deps.pythonInterface.dep.module.organization.value}"
         |  def pythonInterfaceName      = "${Deps.pythonInterface.dep.module.name.value}"
         |  def pythonInterfaceVersion   = "${Deps.pythonInterface.dep.version}"
         |  def launcherTypeResourcePath = "${launcherTypeResourcePath.toString}"
         |  def defaultFilesResourcePath = "$defaultFilesResourcePath"
         |  def maxAmmoniteScala3Version = "${Scala.maxAmmoniteScala3Version}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code, createFolders = true)
    PathRef(dir)
  }
  def optionsConstantsFile = T.persistent {
    val dir  = T.dest / "constants"
    val dest = dir / "Constants.scala"
    val code =
      s"""package scala.cli.commands
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def defaultScalaVersion = "${Scala.defaultUser}"
         |  def scalaJsVersion = "${Scala.scalaJs}"
         |  def scalaJsCliVersion = "${Scala.scalaJsCli}"
         |  def scalaNativeVersion = "${Deps.nativeTools.dep.version}"
         |  def ammoniteVersion = "${Deps.ammonite.dep.version}"
         |  def defaultScalafmtVersion = "${Deps.scalafmtCli.dep.version}"
         |  def defaultGraalVMJavaVersion = "${deps.graalVmJavaVersion}"
         |  def defaultGraalVMVersion = "${deps.graalVmVersion}"
         |  def scalaPyVersion = "${Deps.scalaPy.dep.version}"
         |  def signingCliJvmVersion = ${Deps.Versions.signingCliJvmVersion}
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code, createFolders = true)
    PathRef(dir)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile(), optionsConstantsFile())

  def defaultFilesResources = T.persistent {
    val dir = T.dest / "resources"
    def transformWorkflow(content: Array[Byte]): Array[Byte] =
      new String(content, "UTF-8")
        .replaceAll(" ./scala-cli", " scala-cli")
        .getBytes("UTF-8")
    val resources = Seq[(String, os.SubPath, Array[Byte] => Array[Byte])](
      (
        "https://raw.githubusercontent.com/scala-cli/default-workflow/main/.github/workflows/ci.yml",
        os.sub / "workflows" / "default.yml",
        transformWorkflow _
      ),
      (
        "https://raw.githubusercontent.com/scala-cli/default-workflow/main/.gitignore",
        os.sub / "gitignore",
        identity
      )
    )
    for ((srcUrl, destRelPath, transform) <- resources) {
      val dest = dir / defaultFilesResourcePath / destRelPath
      if (!os.isFile(dest)) {
        val content = Using.resource(new URL(srcUrl).openStream())(_.readAllBytes())
        os.write(dest, transform(content), createFolders = true)
      }
    }
    PathRef(dir)
  }
  override def resources = T.sources {
    super.resources() ++ Seq(defaultFilesResources())
  }

  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(crossScalaVersion)
  }
  def javacOptions = T {
    super.javacOptions() ++ Seq("--release", "16")
  }
  def moduleDeps = Seq(
    `build-module`(crossScalaVersion),
    config(crossScalaVersion),
    `scala3-graal`(crossScalaVersion),
    `specification-level`(crossScalaVersion)
  )

  def repositoriesTask = T.task(super.repositoriesTask() ++ customRepositories)

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.caseApp,
    Deps.coursierLauncher,
    Deps.coursierProxySetup,
    Deps.coursierPublish.exclude((organization, "config_2.13")),
    Deps.jimfs, // scalaJsEnvNodeJs pulls jimfs:1.1, whose class path seems borked (bin compat issue with the guava version it depends on)
    Deps.jniUtils,
    Deps.jsoniterCore,
    Deps.libsodiumjni,
    Deps.metaconfigTypesafe,
    Deps.pythonNativeLibs,
    Deps.scalaPackager.exclude("com.lihaoyi" -> "os-lib_2.13"),
    Deps.signingCli.exclude((organization, "config_2.13")),
    Deps.slf4jNop, // to silence jgit
    Deps.sttp
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.jsoniterMacros,
    Deps.svm
  )
  def mainClass = Some("scala.cli.ScalaCli")

  override def nativeImageClassPath = T {
    val classpath = super.nativeImageClassPath().map(_.path).mkString(File.pathSeparator)
    val cache     = T.dest / "native-cp"
    // `scala3-graal-processor`.run() do not give me output and I cannot pass dynamically computed values like classpath
    val res = mill.util.Jvm.callSubprocess(
      mainClass = `scala3-graal-processor`(crossScalaVersion).finalMainClass(),
      classPath = `scala3-graal-processor`(crossScalaVersion).runClasspath().map(_.path),
      mainArgs = Seq(cache.toNIO.toString, classpath),
      workingDir = os.pwd
    )
    val cp = res.out.trim()
    cp.split(File.pathSeparator).toSeq.map(p => PathRef(os.Path(p)))
  }

  def localRepoJar = `local-repo`.localRepoJar()

  object test extends ScalaCliTests with ScalaCliScalafixModule {
    def moduleDeps = super.moduleDeps ++ Seq(
      `build-module`(crossScalaVersion).test
    )
    def runClasspath = T {
      super.runClasspath() ++ Seq(localRepoJar())
    }

    def compileIvyDeps = super.ivyDeps() ++ Agg(
      Deps.jsoniterMacros
    )

    // Required by the reflection usage in modules/cli/src/test/scala/cli/tests/SetupScalaCLITests.scala
    override def forkArgs: T[Seq[String]] = T {
      super.forkArgs() ++ Seq("--add-opens=java.base/java.util=ALL-UNNAMED")
    }
  }
}

trait CliIntegration extends SbtModule with ScalaCliPublishModule with HasTests
    with ScalaCliScalafixModule {
  def scalaVersion = sv

  def sv = Scala.scala213

  private def prefix = "integration-"

  def tmpDirBase = T.persistent {
    PathRef(T.dest / "working-dir")
  }
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Xasync", "-deprecation")
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )

  private def mainArtifactName = T(artifactName())
  trait IntegrationScalaTests extends super.ScalaCliTests with ScalaCliScalafixModule {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.bsp4j,
      Deps.coursier
        .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros")),
      Deps.dockerClient,
      Deps.jsoniterCore,
      Deps.libsodiumjni,
      Deps.pprint,
      Deps.scalaAsync,
      Deps.slf4jNop,
      Deps.usingDirectives
    )
    def compileIvyDeps = super.compileIvyDeps() ++ Seq(
      Deps.jsoniterMacros
    )
    def forkEnv = super.forkEnv() ++ Seq(
      "SCALA_CLI_TMP"                -> tmpDirBase().path.toString,
      "SCALA_CLI_PRINT_STACK_TRACES" -> "1",
      "SCALA_CLI_CONFIG"             -> (tmpDirBase().path / "config" / "config.json").toString
    )

    def constantsFile = T.persistent {
      val dir  = T.dest / "constants"
      val dest = dir / "Constants.scala"
      val mostlyStaticDockerfile =
        os.rel / ".github" / "scripts" / "docker" / "ScalaCliSlimDockerFile"
      assert(
        os.exists(os.pwd / mostlyStaticDockerfile),
        s"Error: ${os.pwd / mostlyStaticDockerfile} not found"
      )
      val code =
        s"""package scala.cli.integration
           |
           |/** Build-time constants. Generated by mill. */
           |object Constants {
           |  def bspVersion                 = "${Deps.bsp4j.dep.version}"
           |  def scala212                   = "${Scala.scala212}"
           |  def scala213                   = "${Scala.scala213}"
           |  def scalaSnapshot213           = "${TestDeps.scalaSnapshot213}"
           |  def scala3LtsPrefix            = "${Scala.scala3LtsPrefix}"
           |  def scala3Lts                  = "${Scala.scala3Lts}"
           |  def scala3Next                 = "${Scala.scala3Next}"
           |  def defaultScala               = "${Scala.defaultUser}"
           |  def defaultScalafmtVersion     = "${Deps.scalafmtCli.dep.version}"
           |  def maxAmmoniteScala212Version = "${Scala.maxAmmoniteScala212Version}"
           |  def maxAmmoniteScala213Version = "${Scala.maxAmmoniteScala213Version}"
           |  def maxAmmoniteScala3Version   = "${Scala.maxAmmoniteScala3Version}"
           |  def scalaJsVersion             = "${Scala.scalaJs}"
           |  def scalaJsCliVersion          = "${Scala.scalaJsCli}"
           |  def scalaNativeVersion         = "${Deps.nativeTools.dep.version}"
           |  def ammoniteVersion            = "${Deps.ammonite.dep.version}"
           |  def defaultGraalVMJavaVersion  = "${deps.graalVmJavaVersion}"
           |  def defaultGraalVMVersion      = "${deps.graalVmVersion}"
           |  def scalaPyVersion             = "${Deps.scalaPy.dep.version}"
           |  def bloopVersion               = "${Deps.bloopRifle.dep.version}"
           |  def pprintVersion              = "${TestDeps.pprint.dep.version}"
           |  def munitVersion               = "${TestDeps.munit.dep.version}"
           |  def dockerTestImage            = "${Docker.testImage}"
           |  def dockerAlpineTestImage      = "${Docker.alpineTestImage}"
           |  def authProxyTestImage         = "${Docker.authProxyTestImage}"
           |  def mostlyStaticDockerfile     = "${mostlyStaticDockerfile.toString.replace(
            "\\",
            "\\\\"
          )}"
           |  def cs                         = "${settings.cs().replace("\\", "\\\\")}"
           |  def workspaceDirName           = "$workspaceDirName"
           |  def libsodiumVersion           = "${deps.libsodiumVersion}"
           |  def dockerArchLinuxImage       = "${TestDeps.archLinuxImage}"
           |  
           |  def toolkitVersion          = "${Deps.toolkitVersion}"
           |  def typelevelToolkitVersion = "${Deps.typelevelToolkitVersion}"
           |
           |  def ghOrg  = "$ghOrg"
           |  def ghName = "$ghName"
           |}
           |""".stripMargin
      if (!os.isFile(dest) || os.read(dest) != code)
        os.write.over(dest, code, createFolders = true)
      PathRef(dir)
    }
    def generatedSources = super.generatedSources() ++ Seq(constantsFile())

    def runTests(launcherTask: T[PathRef], cliKind: String, args: String*) = {
      val argsTask = T.task {
        val launcher = launcherTask().path
        val debugReg = "^--debug$|^--debug:([0-9]+)$".r
        val debugPortOpt = args.find(debugReg.matches).flatMap {
          case debugReg(port) => Option(port).orElse(Some("5005"))
          case _              => None
        }
        val debugArgs = debugPortOpt match {
          case Some(port) =>
            System.err.println(
              s"--debug option has been passed. Listening for transport dt_socket at address: $port"
            )
            Seq(s"-Dtest.scala-cli.debug.port=$port")
          case _ => Seq.empty
        }
        val extraArgs = Seq(
          s"-Dtest.scala-cli.path=$launcher",
          s"-Dtest.scala-cli.kind=$cliKind"
        )
        args ++ extraArgs ++ debugArgs
      }
      testTask(argsTask, T.task(Seq.empty[String]))
    }

    override def test(args: String*) = jvm(args: _*)

    def forcedLauncher = T.persistent {
      val ext      = if (Properties.isWin) ".exe" else ""
      val launcher = T.dest / s"scala-cli$ext"
      if (!os.exists(launcher)) {
        val dir = Option(System.getenv("SCALA_CLI_IT_FORCED_LAUNCHER_DIRECTORY")).getOrElse {
          sys.error("SCALA_CLI_IT_FORCED_LAUNCHER_DIRECTORY not set")
        }
        val content = importedLauncher(dir)
        os.write(
          launcher,
          content,
          createFolders = true,
          perms = if (Properties.isWin) null else "rwxr-xr-x"
        )
      }
      PathRef(launcher)
    }

    def forcedStaticLauncher = T.persistent {
      val launcher = T.dest / "scala-cli"
      if (!os.exists(launcher)) {
        val dir = Option(System.getenv("SCALA_CLI_IT_FORCED_STATIC_LAUNCHER_DIRECTORY")).getOrElse {
          sys.error("SCALA_CLI_IT_FORCED_STATIC_LAUNCHER_DIRECTORY not set")
        }
        val content = importedLauncher(dir)
        os.write(launcher, content, createFolders = true)
      }
      PathRef(launcher)
    }

    def forcedMostlyStaticLauncher = T.persistent {
      val launcher = T.dest / "scala-cli"
      if (!os.exists(launcher)) {
        val dir =
          Option(System.getenv("SCALA_CLI_IT_FORCED_MOSTLY_STATIC_LAUNCHER_DIRECTORY")).getOrElse {
            sys.error("SCALA_CLI_IT_FORCED_MOSTLY_STATIC_LAUNCHER_DIRECTORY not set")
          }
        val content = importedLauncher(dir)
        os.write(launcher, content, createFolders = true)
      }
      PathRef(launcher)
    }

    private object Launchers {
      def jvm = cli(Scala.defaultInternal).standaloneLauncher

      def jvmBootstrapped = cliBootstrapped.jar

      def native =
        Option(System.getenv("SCALA_CLI_IT_FORCED_LAUNCHER_DIRECTORY")) match {
          case Some(_) => forcedLauncher
          case None    => cli(Scala.defaultInternal).nativeImage
        }

      def nativeStatic =
        Option(System.getenv("SCALA_CLI_IT_FORCED_STATIC_LAUNCHER_DIRECTORY")) match {
          case Some(_) => forcedStaticLauncher
          case None    => cli(Scala.defaultInternal).nativeImageStatic
        }

      def nativeMostlyStatic =
        Option(System.getenv("SCALA_CLI_IT_FORCED_MOSTLY_STATIC_LAUNCHER_DIRECTORY")) match {
          case Some(_) => forcedMostlyStaticLauncher
          case None    => cli(Scala.defaultInternal).nativeImageMostlyStatic
        }
    }

    def jvm(args: String*) = T.command(runTests(Launchers.jvm, "jvm", args: _*))
    def jvmBootstrapped(args: String*) =
      T.command(runTests(Launchers.jvmBootstrapped, "jvmBootstrapped", args: _*))
    def native(args: String*) = T.command(runTests(Launchers.native, "native", args: _*))
    def nativeStatic(args: String*) =
      T.command(runTests(Launchers.nativeStatic, "native-static", args: _*))
    def nativeMostlyStatic(args: String*) =
      T.command(runTests(Launchers.nativeMostlyStatic, "native-mostly-static", args: _*))
  }
}

trait CliIntegrationDocker extends SbtModule with ScalaCliPublishModule with HasTests {
  def scalaVersion = Scala.scala213
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )
}

trait Runner extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-release", "8")
  }
  def mainClass = Some("scala.cli.runner.Runner")
  def sources = T.sources {
    val scala3DirNames =
      if (crossScalaVersion.startsWith("3.")) {
        val name =
          if (crossScalaVersion.contains("-RC")) "scala-3-unstable"
          else "scala-3-stable"
        Seq(name)
      }
      else
        Nil
    val extraDirs = scala3DirNames.map(name => PathRef(millSourcePath / "src" / "main" / name))
    super.sources() ++ extraDirs
  }
}

trait TestRunner extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-release", "8")
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.collectionCompat,
    Deps.testInterface
  )
  def mainClass = Some("scala.build.testrunner.DynamicTestRunner")
}

trait TastyLib extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def crossScalaVersion = crossValue
  def constantsFile = T.persistent {
    val dir  = T.dest / "constants"
    val dest = dir / "Constants.scala"
    val code =
      s"""package scala.build.tastylib.internal
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def latestSupportedScala = "${Scala.defaultUser}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code, createFolders = true)
    PathRef(dir)
  }

  def generatedSources = super.generatedSources() ++ Seq(constantsFile())
}

object `local-repo` extends LocalRepo {

  /*
   * If you are developing locally on any of the stub modules (stubs, runner, test-runner),
   * set this to true, so that Mill's watch mode takes into account changes in those modules
   * when embedding their JARs in the Scala CLI launcher.
   */
  def developingOnStubModules = false

  def stubsModules =
    for {
      sv   <- Scala.runnerScalaVersions
      proj <- Seq(runner, `test-runner`)
    } yield proj(sv)
  def version = runner(Scala.runnerScala3).publishVersion()
}

// Helper CI commands

def publishSonatype(tasks: mill.main.Tasks[PublishModule.PublishData]) = T.command {
  publish.publishSonatype(
    data = define.Target.sequence(tasks.value)(),
    log = T.ctx().log
  )
}

def copyTo(task: mill.main.Tasks[PathRef], dest: String) = T.command {
  val destPath = os.Path(dest, os.pwd)
  if (task.value.length > 1)
    sys.error("Expected a single task")
  val ref = task.value.head()
  os.makeDir.all(destPath / os.up)
  os.copy.over(ref.path, destPath)
}

def writePackageVersionTo(dest: String) = T.command {
  val destPath   = os.Path(dest, os.pwd)
  val rawVersion = cli(Scala.defaultInternal).publishVersion()
  val version =
    if (rawVersion.contains("+")) rawVersion.stripSuffix("-SNAPSHOT")
    else rawVersion
  os.write.over(destPath, version)
}

def writeShortPackageVersionTo(dest: String) = T.command {
  val destPath   = os.Path(dest, os.pwd)
  val rawVersion = cli(Scala.defaultInternal).publishVersion()
  val version    = rawVersion.takeWhile(c => c != '-' && c != '+')
  os.write.over(destPath, version)
}

def importedLauncher(directory: String = "artifacts"): Array[Byte] = {
  val ext  = if (Properties.isWin) ".zip" else ".gz"
  val from = os.Path(directory, os.pwd) / s"scala-cli-${Upload.platformSuffix}$ext"
  System.err.println(s"Importing launcher from $from")
  if (!os.exists(from))
    sys.error(s"$from not found")

  if (Properties.isWin) {
    import java.util.zip.ZipFile
    Using.resource(new ZipFile(from.toIO)) { zf =>
      val ent = zf.getEntry("scala-cli.exe")
      Using.resource(zf.getInputStream(ent)) { is =>
        is.readAllBytes()
      }
    }
  }
  else {
    import java.io.ByteArrayInputStream
    import java.util.zip.GZIPInputStream

    val compressed = os.read.bytes(from)
    val bais       = new ByteArrayInputStream(compressed)
    Using.resource(new GZIPInputStream(bais)) { gzis =>
      gzis.readAllBytes()
    }
  }
}

def copyLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = cli(Scala.defaultInternal).nativeImage().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true
  )
}

def copyJvmLauncher(directory: String = "artifacts") = T.command {
  val launcher = cli(Scala.defaultInternal).standaloneLauncher().path
  os.copy(
    launcher,
    os.Path(directory, os.pwd) / s"scala-cli$platformExecutableJarExtension",
    createFolders = true,
    replaceExisting = true
  )
}
def copyJvmBootstrappedLauncher(directory: String = "artifacts") = T.command {
  val launcher = cliBootstrapped.jar().path
  os.copy(
    launcher,
    os.Path(directory, os.pwd) / s"scala-cli.jar",
    createFolders = true,
    replaceExisting = true
  )
}

def uploadLaunchers(directory: String = "artifacts") = T.command {
  val version = cli(Scala.defaultInternal).publishVersion()

  val path = os.Path(directory, os.pwd)
  val launchers = os.list(path).filter(os.isFile(_)).map { path =>
    path -> path.last
  }
  val (tag, overwriteAssets) =
    if (version.endsWith("-SNAPSHOT")) ("nightly", true)
    else ("v" + version, false)
  System.err.println(s"Uploading to tag $tag (overwrite assets: $overwriteAssets)")
  Upload.upload(ghOrg, ghName, ghToken(), tag, dryRun = false, overwrite = overwriteAssets)(
    launchers: _*
  )
}

def unitTests() = T.command {
  `build-module`(Scala.defaultInternal).test.test()()
  `build-macros`(Scala.defaultInternal).test.test()()
  cli(Scala.defaultInternal).test.test()()
  directives(Scala.defaultInternal).test.test()()
  options(Scala.defaultInternal).test.test()()
}

def scala(args: Task[Args] = T.task(Args())) = T.command {
  cli(Scala.defaultInternal).run(args)()
}

def debug(port: Int, args: Task[Args] = T.task(Args())) = T.command {
  cli(Scala.defaultInternal).debug(port, args)()
}

def defaultNativeImage() =
  T.command {
    cli(Scala.defaultInternal).nativeImage()
  }

def nativeIntegrationTests() =
  T.command {
    integration.test.native()()
  }

def copyDefaultLauncher(directory: String = "artifacts") =
  T.command {
    copyLauncher(directory)()
  }

def copyMostlyStaticLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = cli(Scala.defaultInternal).nativeImageMostlyStatic().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true,
    suffix = "-mostly-static"
  )
}

def copyStaticLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = cli(Scala.defaultInternal).nativeImageStatic().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true,
    suffix = "-static"
  )
}
private def ghToken(): String = Option(System.getenv("UPLOAD_GH_TOKEN")).getOrElse {
  sys.error("UPLOAD_GH_TOKEN not set")
}
private def gitClone(repo: String, branch: String, workDir: os.Path) =
  os.proc("git", "clone", repo, "-q", "-b", branch).call(cwd = workDir)
private def setupGithubRepo(repoDir: os.Path) = {
  val gitUserName = "gh-actions"
  val gitEmail    = "actions@github.com"

  os.proc("git", "config", "user.name", gitUserName).call(cwd = repoDir)
  os.proc("git", "config", "user.email", gitEmail).call(cwd = repoDir)
}

private def commitChanges(
  name: String,
  branch: String,
  repoDir: os.Path,
  force: Boolean = false
): Unit = {
  if (os.proc("git", "status").call(cwd = repoDir).out.trim().contains("nothing to commit"))
    println("Nothing Changes")
  else {
    os.proc("git", "add", "-A").call(cwd = repoDir)
    os.proc("git", "commit", "-am", name).call(cwd = repoDir)
    println(s"Trying to push on $branch branch")
    val pushExtraOptions = if (force) Seq("--force") else Seq.empty
    os.proc("git", "push", "origin", branch, pushExtraOptions).call(cwd = repoDir)
    println(s"Push successfully on $branch branch")
  }
}

// TODO Move most CI-specific tasks there
object ci extends Module {
  def publishVersion() = T.command {
    println(cli(Scala.defaultInternal).publishVersion())
  }
  def updateScalaCliSetup() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val targetDir       = os.pwd / "target-scala-cli-setup"
    val mainDir         = targetDir / "scala-cli-setup"
    val setupScriptPath = mainDir / "src" / "main.ts"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch       = "main"
    val targetBranch = s"update-scala-cli-setup"
    val repo         = "git@github.com:VirtusLab/scala-cli-setup.git"

    // Cloning
    gitClone(repo, branch, targetDir)
    setupGithubRepo(mainDir)

    val setupScript          = os.read(setupScriptPath)
    val scalaCliVersionRegex = "const scalaCLIVersion = '.*'".r
    val updatedSetupScriptPath =
      scalaCliVersionRegex.replaceFirstIn(setupScript, s"const scalaCLIVersion = '$version'")
    os.write.over(setupScriptPath, updatedSetupScriptPath)

    os.proc("git", "switch", "-c", targetBranch).call(cwd = mainDir)
    commitChanges(s"Update scala-cli version to $version", targetBranch, mainDir, force = true)
  }
  def updateStandaloneLauncher() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val targetDir                     = os.pwd / "target"
    val scalaCliDir                   = targetDir / "scala-cli"
    val standaloneLauncherPath        = scalaCliDir / "scala-cli.sh"
    val standaloneWindowsLauncherPath = scalaCliDir / "scala-cli.bat"

    // clean scala-cli directory
    if (os.exists(scalaCliDir)) os.remove.all(scalaCliDir)
    if (!os.exists(targetDir)) os.makeDir.all(targetDir)

    val branch       = "main"
    val targetBranch = s"update-standalone-launcher-$version"
    val repo         = s"https://oauth2:${ghToken()}@github.com/$ghOrg/$ghName.git"

    // Cloning
    gitClone(repo, branch, targetDir)
    setupGithubRepo(scalaCliDir)

    val launcherScript       = os.read(standaloneLauncherPath)
    val scalaCliVersionRegex = "SCALA_CLI_VERSION=\".*\"".r
    val updatedLauncherScript =
      scalaCliVersionRegex.replaceFirstIn(launcherScript, s"""SCALA_CLI_VERSION="$version"""")
    os.write.over(standaloneLauncherPath, updatedLauncherScript)

    val launcherWindowsScript       = os.read(standaloneWindowsLauncherPath)
    val scalaCliWindowsVersionRegex = "SCALA_CLI_VERSION=.*\"".r
    val updatedWindowsLauncherScript =
      scalaCliWindowsVersionRegex.replaceFirstIn(
        launcherWindowsScript,
        s"""SCALA_CLI_VERSION=$version""""
      )
    os.write.over(standaloneWindowsLauncherPath, updatedWindowsLauncherScript)

    os.proc("git", "switch", "-c", targetBranch).call(cwd = scalaCliDir)
    commitChanges(s"Update scala-cli.sh launcher for $version", targetBranch, scalaCliDir)
    os.proc("gh", "auth", "login", "--with-token").call(cwd = scalaCliDir, stdin = ghToken())
    os.proc("gh", "pr", "create", "--fill", "--base", "main", "--head", targetBranch)
      .call(cwd = scalaCliDir)
  }
  def brewLaunchersSha(x86LauncherPath: os.Path, arm64LauncherPath: os.Path, targetDir: os.Path) = {
    val x86BinarySha256 = os.proc("openssl", "dgst", "-sha256", "-binary")
      .call(
        cwd = targetDir,
        stdin = os.read.stream(x86LauncherPath)
      ).out.bytes
    val arm64BinarySha256 = os.proc("openssl", "dgst", "-sha256", "-binary")
      .call(
        cwd = targetDir,
        stdin = os.read.stream(arm64LauncherPath)
      ).out.bytes
    val x86Sha256 = os.proc("xxd", "-p", "-c", "256")
      .call(
        cwd = targetDir,
        stdin = x86BinarySha256
      ).out.trim()
    val arm64Sha256 = os.proc("xxd", "-p", "-c", "256")
      .call(
        cwd = targetDir,
        stdin = arm64BinarySha256
      ).out.trim()

    (x86Sha256, arm64Sha256)
  }
  def updateScalaCliBrewFormula() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val targetDir          = os.pwd / "target"
    val homebrewFormulaDir = targetDir / "homebrew-scala-cli"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch = "main"
    val repo   = s"git@github.com:Virtuslab/homebrew-scala-cli.git"

    // Cloning
    gitClone(repo, branch, targetDir)
    setupGithubRepo(homebrewFormulaDir)

    val x86LauncherURL =
      s"https://github.com/Virtuslab/scala-cli/releases/download/v$version/scala-cli-x86_64-apple-darwin.gz"
    val arm64LauncherURL =
      s"https://github.com/Virtuslab/scala-cli/releases/download/v$version/scala-cli-aarch64-apple-darwin.gz"

    val x86LauncherPath   = os.Path("artifacts", os.pwd) / "scala-cli-x86_64-apple-darwin.gz"
    val arm64LauncherPath = os.Path("artifacts", os.pwd) / "scala-cli-aarch64-apple-darwin.gz"
    val (x86Sha256, arm64Sha256) = brewLaunchersSha(x86LauncherPath, arm64LauncherPath, targetDir)

    val templateFormulaPath = os.pwd / ".github" / "scripts" / "scala-cli.rb.template"
    val template            = os.read(templateFormulaPath)

    val updatedFormula = template
      .replace("@X86_LAUNCHER_URL@", x86LauncherURL)
      .replace("@ARM64_LAUNCHER_URL@", arm64LauncherURL)
      .replace("@X86_LAUNCHER_SHA256@", x86Sha256)
      .replace("@ARM64_LAUNCHER_SHA256@", arm64Sha256)
      .replace("@LAUNCHER_VERSION@", version)

    val formulaPath = homebrewFormulaDir / "scala-cli.rb"
    os.write.over(formulaPath, updatedFormula)

    commitChanges(s"Update for $version", branch, homebrewFormulaDir)
  }
  def updateScalaExperimentalBrewFormula() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val targetDir          = os.pwd / "target"
    val homebrewFormulaDir = targetDir / "homebrew-scala-experimental"

    // clean homebrew-scala-experimental directory
    if (os.exists(homebrewFormulaDir)) os.remove.all(homebrewFormulaDir)

    os.makeDir.all(targetDir)

    val branch = "main"
    val repo   = s"git@github.com:VirtusLab/homebrew-scala-experimental.git"

    // Cloning
    gitClone(repo, branch, targetDir)
    setupGithubRepo(homebrewFormulaDir)

    val x86LauncherURL =
      s"https://github.com/Virtuslab/scala-cli/releases/download/v$version/scala-cli-x86_64-apple-darwin.gz"
    val arm64LauncherURL =
      s"https://github.com/Virtuslab/scala-cli/releases/download/v$version/scala-cli-aarch64-apple-darwin.gz"

    val x86LauncherPath   = os.Path("artifacts", os.pwd) / "scala-cli-x86_64-apple-darwin.gz"
    val arm64LauncherPath = os.Path("artifacts", os.pwd) / "scala-cli-aarch64-apple-darwin.gz"
    val (x86Sha256, arm64Sha256) = brewLaunchersSha(x86LauncherPath, arm64LauncherPath, targetDir)

    val templateFormulaPath = os.pwd / ".github" / "scripts" / "scala.rb.template"
    val template            = os.read(templateFormulaPath)

    val updatedFormula = template
      .replace("@X86_LAUNCHER_URL@", x86LauncherURL)
      .replace("@ARM64_LAUNCHER_URL@", arm64LauncherURL)
      .replace("@X86_LAUNCHER_SHA256@", x86Sha256)
      .replace("@ARM64_LAUNCHER_SHA256@", arm64Sha256)
      .replace("@LAUNCHER_VERSION@", version)

    val formulaPath = homebrewFormulaDir / "scala.rb"
    os.write.over(formulaPath, updatedFormula)

    commitChanges(s"Update for $version", branch, homebrewFormulaDir)
  }
  def updateInstallationScript() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val targetDir              = os.pwd / "target"
    val packagesDir            = targetDir / "scala-cli-packages"
    val installationScriptPath = packagesDir / "scala-setup.sh"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch = "main"
    val repo   = s"git@github.com:Virtuslab/scala-cli-packages.git"

    // Cloning
    gitClone(repo, branch, targetDir)
    setupGithubRepo(packagesDir)

    val installationScript   = os.read(installationScriptPath)
    val scalaCliVersionRegex = "SCALA_CLI_VERSION=\".*\"".r
    val updatedInstallationScript =
      scalaCliVersionRegex.replaceFirstIn(installationScript, s"""SCALA_CLI_VERSION="$version"""")
    os.write.over(installationScriptPath, updatedInstallationScript)

    commitChanges(s"Update installation script for $version", branch, packagesDir)
  }
  def updateDebianPackages() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val targetDir   = os.pwd / "target"
    val packagesDir = targetDir / "scala-cli-packages"
    val debianDir   = packagesDir / "debian"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch = "main"
    val repo   = s"git@github.com:Virtuslab/scala-cli-packages.git"

    // Cloning
    gitClone(repo, branch, targetDir)
    setupGithubRepo(packagesDir)

    // copy deb package to repository
    os.copy(
      os.Path("artifacts", os.pwd) / "scala-cli-x86_64-pc-linux.deb",
      debianDir / s"scala-cli_$version.deb"
    )

    val packagesPath = debianDir / "Packages"
    os.proc("dpkg-scanpackages", "--multiversion", ".").call(cwd = debianDir, stdout = packagesPath)
    os.proc("gzip", "-k", "-f", "Packages").call(cwd = debianDir)

    val releasePath = debianDir / "Release"
    os.proc("apt-ftparchive", "release", ".").call(cwd = debianDir, stdout = releasePath)

    val pgpPassphrase =
      Option(System.getenv("PGP_PASSPHRASE")).getOrElse(sys.error("PGP_PASSPHRASE not set"))
    val keyName = Option(System.getenv("GPG_EMAIL")).getOrElse(sys.error("GPG_EMAIL not set"))
    val releaseGpgPath = debianDir / "Release.gpg"
    val inReleasePath  = debianDir / "InRelease"
    os.proc(
      "gpg",
      "--batch",
      "--yes",
      "--passphrase-fd",
      "0",
      "--default-key",
      keyName,
      "-abs",
      "-o",
      "-",
      "Release"
    )
      .call(cwd = debianDir, stdin = pgpPassphrase, stdout = releaseGpgPath)
    os.proc(
      "gpg",
      "--batch",
      "--yes",
      "--passphrase-fd",
      "0",
      "--default-key",
      keyName,
      "--clearsign",
      "-o",
      "-",
      "Release"
    )
      .call(cwd = debianDir, stdin = pgpPassphrase, stdout = inReleasePath)

    commitChanges(s"Update Debian packages for $version", branch, packagesDir)
  }
  def updateChocolateyPackage() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val packagesDir = os.pwd / "target" / "scala-cli-packages"
    val chocoDir    = os.pwd / ".github" / "scripts" / "choco"

    val msiPackagePath = packagesDir / s"scala-cli_$version.msi"
    os.copy(
      os.pwd / "artifacts" / "scala-cli-x86_64-pc-win32.msi",
      msiPackagePath,
      createFolders = true
    )

    // prepare ps1 file
    val ps1Path = chocoDir / "tools" / "chocolateyinstall.ps1"

    val launcherURL =
      s"https://github.com/VirtusLab/scala-cli/releases/download/v$version/scala-cli-x86_64-pc-win32.msi"
    val bytes  = os.read.stream(msiPackagePath)
    val sha256 = os.proc("sha256sum").call(cwd = packagesDir, stdin = bytes).out.trim().take(64)
    val ps1UpdatedContent = os.read(ps1Path)
      .replace("@LAUNCHER_URL@", launcherURL)
      .replace("@LAUNCHER_SHA256@", sha256.toUpperCase)

    os.write.over(ps1Path, ps1UpdatedContent)

    // prepare nuspec file
    val nuspecPath           = chocoDir / "scala-cli.nuspec"
    val nuspecUpdatedContent = os.read(nuspecPath).replace("@LAUNCHER_VERSION@", version)
    os.write.over(nuspecPath, nuspecUpdatedContent)

    os.proc("choco", "pack", nuspecPath, "--out", chocoDir).call(cwd = chocoDir)
    val chocoKey =
      Option(System.getenv("CHOCO_SECRET")).getOrElse(sys.error("CHOCO_SECRET not set"))
    os.proc(
      "choco",
      "push",
      chocoDir / s"scala-cli.$version.nupkg",
      "-s",
      "https://push.chocolatey.org/",
      "-k",
      chocoKey
    ).call(cwd = chocoDir)
  }
  def updateCentOsPackages() = T.command {
    val version = cli(Scala.defaultInternal).publishVersion()

    val targetDir   = os.pwd / "target"
    val packagesDir = targetDir / "scala-cli-packages"
    val centOsDir   = packagesDir / "CentOS"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch = "main"
    val repo   = s"git@github.com:Virtuslab/scala-cli-packages.git"

    // Cloning
    gitClone(repo, branch, targetDir)
    setupGithubRepo(packagesDir)

    // copy rpm package to repository
    os.copy(
      os.Path("artifacts", os.pwd) / "scala-cli-x86_64-pc-linux.rpm",
      centOsDir / "Packages" / s"scala-cli_$version.rpm"
    )

    // format: off
    val cmd =  Seq[os.Shellable](
      "docker", "run",
      "-v", s"$packagesDir:/packages",
      "-w", "/packages",
      "--env", "PGP_SECRET",
      "--env", "PGP_PASSPHRASE",
      "--env", "GPG_EMAIL",
      "--env", "KEYGRIP",
      "--privileged",
      "fedora",
      "sh", "updateCentOsPackages.sh"
    )
    // format: on

    os.proc(cmd).call(cwd = packagesDir)

    commitChanges(s"Update CentOS packages for $version", branch, packagesDir)
  }
  private def vsBasePaths = Seq(
    os.Path("C:\\Program Files\\Microsoft Visual Studio"),
    os.Path("C:\\Program Files (x86)\\Microsoft Visual Studio")
  )
  def copyVcRedist(directory: String = "artifacts", distName: String = "vc_redist.x64.exe") =
    T.command {
      def vcVersions = Seq("2022", "2019", "2017")
      def vcEditions = Seq("Enterprise", "Community", "BuildTools")
      def candidateBaseDirs =
        for {
          vsBasePath <- vsBasePaths
          year       <- vcVersions
          edition    <- vcEditions
        } yield vsBasePath / year / edition / "VC" / "Redist" / "MSVC"
      val baseDirs = candidateBaseDirs.filter(os.isDir(_))
      if (baseDirs.isEmpty)
        sys.error(
          s"No Visual Studio installation found, tried:" + System.lineSeparator() +
            candidateBaseDirs
              .map("  " + _)
              .mkString(System.lineSeparator())
        )
      val orig = baseDirs
        .iterator
        .flatMap(os.list(_).iterator)
        .filter(os.isDir(_))
        .map(_ / distName)
        .filter(os.isFile(_))
        .take(1)
        .toList
        .headOption
        .getOrElse {
          sys.error(
            s"Error: $distName not found under any of:" + System.lineSeparator() +
              baseDirs
                .map("  " + _)
                .mkString(System.lineSeparator())
          )
        }
      val destDir = os.Path(directory, os.pwd)
      os.copy(orig, destDir / distName, createFolders = true, replaceExisting = true)
    }
  def writeWixConfigExtra(dest: String = "wix-visual-cpp-redist.xml") = T.command {
    val msmPath = {

      val vcVersions = Seq("2022", "2019", "2017")
      val vcEditions = Seq("Enterprise", "Community", "BuildTools")
      val vsDirs = Seq(
        os.Path("""C:\Program Files\Microsoft Visual Studio"""),
        os.Path("""C:\Program Files (x86)\Microsoft Visual Studio""")
      )
      val fileNamePrefix = "Microsoft_VC".toLowerCase(Locale.ROOT)
      val fileNameSuffix = "_CRT_x64.msm".toLowerCase(Locale.ROOT)
      def candidatesIt =
        for {
          vsDir   <- vsDirs.iterator
          version <- vcVersions.iterator
          edition <- vcEditions.iterator
          dir = vsDir / version / edition
          if os.isDir(dir)
          path <- os.walk.stream(dir)
            .filter { p =>
              p.last.toLowerCase(Locale.ROOT).startsWith(fileNamePrefix) &&
              p.last.toLowerCase(Locale.ROOT).endsWith(fileNameSuffix)
            }
            .filter(os.isFile(_))
            .toVector
            .iterator
        } yield path

      candidatesIt.take(1).toList.headOption.getOrElse {
        sys.error(s"$fileNamePrefix*$fileNameSuffix not found")
      }
    }
    val content =
      s"""<DirectoryRef Id="TARGETDIR">
         |  <Merge Id="VCRedist" SourceFile="$msmPath" DiskId="1" Language="0"/>
         |</DirectoryRef>
         |<Feature Id="VCRedist" Title="Visual C++ Redistributable" AllowAdvertise="no" Display="hidden" Level="1">
         |  <MergeRef Id="VCRedist"/>
         |</Feature>
         |""".stripMargin
    val dest0 = os.Path(dest, os.pwd)
    os.write.over(dest0, content.getBytes(Charset.defaultCharset()), createFolders = true)
  }
  def setShouldPublish() = T.command {
    publish.setShouldPublish()
  }
  def shouldPublish() = T.command {
    println(publish.shouldPublish())
  }
  def copyJvm(jvm: String = deps.graalVmJvmId, dest: String = "jvm") = T.command {
    import sys.process._
    val command = Seq(
      settings.cs(),
      "java-home",
      "--jvm",
      jvm,
      "--update",
      "--ttl",
      "0"
    )
    val baseJavaHome = os.Path(command.!!.trim, os.pwd)
    System.err.println(s"Initial Java home $baseJavaHome")
    val destJavaHome = os.Path(dest, os.pwd)
    os.copy(baseJavaHome, destJavaHome, createFolders = true)
    System.err.println(s"New Java home $destJavaHome")
    destJavaHome
  }

  def checkScalaVersions() = T.command {
    website.checkMainScalaVersions(os.pwd / "website" / "docs" / "reference" / "scala-versions.md")
    website.checkScalaJsVersions(
      os.pwd / "website" / "docs" / "guides" / "advanced" / "scala-js.md"
    )
  }
}

def updateLicensesFile() = T.command {
  settings.updateLicensesFile()
}
