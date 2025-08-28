package build.project.deps
import mill._
import scalalib._

object Cli {
  def runnerLegacyVersion = "1.7.1" // last runner version to support pre-LTS Scala 3 versions
}

object Scala {
  def scala212         = "2.12.20"
  def scala213         = "2.13.16"
  def scala3LtsPrefix  = "3.3"                  // used for the LTS version tags
  def scala3Lts        = s"$scala3LtsPrefix.6"  // the LTS version currently used in the build
  def runnerScala3     = scala3Lts
  def scala3NextPrefix = "3.7"
  def scala3Next       = s"$scala3NextPrefix.2" // the newest/next version of Scala
  def scala3NextAnnounced   = scala3Next  // the newest/next version of Scala that's been announced
  def scala3NextRc          = "3.7.3-RC2" // the latest RC version of Scala Next
  def scala3NextRcAnnounced = "3.7.2-RC2" // the latest announced RC version of Scala Next

  // The Scala version used to build the CLI itself.
  def defaultInternal = sys.props.get("scala.version.internal").getOrElse(scala3Lts)

  // The Scala version used by default to compile user input.
  def defaultUser = sys.props.get("scala.version.user").getOrElse(scala3Next)

  val allScala2           = Seq(scala213, scala212)
  val defaults            = Seq(defaultInternal, defaultUser).distinct
  val allScala3           = Seq(scala3Lts, scala3Next, scala3NextAnnounced, scala3NextRc).distinct
  val all                 = (allScala2 ++ allScala3 ++ defaults).distinct
  val scala3MainVersions  = (defaults ++ allScala3).distinct
  val runnerScalaVersions = runnerScala3 +: allScala2
  val testRunnerScalaVersions = (runnerScalaVersions ++ allScala3).distinct

  def scalaJs    = "1.19.0"
  def scalaJsCli = "1.19.0.2" // this must be compatible with the Scala.js version

  private def patchVer(sv: String): Int =
    sv.split('.').drop(2).head.takeWhile(_.isDigit).toInt

  private def minorVer(sv: String): Int =
    sv.split('.').drop(1).head.takeWhile(_.isDigit).toInt

  def listAll: Seq[String] = {
    val max212 = patchVer(scala212)
    val max213 = patchVer(scala213)
    val max30  = 2
    val max31  = 3
    val max32  = 2
    val max33  = patchVer(scala3Lts)
    val max34  = 3
    val max35  = 2
    val max36  = 4
    val max37  = patchVer(scala3Next)
    (8 until max212).map(i => s"2.12.$i") ++ Seq(scala212) ++
      (0 until max213).map(i => s"2.13.$i") ++ Seq(scala213) ++
      (0 to max30).map(i => s"3.0.$i") ++
      (0 to max31).map(i => s"3.1.$i") ++
      (0 to max32).map(i => s"3.2.$i") ++
      (0 to max33).map(i => s"3.3.$i") ++
      (0 to max34).map(i => s"3.4.$i") ++
      (0 to max35).map(i => s"3.5.$i") ++
      (0 to max36).map(i => s"3.6.$i") ++
      (0 until max37).map(i => s"3.7.$i") ++ Seq(scala3Next)
  }

  def legacyScala3Versions: Seq[String] =
    listAll
      .filter(_.startsWith("3"))
      .distinct
      .filter(minorVer(_) < minorVer(scala3Lts))

  def maxAmmoniteScala212Version                    = scala212
  def maxAmmoniteScala213Version                    = scala213
  def maxAmmoniteScala3Version                      = "3.6.3"
  def maxAmmoniteScala3LtsVersion                   = "3.3.5"
  lazy val listMaxAmmoniteScalaVersion: Seq[String] =
    Seq(maxAmmoniteScala212Version, maxAmmoniteScala213Version, maxAmmoniteScala3Version)
}

object Java {
  def minimumBloopJava: Int      = 17
  def minimumInternalJava: Int   = 16
  def defaultJava: Int           = minimumBloopJava
  def mainJavaVersions: Seq[Int] = Seq(8, 11, 17, 21, 23, 24)
  def allJavaVersions: Seq[Int]  =
    (mainJavaVersions ++ Seq(minimumBloopJava, minimumInternalJava, defaultJava)).distinct
}

// Dependencies used in integration test fixtures
object TestDeps {
  def pprint: Dep              = Deps.pprint
  def munit: Dep               = Deps.munit
  def scalaSnapshot213: String = "2.13.8-bin-e814d78"

