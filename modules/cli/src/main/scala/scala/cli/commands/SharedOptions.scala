package scala.cli.commands

import java.io.File

import caseapp._
import caseapp.core.help.Help
import scala.build.{Build, Project}
import scala.build.internal.{CodeWrapper, CustomCodeClassWrapper, CustomCodeWrapper}
import scala.scalanative.{build => sn}
import dependency.ScalaVersion
import dependency.ScalaParameters
import scala.build.internal.Constants

final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),

  @Group("Scala")
  @HelpMessage("Set Scala version")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
    scalaVersion: String = SharedOptions.defaultScalaVersion,

  @Group("Java")
  @HelpMessage("Set Java home")
  @ValueDescription("path")
    javaHome: Option[String] = None,

  @Group("Java")
  @HelpMessage("Use a specific JVM, such as 14, adopt:11, or graalvm:21, or system")
  @ValueDescription("jvm-name")
  @Name("j")
    jvm: Option[String] = None,

  @Group("Java")
  @HelpMessage("Add extra JARs in the class path")
  @ValueDescription("paths")
  @Name("jar")
  @Name("jars")
  @Name("extraJar")
    extraJars: List[String] = Nil,

  @Hidden
    classWrap: Boolean = false,

  @Group("Scala")
  @HelpMessage("Enable Scala.JS")
    js: Boolean = false,
  @Group("Scala")
  @HelpMessage("Enable Scala Native")
    native: Boolean = false,

  @HelpMessage("Watch sources for changes")
  @Name("w")
    watch: Boolean = false,

  @Group("Scala")
  @Hidden
    scalaLibrary: Option[Boolean] = None,
  @Group("Java")
  @Hidden
    java: Option[Boolean] = None,
  @Hidden
    runner: Option[Boolean] = None,

  @HelpMessage("Generate SemanticDBs")
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

  def buildOptions(enableJmh: Boolean, jmhVersion: Option[String]): Build.Options =
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
      generateSemanticDbs = semanticDb,
      extraJars = extraJars.flatMap(_.split(File.pathSeparator).toSeq).filter(_.nonEmpty).map(os.Path(_, os.pwd))
    )
}

object SharedOptions {

  def defaultScalaVersion: String =
    scala.util.Properties.versionNumberString

  implicit val parser = Parser[SharedOptions]
  implicit val help = Help[SharedOptions]
}
