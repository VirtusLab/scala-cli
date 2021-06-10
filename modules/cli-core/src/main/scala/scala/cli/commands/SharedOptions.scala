package scala.cli.commands

import java.io.File

import caseapp._
import caseapp.core.help.Help
import dependency.{ScalaParameters, ScalaVersion}
import scala.build.{Build, Project}
import scala.build.internal.{CodeWrapper, Constants, CustomCodeClassWrapper, CustomCodeWrapper}
import scala.cli.internal.Util
import scala.scalanative.{build => sn}

final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),

  @Group("Scala")
  @HelpMessage("Set Scala version")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
    scalaVersion: String = SharedOptions.defaultScalaVersion,
  @Group("Scala")
  @HelpMessage("Set Scala binary version")
  @ValueDescription("version")
  @Name("scalaBinary")
  @Name("scalaBin")
  @Name("B")
    scalaBinaryVersion: String = "",

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

  def computeScalaVersions(): ScalaVersions = {
    val sv =
      if (Util.isFullScalaVersion(scalaVersion)) scalaVersion
      else {
        import coursier._
        import coursier.core.Version
        import scala.concurrent.ExecutionContext.{global => ec}
        val modules = {
          def scala2 = mod"org.scala-lang:scala-library"
          // No unstable, that *ought* not to be a problem down-the-line…?
          def scala3 = mod"org.scala-lang:scala3-library_3"
          if (scalaVersion == "2" || scalaVersion.startsWith("2.")) Seq(scala2)
          else if (scalaVersion == "3" || scalaVersion.startsWith("3.")) Seq(scala3)
          else Seq(scala2, scala3)
        }
        def isStable(v: String): Boolean =
          !v.endsWith("-NIGHTLY") && !v.contains("-RC")
        def moduleVersions(mod: Module): Seq[String] = {
          val res = Versions()
            .withModule(mod)
            .result()
            .unsafeRun()(ec)
          res.versions.available.filter(isStable)
        }
        val allVersions = modules.flatMap(moduleVersions).distinct
        val prefix = if (scalaVersion.endsWith(".")) scalaVersion else scalaVersion + "."
        val matchingVersions = allVersions.filter(_.startsWith(prefix))
        if (matchingVersions.isEmpty)
          sys.error(s"Cannot find matching Scala version for '$scalaVersion'")
        else
          matchingVersions.map(Version(_)).max.repr
      }
    val sbv =
      if (scalaBinaryVersion.isEmpty) ScalaVersion.binary(sv)
      else scalaBinaryVersion
    ScalaVersions(sv, sbv)
  }

  def scalaParams(scalaVersions: ScalaVersions) =
    ScalaParameters(
      scalaVersions.version,
      scalaVersions.binaryVersion,
      if (js) Some("sjs" + ScalaVersion.jsBinary(Constants.scalaJsVersion))
      else if (native) Some("native" + ScalaVersion.nativeBinary(Constants.scalaNativeVersion))
      else None
    )

  def logger = logging.logger

  lazy val codeWrapper: CodeWrapper =
    if (classWrap) CustomCodeClassWrapper
    else CustomCodeWrapper

  def nativeWorkDir(root: os.Path, projectName: String) = root / ".scala" / projectName / "native"

  def scalaJsOptions(scalaVersions: ScalaVersions): Option[Build.ScalaJsOptions] =
    if (js) Some(scalaJsOptionsIKnowWhatImDoing(scalaVersions))
    else None
  def scalaJsOptionsIKnowWhatImDoing(scalaVersions: ScalaVersions): Build.ScalaJsOptions =
    Build.scalaJsOptions(scalaVersions.version, scalaVersions.binaryVersion)

  def scalaNativeOptions(scalaVersions: ScalaVersions): Option[Build.ScalaNativeOptions] =
    if (native) Some(scalaNativeOptionsIKnowWhatImDoing(scalaVersions))
    else None
  def scalaNativeOptionsIKnowWhatImDoing(scalaVersions: ScalaVersions): Build.ScalaNativeOptions =
    Build.scalaNativeOptions(scalaVersions.version, scalaVersions.binaryVersion)
  def scalaNativeLogger: sn.Logger =
    new sn.Logger {
      def trace(msg: Throwable) = ()
      def debug(msg: String) = logger.debug(msg)
      def info(msg: String) = logger.log(msg)
      def warn(msg: String) = logger.log(msg)
      def error(msg: String) = logger.log(msg)
    }

  def buildOptions(scalaVersions: ScalaVersions, enableJmh: Boolean, jmhVersion: Option[String]): Build.Options =
    Build.Options(
      scalaVersion = scalaVersions.version,
      scalaBinaryVersion = scalaVersions.binaryVersion,
      codeWrapper = codeWrapper,
      scalaJsOptions = scalaJsOptions(scalaVersions),
      scalaNativeOptions = scalaNativeOptions(scalaVersions),
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
