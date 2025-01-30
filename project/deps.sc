import Deps.Versions
import mill._
import scalalib._

object Scala {
  def scala212        = "2.12.20"
  def scala213        = "2.13.16"
  def runnerScala3    = "3.0.2" // the newest version that is compatible with all Scala 3.x versions
  def scala3LtsPrefix = "3.3"   // used for the LTS version tags
  def scala3Lts        = s"$scala3LtsPrefix.5"  // the LTS version currently used in the build
  def scala3NextPrefix = "3.6"
  def scala3Next       = s"$scala3NextPrefix.3" // the newest/next version of Scala
  def scala3NextAnnounced =
    s"$scala3NextPrefix.2" // the newest/next version of Scala that's been announced
  def scala3NextRc          = "3.6.4-RC1"  // the latest RC version of Scala Next
  def scala3NextRcAnnounced = scala3NextRc // the latest announced RC version of Scala Next

  // The Scala version used to build the CLI itself.
  def defaultInternal = sys.props.get("scala.version.internal").getOrElse(scala3Lts)

  // The Scala version used by default to compile user input.
  def defaultUser = sys.props.get("scala.version.user").getOrElse(scala3Next)

  val allScala2           = Seq(scala213, scala212)
  val defaults            = Seq(defaultInternal, defaultUser).distinct
  val allScala3           = Seq(scala3Lts, scala3Next, scala3NextAnnounced, scala3NextRc).distinct
  val all                 = (allScala2 ++ allScala3 ++ defaults).distinct
  val scala3MainVersions  = (defaults ++ allScala3).distinct
  val mainVersions        = (Seq(scala213) ++ scala3MainVersions).distinct
  val runnerScalaVersions = runnerScala3 +: allScala2
  val testRunnerScalaVersions = runnerScalaVersions ++ allScala3

  def scalaJs    = "1.18.2"
  def scalaJsCli = scalaJs // this must be compatible with the Scala.js version

  def listAll: Seq[String] = {
    def patchVer(sv: String): Int =
      sv.split('.').drop(2).head.takeWhile(_.isDigit).toInt
    val max212 = patchVer(scala212)
    val max213 = patchVer(scala213)
    val max30  = 2
    val max31  = 3
    val max32  = 2
    val max33  = patchVer(scala3Lts)
    val max34  = 3
    val max35  = patchVer(scala3Next)
    (8 until max212).map(i => s"2.12.$i") ++ Seq(scala212) ++
      (0 until max213).map(i => s"2.13.$i") ++ Seq(scala213) ++
      (0 to max30).map(i => s"3.0.$i") ++
      (0 to max31).map(i => s"3.1.$i") ++
      (0 to max32).map(i => s"3.2.$i") ++
      (0 to max33).map(i => s"3.3.$i") ++
      (0 to max34).map(i => s"3.4.$i") ++
      (0 until max35).map(i => s"3.5.$i") ++ Seq(scala3Next)
  }

