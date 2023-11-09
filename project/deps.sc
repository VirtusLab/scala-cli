import coursier.mavenRepositoryString
import mill._, scalalib._

import scala.util.Properties
import $file.utils, utils.isArmArchitecture

object Scala {
  def scala212     = "2.12.18"
  def scala213     = "2.13.12"
  def runnerScala3 = "3.0.2" // the newest version that is compatible with all Scala 3.x versions
  def scala3       = "3.3.1"

  // The Scala version used to build the CLI itself.
  def defaultInternal = sys.props.get("scala.version.internal").getOrElse(scala3)

  // The Scala version used by default to compile user input.
  def defaultUser = sys.props.get("scala.version.user").getOrElse(scala3)

  val allScala2           = Seq(scala213, scala212)
  val defaults            = Seq(defaultInternal, defaultUser).distinct
  val all                 = (allScala2 ++ Seq(scala3) ++ defaults).distinct
  val mainVersions        = (Seq(scala3, scala213) ++ defaults).distinct
  val runnerScalaVersions = runnerScala3 +: allScala2

  def scalaJs = "1.14.0"

  def listAll: Seq[String] = {
    def patchVer(sv: String): Int =
      sv.split('.').drop(2).head.takeWhile(_.isDigit).toInt
    val max212 = patchVer(scala212)
    val max213 = patchVer(scala213)
    val max30  = 2
    val max31  = 3
    val max32  = 2
    val max33  = patchVer(scala3)
    (8 until max212).map(i => s"2.12.$i") ++ Seq(scala212) ++
      (0 until max213).map(i => s"2.13.$i") ++ Seq(scala213) ++
      (0 to max30).map(i => s"3.0.$i") ++
      (0 to max31).map(i => s"3.1.$i") ++
      (0 to max32).map(i => s"3.2.$i") ++
      (0 until max33).map(i => s"3.3.$i") ++ Seq(scala3)
  }

  def maxAmmoniteScala212Version = scala212
  def maxAmmoniteScala213Version = scala213
  def maxAmmoniteScala3Version   = scala3
  lazy val listMaxAmmoniteScalaVersion =
    Seq(maxAmmoniteScala212Version, maxAmmoniteScala213Version, maxAmmoniteScala3Version)
  lazy val listAllAmmonite = {
    import coursier.core.Version
    val max212 = Version(maxAmmoniteScala212Version)
    val max213 = Version(maxAmmoniteScala213Version)
    val max3   = Version(maxAmmoniteScala3Version)
    listAll.filter { v =>
      if (v.startsWith("3."))
        Version(v).compareTo(max3) <= 0
      else if (v.startsWith("2.13."))
        Version(v).compareTo(max213) <= 0
      else if (v.startsWith("2.12."))
        Version(v).compareTo(max212) <= 0
      else
        true
    }
  }
}

// Dependencies used in integration test fixtures
object TestDeps {
  def pprint           = Deps.pprint
  def munit            = Deps.munit
  def scalaSnapshot213 = "2.13.8-bin-e814d78"

  def archLinuxImage =
    "archlinux@sha256:b15db21228c7cd5fd3ab364a97193ba38abfad0e8b9593c15b71850b74738153"
}

object InternalDeps {
  object Versions {
    def mill          = os.read(os.pwd / ".mill-version").trim
    def lefouMillwRef = "166bcdf5741de8569e0630e18c3b2ef7e252cd96"
  }
}

