import mill._, scalalib._

object Scala {
  def scala212 = "2.12.14"
  def scala213 = "2.13.6"
  def scala3Extras = Seq("3.0.0-RC2", "3.0.0-RC3")
  def scala3 = "3.0.0"
  val allScala2 = Seq(scala213, scala212)
  val all = allScala2 ++ scala3Extras ++ Seq(scala3)
}

object Deps {
  object Versions {
    def coursier = "2.0.16"
    def coursierInterface = "1.0.4"
    def scalaJs = "1.5.1"
    def scalaNative = "0.4.0"
  }
  def asm = ivy"org.ow2.asm:asm:9.1"
  def bloopConfig = ivy"ch.epfl.scala::bloop-config:1.4.8"
  def bloopgun = ivy"ch.epfl.scala::bloopgun:1.4.8"
  def bsp4j = ivy"ch.epfl.scala:bsp4j:2.0.0-M13"
  def caseApp = ivy"com.github.alexarchambault::case-app:2.1.0-M2"
  def coursierInterface = ivy"io.get-coursier:interface:${Versions.coursierInterface}"
  def coursierInterfaceSvmSubs = ivy"io.get-coursier:interface-svm-subs:${Versions.coursierInterface}"
  def coursierJvm = ivy"io.get-coursier::coursier-jvm:${Versions.coursier}"
  def coursierLauncher = ivy"io.get-coursier::coursier-launcher:${Versions.coursier}"
  def dependency = ivy"io.get-coursier::dependency:0.2.0"
  def expecty = ivy"com.eed3si9n.expecty::expecty:0.15.1"
  def jimfs = ivy"com.google.jimfs:jimfs:1.2"
  def jniUtils = ivy"io.get-coursier.jniutils:windows-jni-utils:0.2.1"
  def metabrowseServer = ivy"org.scalameta::metabrowse-server:0.2.5"
  def munit = ivy"org.scalameta::munit:0.7.25"
  def nativeTestRunner = ivy"org.scala-native::test-runner:${Versions.scalaNative}"
  def nativeTools = ivy"org.scala-native::tools:${Versions.scalaNative}"
  def osLib = ivy"com.lihaoyi::os-lib:0.7.5"
  def pprint = ivy"com.lihaoyi::pprint:0.6.4"
  def prettyStacktraces = ivy"org.virtuslab::pretty-stacktraces:0.0.0+27-b9d69198-SNAPSHOT"
  def scala3Compiler(sv: String) = ivy"org.scala-lang::scala3-compiler:$sv"
  def scalac(sv: String) = ivy"org.scala-lang:scala-compiler:$sv"
  def scalaJsEnvNodeJs = ivy"org.scala-js::scalajs-env-nodejs:1.1.1"
  def scalaJsLinker = ivy"org.scala-js::scalajs-linker:${Versions.scalaJs}"
  def scalaJsTestAdapter = ivy"org.scala-js::scalajs-sbt-test-adapter:${Versions.scalaJs}"
  def scalametaTrees = ivy"org.scalameta::trees:4.4.21"
  def scalaparse = ivy"com.lihaoyi::scalaparse:2.3.2"
  def slf4jNop = ivy"org.slf4j:slf4j-nop:1.8.0-beta4"
  def snailgun = ivy"me.vican.jorge::snailgun-cli:0.4.0"
  def svm = ivy"org.graalvm.nativeimage:svm:$graalVmVersion"
  def svmSubs = ivy"org.scalameta::svm-subs:20.2.0"
  def swoval = ivy"com.swoval:file-tree-views:2.1.5"
  def testInterface = ivy"org.scala-sbt:test-interface:1.0"
}

def graalVmVersion = "21.1.0"
