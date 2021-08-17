import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import $ivy.`io.get-coursier::coursier-launcher:2.0.16+73-gddc6d9cc9`
import $ivy.`io.github.alexarchambault.mill::mill-native-image-upload:0.1.9`
import $file.project.deps, deps.{Deps, Docker, Scala}
import $file.project.publish, publish.{ghOrg, ghName, ScalaCliPublishModule}
import $file.project.settings, settings.{CliLaunchers, FormatNativeImageConf, HasMacroAnnotations, HasTests, LocalRepo, PublishLocalNoFluff, localRepoResourcePath, platformExecutableJarExtension}

import java.io.File

import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.github.alexarchambault.millnativeimage.upload.Upload
import mill._, scalalib.{publish => _, _}
import mill.contrib.bloop.Bloop

import _root_.scala.util.Properties


// Tell mill modules are under modules/
implicit def millModuleBasePath: define.BasePath =
  define.BasePath(super.millModuleBasePath.value / "modules")


object cli                    extends Cli
object `cli-core`             extends CliCore
object build                  extends Cross[Build]             (Scala.defaultInternal)
object stubs                  extends JavaModule with ScalaCliPublishModule
object runner                 extends Cross[Runner]            (Scala.all: _*)
object `test-runner`          extends Cross[TestRunner]        (Scala.all: _*)
object `bloop-rifle`          extends Cross[BloopRifle]        (Scala.allScala2: _*)
object `tasty-lib`            extends Cross[TastyLib]          (Scala.all: _*)

object `integration-core` extends Module {
  object jvm    extends JvmIntegrationCore {
    object test extends Tests
  }
  object native extends NativeIntegrationCore with Bloop.Module {
    def skipBloop = true
    object test extends Tests with Bloop.Module {
      def skipBloop = true
    }
  }
  object `native-static` extends NativeIntegrationCoreStatic with Bloop.Module {
    def skipBloop = true
    object test extends Tests with Bloop.Module {
      def skipBloop = true
    }
  }
  object `native-mostly-static` extends NativeIntegrationCoreMostlyStatic with Bloop.Module {
    def skipBloop = true
    object test extends Tests with Bloop.Module {
      def skipBloop = true
    }
  }
}

object integration extends Module {
  object docker extends CliIntegrationDockerCore {
    object test extends Tests {
      def sources = T.sources {
        super.sources() ++ `integration-core`.jvm.sources()
      }
    }
  }
  object jvm    extends JvmIntegration {
    object test extends Tests {
      def sources = T.sources {
        super.sources() ++ `integration-core`.jvm.test.sources()
      }
    }
  }
  object native extends NativeIntegration with Bloop.Module {
    def skipBloop = true
    object test extends Tests with Bloop.Module {
      def skipBloop = true
      def sources = T.sources {
        super.sources() ++ `integration-core`.native.test.sources()
      }
    }
  }
}

object packager extends ScalaModule with Bloop.Module {
  def skipBloop = true
  def scalaVersion = Scala.scala213
  def ivyDeps = Agg(
    Deps.scalaPackagerCli
  )
  def mainClass = Some("packager.cli.PackagerCli")
}

object `generate-reference-doc` extends SbtModule {
  def scalaVersion = Scala.defaultInternal
  def moduleDeps = Seq(
    cli
  )
  def ivyDeps = Agg(
    Deps.caseApp,
    Deps.munit
  )
  def mainClass = Some("scala.cli.doc.GenerateReferenceDoc")
}

object dummy extends Module {
  // dummy project to get scala steward updates for Ammonite, whose
  // version is used in the repl command, and ensure Ammonite is available
  // for all Scala versions we support
  object amm extends Cross[Amm](Scala.listAll: _*)
  class Amm(val crossScalaVersion: String) extends CrossScalaModule with Bloop.Module {
    def skipBloop = true
    def ivyDeps = Agg(
      Deps.ammonite
    )
  }
}


