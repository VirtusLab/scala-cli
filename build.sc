import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import $ivy.`io.get-coursier::coursier-launcher:2.0.16+73-gddc6d9cc9`
import $ivy.`io.github.alexarchambault.mill::mill-native-image-upload:0.1.12`
import $file.project.deps, deps.{Deps, Docker, Scala, TestDeps}
import $file.project.publish, publish.{ghOrg, ghName, ScalaCliPublishModule}
import $file.project.settings, settings.{
  CliLaunchers,
  FormatNativeImageConf,
  HasMacroAnnotations,
  HasTests,
  LocalRepo,
  PublishLocalNoFluff,
  ScalaCliScalafixModule,
  localRepoResourcePath,
  platformExecutableJarExtension
}

import java.io.File
import java.nio.charset.Charset
import java.util.Locale

import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.github.alexarchambault.millnativeimage.upload.Upload
import mill._, scalalib.{publish => _, _}
import mill.contrib.bloop.Bloop

import _root_.scala.util.Properties

// Tell mill modules are under modules/
implicit def millModuleBasePath: define.BasePath =
  define.BasePath(super.millModuleBasePath.value / "modules")

object cli            extends Cli
object `build-macros` extends Cross[BuildMacros](Scala.defaultInternal)
object build          extends Cross[Build](Scala.defaultInternal)
object runner         extends Cross[Runner](Scala.all: _*)
object `test-runner`  extends Cross[TestRunner](Scala.all: _*)
object `bloop-rifle`  extends Cross[BloopRifle](Scala.allScala2: _*)
object `tasty-lib`    extends Cross[TastyLib](Scala.all: _*)

