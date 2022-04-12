import coursier.mavenRepositoryString
import mill._, scalalib._

import scala.util.Properties

object Scala {
  def scala212  = "2.12.15"
  def scala213  = "2.13.8"
  def scala3    = "3.1.1"
  val allScala2 = Seq(scala213, scala212)
  val all       = allScala2 ++ Seq(scala3)

  def scalaJs = "1.10.0"

  def listAll: Seq[String] = {
    def patchVer(sv: String): Int =
      sv.split('.').drop(2).head.takeWhile(_.isDigit).toInt
    val max212 = patchVer(scala212)
    val max213 = patchVer(scala213)
    val max30  = 2
    val max31  = patchVer(scala3)
    (8 until max212).map(i => s"2.12.$i") ++ Seq(scala212) ++
      (0 until max213).map(i => s"2.13.$i") ++ Seq(scala213) ++
      (0 to max30).map(i => s"3.0.$i") ++
      (0 until max31).map(i => s"3.1.$i") ++ Seq(scala3)
  }

  // The Scala version used to build the CLI itself.
  // We should be able to switch to 3.x when it'll have CPS support
  // (for the either { value(…) } stuff)
  def defaultInternal = scala213

  // The Scala version used by default to compile user input.
  def defaultUser = scala3
}

object TestDeps {
  def pprint           = Deps.pprint
  def munit            = Deps.munit
  def scalaSnapshot213 = "2.13.8-bin-e814d78"
}

object InternalDeps {
  object Versions {
    def mill          = os.read(os.pwd / ".mill-version").trim
    def lefouMillwRef = "166bcdf5741de8569e0630e18c3b2ef7e252cd96"
    def scalaJsCli    = "1.1.1-sc2"
  }
}

