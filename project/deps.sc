import coursier.mavenRepositoryString
import mill._, scalalib._

import scala.util.Properties
import $file.utils, utils.isArmArchitecture

object Scala {
  def scala212     = "2.12.17"
  def scala213     = "2.13.10"
  def runnerScala3 = "3.0.2" // the newest version that is compatible with all Scala 3.x versions
  def scala3       = "3.2.2"
  val allScala2    = Seq(scala213, scala212)
  val all          = allScala2 ++ Seq(scala3)
  val mainVersions = Seq(scala3, scala213)
  val runnerScalaVersions = runnerScala3 +: allScala2

  def scalaJs = "1.13.0"

  def listAll: Seq[String] = {
    def patchVer(sv: String): Int =
      sv.split('.').drop(2).head.takeWhile(_.isDigit).toInt
    val max212 = patchVer(scala212)
    val max213 = patchVer(scala213)
    val max30  = 2
    val max31  = 3
    val max32  = patchVer(scala3)
    (8 until max212).map(i => s"2.12.$i") ++ Seq(scala212) ++
      (0 until max213).map(i => s"2.13.$i") ++ Seq(scala213) ++
      (0 to max30).map(i => s"3.0.$i") ++
      (0 to max31).map(i => s"3.1.$i") ++
      (0 until max32).map(i => s"3.2.$i") ++ Seq(scala3)
  }

  def maxAmmoniteScala212Version = scala212
  def maxAmmoniteScala213Version = scala213
  def maxAmmoniteScala3Version   = scala3
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

  // The Scala version used to build the CLI itself.
  // We should be able to switch to 3.x when it'll have CPS support
  // (for the either { value(…) } stuff)
  def defaultInternal = scala3