object stubs extends JavaModule with ScalaCliPublishModule {
  def javacOptions = T {
    super.javacOptions() ++ Seq("-target", "8", "-source", "8")
  }
}
object integration extends Module {
  object docker extends CliIntegrationDocker {
    object test extends Tests {
      def sources = T.sources {
        super.sources() ++ integration.jvm.sources()
      }
      def tmpDirBase = T.persistent {
        PathRef(T.dest / "working-dir")
      }
      def forkEnv = super.forkEnv() ++ Seq(
        "SCALA_CLI_TMP"   -> tmpDirBase().path.toString,
        "SCALA_CLI_IMAGE" -> "scala-cli"
      )
    }
  }
  object `docker-slim` extends CliIntegrationDocker {
    object test extends Tests {
      def sources = T.sources {
        integration.docker.test.sources()
      }
      def tmpDirBase = T.persistent {
        PathRef(T.dest / "working-dir")
      }
      def forkEnv = super.forkEnv() ++ Seq(
        "SCALA_CLI_TMP"   -> tmpDirBase().path.toString,
        "SCALA_CLI_IMAGE" -> "scala-cli-slim"
      )
    }
  }
  object jvm extends JvmIntegration {
    object test extends Tests
  }
  object native extends NativeIntegration with Bloop.Module {
    def skipBloop = true
    object test extends Tests with Bloop.Module {
      def skipBloop = true
    }
  }
  object `native-static` extends NativeIntegrationStatic with Bloop.Module {
    def skipBloop = true
    object test extends Tests with Bloop.Module {
      def skipBloop = true
    }
  }
  object `native-mostly-static` extends NativeIntegrationMostlyStatic with Bloop.Module {
    def skipBloop = true
    object test extends Tests with Bloop.Module {
      def skipBloop = true
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

object `generate-reference-doc` extends SbtModule with ScalaCliScalafixModule {
  def scalaVersion = Scala.defaultInternal
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Ywarn-unused")
  }
  def moduleDeps = Seq(
    cli
  )
  def repositories = super.repositories ++ Seq(
    coursier.Repositories.sonatype("snapshots")
  )
  def ivyDeps = Agg(
    Deps.caseApp,
    Deps.munit
  )
  def mainClass = Some("scala.cli.doc.GenerateReferenceDoc")
}

object dummy extends Module {
  // dummy projects to get scala steward updates for Ammonite and scalafmt, whose
  // versions are used in the fmt and repl commands, and ensure Ammonite is available
  // for all Scala versions we support.
  object amm extends Cross[Amm](Scala.listAll: _*)
  class Amm(val crossScalaVersion: String) extends CrossScalaModule with Bloop.Module {
    def skipBloop = true
    def ivyDeps = Agg(
      Deps.ammonite
    )
    def compile = T {
      resolvedRunIvyDeps()
      null: mill.scalalib.api.CompilationResult
    }
  }
  object scalafmt extends ScalaModule with Bloop.Module {
    def skipBloop    = true
    def scalaVersion = Scala.defaultInternal
    def ivyDeps = Agg(
      Deps.scalafmtCli
    )
    def compile = T {
      resolvedRunIvyDeps()
      null: mill.scalalib.api.CompilationResult
    }
  }
}

class BuildMacros(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Ywarn-unused")
  }
  def compileIvyDeps = T {
    super.compileIvyDeps() ++ Agg(
      Deps.scalaReflect(scalaVersion())
    )
  }
}

class Build(val crossScalaVersion: String)
    extends CrossSbtModule with ScalaCliPublishModule with HasTests with ScalaCliScalafixModule {
  def moduleDeps = Seq(
    `bloop-rifle`(),
    `build-macros`(),
    `test-runner`(),
    `tasty-lib`()
  )
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Xasync", "-Ywarn-unused", "-deprecation")
  }
  def repositories = super.repositories ++ Seq(
    coursier.Repositories.sonatype("snapshots")
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.svm
  )
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.bloopConfig,
    Deps.collectionCompat,
    Deps.coursierJvm
      // scalaJsEnvNodeJs brings a guava version that conflicts with this
      .exclude(("com.google.collections", "google-collections")),
    Deps.dependency,
    Deps.guava, // for coursierJvm / scalaJsEnvNodeJs, see above
    Deps.nativeTestRunner,
    Deps.nativeTools,
    Deps.osLib,
    Deps.pprint,
    Deps.scalaJsEnvNodeJs,
    Deps.scalaJsLinkerInterface,
    Deps.scalaJsTestAdapter,
    Deps.scalametaTrees,
    Deps.scalaparse,
    Deps.shapeless,
    Deps.swoval,
    Deps.upickle,
    Deps.usingDirectives
  )

  private def vcsState = T.persistent {
    val isCI  = System.getenv("CI") != null
    val state = VcsVersion.vcsState().format()
    if (isCI) state
    else state + "-maybe-stale"
  }
  def constantsFile = T.persistent {
    val dest = T.dest / "Constants.scala"
    val testRunnerMainClass = `test-runner`(Scala.defaultInternal)
      .mainClass()
      .getOrElse(sys.error("No main class defined for test-runner"))
    val runnerMainClass = runner(Scala.defaultInternal)
      .mainClass()
      .getOrElse(sys.error("No main class defined for runner"))
    val runnerNeedsSonatypeSnapshots =
      if (Deps.prettyStacktraces.dep.version.endsWith("SNAPSHOT"))
        """ !sv.startsWith("2.") """
      else
        "false"
    val code =
      s"""package scala.build.internal
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def version = "${publishVersion()}"
         |  def detailedVersion = "${vcsState()}"
         |
         |  def scalaJsVersion = "${Deps.scalaJsLinker.dep.version}"
         |  def scalaNativeVersion = "${Deps.nativeTools.dep.version}"
         |
         |  def stubsOrganization = "${stubs.pomSettings().organization}"
         |  def stubsModuleName = "${stubs.artifactName()}"
         |  def stubsVersion = "${stubs.publishVersion()}"
         |
         |  def testRunnerOrganization = "${`test-runner`(Scala.defaultInternal).pomSettings().organization}"
         |  def testRunnerModuleName = "${`test-runner`(Scala.defaultInternal).artifactName()}"
         |  def testRunnerVersion = "${`test-runner`(Scala.defaultInternal).publishVersion()}"
         |  def testRunnerMainClass = "$testRunnerMainClass"
         |
         |  def runnerOrganization = "${runner(Scala.defaultInternal).pomSettings().organization}"
         |  def runnerModuleName = "${runner(Scala.defaultInternal).artifactName()}"
         |  def runnerVersion = "${runner(Scala.defaultInternal).publishVersion()}"
         |  def runnerMainClass = "$runnerMainClass"
         |  def runnerNeedsSonatypeSnapshots(sv: String): Boolean =
         |    $runnerNeedsSonatypeSnapshots
         |
         |  def semanticDbPluginOrganization = "${Deps.scalametaTrees.dep.module.organization.value}"
         |  def semanticDbPluginModuleName = "semanticdb-scalac"
         |  def semanticDbPluginVersion = "${Deps.scalametaTrees.dep.version}"
         |
         |  def localRepoResourcePath = "$localRepoResourcePath"
         |
         |  def jmhVersion = "1.29"
         |
         |  def ammoniteVersion = "${Deps.ammonite.dep.version}"
         |
         |  def defaultScalafmtVersion = "${Deps.scalafmtCli.dep.version}"
         |
         |  def defaultScalaVersion = "${Scala.defaultUser}"
         |  def defaultScala212Version = "${Scala.scala212}"
         |  def defaultScala213Version = "${Scala.scala213}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code)
    PathRef(dest)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())

  def localRepoJar = T {
    `local-repo`.localRepoJar()
  }

  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.pprint
    )
    def runClasspath = T {
      super.runClasspath() ++ Seq(localRepoJar())
    }

    def generatedSources = super.generatedSources() ++ Seq(constantsFile())

    def constantsFile = T.persistent {
      val dest = T.dest / "Constants2.scala"
      val code =
        s"""package scala.build.tests
           |
           |/** Build-time constants. Generated by mill. */
           |object Constants {
           |  def cs = "${settings.cs().replace("\\", "\\\\")}"
           |}
           |""".stripMargin
      if (!os.isFile(dest) || os.read(dest) != code)
        os.write.over(dest, code)
      PathRef(dest)
    }

    // uncomment below to debug tests in attach mode on 5005 port
    // def forkArgs = T {
    //   super.forkArgs() ++ Seq("-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y")
    // }
  }
}

trait Cli extends SbtModule with CliLaunchers with ScalaCliPublishModule with FormatNativeImageConf
    with HasTests with HasMacroAnnotations with ScalaCliScalafixModule {
  def scalaVersion = Scala.defaultInternal
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Xasync", "-Ywarn-unused", "-deprecation")
  }
  def moduleDeps = Seq(
    build(Scala.defaultInternal),
    `test-runner`(Scala.defaultInternal)
  )
  def repositories = super.repositories ++ Seq(
    coursier.Repositories.sonatype("snapshots")
  )
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.caseApp,
    Deps.coursierLauncher,
    Deps.dataClass,
    Deps.jimfs, // scalaJsEnvNodeJs pulls jimfs:1.1, whose class path seems borked (bin compat issue with the guava version it depends on)
    Deps.jniUtils,
    Deps.scalaJsLinker,
    Deps.scalaPackager,
    Deps.svmSubs,
    Deps.upickle
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.svm
  )
  def mainClass = Some("scala.cli.ScalaCli")

  def localRepoJar   = `local-repo`.localRepoJar()
  def graalVmVersion = deps.graalVmVersion

  object test extends Tests with ScalaCliScalafixModule
}

trait CliIntegrationBase extends SbtModule with ScalaCliPublishModule with HasTests
    with ScalaCliScalafixModule {
  def scalaVersion = sv
  def testLauncher: T[PathRef]
  def cliKind: T[String]

  def sv = Scala.scala213

  def prefix: String

  def tmpDirBase = T.persistent {
    PathRef(T.dest / "working-dir")
  }
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Xasync", "-Ywarn-unused", "-deprecation")
  }

  def sources = T.sources {
    val name                = mainArtifactName().stripPrefix(prefix)
    val baseIntegrationPath = os.Path(millSourcePath.toString.stripSuffix(name))
    val modulesPath = os.Path(
      baseIntegrationPath.toString.stripSuffix(baseIntegrationPath.baseName)
    )
    val mainPath = PathRef(modulesPath / "integration" / "src" / "main" / "scala")
    super.sources() ++ Seq(mainPath)
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )

  private def mainArtifactName = T(artifactName())
  trait Tests extends super.Tests with ScalaCliScalafixModule {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.bsp4j,
      Deps.pprint,
      Deps.scalaAsync,
      Deps.upickle
    )
    def forkEnv = super.forkEnv() ++ Seq(
      "SCALA_CLI"      -> testLauncher().path.toString,
      "SCALA_CLI_KIND" -> cliKind(),
      "SCALA_CLI_TMP"  -> tmpDirBase().path.toString
    )
    def sources = T.sources {
      val name = mainArtifactName().stripPrefix(prefix)
      super.sources().flatMap { ref =>
        val rawPath = ref.path.toString.replace(
          File.separator + name + File.separator,
          File.separator
        )
        val base = PathRef(os.Path(rawPath))
        Seq(base, ref)
      }
    }

    def constantsFile = T.persistent {
      val dest = T.dest / "Constants.scala"
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
           |  def bspVersion = "${Deps.bsp4j.dep.version}"
           |  def scala212 = "${Scala.scala212}"
           |  def scala213 = "${Scala.scala213}"
           |  def scala3   = "${Scala.scala3}"
           |  def defaultScala = "${Scala.defaultUser}"
           |  def bloopVersion = "${Deps.bloopConfig.dep.version}"
           |  def pprintVersion = "${TestDeps.pprint.dep.version}"
           |  def munitVersion = "${TestDeps.munit.dep.version}"
           |  def dockerTestImage = "${Docker.testImage}"
           |  def dockerAlpineTestImage = "${Docker.alpineTestImage}"
           |  def mostlyStaticDockerfile = "${mostlyStaticDockerfile.toString.replace("\\", "\\\\")}"
           |  def cs = "${settings.cs().replace("\\", "\\\\")}"
           |}
           |""".stripMargin
      if (!os.isFile(dest) || os.read(dest) != code)
        os.write.over(dest, code)
      PathRef(dest)
    }
    def generatedSources = super.generatedSources() ++ Seq(constantsFile())

    def test(args: String*) = T.command {
      val res            = super.test(args: _*)()
      val dotScalaInRoot = os.pwd / ".scala"
      assert(
        !os.isDir(dotScalaInRoot),
        s"Expected .scala ($dotScalaInRoot) not to have been created"
      )
      res
    }
  }
}