object Deps {
  object Versions {
    // jni-utils version may need to be sync-ed when bumping the coursier version
    def coursier      = "2.1.0-M5-18-gfebf9838c"
    def jsoniterScala = "2.13.12"
    def scalaMeta     = "4.5.3"
    def scalaNative   = "0.4.4"
    def scalaPackager = "0.1.26"
    def signingCli    = "0.1.2"
  }
  def ammonite = ivy"com.lihaoyi:::ammonite:2.5.2"
  def asm      = ivy"org.ow2.asm:asm:9.3"
  // Force using of 2.13 - is there a better way?
  def bloopConfig      = ivy"io.github.alexarchambault.bleep:bloop-config_2.13:1.4.20"
  def bsp4j            = ivy"ch.epfl.scala:bsp4j:2.0.0"
  def caseApp          = ivy"com.github.alexarchambault:case-app_2.13:2.1.0-M13"
  def collectionCompat = ivy"org.scala-lang.modules::scala-collection-compat:2.7.0"
  // Force using of 2.13 - is there a better way?
  def coursierJvm      = ivy"io.get-coursier:coursier-jvm_2.13:${Versions.coursier}"
  def coursierLauncher = ivy"io.get-coursier:coursier-launcher_2.13:${Versions.coursier}"
  def coursierPublish  = ivy"io.get-coursier.publish:publish_2.13:0.1.0"
  // TODO - update to working version
  def dependency   = ivy"io.get-coursier::dependency:0.2.2"
  def dockerClient = ivy"com.spotify:docker-client:8.16.0"
  // TODO bump once 0.15.5 is out
  def expecty  = ivy"com.eed3si9n.expecty::expecty:0.15.4+22-9c7fb771-SNAPSHOT"
  def guava    = ivy"com.google.guava:guava:31.1-jre"
  def jgit     = ivy"org.eclipse.jgit:org.eclipse.jgit:6.1.0.202203080745-r"
  def jimfs    = ivy"com.google.jimfs:jimfs:1.2"
  def jniUtils = ivy"io.get-coursier.jniutils:windows-jni-utils:0.3.3"
  def jsoniterCore =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:${Versions.jsoniterScala}"
  def jsoniterMacros =
    ivy"com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:${Versions.jsoniterScala}"
  def libdaemonjvm               = ivy"io.github.alexarchambault.libdaemon::libdaemon:0.0.10"
  def macroParadise              = ivy"org.scalamacros:::paradise:2.1.1"
  def munit                      = ivy"org.scalameta::munit:0.7.29"
  def nativeTestRunner           = ivy"org.scala-native::test-runner:${Versions.scalaNative}"
  def nativeTools                = ivy"org.scala-native::tools:${Versions.scalaNative}"
  def organizeImports            = ivy"com.github.liancheng::organize-imports:0.5.0"
  def osLib                      = ivy"com.lihaoyi::os-lib:0.8.1"
  def pprint                     = ivy"com.lihaoyi::pprint:0.7.3"
  def prettyStacktraces          = ivy"org.virtuslab::pretty-stacktraces:0.0.1-M1"
  def scala3Compiler(sv: String) = ivy"org.scala-lang::scala3-compiler:$sv"
  def scalaAsync         = ivy"org.scala-lang.modules::scala-async:1.0.1".exclude("*" -> "*")
  def scalac(sv: String) = ivy"org.scala-lang:scala-compiler:$sv"
  def scalafmtCli        = ivy"org.scalameta::scalafmt-cli:3.5.1"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvJsdomNodejs =
    ivy"org.scala-js:scalajs-env-jsdom-nodejs_2.13:1.1.0"
  // Force using of 2.13 - is there a better way?
  def scalaJsEnvNodeJs = ivy"org.scala-js:scalajs-env-nodejs_2.13:1.3.0"
  def scalaJsLogging   = ivy"org.scala-js:scalajs-logging_2.13:1.1.1"
  // Force using of 2.13 - is there a better way?
  def scalaJsTestAdapter = ivy"org.scala-js:scalajs-sbt-test-adapter_2.13:${Scala.scalaJs}"
  // Force using of 2.13 - is there a better way?
  def scalametaTrees   = ivy"org.scalameta:trees_2.13:${Versions.scalaMeta}"
  def scalaPackager    = ivy"org.virtuslab:scala-packager_2.13:${Versions.scalaPackager}"
  def scalaPackagerCli = ivy"org.virtuslab:scala-packager-cli_2.13:${Versions.scalaPackager}"
  // Force using of 2.13 - is there a better way?
  def scalaparse               = ivy"com.lihaoyi:scalaparse_2.13:2.3.3"
  def scalaReflect(sv: String) = ivy"org.scala-lang:scala-reflect:$sv"
  def semanticDbJavac          = ivy"com.sourcegraph:semanticdb-javac:0.7.4"
  def semanticDbScalac         = ivy"org.scalameta:::semanticdb-scalac:${Versions.scalaMeta}"
  def shapeless                = ivy"com.chuusai::shapeless:2.3.9"
  def signingCliShared =
    ivy"io.github.alexarchambault.scala-cli.signing:shared_2.13:${Versions.signingCli}"
  def signingCli = ivy"io.github.alexarchambault.scala-cli.signing:cli_2.13:${Versions.signingCli}"
  def slf4jNop   = ivy"org.slf4j:slf4j-nop:1.8.0-beta4"
  // Force using of 2.13 - is there a better way?
  def snailgun(force213: Boolean = false) =
    if (force213) ivy"me.vican.jorge:snailgun-core_2.13:0.4.0"
    else ivy"me.vican.jorge::snailgun-core:0.4.0"
  def svm                = ivy"org.graalvm.nativeimage:svm:$graalVmVersion"
  def swoval             = ivy"com.swoval:file-tree-views:2.1.9"
  def testInterface      = ivy"org.scala-sbt:test-interface:1.0"
  def usingDirectives    = ivy"org.virtuslab:using_directives:0.0.8"
  val metaconfigTypesafe = ivy"com.geirsson::metaconfig-typesafe-config:0.10.0"
}

def graalVmVersion     = "22.0.0"
def graalVmJavaVersion = 17
def graalVmJvmId       = s"graalvm-java$graalVmJavaVersion:$graalVmVersion"

def csDockerVersion = Deps.Versions.coursier

def buildCsVersion = Deps.Versions.coursier

object Docker {
  def customMuslBuilderImageName = "scala-cli-base-musl"
  def muslBuilder =
    s"$customMuslBuilderImageName:latest"

  def testImage = "ubuntu:18.04"
  def alpineTestImage =
    "alpine@sha256:234cb88d3020898631af0ccbbcca9a66ae7306ecd30c9720690858c1b007d2a0"
  def authProxyTestImage =
    "bahamat/authenticated-proxy@sha256:568c759ac687f93d606866fbb397f39fe1350187b95e648376b971e9d7596e75"
}

def customRepositories =
  Seq(
    coursier.Repositories.sonatype("snapshots")
    // Uncomment for local development
    // coursier.LocalRepositories.Dangerous.maven2Local
  )