  def archLinuxImage: String =
    "archlinux@sha256:b15db21228c7cd5fd3ab364a97193ba38abfad0e8b9593c15b71850b74738153"
}

object InternalDeps {
  object Versions {
    def mill: String  = _root_.mill.main.BuildInfo.millVersion
    def lefouMillwRef = "166bcdf5741de8569e0630e18c3b2ef7e252cd96"
  }
}

object Deps {
  object Versions {
    def ammonite             = "3.0.2"
    def ammoniteForScala3Lts = ammonite
    def argonautShapeless    = "1.3.1"
    // jni-utils version may need to be sync-ed when bumping the coursier version
    def coursierDefault                   = "2.1.24"
    def coursier                          = coursierDefault
    def coursierCli                       = coursierDefault
    def coursierM1Cli                     = coursierDefault
    def coursierPublish                   = "0.4.3"
    def jmh                               = "1.37"
    def jsoniterScalaJava8                = "2.13.5.2"
    def jsoup                             = "1.21.2"
    def scalaMeta                         = "4.13.9"
    def scalafmt                          = "3.9.9"
    def scalaNative04                     = "0.4.17"
    def scalaNative05                     = "0.5.8"
    def scalaNative                       = scalaNative05
    def maxScalaNativeForToolkit          = scalaNative05
    def maxScalaNativeForTypelevelToolkit = scalaNative04
    def maxScalaNativeForScalaPy          = scalaNative04
    def maxScalaNativeForMillExport       = scalaNative05
    def scalaPackager                     = "0.1.33"
    def signingCli                        = "0.2.11"
    def signingCliJvmVersion              = Java.defaultJava
    def javaSemanticdb                    = "0.10.0"
    def javaClassName                     = "0.1.8"
    def bloop                             = "2.0.13"
    def sbtVersion                        = "1.11.4"
    def mavenVersion                      = "3.8.1"
    def mavenScalaCompilerPluginVersion   = "4.9.1"
    def mavenExecPluginVersion            = "3.3.0"
    def mavenAppArtifactId                = "maven-app"
    def mavenAppGroupId                   = "com.example"
    def mavenAppVersion                   = "0.1-SNAPSHOT"
    def scalafix                          = "0.14.3"
  }
  // DO NOT hardcode a Scala version in this dependency string
  // This dependency is used to ensure that Ammonite is available for Scala versions
  // that Scala CLI supports.
  def ammonite             = mvn"com.lihaoyi:::ammonite:${Versions.ammonite}"
  def ammoniteForScala3Lts = mvn"com.lihaoyi:::ammonite:${Versions.ammoniteForScala3Lts}"
  def argonautShapeless    =
    mvn"com.github.alexarchambault:argonaut-shapeless_6.3_2.13:${Versions.argonautShapeless}"
  def asm = mvn"org.ow2.asm:asm:9.8"
  // Force using of 2.13 - is there a better way?
  def bloopConfig = mvn"ch.epfl.scala:bloop-config_2.13:2.3.2"
    .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13"))
  def bloopRifle       = mvn"ch.epfl.scala:bloop-rifle_2.13:${Versions.bloop}"
  def bsp4j            = mvn"ch.epfl.scala:bsp4j:2.1.1"
  def caseApp          = mvn"com.github.alexarchambault::case-app:2.1.0"
  def collectionCompat = mvn"org.scala-lang.modules::scala-collection-compat:2.13.0"
  // Force using of 2.13 - is there a better way?
  def coursier    = mvn"io.get-coursier:coursier_2.13:${Versions.coursier}"
  def coursierCli = mvn"io.get-coursier:coursier-cli_2.13:${Versions.coursierCli}"
  def coursierJvm = mvn"io.get-coursier:coursier-jvm_2.13:${Versions.coursier}"
    .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13"))
    .exclude("io.get-coursier" -> "dependency_2.13")
  def coursierLauncher = mvn"io.get-coursier:coursier-launcher_2.13:${Versions.coursier}"
    .exclude(("ai.kien", "python-native-libs_2.13"))
    .exclude(("org.scala-lang.modules", "scala-collection-compat_2.13"))
  def coursierProxySetup = mvn"io.get-coursier:coursier-proxy-setup:${Versions.coursier}"
  def coursierPublish    = mvn"io.get-coursier.publish::publish:${Versions.coursierPublish}"
    .exclude(("org.scala-lang.modules", "scala-collection-compat_2.13"))
    .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_3"))
  def dependency    = mvn"io.get-coursier::dependency:0.3.2"
  def dockerClient  = mvn"com.spotify:docker-client:8.16.0"
  def expecty       = mvn"com.eed3si9n.expecty::expecty:0.17.0"
  def fansi         = mvn"com.lihaoyi::fansi:0.5.1"
  def giter8        = mvn"org.foundweekends.giter8:giter8:0.16.2"
  def guava         = mvn"com.google.guava:guava:33.4.8-jre"
  def javaClassName =
    mvn"org.virtuslab.scala-cli.java-class-name:java-class-name_3:${Versions.javaClassName}"
      .exclude(
        "org.jline" -> "jline-reader",
        "org.jline" -> "jline-terminal",
        "org.jline" -> "jline-terminal-jna"
      )
  def jgit                 = mvn"org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r"
  def jimfs                = mvn"com.google.jimfs:jimfs:1.3.1"
  def jmhGeneratorBytecode = mvn"org.openjdk.jmh:jmh-generator-bytecode:${Versions.jmh}"
  def jmhCore              = mvn"org.openjdk.jmh:jmh-core:${Versions.jmh}"
  def jniUtils             = mvn"io.get-coursier.jniutils:windows-jni-utils:0.3.3"
  def jsoniterCore         =
    mvn"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScalaJava8}"
  def jsoniterCoreJava8 =
    mvn"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScalaJava8}"
  def jsoniterMacros =
    mvn"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScalaJava8}"
  def jsoniterMacrosJava8 =
    mvn"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScalaJava8}"
  def jsoup              = mvn"org.jsoup:jsoup:${Versions.jsoup}"
  def libsodiumjni       = mvn"org.virtuslab.scala-cli:libsodiumjni:0.0.4"
  def metaconfigTypesafe =
    mvn"org.scalameta::metaconfig-typesafe-config:0.16.0"
      .exclude(("org.scala-lang", "scala-compiler"))
  def munit              = mvn"org.scalameta::munit:1.1.1"
  def nativeTestRunner   = mvn"org.scala-native::test-runner:${Versions.scalaNative}"
  def nativeTools        = mvn"org.scala-native::tools:${Versions.scalaNative}"
  def osLib              = mvn"com.lihaoyi::os-lib:0.11.5"
  def pprint             = mvn"com.lihaoyi::pprint:0.9.3"
  def pythonInterface    = mvn"io.github.alexarchambault.python:interface:0.1.0"
  def pythonNativeLibs   = mvn"ai.kien::python-native-libs:0.2.4"
  def scalaAsync         = mvn"org.scala-lang.modules::scala-async:1.0.1".exclude("*" -> "*")
  def scalac(sv: String) = mvn"org.scala-lang:scala-compiler:$sv"
  def scalafmtCli        = mvn"org.scalameta:scalafmt-cli_2.13:${Versions.scalafmt}"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvJsdomNodejs =
    mvn"org.scala-js:scalajs-env-jsdom-nodejs_2.13:1.1.0"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvNodeJs = mvn"org.scala-js:scalajs-env-nodejs_2.13:1.4.0"
  def scalaJsLogging   = mvn"org.scala-js:scalajs-logging_2.13:1.1.1"
  // Force using of 2.13 - is there a better way?
  def scalaJsTestAdapter = mvn"org.scala-js:scalajs-sbt-test-adapter_2.13:${Scala.scalaJs}"
  def scalaPackager      = mvn"org.virtuslab:scala-packager_2.13:${Versions.scalaPackager}"
  def scalaPackagerCli   = mvn"org.virtuslab:scala-packager-cli_2.13:${Versions.scalaPackager}"
  def scalaPy            = mvn"dev.scalapy::scalapy-core::0.5.3"
  def scalaReflect(sv: String)  = mvn"org.scala-lang:scala-reflect:$sv"
  def semanticDbJavac           = mvn"com.sourcegraph:semanticdb-javac:${Versions.javaSemanticdb}"
  def semanticDbScalac          = mvn"org.scalameta:::semanticdb-scalac:${Versions.scalaMeta}"
  def scalametaSemanticDbShared =
    mvn"org.scalameta:semanticdb-shared_2.13:${Versions.scalaMeta}"
      .exclude("org.jline" -> "jline") // to prevent incompatibilities with GraalVM <23
      .exclude("com.lihaoyi" -> "sourcecode_2.13")
      .exclude("org.scala-lang.modules" -> "scala-collection-compat_2.13")
  def signingCliShared =
    mvn"org.virtuslab.scala-cli-signing::shared:${Versions.signingCli}"
      // to prevent collisions with scala-cli's case-app version
      .exclude(("com.github.alexarchambault", "case-app_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_3"))
      .exclude(("com.lihaoyi", "os-lib_3"))
      .exclude(("com.lihaoyi", "os-lib_2.13"))
  def signingCli =
    mvn"org.virtuslab.scala-cli-signing::cli:${Versions.signingCli}"
      // to prevent collisions with scala-cli's case-app version
      .exclude(("com.github.alexarchambault", "case-app_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-macros_3"))
      .exclude(("com.github.plokhotnyuk.jsoniter-scala", "jsoniter-scala-core_2.13"))
      .exclude(("io.get-coursier.publish", "publish_2.13"))
      .exclude(("org.scala-lang.modules", "scala-collection-compat_2.13"))
      .exclude(("com.lihaoyi", "os-lib_3"))
      .exclude(("com.lihaoyi", "os-lib_2.13"))
  def slf4jNop                  = mvn"org.slf4j:slf4j-nop:2.0.17"
  def sttp                      = mvn"com.softwaremill.sttp.client3::core:3.11.0"
  def svm                       = mvn"org.graalvm.nativeimage:svm:$graalVmVersion"
  def swoval                    = mvn"com.swoval:file-tree-views:2.1.12"
  def testInterface             = mvn"org.scala-sbt:test-interface:1.0"
  val toolkitVersion            = "0.7.0"
  val toolkitVersionForNative04 = "0.3.0"
  val toolkitVersionForNative05 = toolkitVersion
  def toolkit                   = mvn"org.scala-lang:toolkit:$toolkitVersion"
  def toolkitTest               = mvn"org.scala-lang:toolkit-test:$toolkitVersion"
  val typelevelToolkitVersion   = "0.1.29"
  def typelevelToolkit          = mvn"org.typelevel:toolkit:$typelevelToolkitVersion"
  def usingDirectives           = mvn"org.virtuslab:using_directives:1.1.4"
  // Lives at https://github.com/VirtusLab/no-crc32-zip-input-stream, see #865
  // This provides a ZipInputStream that doesn't verify CRC32 checksums, that users
  // can enable by setting SCALA_CLI_VENDORED_ZIS=true in the environment, to workaround
  // some bad GraalVM / zlib issues (see #828 and linked issues for more details).
  def zipInputStream     = mvn"org.virtuslab.scala-cli.zip-input-stream:zip-input-stream:0.1.3"
  def scalafixInterfaces = mvn"ch.epfl.scala:scalafix-interfaces:${Versions.scalafix}"
}