trait CliIntegrationDocker extends SbtModule with ScalaCliPublishModule with HasTests {
  def scalaVersion = Scala.scala213
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )
}

trait CliIntegration extends CliIntegrationBase {
  def prefix = "integration-"
}

trait NativeIntegration extends CliIntegration {
  def testLauncher = cli.nativeImage()
  def cliKind      = "native"
}

trait NativeIntegrationStatic extends CliIntegration {
  def testLauncher = cli.nativeImageStatic()
  def cliKind      = "native-static"
}

trait NativeIntegrationMostlyStatic extends CliIntegration {
  def testLauncher = cli.nativeImageMostlyStatic()
  def cliKind      = "native-mostly-static"
}

trait JvmIntegration extends CliIntegration {
  def testLauncher = cli.launcher()
  def cliKind      = "jvm"
}

class Runner(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def scalacOptions = T {
    super.scalacOptions() ++ {
      if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused")
      else Nil
    } ++ Seq("-release", "8")

  }
  def mainClass = Some("scala.cli.runner.Runner")
  def ivyDeps =
    if (crossScalaVersion.startsWith("3.") && !crossScalaVersion.contains("-RC"))
      Agg(Deps.prettyStacktraces)
    else
      Agg.empty[Dep]
  def repositories = {
    val base = super.repositories
    val extra =
      if (Deps.prettyStacktraces.dep.version.endsWith("SNAPSHOT"))
        Seq(coursier.Repositories.sonatype("snapshots"))
      else
        Nil

    base ++ extra
  }
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

class TestRunner(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def scalacOptions = T {
    super.scalacOptions() ++ {
      if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused", "-deprecation")
      else Nil
    } ++ Seq("-release", "8")
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.collectionCompat,
    Deps.testInterface
  )
  def mainClass = Some("scala.build.testrunner.DynamicTestRunner")
}

class BloopRifle(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule
    with HasTests
    with ScalaCliScalafixModule {
  def scalacOptions = T {
    super.scalacOptions() ++ Seq("-Ywarn-unused", "-deprecation")
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.bsp4j,
    Deps.collectionCompat,
    Deps.ipcSocket,
    Deps.snailgun
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.svm
  )
  def mainClass = Some("scala.build.blooprifle.BloopRifle")

  def constantsFile = T.persistent {
    val dest = T.dest / "Constants.scala"
    val code =
      s"""package scala.build.blooprifle.internal
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def bloopVersion = "${Deps.bloopConfig.dep.version}"
         |  def bloopScalaVersion = "${Scala.scala212}"
         |  def bspVersion = "${Deps.bsp4j.dep.version}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code)
    PathRef(dest)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())

  object test extends Tests with ScalaCliScalafixModule
}

class TastyLib(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def scalacOptions = T(
    super.scalacOptions() ++ {
      if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused")
      else Nil
    }
  )

}

object `local-repo` extends LocalRepo {
  def stubsModules = {
    val javaModules = Seq(
      stubs
    )
    val crossModules = for {
      sv   <- Scala.all
      proj <- Seq(runner, `test-runner`)
    } yield proj(sv)
    javaModules ++ crossModules
  }
  def version = runner(Scala.defaultInternal).publishVersion()
}

// Helper CI commands

def publishSonatype(tasks: mill.main.Tasks[PublishModule.PublishData]) = T.command {
  publish.publishSonatype(
    data = define.Task.sequence(tasks.value)(),
    log = T.ctx().log
  )
}

def copyTo(task: mill.main.Tasks[PathRef], dest: os.Path) = T.command {
  if (task.value.length > 1)
    sys.error("Expected a single task")
  val ref = task.value.head()
  os.makeDir.all(dest / os.up)
  os.copy.over(ref.path, dest)
}

def writePackageVersionTo(dest: os.Path) = T.command {
  val rawVersion = cli.publishVersion()
  val version =
    if (rawVersion.contains("+")) rawVersion.stripSuffix("-SNAPSHOT")
    else rawVersion
  os.write.over(dest, version)
}

def writeShortPackageVersionTo(dest: os.Path) = T.command {
  val rawVersion = cli.publishVersion()
  val version    = rawVersion.takeWhile(c => c != '-' && c != '+')
  os.write.over(dest, version)
}

def copyLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = cli.nativeImage().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true
  )
}

def copyJvmLauncher(directory: String = "artifacts") = T.command {
  val launcher = cli.standaloneLauncher().path
  os.copy(
    launcher,
    os.Path(directory, os.pwd) / s"scala-cli$platformExecutableJarExtension",
    createFolders = true,
    replaceExisting = true
  )
}

def uploadLaunchers(directory: String = "artifacts") = T.command {
  val version = cli.publishVersion()

  val path = os.Path(directory, os.pwd)
  val launchers = os.list(path).filter(os.isFile(_)).map { path =>
    path.toNIO -> path.last
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
  build(Scala.defaultInternal).test.test()()
  cli.test.test()()
}

def scala(args: String*) = T.command {
  cli.run(args: _*)()
}

def defaultNativeImage() =
  T.command {
    cli.nativeImage()
  }

def nativeIntegrationTests() =
  T.command {
    integration.native.test.test()()
  }

def copyDefaultLauncher(directory: String = "artifacts") =
  T.command {
    copyLauncher(directory)()
  }

def copyMostlyStaticLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = cli.nativeImageMostlyStatic().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true,
    suffix = "-mostly-static"
  )
}

def copyStaticLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = cli.nativeImageStatic().path
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

private def commitChanges(name: String, branch: String, repoDir: os.Path): Unit = {
  if (os.proc("git", "status").call(cwd = repoDir).out.text().trim.contains("nothing to commit"))
    println("Nothing Changes")
  else {
    os.proc("git", "add", "-A").call(cwd = repoDir)
    os.proc("git", "commit", "-am", name).call(cwd = repoDir)
    println(s"Trying to push on $branch branch")
    os.proc("git", "push", "origin", branch).call(cwd = repoDir)
    println(s"Push successfully on $branch branch")
  }
}

// TODO Move most CI-specific tasks there
object ci extends Module {
  def publishVersion() = T.command {
    println(cli.publishVersion())
  }
  def updateStandaloneLauncher() = T.command {
    val version = cli.publishVersion()

    val targetDir                     = os.pwd / "target"
    val scalaCliDir                   = targetDir / "scala-cli"
    val standaloneLauncherPath        = scalaCliDir / "scala-cli.sh"
    val standaloneWindowsLauncherPath = scalaCliDir / "scala-cli.bat"

    // clean scala-cli directory
    if (os.exists(scalaCliDir)) os.remove.all(scalaCliDir)
    if (!os.exists(targetDir)) os.makeDir.all(targetDir)

    val branch = "master"
    val repo   = s"https://oauth2:${ghToken()}@github.com/VirtusLab/scala-cli.git"

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

    commitChanges(s"Update scala-cli.sh launcher for $version", branch, scalaCliDir)
  }
  def updateBrewFormula() = T.command {
    val version = cli.publishVersion()

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

    val launcherPath = os.Path("artifacts", os.pwd) / "scala-cli-x86_64-apple-darwin.gz"
    val launcherURL =
      s"https://github.com/Virtuslab/scala-cli/releases/download/v$version/scala-cli-x86_64-apple-darwin.gz"

    val binarySha256 = os.proc("openssl", "dgst", "-sha256", "-binary")
      .call(
        cwd = targetDir,
        stdin = os.read.stream(launcherPath)
      ).out.bytes

    val sha256 = os.proc("xxd", "-p", "-c", "256")
      .call(
        cwd = targetDir,
        stdin = binarySha256
      ).out.text().trim

    val templateFormulaPath = os.pwd / ".github" / "scripts" / "scala-cli.rb.template"
    val template            = os.read(templateFormulaPath)

    val updatedFormula = template
      .replace("\"@LAUNCHER_URL@\"", s""""$launcherURL"""")
      .replace("\"@LAUNCHER_VERSION@\"", s""""$version"""")
      .replace("\"@LAUNCHER_SHA256@\"", s""""$sha256"""")

    val formulaPath = homebrewFormulaDir / "scala-cli.rb"
    os.write.over(formulaPath, updatedFormula)

    commitChanges(s"Update for $version", branch, homebrewFormulaDir)
  }
  def updateInstallationScript() = T.command {
    val version = cli.publishVersion()

    val targetDir              = os.pwd / "target"
    val packagesDir            = targetDir / "scala-cli-packages"
    val installationScriptPath = packagesDir / "scala-setup.sh"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch = "master"
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
    val version = cli.publishVersion()

    val targetDir   = os.pwd / "target"
    val packagesDir = targetDir / "scala-cli-packages"
    val debianDir   = packagesDir / "debian"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch = "master"
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

    commitChanges(s"Update Debian packages for $version", "master", packagesDir)
  }
  def updateCentOsPackages() = T.command {
    val version = cli.publishVersion()

    val targetDir   = os.pwd / "target"
    val packagesDir = targetDir / "scala-cli-packages"
    val centOsDir   = packagesDir / "CentOS"

    // clean target directory
    if (os.exists(targetDir)) os.remove.all(targetDir)

    os.makeDir.all(targetDir)

    val branch = "master"
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
  private def vsBasePath = os.Path("C:\\Program Files (x86)\\Microsoft Visual Studio")
  def copyVcRedist(directory: String = "artifacts", distName: String = "vc_redist.x64.exe") =
    T.command {
      def vcVersions = Seq("2019", "2017")
      def vcEditions = Seq("Enterprise", "Community", "BuildTools")
      def candidateBaseDirs =
        for {
          year    <- vcVersions
          edition <- vcEditions
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

      val vcVersions     = Seq("2019", "2017")
      val vcEditions     = Seq("Enterprise", "Community", "BuildTools")
      val vsDir          = os.Path("""C:\Program Files (x86)\Microsoft Visual Studio""")
      val fileNamePrefix = "Microsoft_VC".toLowerCase(Locale.ROOT)
      val fileNameSuffix = "_CRT_x64.msm".toLowerCase(Locale.ROOT)
      def candidatesIt =
        for {
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
}