object Deps {
  object Versions {
    // jni-utils version may need to be sync-ed when bumping the coursier version
    def coursierDefault      = "2.1.7"
    def coursier             = coursierDefault
    def coursierCli          = coursierDefault
    def coursierM1Cli        = coursierDefault
    def jsoniterScala        = "2.23.2"
    def jsoniterScalaJava8   = "2.13.5.2"
    def scalaMeta            = "4.8.12"
    def scalaNative          = "0.4.16"
    def scalaPackager        = "0.1.29"
    def signingCli           = "0.2.3"
    def signingCliJvmVersion = 17
    def javaClassName        = "0.1.3"
  }
  // DO NOT hardcode a Scala version in this dependency string
  // This dependency is used to ensure that Ammonite is available for Scala versions
  // that Scala CLI supports.
  def ammonite = ivy"com.lihaoyi:::ammonite:3.0.0-M0-56-1bcbe7f6"
  def asm      = ivy"org.ow2.asm:asm:9.6"
  // Force using of 2.13 - is there a better way?
  def bloopConfig      = ivy"ch.epfl.scala:bloop-config_2.13:1.5.5"
  def bloopRifle       = ivy"io.github.alexarchambault.bleep:bloop-rifle_2.13:1.5.11-sc-2"
  def bsp4j            = ivy"ch.epfl.scala:bsp4j:2.1.0-M7"
  def caseApp          = ivy"com.github.alexarchambault::case-app:2.1.0-M26"
  def collectionCompat = ivy"org.scala-lang.modules::scala-collection-compat:2.11.0"
  // Force using of 2.13 - is there a better way?
  def coursier           = ivy"io.get-coursier:coursier_2.13:${Versions.coursier}"
  def coursierJvm        = ivy"io.get-coursier:coursier-jvm_2.13:${Versions.coursier}"
  def coursierLauncher   = ivy"io.get-coursier:coursier-launcher_2.13:${Versions.coursier}"
  def coursierProxySetup = ivy"io.get-coursier:coursier-proxy-setup:${Versions.coursier}"
  def coursierPublish    = ivy"io.get-coursier.publish:publish_2.13:0.1.6"
  def dependency         = ivy"io.get-coursier::dependency:0.2.3"
  def dockerClient       = ivy"com.spotify:docker-client:8.16.0"
  // TODO bump once 0.15.5 is out
  def expecty = ivy"com.eed3si9n.expecty::expecty:0.16.0"
  def fansi   = ivy"com.lihaoyi::fansi:0.4.0"
  def giter8  = ivy"org.foundweekends.giter8:giter8:0.16.2"
  def guava   = ivy"com.google.guava:guava:32.1.3-jre"
  def javaClassName =
    ivy"org.virtuslab.scala-cli.java-class-name:java-class-name_3:${Versions.javaClassName}"
  def jgit     = ivy"org.eclipse.jgit:org.eclipse.jgit:6.7.0.202309050840-r"
  def jimfs    = ivy"com.google.jimfs:jimfs:1.3.0"
  def jniUtils = ivy"io.get-coursier.jniutils:windows-jni-utils:0.3.3"
  def jsoniterCore =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScalaJava8}"
  def jsoniterCoreJava8 =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScalaJava8}"
  def jsoniterMacros =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScalaJava8}"
  def jsoniterMacrosJava8 =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScalaJava8}"
  def libsodiumjni  = ivy"org.virtuslab.scala-cli:libsodiumjni:0.0.3"
  def macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  def metaconfigTypesafe =
    ivy"com.geirsson::metaconfig-typesafe-config:0.12.0"
      .exclude(("org.scala-lang", "scala-compiler"))
  def munit                      = ivy"org.scalameta::munit:0.7.29"
  def nativeTestRunner           = ivy"org.scala-native::test-runner:${Versions.scalaNative}"
  def nativeTools                = ivy"org.scala-native::tools:${Versions.scalaNative}"
  def osLib                      = ivy"com.lihaoyi::os-lib:0.9.2"
  def pprint                     = ivy"com.lihaoyi::pprint:0.8.1"
  def pythonInterface            = ivy"io.github.alexarchambault.python:interface:0.1.0"
  def pythonNativeLibs           = ivy"ai.kien::python-native-libs:0.2.4"
  def scala3Compiler(sv: String) = ivy"org.scala-lang:scala3-compiler_3:$sv"
  def scalaAsync         = ivy"org.scala-lang.modules::scala-async:1.0.1".exclude("*" -> "*")
  def scalac(sv: String) = ivy"org.scala-lang:scala-compiler:$sv"
  def scalafmtCli        = ivy"org.scalameta:scalafmt-cli_2.13:3.7.15"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvJsdomNodejs =
    ivy"org.scala-js:scalajs-env-jsdom-nodejs_2.13:1.1.0"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvNodeJs = ivy"org.scala-js:scalajs-env-nodejs_2.13:1.4.0"
  def scalaJsLogging   = ivy"org.scala-js:scalajs-logging_2.13:1.1.1"
  // Force using of 2.13 - is there a better way?
  def scalaJsTestAdapter = ivy"org.scala-js:scalajs-sbt-test-adapter_2.13:${Scala.scalaJs}"
  // Force using of 2.13 - is there a better way?
  def scalametaTrees   = ivy"org.scalameta:trees_2.13:${Versions.scalaMeta}"
  def scalaPackager    = ivy"org.virtuslab:scala-packager_2.13:${Versions.scalaPackager}"
  def scalaPackagerCli = ivy"org.virtuslab:scala-packager-cli_2.13:${Versions.scalaPackager}"
  def scalaPy          = ivy"dev.scalapy::scalapy-core::0.5.3"
  def scalaReflect(sv: String) = ivy"org.scala-lang:scala-reflect:$sv"
  def semanticDbJavac          = ivy"com.sourcegraph:semanticdb-javac:0.7.4"
  def semanticDbScalac         = ivy"org.scalameta:::semanticdb-scalac:${Versions.scalaMeta}"
  def signingCliShared =
    ivy"org.virtuslab.scala-cli-signing::shared:${Versions.signingCli}"
      // to prevent collisions with scala-cli's case-app version
      .exclude(("com.github.alexarchambault", "case-app_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_3"))
  def signingCli =
    ivy"org.virtuslab.scala-cli-signing::cli:${Versions.signingCli}"
      // to prevent collisions with scala-cli's case-app version
      .exclude(("com.github.alexarchambault", "case-app_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_3"))
  def slf4jNop                = ivy"org.slf4j:slf4j-nop:2.0.9"
  def sttp                    = ivy"com.softwaremill.sttp.client3:core_2.13:3.9.1"
  def svm                     = ivy"org.graalvm.nativeimage:svm:$graalVmVersion"
  def swoval                  = ivy"com.swoval:file-tree-views:2.1.12"
  def testInterface           = ivy"org.scala-sbt:test-interface:1.0"
  val toolkitVersion          = "0.1.7"
  def toolkit                 = ivy"org.scala-lang:toolkit:$toolkitVersion"
  def toolkitTest             = ivy"org.scala-lang:toolkit-test:$toolkitVersion"
  val typelevelToolkitVersion = "0.0.11"
  def typelevelToolkit        = ivy"org.typelevel:toolkit:$typelevelToolkitVersion"
  def typelevelToolkitTest    = ivy"org.typelevel:toolkit-test:$typelevelToolkitVersion"
  def usingDirectives         = ivy"org.virtuslab:using_directives:1.1.0"
  // Lives at https://github.com/VirtusLab/no-crc32-zip-input-stream, see #865
  // This provides a ZipInputStream that doesn't verify CRC32 checksums, that users
  // can enable by setting SCALA_CLI_VENDORED_ZIS=true in the environment, to workaround
  // some bad GraalVM / zlib issues (see #828 and linked issues for more details).
  def zipInputStream = ivy"org.virtuslab.scala-cli.zip-input-stream:zip-input-stream:0.1.2"
}

