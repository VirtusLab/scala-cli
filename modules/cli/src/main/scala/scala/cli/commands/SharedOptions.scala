package scala.cli.commands

import caseapp._
import caseapp.core.help.Help
import scala.build.{Build, Project}
import scala.build.internal.{CodeWrapper, CustomCodeClassWrapper, CustomCodeWrapper}
import scala.scalanative.{build => sn}
import dependency.ScalaVersion
import dependency.ScalaParameters
import scala.build.internal.Constants

// TODO Add support for a --watch option

// TODO Add support for a --js option

final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
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
    watch: Boolean = false,
  jmh: Option[Boolean] = None,
  jmhVersion: Option[String] = None,
  scalaLibrary: Option[Boolean] = None,
  java: Option[Boolean] = None,
  runner: Option[Boolean] = None,
  semanticDb: Boolean = false
) {

  lazy val scalaBinaryVersion = ScalaVersion.binary(scalaVersion)

  lazy val scalaParams = ScalaParameters(
    scalaVersion,
    ScalaVersion.binary(scalaVersion),
    if (js) Some("sjs" + ScalaVersion.jsBinary(Constants.scalaJsVersion))
    else if (native) Some("native" + ScalaVersion.nativeBinary(Constants.scalaNativeVersion))
    else None
  )

  def enableJmh: Boolean = jmh.getOrElse(jmhVersion.nonEmpty)

  def logger = logging.logger

  lazy val codeWrapper: CodeWrapper =
    if (classWrap) CustomCodeClassWrapper
    else CustomCodeWrapper

  def nativeWorkDir(root: os.Path, projectName: String) = root / ".scala" / projectName / "native"

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
      addJmhDependencies =
        if (enableJmh) jmhVersion.orElse(Some("1.29"))
        else None,
      runJmh = enableJmh,
      addScalaLibrary = scalaLibrary.getOrElse(!java.getOrElse(false)),
      addRunnerDependencyOpt = runner,
      generateSemanticDbs = semanticDb
    )
}

object SharedOptions {

  def defaultScalaVersion: String =
    scala.util.Properties.versionNumberString

  implicit val parser = Parser[SharedOptions]
  implicit val help = Help[SharedOptions]
}