class Build(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule with HasTests {
  def moduleDeps = Seq(
    `bloop-rifle`(),
    `test-runner`(),
    `tasty-lib`()
  )
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.bloopConfig,
    Deps.coursierJvm
      // scalaJsEnvNodeJs brings a guava version that conflicts with this
      .exclude(("com.google.collections", "google-collections")),
    Deps.dependency,
    Deps.guava, // for coursierJvm / scalaJsEnvNodeJs, see above
    Deps.nativeTestRunner,
    Deps.nativeTools,
    Deps.osLib,
    Deps.pprint,
    Deps.pureconfig,
    Deps.scalaJsEnvNodeJs,
    Deps.scalaJsLinkerInterface,
    Deps.scalaJsTestAdapter,
    Deps.scalametaTrees,
    Deps.scalaparse,
    Deps.shapeless,
    Deps.swoval
  )

  private def vcsState = {
    val isCI = System.getenv("CI") != null
    if (isCI)
      T.persistent {
        VcsVersion.vcsState()
      }
    else
      T {
        VcsVersion.vcsState()
      }
  }
  def constantsFile = T.persistent {
    val dest = T.dest / "Constants.scala"
    val code =
      s"""package scala.build.internal
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def version = "${publishVersion()}"
         |  def detailedVersion = "${vcsState().format()}"
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
         |  def testRunnerMainClass = "${`test-runner`(Scala.defaultInternal).mainClass().getOrElse(sys.error("No main class defined for test-runner"))}"
         |
         |  def runnerOrganization = "${runner(Scala.defaultInternal).pomSettings().organization}"
         |  def runnerModuleName = "${runner(Scala.defaultInternal).artifactName()}"
         |  def runnerVersion = "${runner(Scala.defaultInternal).publishVersion()}"
         |  def runnerMainClass = "${runner(Scala.defaultInternal).mainClass().getOrElse(sys.error("No main class defined for runner"))}"
         |  def runnerNeedsSonatypeSnapshots(sv: String): Boolean =
         |    ${ if (Deps.prettyStacktraces.dep.version.endsWith("SNAPSHOT")) """ !sv.startsWith("2.") """ else "false" }
         |
         |  def semanticDbPluginOrganization = "${Deps.scalametaTrees.dep.module.organization.value}"
         |  def semanticDbPluginModuleName = "semanticdb-scalac"
         |  def semanticDbPluginVersion = "${Deps.scalametaTrees.dep.version}"
         |
         |  def localRepoResourcePath = "$localRepoResourcePath"
         |  def localRepoVersion = "${vcsState().format()}"
         |
         |  def jmhVersion = "1.29"
         |
         |  def ammoniteVersion = "${Deps.ammonite.dep.version}"
         |
         |  def defaultScalaVersion = "${Scala.defaultUser}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code)
    PathRef(dest)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())

  def localRepoJar = T{
    `local-repo`.localRepoJar()
  }

  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.pprint
    )
    def runClasspath = T{
      super.runClasspath() ++ Seq(localRepoJar())
    }
  }
}

trait Cli extends SbtModule with CliLaunchers with ScalaCliPublishModule with FormatNativeImageConf with HasTests {
  def scalaVersion = Scala.defaultInternal
  def moduleDeps = Seq(
    `cli-core`
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.svm
  )
  def mainClass = Some("scala.cli.ScalaCli")

  def localRepoJar = `local-repo`.localRepoJar()
  def graalVmVersion = deps.graalVmVersion

  object test extends Tests
}

trait CliCore extends SbtModule with CliLaunchers with ScalaCliPublishModule with FormatNativeImageConf with HasMacroAnnotations {
  def scalaVersion = Scala.defaultInternal
  def moduleDeps = Seq(
    build(Scala.defaultInternal),
    `test-runner`(Scala.defaultInternal)
  )
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.caseApp,
    Deps.coursierLauncher,
    Deps.dataClass,
    Deps.jimfs, // scalaJsEnvNodeJs pulls jimfs:1.1, whose class path seems borked (bin compat issue with the guava version it depends on)
    Deps.jniUtils,
    Deps.scalaJsLinker,
    Deps.scalaPackager,
    Deps.svmSubs
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.svm
  )
  def mainClass = Some("scala.cli.ScalaCliCore")

  def localRepoJar = `local-repo`.localRepoJar()
  def graalVmVersion = deps.graalVmVersion
}

trait CliIntegrationBase extends SbtModule with ScalaCliPublishModule with HasTests {
  def scalaVersion = sv
  def testLauncher: T[PathRef]
  def cliKind: T[String]

  def sv = Scala.scala213

  def prefix: String

  def sources = T.sources {
    val name = mainArtifactName().stripPrefix(prefix)
    val baseIntegrationPath = os.Path(millSourcePath.toString.stripSuffix(name))
    val modulesPath = os.Path(baseIntegrationPath.toString.stripSuffix(baseIntegrationPath.baseName))
    val mainPath = PathRef(modulesPath / "integration-core" / "src" / "main" / "scala")
    super.sources() ++ Seq(mainPath)
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )

  private def mainArtifactName = T{ artifactName() }
  trait Tests extends super.Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.bsp4j,
      Deps.pprint,
      Deps.scalaAsync,
      Deps.upickle
    )
    def forkEnv = super.forkEnv() ++ Seq(
      "SCALA_CLI" -> testLauncher().path.toString,
      "SCALA_CLI_KIND" -> cliKind()
    )
    def sources = T.sources {
      val name = mainArtifactName().stripPrefix(prefix)
      super.sources().flatMap { ref =>
        val base = PathRef(os.Path(ref.path.toString.replace(File.separator + name + File.separator, File.separator)))
        Seq(base, ref)
      }
    }

    def constantsFile = T.persistent {
      val dest = T.dest / "Constants.scala"
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
           |
           |  def dockerTestImage = "${Docker.testImage}"
           |  def dockerAlpineTestImage = "${Docker.alpineTestImage}"
           |}
           |""".stripMargin
      if (!os.isFile(dest) || os.read(dest) != code)
        os.write.over(dest, code)
      PathRef(dest)
    }
    def generatedSources = super.generatedSources() ++ Seq(constantsFile())

    def test(args: String*) = T.command {
      val res = super.test(args: _*)()
      val dotScalaInRoot = os.pwd / ".scala"
      assert(!os.isDir(dotScalaInRoot), s"Expected .scala ($dotScalaInRoot) not to have been created")
      res
    }
  }
}

