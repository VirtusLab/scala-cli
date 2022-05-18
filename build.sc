import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import $ivy.`io.get-coursier::coursier-launcher:2.1.0-M2`
import $ivy.`io.github.alexarchambault.mill::mill-native-image-upload:0.1.19`
import $file.project.deps, deps.{Deps, Docker, InternalDeps, Scala, TestDeps}
import $file.project.publish, publish.{ghOrg, ghName, ScalaCliPublishModule}
import $file.project.settings, settings.{
  CliLaunchers,
  FormatNativeImageConf,
  HasMacroAnnotations,
  HasTests,
  LocalRepo,
  PublishLocalNoFluff,
  ScalaCliCrossSbtModule,
  ScalaCliScalafixModule,
  ScalaCliCompile,
  localRepoResourcePath,
  platformExecutableJarExtension,
  workspaceDirName
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
implicit def millModuleBasePath: define.BasePath =
  define.BasePath(super.millModuleBasePath.value / "modules")

object cli extends Cli3 with Bloop.Module {
  def skipBloop = true
  object test extends Tests {
    def moduleDeps = super.moduleDeps ++ Seq(
      `build-module`(myScalaVersion).test
    )
  }
}

// remove once we do not have blockers with Scala 3
object cli2 extends Cli {
  def myScalaVersion = Scala.scala213
  def sources = T.sources {
    super.sources() ++ cli.sources()
  }
  def resources = T.sources {
    super.resources() ++ cli.resources()
  }
  object test extends Tests {
    def sources = T.sources {
      super.sources() ++ cli.test.sources()
    }
    def resources = T.sources {
      super.resources() ++ cli.test.resources()
    }
    def moduleDeps = super.moduleDeps ++ Seq(
      `build-module`(myScalaVersion).test
    )
  }
}

object `cli-options`  extends CliOptions
object `build-macros` extends Cross[BuildMacros](Scala.mainVersions: _*)
object options        extends Cross[Options](Scala.mainVersions: _*)
object scalaparse     extends ScalaParse
object javaparse      extends JavaParse
object directives     extends Cross[Directives](Scala.mainVersions: _*)
object core           extends Cross[Core](Scala.mainVersions: _*)
object `build-module` extends Cross[Build](Scala.mainVersions: _*)
object runner         extends Cross[Runner](Scala.all: _*)
object `test-runner`  extends Cross[TestRunner](Scala.all: _*)
object `bloop-rifle`  extends Cross[BloopRifle](Scala.all: _*)
object `tasty-lib`    extends Cross[TastyLib](Scala.all: _*)
// Runtime classes used within native image on Scala 3 replacing runtime from Scala
object `scala3-runtime` extends Scala3Runtime
// Logic to process classes that is shared between build and the scala-cli itself
object `scala3-graal` extends Cross[Scala3Graal](Scala.mainVersions: _*)
// Main app used to process classpath within build itself
object `scala3-graal-processor` extends Scala3GraalProcessor

object stubs extends JavaModule with ScalaCliPublishModule {
  def javacOptions = T {
    super.javacOptions() ++ Seq("-target", "8", "-source", "8")
  }
}
object `scala-cli-bsp` extends JavaModule with ScalaCliPublishModule {
  def ivyDeps = super.ivyDeps() ++ Seq(
    Deps.bsp4j
  )
  def javacOptions = T {
    super.javacOptions() ++ Seq("-target", "8", "-source", "8")
  }
}
object integration extends CliIntegration {
  object test extends Tests
  object docker extends CliIntegrationDocker {
    object test extends Tests {
      def sources = T.sources {
        super.sources() ++ integration.sources()
      }
      def tmpDirBase = T.persistent {
        PathRef(T.dest / "working-dir")
      }
      def forkEnv = super.forkEnv() ++ Seq(
        "SCALA_CLI_TMP"   -> tmpDirBase().path.toString,
        "SCALA_CLI_IMAGE" -> "scala-cli",
        "CI"              -> "1",
        "ACTUAL_CI"       -> (if (System.getenv("CI") == null) "" else "1")
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
        "SCALA_CLI_IMAGE" -> "scala-cli-slim",
        "CI"              -> "1",
        "ACTUAL_CI"       -> (if (System.getenv("CI") == null) "" else "1")
      )
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
  def repositories = super.repositories ++ customRepositories
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

class BuildMacros(val crossScalaVersion: String) extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule
    with HasTests {
  def scalacOptions = T {
    super.scalacOptions() ++
      (if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused") else Nil)
  }
  def compileIvyDeps = T {
    if (scalaVersion().startsWith("3"))
      super.compileIvyDeps()
    else
      super.compileIvyDeps() ++ Agg(
        Deps.scalaReflect(scalaVersion())
      )
  }

  object test extends Tests {

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
      if (crossScalaVersion.startsWith("3.")) {
        val base = os.pwd / "modules" / "build-macros" / "src"
        val negativeTests = Seq(
          "MismatchedLeft.scala" -> Seq(
            "Found\\: +EE1".r,
            "Found\\: +EE2".r,
            "Required\\: +E2".r
          )
        )

        val cpsSource = base / "main" / "scala-3.1" / "scala" / "build" / "EitherCps.scala"
        assert(os.exists(cpsSource))

        val cli = compileScalaCli.get.path // we need scala-cli
        def compile(extraSources: os.Path*) =
          os.proc(cli, "compile", "-S", crossScalaVersion, cpsSource, extraSources).call(
            check =
              false,
            mergeErrIntoOut = true
          )
        assert(0 == compile().exitCode)

        val notPassed = negativeTests.filter { case (testName, expectedErrors) =>
          val testFile = base / "negative-tests" / testName
          val res      = compile(testFile)
          println(s"Compiling $testName:")
          println(res.out.text)
          val name = testFile.last
          if (res.exitCode != 0) {
            println(s"Test case $name failed to compile as expected")
            val lines = res.out.lines
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
      ()
    }
  }
}

def asyncScalacOptions(scalaVersion: String) =
  if (scalaVersion.startsWith("3")) Nil else Seq("-Xasync")

trait ProtoBuildModule extends ScalaCliPublishModule with HasTests
    with ScalaCliScalafixModule

trait BuildLikeModule extends ScalaCliCrossSbtModule with ProtoBuildModule {

  def scalacOptions = T {
    super.scalacOptions() ++
      (if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused") else Nil) ++
      Seq("-deprecation")
  }

  def repositories = super.repositories ++ customRepositories

  def localRepoJar = T {
    `local-repo`.localRepoJar()
  }
}

class Core(val crossScalaVersion: String) extends BuildLikeModule {
  def moduleDeps = Seq(
    `bloop-rifle`(),
    `build-macros`()
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(scalaVersion())
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.collectionCompat,
    Deps.coursierJvm
      // scalaJsEnvNodeJs brings a guava version that conflicts with this
      .exclude(("com.google.collections", "google-collections"))
      // Coursier is not cross-compiled and pulls jsoniter-scala-macros in 2.13
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros")),
    Deps.jsoniterMacros, // pulls jsoniter macros manually
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

  private def vcsState = T.persistent {
    val isCI  = System.getenv("CI") != null
    val state = VcsVersion.vcsState().format()
    if (isCI) state
    else state + "-maybe-stale"
  }

  def constantsFile = T.persistent {
    val dir  = T.dest / "constants"
    val dest = dir / "Constants.scala"
    val testRunnerMainClass = `test-runner`(Scala.defaultInternal)
      .mainClass()
      .getOrElse(sys.error("No main class defined for test-runner"))
    val runnerMainClass = runner(Scala.defaultInternal)
      .mainClass()
      .getOrElse(sys.error("No main class defined for runner"))
    val detailedVersionValue =
      if (`local-repo`.developingOnStubModules) s"""Some("${vcsState()}")"""
      else "None"
    val testRunnerOrganization = `test-runner`(Scala.defaultInternal)
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
         |  def scalajsEnvJsdomNodejsVersion = "${Deps.scalaJsEnvJsdomNodejs.dep.version}"
         |  def scalaNativeVersion = "${Deps.nativeTools.dep.version}"
         |
         |  def scalaJsCliVersion = "${InternalDeps.Versions.scalaJsCli}"
         |
         |  def stubsOrganization = "${stubs.pomSettings().organization}"
         |  def stubsModuleName = "${stubs.artifactName()}"
         |  def stubsVersion = "${stubs.publishVersion()}"
         |
         |  def testRunnerOrganization = "$testRunnerOrganization"
         |  def testRunnerModuleName = "${`test-runner`(Scala.defaultInternal).artifactName()}"
         |  def testRunnerVersion = "${`test-runner`(Scala.defaultInternal).publishVersion()}"
         |  def testRunnerMainClass = "$testRunnerMainClass"
         |
         |  def runnerOrganization = "${runner(Scala.defaultInternal).pomSettings().organization}"
         |  def runnerModuleName = "${runner(Scala.defaultInternal).artifactName()}"
         |  def runnerVersion = "${runner(Scala.defaultInternal).publishVersion()}"
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
         |  def defaultScalafmtVersion = "${Deps.scalafmtCli.dep.version}"
         |
         |  def defaultScalaVersion = "${Scala.defaultUser}"
         |  def defaultScala212Version = "${Scala.scala212}"
         |  def defaultScala213Version = "${Scala.scala213}"
         |
         |  def workspaceDirName = "$workspaceDirName"
         |
         |  def defaultGraalVMJavaVersion = ${deps.graalVmJavaVersion}
         |  def defaultGraalVMVersion = "${deps.graalVmVersion}"
         |
         |  def scalaCliSigningVersion = "${Deps.signingCli.dep.version}"
         |
         |  def libsodiumVersion = "${deps.libsodiumVersion}"
         |  def libsodiumjniVersion = "${Deps.libsodiumjni.dep.version}"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code, createFolders = true)
    PathRef(dir)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())
}

class Directives(val crossScalaVersion: String) extends BuildLikeModule {
  def moduleDeps = Seq(
    `options`(),
    `core`()
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(scalaVersion())
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
    Deps.scalametaTrees,
    Deps.scalaparse,
    Deps.usingDirectives
  )

  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.pprint
    )
    def runClasspath = T {
      super.runClasspath() ++ Seq(localRepoJar())
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

class Options(val crossScalaVersion: String) extends BuildLikeModule {
  def moduleDeps = Seq(
    `core`(),
    `build-macros`()
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(scalaVersion())
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.bloopConfig,
    Deps.signingCliShared
  )

  object test extends Tests {
    // uncomment below to debug tests in attach mode on 5005 port
    // def forkArgs = T {
    //   super.forkArgs() ++ Seq("-agentlib:jdwp=transport=dt_socket,server=n,address=localhost:5005,suspend=y")
    // }
  }
}

trait ScalaParse extends SbtModule with ScalaCliPublishModule with ScalaCliCompile {
  def ivyDeps      = super.ivyDeps() ++ Agg(Deps.scalaparse)
  def scalaVersion = Scala.scala213
}

trait JavaParse extends SbtModule with ScalaCliPublishModule with ScalaCliCompile {
  def ivyDeps = super.ivyDeps() ++ Agg(Deps.scala3Compiler(scalaVersion()))

  // pin scala3-library suffix, so that 2.13 modules can have us as moduleDep fine
  def mandatoryIvyDeps = T {
    super.mandatoryIvyDeps().map { dep =>
      val isScala3Lib =
        dep.dep.module.organization.value == "org.scala-lang" &&
        dep.dep.module.name.value == "scala3-library" &&
        (dep.cross match {
          case _: CrossVersion.Binary => true
          case _                      => false
        })
      if (isScala3Lib)
        dep.copy(
          dep = dep.dep.withModule(
            dep.dep.module.withName(
              coursier.ModuleName(dep.dep.module.name.value + "_3")
            )
          ),
          cross = CrossVersion.empty(dep.cross.platformed)
        )
      else dep
    }
  }
  def scalaVersion = Scala.scala3
}

trait Scala3Runtime extends SbtModule with ScalaCliPublishModule with ScalaCliCompile {
  def ivyDeps      = super.ivyDeps()
  def scalaVersion = Scala.scala3
}

class Scala3Graal(val crossScalaVersion: String) extends BuildLikeModule {
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.osLib
  )

  def resources = T.sources {
    val extraResourceDir = T.dest / "extra"
    // scala3RuntimeFixes.jar is also used within
    // resource-config.json and BytecodeProcessor.scala
    os.copy.over(
      `scala3-runtime`.jar().path,
      extraResourceDir / "scala3RuntimeFixes.jar",
      createFolders = true
    )
    super.resources() ++ Seq(mill.PathRef(extraResourceDir))
  }
}

trait Scala3GraalProcessor extends ScalaModule {
  def moduleDeps     = Seq(`scala3-graal`(Scala.scala3))
  def scalaVersion   = Scala.scala3
  def finalMainClass = "scala.cli.graal.CoursierCacheProcessor"
}

class Build(val crossScalaVersion: String) extends BuildLikeModule {
  def millSourcePath = super.millSourcePath / os.up / "build"
  def moduleDeps = Seq(
    `options`(),
    scalaparse,
    javaparse,
    `directives`(),
    `scala-cli-bsp`,
    `test-runner`(),
    `tasty-lib`()
  )
  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(scalaVersion())
  }

  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.jsoniterMacros,
    Deps.svm
  )
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.asm,
    Deps.collectionCompat,
    Deps.jsoniterCore,
    Deps.nativeTestRunner,
    Deps.osLib,
    Deps.pprint,
    Deps.scalaJsEnvNodeJs,
    Deps.scalaJsTestAdapter,
    Deps.scalametaTrees,
    Deps.swoval,
    Deps.zipInputStream
  ) ++ (if (scalaVersion().startsWith("3")) Agg() else Agg(Deps.shapeless))

  object test extends Tests {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.pprint,
      Deps.slf4jNop
    )
    def runClasspath = T {
      super.runClasspath() ++ Seq(localRepoJar())
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

trait CliOptions extends SbtModule with ScalaCliPublishModule with ScalaCliCompile {
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.caseApp,
    Deps.jsoniterCore,
    Deps.jsoniterMacros,
    Deps.osLib,
    Deps.signingCliOptions
  )
  def scalaVersion = Scala.scala213
  def repositories = super.repositories ++ customRepositories
}

trait Cli extends SbtModule with ProtoBuildModule with CliLaunchers
    with HasMacroAnnotations with FormatNativeImageConf {

  def constantsFile = T.persistent {
    val dir  = T.dest / "constants"
    val dest = dir / "Constants.scala"
    val code =
      s"""package scala.cli.internal
         |
         |/** Build-time constants. Generated by mill. */
         |object Constants {
         |  def launcherTypeResourcePath = "${launcherTypeResourcePath.toString}"
         |  def defaultFilesResourcePath = "$defaultFilesResourcePath"
         |}
         |""".stripMargin
    if (!os.isFile(dest) || os.read(dest) != code)
      os.write.over(dest, code, createFolders = true)
    PathRef(dir)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())

  def defaultFilesResources = T.persistent {
    val dir = T.dest / "resources"
    val resources = Seq(
      "https://raw.githubusercontent.com/scala-cli/default-workflow/main/.github/workflows/ci.yml" -> (os.sub / "workflows" / "default.yml"),
      "https://raw.githubusercontent.com/scala-cli/default-workflow/main/.gitignore" -> (os.sub / "gitignore")
    )
    for ((srcUrl, destRelPath) <- resources) {
      val dest = dir / defaultFilesResourcePath / destRelPath
      if (!os.isFile(dest)) {
        val content = Using.resource(new URL(srcUrl).openStream())(_.readAllBytes())
        os.write(dest, content, createFolders = true)
      }
    }
    PathRef(dir)
  }
  override def resources = T.sources {
    super.resources() ++ Seq(defaultFilesResources())
  }

  def myScalaVersion: String

  def scalaVersion = T(myScalaVersion)

  def scalacOptions = T {
    super.scalacOptions() ++ asyncScalacOptions(scalaVersion()) ++
      (if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused") else Nil)
  }
  def javacOptions = T {
    super.javacOptions() ++ Seq("--release", "16")
  }
  def moduleDeps = Seq(
    `build-module`(myScalaVersion),
    `cli-options`,
    `test-runner`(myScalaVersion),
    `scala3-graal`(myScalaVersion)
  )

  def repositories = super.repositories ++ customRepositories

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.coursierLauncher,
    Deps.coursierPublish,
    Deps.jimfs, // scalaJsEnvNodeJs pulls jimfs:1.1, whose class path seems borked (bin compat issue with the guava version it depends on)
    Deps.jniUtils,
    Deps.jsoniterCore,
    Deps.libsodiumjni,
    Deps.metaconfigTypesafe,
    Deps.scalaPackager,
    Deps.signingCli,
    Deps.slf4jNop, // to silence jgit
    Deps.sttp
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.jsoniterMacros,
    Deps.svm
  )
  def mainClass = Some("scala.cli.ScalaCli")

  def localRepoJar = `local-repo`.localRepoJar()

  trait Tests extends super.Tests with ScalaCliScalafixModule {
    def runClasspath = T {
      super.runClasspath() ++ Seq(localRepoJar())
    }
  }
}

trait Cli3 extends Cli {
  def myScalaVersion = Scala.scala3

  override def nativeImageClassPath = T {
    val classpath = super.nativeImageClassPath().map(_.path).mkString(File.pathSeparator)
    val cache     = T.dest / "native-cp"
    // `scala3-graal-processor`.run() do not give me output and I cannot pass dynamically computed values like classpath
    val res = mill.modules.Jvm.callSubprocess(
      mainClass = `scala3-graal-processor`.finalMainClass(),
      classPath = `scala3-graal-processor`.runClasspath().map(_.path),
      mainArgs = Seq(cache.toNIO.toString, classpath),
      workingDir = os.pwd
    )
    val cp = res.out.text.trim
    cp.split(File.pathSeparator).toSeq.map(p => mill.PathRef(os.Path(p)))
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
    super.scalacOptions() ++ Seq("-Xasync", "-Ywarn-unused", "-deprecation")
  }

  def modulesPath = T {
    val name                = mainArtifactName().stripPrefix(prefix)
    val baseIntegrationPath = os.Path(millSourcePath.toString.stripSuffix(name))
    baseIntegrationPath.toString.stripSuffix(baseIntegrationPath.baseName)
  }
  def sources = T.sources {
    val mainPath = PathRef(os.Path(modulesPath()) / "integration" / "src" / "main" / "scala")
    super.sources() ++ Seq(mainPath)
  }
  def resources = T.sources {
    val mainPath = PathRef(os.Path(modulesPath()) / "integration" / "src" / "main" / "resources")
    super.resources() ++ Seq(mainPath)
  }

  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )

  private def mainArtifactName = T(artifactName())
  trait Tests extends super.Tests with ScalaCliScalafixModule {
    def ivyDeps = super.ivyDeps() ++ Agg(
      Deps.bsp4j,
      Deps.coursier,
      Deps.dockerClient,
      Deps.jsoniterCore,
      Deps.jsoniterMacros,
      Deps.libsodiumjni,
      Deps.pprint,
      Deps.scalaAsync,
      Deps.slf4jNop
    )
    def forkEnv = super.forkEnv() ++ Seq(
      "SCALA_CLI_TMP" -> tmpDirBase().path.toString,
      "CI"            -> "1",
      "ACTUAL_CI"     -> (if (System.getenv("CI") == null) "" else "1")
    )
    private def updateRef(name: String, ref: PathRef): PathRef = {
      val rawPath = ref.path.toString.replace(
        File.separator + name + File.separator,
        File.separator
      )
      PathRef(os.Path(rawPath))
    }
    def sources = T.sources {
      val name = mainArtifactName().stripPrefix(prefix)
      super.sources().flatMap { ref =>
        Seq(updateRef(name, ref), ref)
      }
    }
    def resources = T.sources {
      val name = mainArtifactName().stripPrefix(prefix)
      super.resources().flatMap { ref =>
        Seq(updateRef(name, ref), ref)
      }
    }

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
           |  def bspVersion = "${Deps.bsp4j.dep.version}"
           |  def scala212 = "${Scala.scala212}"
           |  def scala213 = "${Scala.scala213}"
           |  def scalaSnapshot213 = "${TestDeps.scalaSnapshot213}"
           |  def scala3   = "${Scala.scala3}"
           |  def defaultScala = "${Scala.defaultUser}"
           |  def defaultScalafmtVersion = "${Deps.scalafmtCli.dep.version}"
           |  def bloopVersion = "${Deps.bloopConfig.dep.version}"
           |  def pprintVersion = "${TestDeps.pprint.dep.version}"
           |  def munitVersion = "${TestDeps.munit.dep.version}"
           |  def dockerTestImage = "${Docker.testImage}"
           |  def dockerAlpineTestImage = "${Docker.alpineTestImage}"
           |  def authProxyTestImage = "${Docker.authProxyTestImage}"
           |  def mostlyStaticDockerfile = "${mostlyStaticDockerfile.toString.replace("\\", "\\\\")}"
           |  def cs = "${settings.cs().replace("\\", "\\\\")}"
           |  def workspaceDirName = "$workspaceDirName"
           |  def libsodiumVersion = "${deps.libsodiumVersion}"
           |  def dockerArchLinuxImage = "${TestDeps.archLinuxImage}"
           |
           |  def ghOrg = "$ghOrg"
           |  def ghName = "$ghName"
           |}
           |""".stripMargin
      if (!os.isFile(dest) || os.read(dest) != code)
        os.write.over(dest, code, createFolders = true)
      PathRef(dir)
    }
    def generatedSources = super.generatedSources() ++ Seq(constantsFile())

    private final class TestHelper(
      launcherTask: T[PathRef],
      cliKind: String
    ) {
      def doTest(args: String*) =
        T.command {
          val argsTask = T.task {
            val launcher = launcherTask().path
            val extraArgs = Seq(
              s"-Dtest.scala-cli.path=$launcher",
              s"-Dtest.scala-cli.kind=$cliKind"
            )
            args ++ extraArgs
          }
          testTask(argsTask, T.task(Seq.empty[String]))
        }
      def test(args: String*) =
        T.command {
          val res            = doTest(args: _*)()
          val dotScalaInRoot = os.pwd / workspaceDirName
          assert(
            !os.isDir(dotScalaInRoot),
            s"Expected $workspaceDirName ($dotScalaInRoot) not to have been created"
          )
          res
        }
    }

    def test(args: String*) =
      jvm(args: _*)

    def jvm(args: String*) =
      new TestHelper(
        cli.standaloneLauncher,
        "jvm"
      ).test(args: _*)
    def native(args: String*) =
      new TestHelper(
        cli.nativeImage,
        "native"
      ).test(args: _*)
    def nativeStatic(args: String*) =
      new TestHelper(
        cli.nativeImageStatic,
        "native-static"
      ).test(args: _*)
    def nativeMostlyStatic(args: String*) =
      new TestHelper(
        cli.nativeImageMostlyStatic,
        "native-mostly-static"
      ).test(args: _*)
  }
}

trait CliIntegrationDocker extends SbtModule with ScalaCliPublishModule with HasTests {
  def scalaVersion = Scala.scala213
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.osLib
  )
}

class Runner(val crossScalaVersion: String) extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def scalacOptions = T {
    super.scalacOptions() ++ {
      if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused")
      else Nil
    } ++ Seq("-release", "8")

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

class TestRunner(val crossScalaVersion: String) extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
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

class BloopRifle(val crossScalaVersion: String) extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with HasTests
    with ScalaCliScalafixModule {
  def scalacOptions = T {
    super.scalacOptions() ++
      (if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused") else Nil) ++
      Seq("-deprecation")
  }
  def ivyDeps = super.ivyDeps() ++ Agg(
    Deps.bsp4j,
    Deps.collectionCompat,
    Deps.libdaemonjvm,
    Deps.snailgun(force213 = scalaVersion().startsWith("3."))
  )
  def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    Deps.svm
  )
  def mainClass = Some("scala.build.blooprifle.BloopRifle")

  def constantsFile = T.persistent {
    val dir  = T.dest / "constants"
    val dest = dir / "Constants.scala"
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
      os.write.over(dest, code, createFolders = true)
    PathRef(dir)
  }
  def generatedSources = super.generatedSources() ++ Seq(constantsFile())

  object test extends Tests with ScalaCliScalafixModule
}

class TastyLib(val crossScalaVersion: String) extends ScalaCliCrossSbtModule
    with ScalaCliPublishModule
    with ScalaCliScalafixModule {
  def scalacOptions = T(
    super.scalacOptions() ++ {
      if (scalaVersion().startsWith("2.")) Seq("-Ywarn-unused")
      else Nil
    }
  )

}

object `local-repo` extends LocalRepo {

  /*
   * If you are developing locally on any of the stub modules (stubs, runner, test-runner),
   * set this to true, so that Mill's watch mode takes into account changes in those modules
   * when embedding their JARs in the Scala CLI launcher.
   */
  def developingOnStubModules = false

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
    data = define.Target.sequence(tasks.value)(),
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
  `build-module`(Scala.defaultInternal).test.test()()
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
    integration.test.native()()
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
  def updateScalaCliSetup() = T.command {
    val version = cli.publishVersion()

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
    commitChanges(s"Update scala-cli version to $version", targetBranch, mainDir)
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
    val version = cli.publishVersion()

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
  def updateCentOsPackages() = T.command {
    val version = cli.publishVersion()

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
    website.checkScalaJsVersions(os.pwd / "website" / "docs" / "guides" / "scala-js.md")
  }
}

def updateLicensesFile() = T.command {
  settings.updateLicensesFile()
}