  // The Scala version used by default to compile user input.
  def defaultUser = scala3
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
    def coursier           = "2.1.0-RC4"
    def coursierCli        = "2.1.0-RC2"
    def coursierM1Cli      = "2.1.0-RC4"
    def jsoniterScala      = "2.20.3"
    def jsoniterScalaJava8 = "2.13.5.2"
    def scalaMeta          = "4.7.1"
    def scalaNative        = "0.4.9"
    def scalaPackager      = "0.1.29"
    def signingCli         = "0.1.16"
  }
  // DO NOT hardcode a Scala version in this dependency string
  // This dependency is used to ensure that Ammonite is available for Scala versions
  // that Scala CLI supports.
  def ammonite = ivy"com.lihaoyi:::ammonite:2.5.6-1-f8bff243"
  def asm      = ivy"org.ow2.asm:asm:9.4"
  def bloop    = ivy"io.github.alexarchambault.bleep:bloop-frontend_2.12:1.5.6-sc-3"
  // Force using of 2.13 - is there a better way?
  def bloopConfig      = ivy"ch.epfl.scala:bloop-config_2.13:1.5.5"
  def bsp4j            = ivy"ch.epfl.scala:bsp4j:2.1.0-M3"
  def caseApp          = ivy"com.github.alexarchambault::case-app:2.1.0-M24"
  def collectionCompat = ivy"org.scala-lang.modules::scala-collection-compat:2.9.0"
  // Force using of 2.13 - is there a better way?
  def coursier           = ivy"io.get-coursier:coursier_2.13:${Versions.coursier}"
  def coursierJvm        = ivy"io.get-coursier:coursier-jvm_2.13:${Versions.coursier}"
  def coursierLauncher   = ivy"io.get-coursier:coursier-launcher_2.13:${Versions.coursier}"
  def coursierProxySetup = ivy"io.get-coursier:coursier-proxy-setup:${Versions.coursier}"
  def coursierPublish    = ivy"io.get-coursier.publish:publish_2.13:0.1.4"
  def dependency         = ivy"io.get-coursier::dependency:0.2.2"
  def dockerClient       = ivy"com.spotify:docker-client:8.16.0"
  // TODO bump once 0.15.5 is out
  def expecty       = ivy"com.eed3si9n.expecty::expecty:0.16.0"
  def fansi         = ivy"com.lihaoyi::fansi:0.4.0"
  def guava         = ivy"com.google.guava:guava:31.1-jre"
  def javaClassName = ivy"io.github.alexarchambault.scala-cli:java-class-name_3:0.1.0"
  def jgit          = ivy"org.eclipse.jgit:org.eclipse.jgit:6.4.0.202211300538-r"
  def jimfs         = ivy"com.google.jimfs:jimfs:1.2"
  def jniUtils      = ivy"io.get-coursier.jniutils:windows-jni-utils:0.3.3"
  def jsoniterCore213 =
    ivy"com.github.plokhotnyuk.jsoniter-scala:jsoniter-scala-core_2.13:${Versions.jsoniterScala}"
  def jsoniterCore =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScala}"
  def jsoniterCoreJava8 =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScalaJava8}"
  def jsoniterMacros =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScala}"
  def jsoniterMacrosJava8 =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScalaJava8}"
  def libdaemonjvm  = ivy"io.github.alexarchambault.libdaemon::libdaemon:0.0.11"
  def libsodiumjni  = ivy"org.virtuslab.scala-cli:libsodiumjni:0.0.3"
  def macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  def metaconfigTypesafe =
    ivy"com.geirsson::metaconfig-typesafe-config:0.11.1"
      .exclude(("org.scala-lang", "scala-compiler"))
  def munit                      = ivy"org.scalameta::munit:0.7.29"
  def nativeTestRunner           = ivy"org.scala-native::test-runner:${Versions.scalaNative}"
  def nativeTools                = ivy"org.scala-native::tools:${Versions.scalaNative}"
  def organizeImports            = ivy"com.github.liancheng::organize-imports:0.6.0"
  def osLib                      = ivy"com.lihaoyi::os-lib:0.9.0"
  def pprint                     = ivy"com.lihaoyi::pprint:0.8.1"
  def pythonInterface            = ivy"io.github.alexarchambault.python:interface:0.1.0"
  def pythonNativeLibs           = ivy"ai.kien::python-native-libs:0.2.4"
  def scala3Compiler(sv: String) = ivy"org.scala-lang:scala3-compiler_3:$sv"
  def scalaAsync         = ivy"org.scala-lang.modules::scala-async:1.0.1".exclude("*" -> "*")
  def scalac(sv: String) = ivy"org.scala-lang:scala-compiler:$sv"
  def scalafmtCli        = ivy"org.scalameta:scalafmt-cli_2.13:3.6.1"
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
  // Force using of 2.13 - is there a better way?
  def scalaparse               = ivy"com.lihaoyi:scalaparse_2.13:2.3.3"
  def scalaPy                  = ivy"dev.scalapy::scalapy-core::0.5.3"
  def scalaReflect(sv: String) = ivy"org.scala-lang:scala-reflect:$sv"
  def semanticDbJavac          = ivy"com.sourcegraph:semanticdb-javac:0.7.4"
  def semanticDbScalac         = ivy"org.scalameta:::semanticdb-scalac:${Versions.scalaMeta}"
  def shapeless                = ivy"com.chuusai::shapeless:2.3.9"
  def signingCliShared =
    ivy"org.virtuslab.scala-cli-signing::shared:${Versions.signingCli}"
      // to prevent collisions with scala-cli's case-app version
      .exclude(("com.github.alexarchambault", "case-app_3"))
  def signingCli =
    ivy"org.virtuslab.scala-cli-signing::cli:${Versions.signingCli}"
      // to prevent collisions with scala-cli's case-app version
      .exclude(("com.github.alexarchambault", "case-app_3"))
  def slf4jNop = ivy"org.slf4j:slf4j-nop:2.0.6"
  // Force using of 2.13 - is there a better way?
  def snailgun(force213: Boolean = false) =
    if (force213) ivy"io.github.alexarchambault.scala-cli.snailgun:snailgun-core_2.13:0.4.1-sc2"
    else ivy"io.github.alexarchambault.scala-cli.snailgun::snailgun-core:0.4.1-sc2"
  def sttp            = ivy"com.softwaremill.sttp.client3:core_2.13:3.8.8"
  def svm             = ivy"org.graalvm.nativeimage:svm:$graalVmVersion"
  def swoval          = ivy"com.swoval:file-tree-views:2.1.9"
  def testInterface   = ivy"org.scala-sbt:test-interface:1.0"
  def toolkit         = ivy"org.scala-lang:toolkit:0.1.6"
  def usingDirectives = ivy"org.virtuslab:using_directives:0.1.0"
  // Lives at https://github.com/scala-cli/no-crc32-zip-input-stream, see #865
  // This provides a ZipInputStream that doesn't verify CRC32 checksums, that users
  // can enable by setting SCALA_CLI_VENDORED_ZIS=true in the environment, to workaround
  // some bad GraalVM / zlib issues (see #828 and linked issues for more details).
  def zipInputStream = ivy"io.github.alexarchambault.scala-cli.tmp:zip-input-stream:0.1.1"
}

object BuildDeps {
  def scalaCliVersion = "0.1.9"
}

def graalVmVersion     = "22.3.0"
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