  def maxAmmoniteScala212Version  = scala212
  def maxAmmoniteScala213Version  = "2.13.14"
  def maxAmmoniteScala3Version    = "3.5.1"
  def maxAmmoniteScala3LtsVersion = "3.3.4"
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

object Java {
  def minimumBloopJava    = 17
  def minimumInternalJava = 16
  def defaultJava         = minimumBloopJava
  def mainJavaVersions    = Seq(8, 11, 17, 21, 23)
  def allJavaVersions =
    (mainJavaVersions ++ Seq(minimumBloopJava, minimumInternalJava, defaultJava)).distinct
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
    def ammonite             = "3.0.0-2-6342755f"
    def ammoniteForScala3Lts = ammonite
    def argonautShapeless    = "1.3.1"
    // jni-utils version may need to be sync-ed when bumping the coursier version
    def coursierDefault                   = "2.1.24"
    def coursier                          = coursierDefault
    def coursierCli                       = coursierDefault
    def coursierM1Cli                     = coursierDefault
    def jmh                               = "1.37"
    def jsoniterScala                     = "2.23.2"
    def jsoniterScalaJava8                = "2.13.5.2"
    def jsoup                             = "1.18.3"
    def scalaMeta                         = "4.9.9"
    def scalaNative04                     = "0.4.17"
    def scalaNative05                     = "0.5.6"
    def scalaNative                       = scalaNative05
    def maxScalaNativeForToolkit          = scalaNative05
    def maxScalaNativeForTypelevelToolkit = scalaNative04
    def maxScalaNativeForScalaPy          = scalaNative04
    def maxScalaNativeForMillExport       = scalaNative04
    def scalaPackager                     = "0.1.31"
    def signingCli                        = "0.2.4"
    def signingCliJvmVersion              = Java.defaultJava
    def javaSemanticdb                    = "0.10.0"
    def javaClassName                     = "0.1.4"
    def bloop                             = "2.0.8"
    def sbtVersion                        = "1.10.7"
    def mavenVersion                      = "3.8.1"
    def mavenScalaCompilerPluginVersion   = "4.9.1"
    def mavenExecPluginVersion            = "3.3.0"
    def mavenAppArtifactId                = "maven-app"
    def mavenAppGroupId                   = "com.example"
    def mavenAppVersion                   = "0.1-SNAPSHOT"
    def scalafix                          = "0.14.0"
  }
  // DO NOT hardcode a Scala version in this dependency string
  // This dependency is used to ensure that Ammonite is available for Scala versions
  // that Scala CLI supports.
  def ammonite             = ivy"com.lihaoyi:::ammonite:${Versions.ammonite}"
  def ammoniteForScala3Lts = ivy"com.lihaoyi:::ammonite:${Versions.ammoniteForScala3Lts}"
  def argonautShapeless =
    ivy"com.github.alexarchambault:argonaut-shapeless_6.3_2.13:${Versions.argonautShapeless}"
  def asm = ivy"org.ow2.asm:asm:9.7.1"
  // Force using of 2.13 - is there a better way?
  def bloopConfig = ivy"ch.epfl.scala:bloop-config_2.13:2.3.1"
    .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13"))
  def bloopRifle       = ivy"ch.epfl.scala:bloop-rifle_2.13:${Versions.bloop}"
  def bsp4j            = ivy"ch.epfl.scala:bsp4j:2.1.1"
  def caseApp          = ivy"com.github.alexarchambault::case-app:2.1.0-M29"
  def collectionCompat = ivy"org.scala-lang.modules::scala-collection-compat:2.12.0"
  // Force using of 2.13 - is there a better way?
  def coursier    = ivy"io.get-coursier:coursier_2.13:${Versions.coursier}"
  def coursierCli = ivy"io.get-coursier:coursier-cli_2.13:${Versions.coursierCli}"
  def coursierJvm = ivy"io.get-coursier:coursier-jvm_2.13:${Versions.coursier}"
    .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13"))
  def coursierLauncher = ivy"io.get-coursier:coursier-launcher_2.13:${Versions.coursier}"
    .exclude(("ai.kien", "python-native-libs_2.13"))
    .exclude(("org.scala-lang.modules", "scala-collection-compat_2.13"))
  def coursierProxySetup = ivy"io.get-coursier:coursier-proxy-setup:${Versions.coursier}"
  def coursierPublish = ivy"io.get-coursier.publish:publish_2.13:0.2.0"
    .exclude(("org.scala-lang.modules", "scala-collection-compat_2.13"))
    .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13"))
  def dependency   = ivy"io.get-coursier::dependency:0.3.2"
  def dockerClient = ivy"com.spotify:docker-client:8.16.0"
  // TODO bump once 0.15.5 is out
  def expecty = ivy"com.eed3si9n.expecty::expecty:0.17.0"
  def fansi   = ivy"com.lihaoyi::fansi:0.5.0"
  def giter8  = ivy"org.foundweekends.giter8:giter8:0.16.2"
  def guava   = ivy"com.google.guava:guava:33.4.0-jre"
  def javaClassName =
    ivy"org.virtuslab.scala-cli.java-class-name:java-class-name_3:${Versions.javaClassName}"
      .exclude(
        "org.jline" -> "jline-reader",
        "org.jline" -> "jline-terminal",
        "org.jline" -> "jline-terminal-jna"
      )
  def jgit                 = ivy"org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r"
  def jimfs                = ivy"com.google.jimfs:jimfs:1.3.0"
  def jmhGeneratorBytecode = ivy"org.openjdk.jmh:jmh-generator-bytecode:${Versions.jmh}"
  def jmhCore              = ivy"org.openjdk.jmh:jmh-core:${Versions.jmh}"
  def jniUtils             = ivy"io.get-coursier.jniutils:windows-jni-utils:0.3.3"
  def jsoniterCore =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScalaJava8}"
  def jsoniterCoreJava8 =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScalaJava8}"
  def jsoniterMacros =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScalaJava8}"
  def jsoniterMacrosJava8 =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScalaJava8}"
  def jsoup         = ivy"org.jsoup:jsoup:${Versions.jsoup}"
  def libsodiumjni  = ivy"org.virtuslab.scala-cli:libsodiumjni:0.0.4"
  def macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  def metaconfigTypesafe =
    ivy"org.scalameta::metaconfig-typesafe-config:0.14.0"
      .exclude(("org.scala-lang", "scala-compiler"))
  def munit                      = ivy"org.scalameta::munit:1.1.0"
  def nativeTestRunner           = ivy"org.scala-native::test-runner:${Versions.scalaNative}"
  def nativeTools                = ivy"org.scala-native::tools:${Versions.scalaNative}"
  def osLib                      = ivy"com.lihaoyi::os-lib:0.11.3"
  def pprint                     = ivy"com.lihaoyi::pprint:0.9.0"
  def pythonInterface            = ivy"io.github.alexarchambault.python:interface:0.1.0"
  def pythonNativeLibs           = ivy"ai.kien::python-native-libs:0.2.4"
  def scala3Compiler(sv: String) = ivy"org.scala-lang:scala3-compiler_3:$sv"
  def scalaAsync         = ivy"org.scala-lang.modules::scala-async:1.0.1".exclude("*" -> "*")
  def scalac(sv: String) = ivy"org.scala-lang:scala-compiler:$sv"
  def scalafmtCli        = ivy"org.scalameta:scalafmt-cli_2.13:3.8.6"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvJsdomNodejs =
    ivy"org.scala-js:scalajs-env-jsdom-nodejs_2.13:1.1.0"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvNodeJs = ivy"org.scala-js:scalajs-env-nodejs_2.13:1.4.0"
  def scalaJsLogging   = ivy"org.scala-js:scalajs-logging_2.13:1.1.1"
  // Force using of 2.13 - is there a better way?
  def scalaJsTestAdapter = ivy"org.scala-js:scalajs-sbt-test-adapter_2.13:${Scala.scalaJs}"
  def scalaPackager      = ivy"org.virtuslab:scala-packager_2.13:${Versions.scalaPackager}"
  def scalaPackagerCli   = ivy"org.virtuslab:scala-packager-cli_2.13:${Versions.scalaPackager}"
  def scalaPy            = ivy"dev.scalapy::scalapy-core::0.5.3"
  def scalaReflect(sv: String) = ivy"org.scala-lang:scala-reflect:$sv"
  def semanticDbJavac          = ivy"com.sourcegraph:semanticdb-javac:${Versions.javaSemanticdb}"
  def semanticDbScalac         = ivy"org.scalameta:::semanticdb-scalac:${Versions.scalaMeta}"
  def scalametaSemanticDbShared =
    ivy"org.scalameta:semanticdb-shared_${Scala.scala213}:${Versions.scalaMeta}"
      .exclude("org.jline" -> "jline") // to prevent incompatibilities with GraalVM <23
      .exclude("com.lihaoyi" -> "sourcecode_2.13")
      .exclude("org.scala-lang.modules" -> "scala-collection-compat_2.13")
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
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13"))
      .exclude(("io.get-coursier.publish", "publish_2.13"))
      .exclude(("org.scala-lang.modules", "scala-collection-compat_2.13"))
  def slf4jNop                  = ivy"org.slf4j:slf4j-nop:2.0.16"
  def sttp                      = ivy"com.softwaremill.sttp.client3:core_2.13:3.10.2"
  def svm                       = ivy"org.graalvm.nativeimage:svm:$graalVmVersion"
  def swoval                    = ivy"com.swoval:file-tree-views:2.1.12"
  def testInterface             = ivy"org.scala-sbt:test-interface:1.0"
  val toolkitVersion            = "0.5.0"
  val toolkitVersionForNative04 = "0.3.0"
  val toolkitVersionForNative05 = toolkitVersion
  def toolkit                   = ivy"org.scala-lang:toolkit:$toolkitVersion"
  def toolkitTest               = ivy"org.scala-lang:toolkit-test:$toolkitVersion"
  val typelevelToolkitVersion   = "0.1.29"
  def typelevelToolkit          = ivy"org.typelevel:toolkit:$typelevelToolkitVersion"
  def typelevelToolkitTest      = ivy"org.typelevel:toolkit-test:$typelevelToolkitVersion"
  def usingDirectives           = ivy"org.virtuslab:using_directives:1.1.4"
  // Lives at https://github.com/VirtusLab/no-crc32-zip-input-stream, see #865
  // This provides a ZipInputStream that doesn't verify CRC32 checksums, that users
  // can enable by setting SCALA_CLI_VENDORED_ZIS=true in the environment, to workaround
  // some bad GraalVM / zlib issues (see #828 and linked issues for more details).
  def zipInputStream     = ivy"org.virtuslab.scala-cli.zip-input-stream:zip-input-stream:0.1.2"
  def scalafixInterfaces = ivy"ch.epfl.scala:scalafix-interfaces:${Versions.scalafix}"
}

def graalVmVersion     = "22.3.1"
def graalVmJavaVersion = Java.defaultJava
def graalVmJvmId       = s"graalvm-java$graalVmJavaVersion:$graalVmVersion"

def csDockerVersion = Deps.Versions.coursierCli

def buildCsVersion   = Deps.Versions.coursierCli
def buildCsM1Version = Deps.Versions.coursierM1Cli

def ubuntuDockerVersion = "24.04"

// Native library used to encrypt GitHub secrets
def libsodiumVersion = "1.0.18"
// Using the libsodium static library from this Alpine version (in the static launcher)
def alpineVersion = "3.15"

object Docker {
  def customMuslBuilderImageName = "scala-cli-base-musl"
  def muslBuilder =
    s"$customMuslBuilderImageName:latest"

  def testImage = s"ubuntu:$ubuntuDockerVersion"
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
