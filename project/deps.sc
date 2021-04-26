import mill._, scalalib._

object Scala {
  val scala212 = "2.12.13"
  val scala213 = "2.13.5"
  val scalaVersions = Seq(scala212)
}

object Deps {
  object Versions {
    def ammonite = "2.3.8-58-aa8b2ab1"
    def coursier = "2.0.16"
  }
  def ammInterp = ivy"com.lihaoyi:::ammonite-interp:${Versions.ammonite}"
  def ammCompiler = ivy"com.lihaoyi:::ammonite-compiler:${Versions.ammonite}"
  def asm = ivy"org.ow2.asm:asm:9.1"
  def bloopConfig = ivy"ch.epfl.scala::bloop-config:1.4.8"
  def bloopgun = ivy"ch.epfl.scala::bloopgun:1.4.8"
  def caseApp = ivy"com.github.alexarchambault::case-app:2.0.5"
  def coursierJvm = ivy"io.get-coursier::coursier-jvm:${Versions.coursier}"
  def coursierLauncher = ivy"io.get-coursier::coursier-launcher:${Versions.coursier}"
  def jniUtils = ivy"io.get-coursier.jniutils:windows-jni-utils:0.2.1"
  def munit = ivy"org.scalameta::munit:0.7.25"
  def nativeTools = ivy"org.scala-native::tools:0.4.0"
  def osLib = ivy"com.lihaoyi::os-lib:0.7.5"
  def pprint = ivy"com.lihaoyi::pprint:0.6.4"
  def scalaJsLinker = ivy"org.scala-js::scalajs-linker:1.5.1"
  def scalaparse = ivy"com.lihaoyi::scalaparse:2.3.2"
  def svm = ivy"org.graalvm.nativeimage:svm:$graalVmVersion"
  def svmSubs = ivy"org.scalameta::svm-subs:20.2.0"
  def swoval = ivy"com.swoval:file-tree-views:2.1.5"
}

def graalVmVersion = "20.3.1.2"