trait CliIntegrationDockerCore extends SbtModule with ScalaCliPublishModule with HasTests {
  def scalaVersion = Scala.scala213
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )
}

trait CliIntegration extends CliIntegrationBase {
  def prefix = "integration-"
}

trait CliIntegrationCore extends CliIntegration {
  def prefix = "integration-core-"
}

trait NativeIntegration extends CliIntegration {
  def testLauncher = cli.nativeImage()
  def cliKind = "native"
}

trait JvmIntegration extends CliIntegration {
  def testLauncher = cli.launcher()
  def cliKind = "jvm"
}

trait NativeIntegrationCore extends CliIntegrationCore {
  def testLauncher = `cli-core`.nativeImage()
  def cliKind = "native"
}

trait NativeIntegrationCoreStatic extends CliIntegrationCore {
  def testLauncher = `cli-core`.nativeImageStatic()
  def cliKind = "native-static"
}

trait NativeIntegrationCoreMostlyStatic extends CliIntegrationCore {
  def testLauncher = `cli-core`.nativeImageMostlyStatic()
  def cliKind = "native-mostly-static"
}

trait JvmIntegrationCore extends CliIntegrationCore {
  def testLauncher = `cli-core`.launcher()
  def cliKind = "jvm"
}

class Runner(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule {
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
      } else Nil
    val extraDirs = scala3DirNames.map(name => PathRef(millSourcePath / "src" / "main" / name))
    super.sources() ++ extraDirs
  }
}

class TestRunner(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule {
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.testInterface
  )
  def mainClass = Some("scala.build.testrunner.DynamicTestRunner")
}

class BloopRifle(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule {
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.bsp4j,
    Deps.ipcSocket,
    Deps.snailgun
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
         |  def bspVersion = "${Deps.bsp4j.dep.version}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code)
    PathRef(dest)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())
}