def graalVmVersion     = "22.3.1"
def graalVmJavaVersion = Java.defaultJava
def graalVmJvmId       = s"graalvm-java$graalVmJavaVersion:$graalVmVersion"

def csDockerVersion = Deps.Versions.coursierCli

def buildCsVersion   = Deps.Versions.coursierCli
def buildCsM1Version = Deps.Versions.coursierM1Cli

// Native library used to encrypt GitHub secrets
def libsodiumVersion = "1.0.18"
// Using the libsodium static library from this Alpine version (in the static launcher)
def alpineVersion = "3.16"
def ubuntuVersion = "24.04"

object Docker {
  def customMuslBuilderImageName = "scala-cli-base-musl"
  def muslBuilder                =
    s"$customMuslBuilderImageName:latest"

  def testImage          = s"ubuntu:$ubuntuVersion"
  def alpineTestImage    = s"alpine:$alpineVersion"
  def authProxyTestImage =
    "bahamat/authenticated-proxy@sha256:568c759ac687f93d606866fbb397f39fe1350187b95e648376b971e9d7596e75"
}

def customRepositories =
  Seq(
    coursier.Repositories.sonatype("snapshots"),
    coursier.MavenRepository("https://s01.oss.sonatype.org/content/repositories/snapshots"),
    coursier.MavenRepository("https://central.sonatype.com/repository/maven-snapshots")
    // Uncomment for local development
    // coursier.LocalRepositories.Dangerous.maven2Local
  )
