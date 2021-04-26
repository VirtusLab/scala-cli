package scala.cli.commands

import ammonite.compiler.iface.CodeWrapper
import caseapp._
import caseapp.core.help.Help
import scala.cli.{Build, CustomCodeClassWrapper, CustomCodeWrapper, Logger, Project}
import scala.scalanative.{build => sn}

// TODO Add support for a --watch option

// TODO Add support for a --js option

final case class SharedOptions(
  @Name("v")
    verbose: Int @@ Counter = Tag.of(0),
  @Name("q")
    quiet: Boolean = false,
  @Name("scala")
  @Name("S")
    scalaVersion: String = SharedOptions.defaultScalaVersion,
  javaHome: Option[String] = None,
  @Name("j")
    jvm: Option[String] = None,
  classWrap: Boolean = false,
  js: Boolean = false,
  native: Boolean = false,
  @Name("w")
    watch: Boolean = false
) {

  lazy val scalaBinaryVersion =
    // FIXME Many cases are probably missing, see how mill does it.
    if (scalaVersion.contains("-RC") || scalaVersion.contains("-M")) scalaVersion
    else if (scalaVersion.startsWith("3.")) "3"
    else scalaVersion.split('.').take(2).mkString(".")

  lazy val verbosity = Tag.unwrap(verbose) - (if (quiet) 1 else 0)

  lazy val logger: Logger =
    new Logger {
      def log(message: => String): Unit =
        if (verbosity >= 1)
          System.err.println(message)
      def log(message: => String, debugMessage: => String): Unit =
        if (verbosity >= 2)
          System.err.println(debugMessage)
        else if (verbosity >= 1)
          System.err.println(message)
      def debug(message: => String): Unit =
        if (verbosity >= 2)
          System.err.println(message)
    }

  lazy val codeWrapper: CodeWrapper =
    if (classWrap) CustomCodeClassWrapper
    else CustomCodeWrapper

  def projectName = "project"

  def root = os.pwd
  def nativeWorkDir = os.pwd / ".scala" / projectName / "native"

  def scalaJsOptions: Option[Build.ScalaJsOptions] =
    if (js) Some(scalaJsOptionsIKnowWhatImDoing)
    else None
  def scalaJsOptionsIKnowWhatImDoing: Build.ScalaJsOptions =
    Build.scalaJsOptions(scalaVersion, scalaBinaryVersion)

  def scalaNativeOptions: Option[Build.ScalaNativeOptions] =
    if (native) Some(scalaNativeOptionsIKnowWhatImDoing)
    else None
  def scalaNativeOptionsIKnowWhatImDoing: Build.ScalaNativeOptions =
    Build.scalaNativeOptions(scalaVersion, scalaBinaryVersion)
  def scalaNativeLogger: sn.Logger =
    new sn.Logger {
      def trace(msg: Throwable) = ()
      def debug(msg: String) = logger.debug(msg)
      def info(msg: String) = logger.log(msg)
      def warn(msg: String) = logger.log(msg)
      def error(msg: String) = logger.log(msg)
    }

  def buildOptions: Build.Options =
    Build.Options(
      scalaVersion = scalaVersion,
      scalaBinaryVersion = scalaBinaryVersion,
      codeWrapper = codeWrapper,
      scalaJsOptions = scalaJsOptions,
      scalaNativeOptions = scalaNativeOptions,
      javaHomeOpt = javaHome.filter(_.nonEmpty),
      jvmIdOpt = jvm.filter(_.nonEmpty),
      projectName = projectName
    )
}

object SharedOptions {

  def defaultScalaVersion: String =
    scala.util.Properties.versionNumberString

  implicit val parser = Parser[SharedOptions]
  implicit val help = Help[SharedOptions]
}