def graalVmVersion     = "22.3.1"
def graalVmJavaVersion = 17
def graalVmJvmId       = s"graalvm-java$graalVmJavaVersion:$graalVmVersion"

def csDockerVersion = Deps.Versions.coursierCli

def buildCsVersion   = Deps.Versions.coursierCli
def buildCsM1Version = Deps.Versions.coursierM1Cli

// Native library used to encrypt GitHub secrets
def libsodiumVersion = "1.0.18"
// Using the libsodium static library from this Alpine version (in the static launcher)
def alpineVersion = "3.15"

object Docker {
  def customMuslBuilderImageName = "scala-cli-base-musl"
  def muslBuilder =
    s"$customMuslBuilderImageName:latest"

  def testImage = "ubuntu:18.04"
  def alpineTestImage =
    "alpine@sha256:4edbd2beb5f78b1014028f4fbb99f3237d9561100b6881aabbf5acce2c4f9454"
  def authProxyTestImage =
    "bahamat/authenticated-proxy@sha256:568c759ac687f93d606866fbb397f39fe1350187b95e648376b971e9d7596e75"
}

def customRepositories =
  Seq(
    coursier.Repositories.sonatype("snapshots"),
    coursier.MavenRepository("https://s01.oss.sonatype.org/content/repositories/snapshots")
    // Uncomment for local development
    // coursier.LocalRepositories.Dangerous.maven2Local
  )