class TastyLib(val crossScalaVersion: String) extends CrossSbtModule with ScalaCliPublishModule

object `local-repo` extends LocalRepo {
  def stubsModules = {
    val javaModules = Seq(
      stubs
    )
    val crossModules = for {
      sv <- Scala.all
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
  val version = rawVersion.takeWhile(c => c != '-' && c != '+')
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

def copyCoreLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = `cli-core`.nativeImage().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true
  )
}

def copyJvmLauncher(directory: String = "artifacts") = T.command {
  val launcher = cli.standaloneLauncher().path
  os.copy(launcher, os.Path(directory, os.pwd) / s"scala-cli$platformExecutableJarExtension", createFolders = true, replaceExisting = true)
}

def uploadLaunchers(directory: String = "artifacts") = T.command {
  val version = cli.publishVersion()

  val path = os.Path(directory, os.pwd)
  val launchers = os.list(path).filter(os.isFile(_)).map { path =>
    path.toNIO -> path.last
  }
  val ghToken = Option(System.getenv("UPLOAD_GH_TOKEN")).getOrElse {
    sys.error("UPLOAD_GH_TOKEN not set")
  }
  val (tag, overwriteAssets) =
    if (version.endsWith("-SNAPSHOT")) ("nightly", true)
    else ("v" + version, false)
  Upload.upload(ghOrg, ghName, ghToken, tag, dryRun = false, overwrite = overwriteAssets)(launchers: _*)
}

def unitTests() = T.command {
  build(Scala.defaultInternal).test.test()()
  cli.test.test()()
}

def scala(args: String*) = T.command {
  cli.run(args: _*)()
}

def tightMemory = Properties.isLinux || Properties.isWin

def defaultNativeImage() =
  if (tightMemory)
    T.command {
      `cli-core`.nativeImage()
    }
  else
    T.command {
      cli.nativeImage()
    }

def nativeIntegrationTests() =
  if (tightMemory)
    T.command {
      `integration-core`.native.test.test()()
    }
  else
    T.command {
      integration.native.test.test()()
    }

def copyDefaultLauncher(directory: String = "artifacts") =
  if (tightMemory)
    T.command {
      copyCoreLauncher(directory)()
    }
  else
    T.command {
      copyLauncher(directory)()
    }

def copyMostlyStaticLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = `cli-core`.nativeImageMostlyStatic().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true,
    suffix = "-mostly-static"
  )
}

def copyStaticLauncher(directory: String = "artifacts") = T.command {
  val nativeLauncher = `cli-core`.nativeImageStatic().path
  Upload.copyLauncher(
    nativeLauncher,
    directory,
    "scala-cli",
    compress = true,
    suffix = "-static"
  )
}

// TODO Move most CI-specific tasks there
object ci extends Module {
  def copyVcRedist(directory: String = "artifacts", distName: String = "vc_redist.x64.exe") = T.command {
    def vcVersions = Seq("2019", "2017")
    def vcEditions = Seq("Enterprise", "Community", "BuildTools")
    def candidateBaseDirs =
      for {
        year <- vcVersions
        edition <- vcEditions
      } yield os.Path("C:\\Program Files (x86)\\Microsoft Visual Studio") / year / edition / "VC" / "Redist" / "MSVC"
    val baseDirs = candidateBaseDirs.filter(os.isDir(_))
    if (baseDirs.isEmpty)
      sys.error(s"No Visual Studio installation found, tried:" + System.lineSeparator() + candidateBaseDirs.map("  " + _).mkString(System.lineSeparator()))
    val orig = baseDirs
      .iterator
      .flatMap(os.list(_).iterator)
      .filter(os.isDir(_))
      .map(_ / distName)
      .filter(os.isFile(_))
      .toStream
      .headOption
      .getOrElse {
        sys.error(s"Error: $distName not found under any of:" + System.lineSeparator() + baseDirs.map("  " + _).mkString(System.lineSeparator()))
      }
    val destDir = os.Path(directory, os.pwd)
    os.copy(orig, destDir / distName, createFolders = true, replaceExisting = true)
  }
}
